package com.zaitul.servicedesk.attachment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

/** The real S3 code against RustFS, the same S3-compatible store used locally and in Kubernetes. */
@Testcontainers
class S3AttachmentStorageTests {

	@Container
	private static final GenericContainer<?> rustfs = new GenericContainer<>(
			DockerImageName.parse("rustfs/rustfs:1.0.3"))
		.withEnv("RUSTFS_ACCESS_KEY", "test-access")
		.withEnv("RUSTFS_SECRET_KEY", "test-secret-key")
		.withEnv("RUSTFS_OBS_LOG_DIRECTORY", "")
		.withExposedPorts(9000)
		.waitingFor(Wait.forHttp("/health").forPort(9000).forStatusCode(200));

	@Test
	void storesAndReadsBackAFile() throws Exception {
		AttachmentProperties properties = new AttachmentProperties("test-bucket",
				"http://" + rustfs.getHost() + ":" + rustfs.getMappedPort(9000), "ap-southeast-1", "test-access",
				"test-secret-key", true);
		S3Client s3 = StorageConfig.buildClient(properties);
		StorageConfig.ensureBucket(s3, properties.bucket());
		StorageConfig.ensureBucket(s3, properties.bucket());
		S3AttachmentStorage storage = new S3AttachmentStorage(s3, properties.bucket());
		byte[] bytes = "screenshot bytes".getBytes(StandardCharsets.UTF_8);

		storage.put("tickets/1/abc", "image/png", bytes.length, new ByteArrayInputStream(bytes));

		try (InputStream read = storage.open("tickets/1/abc")) {
			assertThat(read.readAllBytes()).isEqualTo(bytes);
		}
	}

}
