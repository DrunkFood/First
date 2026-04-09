package com.jy.eletender.tenderdocument.support.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FinalPackageCryptoCliTest {

    @Test
    void shouldEncryptAndDecryptLocalFile(@TempDir Path tempDir) throws Exception {
        String keyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
        Path plainFile = tempDir.resolve("plain.json");
        Path encryptedFile = tempDir.resolve("plain.HzctZbs");
        Path decryptedFile = tempDir.resolve("plain.dec.json");
        Files.writeString(plainFile, "{\"hello\":\"world\"}", StandardCharsets.UTF_8);

        int encryptCode = FinalPackageCryptoCli.run(new String[]{
                "encrypt",
                plainFile.toString(),
                encryptedFile.toString(),
                keyBase64
        });
        int decryptCode = FinalPackageCryptoCli.run(new String[]{
                "decrypt",
                encryptedFile.toString(),
                decryptedFile.toString(),
                keyBase64
        });

        assertThat(encryptCode).isZero();
        assertThat(decryptCode).isZero();
        assertThat(Files.readString(decryptedFile, StandardCharsets.UTF_8))
                .isEqualTo(Files.readString(plainFile, StandardCharsets.UTF_8));
    }
}
