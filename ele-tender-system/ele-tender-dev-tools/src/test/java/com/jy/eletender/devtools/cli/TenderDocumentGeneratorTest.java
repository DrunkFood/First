package com.jy.eletender.devtools.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

class TenderDocumentGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void run_autoGeneratesKeyAndFiles() {
        int exitCode = TenderDocumentGenerator.run(new String[]{"-o", tempDir.toString()});
        assertEquals(0, exitCode);
        assertTrue(Files.exists(tempDir.resolve("sample-tender-document.json")));
        assertTrue(Files.exists(tempDir.resolve("sample-tender-document.HzctZbs")));
        assertTrue(Files.exists(tempDir.resolve("tender-manifest.json")));
    }

    @Test
    void run_withCustomKey_encryptDecryptRoundTrip() throws Exception {
        // 先生成，获取 key
        int genCode = TenderDocumentGenerator.run(new String[]{"-o", tempDir.toString()});
        assertEquals(0, genCode);

        // 读 manifest 获取 key
        String manifest = Files.readString(tempDir.resolve("tender-manifest.json"));
        // manifest 格式 JSON，直接提取 aesKeyBase64 字段
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String keyBase64 = mapper.readTree(manifest).get("aesKeyBase64").asText();

        // 解密
        Path encFile = tempDir.resolve("sample-tender-document.HzctZbs");
        Path decFile = tempDir.resolve("sample-tender-document.dec.json");
        int decCode = TenderDocumentDecryptor.run(new String[]{
                "-i", encFile.toString(),
                "-k", keyBase64,
                "-o", decFile.toString()
        });
        assertEquals(0, decCode);

        // 对比原文
        byte[] originalPlain = Files.readAllBytes(tempDir.resolve("sample-tender-document.json"));
        byte[] decryptedPlain = Files.readAllBytes(decFile);
        assertTrue(java.util.Arrays.equals(originalPlain, decryptedPlain));
    }

    @Test
    void run_generatedJsonContainsV7RequiredFields() throws Exception {
        int exitCode = TenderDocumentGenerator.run(new String[]{"-o", tempDir.toString()});
        assertEquals(0, exitCode);

        String json = Files.readString(tempDir.resolve("sample-tender-document.json"));
        // baseInfo v7 字段
        assertThat(json).contains("\"purchaserName\"");
        assertThat(json).contains("\"bidEndTime\"");
        assertThat(json).contains("\"purchaseMethod\"");
        assertThat(json).contains("\"tenderAmount\"");
        assertThat(json).contains("\"purchaseContext\"");
        assertThat(json).doesNotContain("\"purchaserName\" : null");
        assertThat(json).doesNotContain("\"bidEndTime\" : null");
        // tenders bidForms & bidEvalRules
        assertThat(json).contains("\"bidForms\"");
        assertThat(json).contains("\"schemeContent\"");
        assertThat(json).doesNotContain("\"bidFormData\"");  // 招标文件不含 bidFormData
        assertThat(json).contains("\"bidEvalRules\"");
        assertThat(json).contains("\"QUALIFICATION\"");
        assertThat(json).contains("\"CONFORMITY\"");
        assertThat(json).contains("\"objectiveType\"");
        // 2 个标段
        assertThat(json).contains("T-DEV-2026-001");
        assertThat(json).contains("T-DEV-2026-002");
    }

    @Test
    void run_missingRequiredOutputArg_returns1() {
        assertEquals(1, TenderDocumentGenerator.run(new String[]{"-o"}));
    }
}
