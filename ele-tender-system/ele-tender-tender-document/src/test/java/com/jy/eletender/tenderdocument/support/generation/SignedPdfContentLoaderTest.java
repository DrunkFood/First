package com.jy.eletender.tenderdocument.support.generation;

import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.support.interaction.DownloadedFileInfo;
import com.jy.eletender.tenderdocument.support.interaction.FileTransferClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignedPdfContentLoaderTest {

    @Mock
    private FileTransferClient fileTransferClient;

    @InjectMocks
    private SignedPdfContentLoader signedPdfContentLoader;

    @Test
    void shouldLoadSignedPdfContentAsBase64Payload() {
        byte[] content = "signed-pdf".getBytes();
        TenderDocumentFile file = new TenderDocumentFile();
        file.setFileId(100L);
        file.setFileName("signed.pdf");
        file.setFileSha256("sha256-demo");
        when(fileTransferClient.download(100L))
                .thenReturn(new DownloadedFileInfo(100L, "downloaded.pdf", "application/pdf", content));

        var payload = signedPdfContentLoader.load(file);

        assertThat(payload.getFileName()).isEqualTo("downloaded.pdf");
        assertThat(payload.getSize()).isEqualTo((long) content.length);
        assertThat(payload.getSha256()).isEqualTo("sha256-demo");
        assertThat(payload.getSignedFileBase64()).isEqualTo(Base64.getEncoder().encodeToString(content));
    }
}
