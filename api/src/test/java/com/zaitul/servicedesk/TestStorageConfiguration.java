package com.zaitul.servicedesk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zaitul.servicedesk.attachment.AttachmentStorage;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Keeps attachments in memory so API tests do not need an S3 server. */
@TestConfiguration(proxyBeanMethods = false)
public class TestStorageConfiguration {

	@Bean
	@Primary
	InMemoryAttachmentStorage inMemoryAttachmentStorage() {
		return new InMemoryAttachmentStorage();
	}

	public static class InMemoryAttachmentStorage implements AttachmentStorage {

		private final Map<String, byte[]> files = new ConcurrentHashMap<>();

		@Override
		public void put(String key, String contentType, long sizeBytes, InputStream content) {
			try {
				this.files.put(key, content.readAllBytes());
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		@Override
		public InputStream open(String key) {
			return new ByteArrayInputStream(this.files.get(key));
		}

	}

}
