package com.jy.eletender.crypto.native_bridge;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StubCryptoNativeTest {

    private final StubCryptoNative cryptoNative = new StubCryptoNative();

    @Test
    void shouldPackAndUnpackUsingBinaryContract() {
        byte[] segment1 = "segment-1".getBytes(StandardCharsets.UTF_8);
        byte[] segment2 = "segment-2".getBytes(StandardCharsets.UTF_8);
        byte[] projectInfo = "project-info".getBytes(StandardCharsets.UTF_8);

        byte[] packed = cryptoNative.packFile(List.of(segment1, segment2), projectInfo, "2.0.0");

        UnpackResult unpackResult = cryptoNative.unpackFile(packed);

        assertEquals(2, unpackResult.getEncryptedSegments().size());
        assertArrayEquals(segment1, unpackResult.getEncryptedSegments().get(0));
        assertArrayEquals(segment2, unpackResult.getEncryptedSegments().get(1));
        assertArrayEquals(projectInfo, unpackResult.getEncryptedProjectInfoRsa());
        assertEquals("2.0.0", unpackResult.getFormatVersion());
    }

    @Test
    void shouldDecryptSegmentsByConcatenatingStubSegments() {
        byte[] plain = cryptoNative.decryptSegments(
                List.of(
                        "hello ".getBytes(StandardCharsets.UTF_8),
                        "world".getBytes(StandardCharsets.UTF_8)
                ),
                new byte[32]
        );

        assertEquals("hello world", new String(plain, StandardCharsets.UTF_8));
    }

    @Test
    void shouldRequire32ByteKeyForSegmentDecrypt() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> cryptoNative.decryptSegments(List.of("x".getBytes(StandardCharsets.UTF_8)), new byte[31]));

        assertEquals("keyBytes must be exactly 32 bytes", ex.getMessage());
    }

    @Test
    void shouldHashBinaryDataAsLowerHex() {
        String digest = cryptoNative.sha256("abc".getBytes(StandardCharsets.UTF_8));

        assertEquals(HexFormat.of().formatHex(new byte[]{
                (byte) 0xba, 0x78, 0x16, (byte) 0xbf, (byte) 0x8f, 0x01, (byte) 0xcf, (byte) 0xea,
                0x41, 0x41, 0x40, (byte) 0xde, 0x5d, (byte) 0xae, 0x22, 0x23,
                (byte) 0xb0, 0x03, 0x61, (byte) 0xa3, (byte) 0x96, 0x17, 0x7a, (byte) 0x9c,
                (byte) 0xb4, 0x10, (byte) 0xff, 0x61, (byte) 0xf2, 0x00, 0x15, (byte) 0xad
        }), digest);
    }
}
