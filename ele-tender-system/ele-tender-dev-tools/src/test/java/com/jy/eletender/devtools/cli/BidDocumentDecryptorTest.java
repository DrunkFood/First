package com.jy.eletender.devtools.cli;

import com.jy.eletender.crypto.native_bridge.JniCryptoNative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BidDocumentDecryptorTest {

    @TempDir
    Path tempDir;

    @Test
    void run_missingInputFile_returns1() {
        assertEquals(1, BidDocumentDecryptor.run(new String[]{"-p", "SomePwd"}));
    }

    @Test
    void run_missingPassword_returns1() {
        assertEquals(1, BidDocumentDecryptor.run(new String[]{"-i", "/some/file.HzctTbs"}));
    }

    @Test
    void run_decryptFileGeneratedByGenerator() throws Exception {
        // 先生成投标文件
        String password = BidDocumentGenerator.DEFAULT_PASSWORD;
        BidDocumentGenerator.export(tempDir, password, new JniCryptoNative());

        Path encFile = tempDir.resolve("sample.HzctTbs");
        Path decFile = tempDir.resolve("sample.dec.json");

        int code = BidDocumentDecryptor.run(new String[]{
                "-i", encFile.toString(),
                "-p", password,
                "-o", decFile.toString()
        });
        assertEquals(0, code);
        assertTrue(Files.exists(decFile));

        // 解密内容应与原始明文一致
        byte[] original = Files.readAllBytes(tempDir.resolve("sample-bid-document.json"));
        byte[] decrypted = Files.readAllBytes(decFile);
        assertTrue(java.util.Arrays.equals(original, decrypted),
                "Decrypted content must match original plaintext");
    }

    @Test
    void run_missingArgForOption_returns1() {
        assertEquals(1, BidDocumentDecryptor.run(new String[]{"-i"}));
        assertEquals(1, BidDocumentDecryptor.run(new String[]{"-p"}));
        assertEquals(1, BidDocumentDecryptor.run(new String[]{"--private-key"}));
    }
}
