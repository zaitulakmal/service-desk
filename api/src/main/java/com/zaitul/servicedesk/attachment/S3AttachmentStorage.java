package com.zaitul.servicedesk.attachment;

import java.io.InputStream;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3AttachmentStorage implements AttachmentStorage {

	private final S3Client s3;

	private final String bucket;

	public S3AttachmentStorage(S3Client s3, String bucket) {
		this.s3 = s3;
		this.bucket = bucket;
	}

	@Override
	public void put(String key, String contentType, long sizeBytes, InputStream content) {
		PutObjectRequest request = PutObjectRequest.builder()
			.bucket(this.bucket)
			.key(key)
			.contentType(contentType)
			.contentLength(sizeBytes)
			.build();
		this.s3.putObject(request, RequestBody.fromInputStream(content, sizeBytes));
	}

	@Override
	public InputStream open(String key) {
		return this.s3.getObject(GetObjectRequest.builder().bucket(this.bucket).key(key).build());
	}

}
