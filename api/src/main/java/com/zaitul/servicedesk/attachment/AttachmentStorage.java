package com.zaitul.servicedesk.attachment;

import java.io.InputStream;

/** Stores attachment bytes. The S3 implementation is used everywhere; tests swap in a fake. */
public interface AttachmentStorage {

	void put(String key, String contentType, long sizeBytes, InputStream content);

	/** The caller must close the stream. */
	InputStream open(String key);

}
