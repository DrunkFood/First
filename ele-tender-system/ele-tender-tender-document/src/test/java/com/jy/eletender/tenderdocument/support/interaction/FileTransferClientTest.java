package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FileTransferClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private FileTransferClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        TenderDocumentInteractionProperties properties = new TenderDocumentInteractionProperties();
        properties.setFileServiceBaseUrl("http://file-service");
        client = new FileTransferClient(restTemplate, properties);
    }

    @Test
    void shouldUploadGeneratedFileAndReturnFileInfo() {
        server.expect(requestTo("http://file-service/api/file/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.MULTIPART_FORM_DATA_VALUE)))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"操作成功\",\"data\":{\"fileId\":123,\"fileName\":\"receipt.pdf\",\"fileSize\":20,\"fileSha256\":\"sha-123\"}}",
                        MediaType.APPLICATION_JSON));

        UploadedFileInfo response = client.upload("receipt.pdf", "tender-document", "application/pdf", "hello".getBytes());

        assertThat(response.getFileId()).isEqualTo(123L);
        assertThat(response.getFileSha256()).isEqualTo("sha-123");
    }

    @Test
    void shouldDownloadReferencedFileContent() {
        server.expect(requestTo("http://file-service/api/file/download/321"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("demo-content".getBytes(), MediaType.APPLICATION_OCTET_STREAM)
                        .headers(headersWithAttachment("source.pdf")));

        DownloadedFileInfo response = client.download(321L);

        assertThat(response.getFileName()).isEqualTo("source.pdf");
        assertThat(response.getContent()).isEqualTo("demo-content".getBytes());
    }

    @Test
    void shouldExposeDownstreamErrorDetailWhenUploadRejected() {
        server.expect(requestTo("http://file-service/api/file/upload"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":5003,\"message\":\"不允许的文件类型，允许的类型：.jar,.war,.zip,.tar.gz,.pdf\",\"data\":null}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.upload("package.HzctZbs", "tender-document", "application/octet-stream", "hello".getBytes()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5003")
                .hasMessageContaining("不允许的文件类型");
    }

    private HttpHeaders headersWithAttachment(String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        return headers;
    }
}
