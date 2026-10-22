package com.zaitul.servicedesk.attachment;

import java.nio.charset.StandardCharsets;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import com.zaitul.servicedesk.ApiTestSupport;

import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttachmentApiTests extends ApiTestSupport {

	private static final byte[] LOG = "dock firmware 1.2.3\nflicker at 14:02".getBytes(StandardCharsets.UTF_8);

	@Test
	void requesterAttachesAFileAndTheAgentDownloadsIt() throws Exception {
		long ticketId = createIncident(this.aina);
		MockMultipartFile file = new MockMultipartFile("file", "C:\\Users\\aina\\dock-log.txt", "text/plain", LOG);

		String body = this.mockMvc
			.perform(multipart("/api/tickets/{id}/attachments", ticketId).file(file).with(as(this.aina)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.attachments[0].fileName").value("dock-log.txt"))
			.andExpect(jsonPath("$.attachments[0].sizeBytes").value(LOG.length))
			.andExpect(jsonPath("$.attachments[0].uploadedBy").value("Aina Rahman"))
			.andExpect(jsonPath("$.activity[0].message").value("Aina Rahman attached dock-log.txt"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long attachmentId = JsonPath.<Number>read(body, "$.attachments[0].id").longValue();

		this.mockMvc.perform(get("/api/tickets/{id}/attachments/{a}", ticketId, attachmentId).with(as(this.daniel)))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Disposition", containsString("attachment")))
			.andExpect(header().string("Content-Disposition", containsString("dock-log.txt")))
			.andExpect(content().bytes(LOG));
	}

	@Test
	void outsidersCannotDownload() throws Exception {
		long ticketId = createIncident(this.aina);
		String body = this.mockMvc
			.perform(multipart("/api/tickets/{id}/attachments", ticketId)
				.file(new MockMultipartFile("file", "log.txt", "text/plain", LOG))
				.with(as(this.aina)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long attachmentId = JsonPath.<Number>read(body, "$.attachments[0].id").longValue();

		this.mockMvc.perform(get("/api/tickets/{id}/attachments/{a}", ticketId, attachmentId).with(as(this.hafiz)))
			.andExpect(status().isNotFound());
	}

	@Test
	void refusesFileTypesThatCouldBeDangerous() throws Exception {
		long ticketId = createIncident(this.aina);

		this.mockMvc
			.perform(multipart("/api/tickets/{id}/attachments", ticketId)
				.file(new MockMultipartFile("file", "invoice.html", "text/html", "<script>".getBytes()))
				.with(as(this.aina)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.file").exists());
	}

	@Test
	void fileNamesAreReducedToTheirLastPart() {
		assertThat(AttachmentService.cleanFileName("../../etc/passwd")).isEqualTo("passwd");
		assertThat(AttachmentService.cleanFileName("C:\\temp\\shot.png")).isEqualTo("shot.png");
		assertThat(AttachmentService.cleanFileName("  ")).isEqualTo("file");
	}

}
