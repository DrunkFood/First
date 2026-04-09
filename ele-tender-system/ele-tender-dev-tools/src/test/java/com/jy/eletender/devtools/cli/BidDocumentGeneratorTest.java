package com.jy.eletender.devtools.cli;

import com.jy.eletender.crypto.native_bridge.JniCryptoNative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BidDocumentGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void run_defaultPassword_generatesFiles() {
        int exitCode = BidDocumentGenerator.run(new String[]{"-o", tempDir.toString()});
        assertEquals(0, exitCode);
        assertTrue(Files.exists(tempDir.resolve("sample.HzctTbs")));
        assertTrue(Files.exists(tempDir.resolve("sample-bid-document.json")));
        assertTrue(Files.exists(tempDir.resolve("bid-manifest.json")));
        assertTrue(Files.exists(tempDir.resolve("public-key.pem")));
        assertTrue(Files.exists(tempDir.resolve("private-key.pem")));
    }

    @Test
    void run_customPassword_generatesFiles() {
        int exitCode = BidDocumentGenerator.run(new String[]{"-o", tempDir.toString(), "-p", "MyCustomPwd@2026"});
        assertEquals(0, exitCode);
        assertTrue(Files.exists(tempDir.resolve("sample.HzctTbs")));
    }

    @Test
    void export_roundTrip_defaultPassword() throws Exception {
        BidDocumentGenerator.export(tempDir, BidDocumentGenerator.DEFAULT_PASSWORD, new JniCryptoNative());

        // 验证 manifest 记录了 password
        String manifest = Files.readString(tempDir.resolve("bid-manifest.json"));
        assertTrue(manifest.contains(BidDocumentGenerator.DEFAULT_PASSWORD));
        assertTrue(manifest.contains("sample.HzctTbs"));

        // 加密文件非空
        byte[] encBytes = Files.readAllBytes(tempDir.resolve("sample.HzctTbs"));
        assertTrue(encBytes.length > 0);
    }

    @Test
    void export_generatedBidJsonContainsV8Structure() throws Exception {
        BidDocumentGenerator.export(tempDir, BidDocumentGenerator.DEFAULT_PASSWORD, new JniCryptoNative());

        String bidJson = Files.readString(tempDir.resolve("sample-bid-document.json"));
        // v8 规范三大顶层字段
        assertThat(bidJson).contains("\"baseInfo\"");
        assertThat(bidJson).contains("\"tender\"");
        assertThat(bidJson).contains("\"versionInfo\"");
        // baseInfo 子字段
        assertThat(bidJson).contains("\"projectId\"");
        assertThat(bidJson).contains("\"tenderId\"");
        assertThat(bidJson).contains("\"tenderNo\"");
        // tender 子字段
        assertThat(bidJson).contains("\"bidForms\"");
        assertThat(bidJson).contains("\"schemeContent\"");
        assertThat(bidJson).contains("\"bidFormData\"");  // 投标文件含 bidFormData
        assertThat(bidJson).contains("\"bidEvalRules\"");
        assertThat(bidJson).contains("\"QUALIFICATION\"");
        assertThat(bidJson).contains("\"CONFORMITY\"");
        assertThat(bidJson).contains("\"CREDIT\"");
        // v8 新增：投标函
        assertThat(bidJson).contains("\"bidLetter\"");
        assertThat(bidJson).contains("\"bidderName\"");
        assertThat(bidJson).contains("\"legalRepresentative\"");
        assertThat(bidJson).contains("\"representativeFlag\"");
        assertThat(bidJson).contains("\"signedFileBufferBase64\"");
        // versionInfo 双版本字段
        assertThat(bidJson).contains("\"tenderDocumentFormatVersion\"");
        assertThat(bidJson).contains("\"bidDocumentFormatVersion\"");
        assertThat(bidJson).contains("\"bidDocumentUniqueCode\"");
        assertThat(bidJson).contains("\"macDiskCpu\"");
        assertThat(bidJson).contains("\"creatorName\"");
        // 无旧的扁平字段
        assertThat(bidJson).doesNotContain("\"tenderInfo\"");
        assertThat(bidJson).doesNotContain("\"biddinguserInfo\"");
        assertThat(bidJson).doesNotContain("\"recordingInfo\"");

        String projectInfoJson = Files.readString(tempDir.resolve("sample-project-info-rsa-plain.json"));
        // projectInfoRSA.baseInfo 完整字段
        assertThat(projectInfoJson).contains("\"projectNo\"");
        assertThat(projectInfoJson).contains("\"tenderNo\"");
        assertThat(projectInfoJson).contains("\"totalStage\"");
        assertThat(projectInfoJson).contains("\"currentStage\"");
        // projectInfoRSA.versionInfo 双版本字段
        assertThat(projectInfoJson).contains("\"tenderDocumentFormatVersion\"");
        assertThat(projectInfoJson).contains("\"tenderDocumentSha256\"");
        assertThat(projectInfoJson).contains("\"bidDocumentFormatVersion\"");
        assertThat(projectInfoJson).contains("\"bidDocumentUniqueCode\"");
        // encryptedSha256
        assertThat(projectInfoJson).contains("\"encryptedSha256\"");
    }

    @Test
    void run_missingArgForOption_returns1() {
        assertEquals(1, BidDocumentGenerator.run(new String[]{"-o"}));
        assertEquals(1, BidDocumentGenerator.run(new String[]{"-p"}));
    }
}
