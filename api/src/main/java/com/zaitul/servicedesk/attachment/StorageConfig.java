package com.zaitul.servicedesk.attachment;

import java.net.URI;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AttachmentProperties.class)
class StorageConfig {

	@Bean
	S3Client s3Client(AttachmentProperties properties) {
		return buildClient(properties);
	}

	@Bean
	AttachmentStorage attachmentStorage(S3Client s3Client, AttachmentProperties properties) {
		return new S3AttachmentStorage(s3Client, properties.bucket());
	}

	@Bean
	ApplicationRunner createAttachmentBucket(S3Client s3Client, AttachmentProperties properties) {
		return (args) -> {
			if (properties.createBucket()) {
				ensureBucket(s3Client, properties.bucket());
			}
		};
	}

	static S3Client buildClient(AttachmentProperties properties) {
		S3ClientBuilder builder = S3Client.builder()
			.region(Region.of(properties.region()))
			// Only add checksums S3 insists on; some S3-compatible stores reject the newer defaults.
			.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
			.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
		if (StringUtils.hasText(properties.endpoint())) {
			builder.endpointOverride(URI.create(properties.endpoint())).forcePathStyle(true);
		}
		if (StringUtils.hasText(properties.accessKey())) {
			builder.credentialsProvider(StaticCredentialsProvider
				.create(AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
		}
		else {
			builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
		}
		return builder.build();
	}

	static void ensureBucket(S3Client s3Client, String bucket) {
		try {
			s3Client.headBucket((request) -> request.bucket(bucket));
		}
		catch (NoSuchBucketException ex) {
			try {
				s3Client.createBucket((request) -> request.bucket(bucket));
			}
			catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException raced) {
				// Another API replica created it at the same moment. Nothing to do.
			}
		}
	}

}
