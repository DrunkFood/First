package com.jy.eletender.tenderdocument.support.generation;

import com.jy.eletender.tenderdocument.support.interaction.TenderDocumentInteractionProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFinalPackageEncryptorTest {

    @Test
    void shouldEncryptJsonBytesToAesGcmBinary() throws Exception {
        TenderDocumentInteractionProperties properties = new TenderDocumentInteractionProperties();
        DefaultFinalPackageEncryptor encryptor = new DefaultFinalPackageEncryptor(properties);
        byte[] plain = "{\"projectId\":\"P-100\"}".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptor.encrypt(plain);

        assertThat(encrypted).isNotEmpty();
        assertThat(new String(encrypted, StandardCharsets.UTF_8)).doesNotContain("projectId");

        ByteBuffer buffer = ByteBuffer.wrap(encrypted);
        byte[] magic = new byte[4];
        buffer.get(magic);
        byte version = buffer.get();
        byte[] nonce = new byte[12];
        buffer.get(nonce);
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        assertThat(magic).containsExactly((byte) 'H', (byte) 'Z', (byte) 'P', (byte) 'K');
        assertThat(version).isEqualTo((byte) 1);
        assertThat(cipherText).isNotEmpty();

        Cipher cipher = Cipher.getInstance(properties.getFinalPackageEncryptAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(Base64.getDecoder().decode(properties.getFinalPackageAesKeyBase64()), "AES"),
                new GCMParameterSpec(128, nonce));
        byte[] decrypted = cipher.doFinal(cipherText);
        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void shouldExposeDefaultFinalPackageEncryptionProperties() {
        TenderDocumentInteractionProperties properties = new TenderDocumentInteractionProperties();

        assertThat(properties.getFinalPackageEncryptAlgorithm()).isEqualTo("AES/GCM/NoPadding");
        assertThat(Base64.getDecoder().decode(properties.getFinalPackageAesKeyBase64())).hasSize(32);
        assertThat(properties.getFinalPackageSuffix()).isEqualTo(".HzctZbs");
        assertThat(properties.getCompileInfoWatermarkText()).isEqualTo("城投集团采购平台");
    }
}
