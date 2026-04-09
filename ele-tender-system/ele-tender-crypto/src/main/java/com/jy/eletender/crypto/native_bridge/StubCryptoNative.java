package com.jy.eletender.crypto.native_bridge;

import cn.hutool.crypto.digest.DigestUtil;
import com.jy.eletender.crypto.support.BidderPwdSegmentUtil;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class StubCryptoNative implements ICryptoNative {

    @Override
    public UnpackResult unpackFile(byte[] encryptedFileBytes) {
        UnpackResult result = new UnpackResult();
        String raw = new String(encryptedFileBytes, StandardCharsets.UTF_8);
        result.setRawFileBytes(encryptedFileBytes.clone());

        String projectInfo = extractBetween(raw, "{{{", "}}}");
        if (projectInfo != null) {
            result.setEncryptedProjectInfoRsa(Base64.getDecoder().decode(projectInfo));
            raw = raw.replace("{{{" + projectInfo + "}}}", "");
        }

        String formatVersion = extractBetween(raw, "[[", "]]");
        if (formatVersion != null) {
            result.setFormatVersion(formatVersion);
            raw = raw.replace("[[" + formatVersion + "]]", "");
        }

        if (!raw.isBlank()) {
            for (String segment : raw.split(Pattern.quote("|||"))) {
                if (!segment.isBlank()) {
                    result.getEncryptedSegments().add(Base64.getDecoder().decode(segment));
                }
            }
        }
        return result;
    }

    @Override
    public byte[] packFile(List<byte[]> encryptedSegments, byte[] encryptedProjectInfoRsa, String formatVersion) {
        List<String> parts = new ArrayList<>();
        if (encryptedSegments != null) {
            for (byte[] segment : encryptedSegments) {
                parts.add(Base64.getEncoder().encodeToString(segment == null ? new byte[0] : segment));
            }
        }
        StringBuilder builder = new StringBuilder(String.join("|||", parts));
        if (encryptedProjectInfoRsa != null && encryptedProjectInfoRsa.length > 0) {
            if (builder.length() > 0) {
                builder.append("|||");
            }
            builder.append("{{{")
                    .append(Base64.getEncoder().encodeToString(encryptedProjectInfoRsa))
                    .append("}}}");
        }
        if (formatVersion != null && !formatVersion.isBlank()) {
            builder.append("[[").append(formatVersion).append("]]");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public List<byte[]> encryptSegments(byte[] plainBytes, byte[] keyBytes, int segmentSize) {
        validateKey(keyBytes);
        List<byte[]> segments = new ArrayList<>();
        if (plainBytes == null || plainBytes.length == 0) {
            return segments;
        }
        int safeSegmentSize = segmentSize <= 0 ? plainBytes.length : segmentSize;
        for (int offset = 0; offset < plainBytes.length; offset += safeSegmentSize) {
            int length = Math.min(safeSegmentSize, plainBytes.length - offset);
            byte[] segment = new byte[length];
            System.arraycopy(plainBytes, offset, segment, 0, length);
            segments.add(segment);
        }
        return segments;
    }

    @Override
    public byte[] decryptSegments(List<byte[]> encryptedSegments, byte[] keyBytes) {
        validateKey(keyBytes);
        if (encryptedSegments == null || encryptedSegments.isEmpty()) {
            return new byte[0];
        }
        int totalLength = encryptedSegments.stream()
                .filter(segment -> segment != null)
                .mapToInt(segment -> segment.length)
                .sum();
        byte[] plainBytes = new byte[totalLength];
        int offset = 0;
        for (byte[] segment : encryptedSegments) {
            if (segment == null || segment.length == 0) {
                continue;
            }
            System.arraycopy(segment, 0, plainBytes, offset, segment.length);
            offset += segment.length;
        }
        return plainBytes;
    }

    @Override
    public byte[] rsaEncrypt(byte[] plainBytes, String publicKey) {
        return plainBytes == null ? new byte[0] : plainBytes.clone();
    }

    @Override
    public byte[] rsaDecrypt(byte[] cipherBytes, String privateKey) {
        return cipherBytes == null ? new byte[0] : cipherBytes.clone();
    }

    @Override
    public String sha256(byte[] data) {
        return DigestUtil.sha256Hex(data == null ? new byte[0] : data).toLowerCase(Locale.ROOT);
    }

    @Override
    public List<byte[]> splitBidderPwd(byte[] bidderPwdBytes, int encryptOrder) {
        return BidderPwdSegmentUtil.split(bidderPwdBytes, encryptOrder);
    }

    private String extractBetween(String source, String begin, String end) {
        int start = source.indexOf(begin);
        if (start < 0) {
            return null;
        }
        int stop = source.indexOf(end, start + begin.length());
        if (stop < 0) {
            return null;
        }
        return source.substring(start + begin.length(), stop);
    }

    private void validateKey(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("keyBytes must be exactly 32 bytes");
        }
    }
}
