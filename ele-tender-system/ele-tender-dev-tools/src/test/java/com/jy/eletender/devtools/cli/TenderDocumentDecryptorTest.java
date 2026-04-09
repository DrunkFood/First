package com.jy.eletender.devtools.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenderDocumentDecryptorTest {

    @TempDir
    Path tempDir;

    @Test
    void run_missingInputFile_returns1() {
        assertEquals(1, TenderDocumentDecryptor.run(new String[]{"-k", "dGVzdA=="}));
    }

    @Test
    void run_missingKey_returns1() {
        assertEquals(1, TenderDocumentDecryptor.run(new String[]{"-i", "/some/file.HzctZbs"}));
    }

    @Test
    void run_invalidKeyBase64_returns1() throws Exception {
        // 生成一个加密文件但用错误 key 解密
        Path genDir = tempDir.resolve("gen");
        TenderDocumentGenerator.run(new String[]{"-o", genDir.toString()});
        int code = TenderDocumentDecryptor.run(new String[]{
                "-i", genDir.resolve("sample-tender-document.HzctZbs").toString(),
                "-k", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="  // wrong 32-byte key
        });
        assertEquals(1, code);
    }

    @Test
    void run_missingArgForOption_returns1() {
        assertEquals(1, TenderDocumentDecryptor.run(new String[]{"-i"}));
        assertEquals(1, TenderDocumentDecryptor.run(new String[]{"-k"}));
        assertEquals(1, TenderDocumentDecryptor.run(new String[]{"-o"}));
    }
}
