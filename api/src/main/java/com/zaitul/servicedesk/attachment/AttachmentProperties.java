package com.zaitul.servicedesk.attachment;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where attachments are stored.
 *
 * @param endpoint set for S3-compatible storage (RustFS locally); leave empty for AWS S3
 * @param accessKey leave empty on AWS so the SDK uses the pod's IAM role instead of a key
 * @param createBucket create the bucket on startup if missing (local only; on AWS
 * Terraform creates it)
 */
@ConfigurationProperties("servicedesk.attachments")
public record AttachmentProperties(String bucket, @Nullable String endpoint, String region,
		@Nullable String accessKey, @Nullable String secretKey, boolean createBucket) {
}
