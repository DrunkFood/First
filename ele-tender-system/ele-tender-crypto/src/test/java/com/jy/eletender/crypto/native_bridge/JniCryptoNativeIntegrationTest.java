package com.jy.eletender.crypto.native_bridge;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JniCryptoNativeIntegrationTest {

    private static final String PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCcXlbzrBT7yc+7
            a2Vrtf/HCs0DO5vGSUFHW7M8CW1hDcYcWBFPJaPgGW/NgdzU1brm7nad0IvB7tvE
            72/1VFxmhUTu+rWirzyZ7+PRe/jJrnN9RwrhyeK3FdAkIEt/f1NF8CIakYkWkBdk
            8fZ5XCaI2JSwd5ZugUr7f0r5tii0IkA/YFtia6Ih4/0oJLwd5JarK7G2+GASU/Pr
            4PKdowRuTcm2hSUkKUVk0TUOQzDrxb4aq6Aozbuj77qUXtFwnlqDxSoPE8rblx9k
            xiMJokw5Qs1GP3X6LkmrEiTeDyUAkhtpySkpJdBPYRIrPi7TKmJ3ls7WSit7AWZi
            CbOyaW4HAgMBAAECggEAEklQEYgGyNlK2Kf7FjO/A+0bQ5ewJGoDCdAgr6ffPucy
            9glF4O54cdKCX3332tCx3k7LLV0q8uo25kV6tNiBmSBYYDrj4fDB/V3vU7C+oVYB
            kzLV2XOCzBbEyWzDbNdxisTF4Dsz1SEoXeQ+sZDuwA3VTvvKlQVmxO7tJYhsIys4
            DMSbY7gc+sdjGUrHdEZbl3nfWUkn0rqrQE2NmdsISLkcoLpL6s1lSZd5TrW9Bwe+
            +6rhY9r4fT+BwIpL7Yx8cFz127uR2quCTlwHI/LJmoKcw10P9KpVI+n27u+yhGUd
            8Z0gTlFTpbnmaOyT1HOqE7fitG/VJbwhIJ4QXM51IQKBgQDSr8RAvJz+wqrwwZC/
            js3xO/bTKbep4Q4Xqa3McETDBtM6b8pRA/sb+QrSxew3erBqotZkG5bHx1J6GvRc
            vyNTZ6ckaf3E1y7+jIQSHA33KHHFcjhtL0wCsHshYo7bUaKpR+mrpIP0GMFl281x
            RhCiu7JwQVHxvz362bF7jj07IQKBgQC9/90fLIYSIbYqgFAfLEY7a9+BNxYK1PjE
            zj1Ofbwn0K60mmCCgnvRo/RLBHLAYyrv06ByME7t7l+L/r00jK4HG/tBDL8f8i3l
            3zvJWRHx8LcifsB+tK8fareyetcAYQ8nzJVvS9PQee7hwQ7yb9+0w+d+wRCZylQf
            eosJLTPsJwKBgHUSiGo0pMSH5bcMyGM5dkSjPn+OQembDlqlxdbBV+RLaZqiPfkQ
            zjt4AsSmiKE3gspum9Va40k2ACWrzreu2nFhOqZoY0Q7EnkOGeF6R2RczAOcebBq
            RMGF0ZX2j01dqpaISFdBfrVoACeaoSlddqcGx5vLID7GNymqSA5RNsMhAoGARE8b
            JrwRL6+jGMCtDagTUAXGg2RUrmxHTCqB7BhUb1Qdm5ztGb7j2UlC6T2eLAD7TOIf
            Cy7HEc/j1ictyxjQ8Ilk2cxFYqzlR4HsssUtKHjMvsAnYOaBF6B8jtSPO/mpQzvQ
            dgUjEA7mjY+lWhBSs2DDd9TdrQ0LFY4vMotn4X0CgYAURn1Kbhu21I+A/gv4VGaG
            BKut8g3Cp9AIQJcNvl4wVIlbKlcxxpBGqSJS5xdjIwzmpGjhAEosCutuFTr+idGK
            jKepjvuoocWKreeskTupT/lbLT3uMibIoyLRgG+oH4ZLaImouhpt9hfKCnLQFRlr
            VF0ppiv5CPXM/YJU3uZJ4Q==
            -----END PRIVATE KEY-----
            """;

    private static final String PUBLIC_KEY = """
            -----BEGIN PUBLIC KEY-----
            MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnF5W86wU+8nPu2tla7X/
            xwrNAzubxklBR1uzPAltYQ3GHFgRTyWj4BlvzYHc1NW65u52ndCLwe7bxO9v9VRc
            ZoVE7vq1oq88me/j0Xv4ya5zfUcK4cnitxXQJCBLf39TRfAiGpGJFpAXZPH2eVwm
            iNiUsHeWboFK+39K+bYotCJAP2BbYmuiIeP9KCS8HeSWqyuxtvhgElPz6+DynaME
            bk3JtoUlJClFZNE1DkMw68W+GqugKM27o++6lF7RcJ5ag8UqDxPK25cfZMYjCaJM
            OULNRj91+i5JqxIk3g8lAJIbackpKSXQT2ESKz4u0ypid5bO1korewFmYgmzsmlu
            BwIDAQAB
            -----END PUBLIC KEY-----
            """;

    @BeforeAll
    static void buildNativeLibrary() throws Exception {
        NativeTestSupport.ensureNativeLibraryBuilt(Path.of(System.getProperty("user.dir")));
    }

    @Test
    void shouldPackAndUnpackThroughJni() {
        JniCryptoNative nativeCrypto = new JniCryptoNative();
        byte[] projectInfo = "{\"projectId\":\"P1\"}".getBytes(StandardCharsets.UTF_8);
        byte[] packed = nativeCrypto.packFile(
                List.of("seg-1".getBytes(StandardCharsets.UTF_8), "seg-2".getBytes(StandardCharsets.UTF_8)),
                projectInfo,
                "2.0.0"
        );

        UnpackResult unpackResult = nativeCrypto.unpackFile(packed);

        assertEquals(2, unpackResult.getEncryptedSegments().size());
        assertArrayEquals("seg-1".getBytes(StandardCharsets.UTF_8), unpackResult.getEncryptedSegments().get(0));
        assertArrayEquals("seg-2".getBytes(StandardCharsets.UTF_8), unpackResult.getEncryptedSegments().get(1));
        assertArrayEquals(projectInfo, unpackResult.getEncryptedProjectInfoRsa());
        assertEquals("2.0.0", unpackResult.getFormatVersion());
    }

    @Test
    void shouldEncryptAndDecryptSegmentsThroughJni() {
        JniCryptoNative nativeCrypto = new JniCryptoNative();
        byte[] keyBytes = new byte[32];
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = (byte) i;
        }
        byte[] plain = "hello native jni".getBytes(StandardCharsets.UTF_8);

        List<byte[]> encryptedSegments = nativeCrypto.encryptSegments(plain, keyBytes, 5);
        byte[] decrypted = nativeCrypto.decryptSegments(encryptedSegments, keyBytes);

        assertArrayEquals(plain, decrypted);
    }

    @Test
    void shouldEncryptAndDecryptRsaThroughJni() {
        JniCryptoNative nativeCrypto = new JniCryptoNative();
        byte[] plain = "rsa-jni-round-trip".repeat(32).getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = nativeCrypto.rsaEncrypt(plain, PUBLIC_KEY);
        byte[] decrypted = nativeCrypto.rsaDecrypt(encrypted, PRIVATE_KEY);

        assertArrayEquals(plain, decrypted);
    }
}
