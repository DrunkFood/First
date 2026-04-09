package com.jy.eletender.crypto.native_bridge;

import java.util.List;

public interface ICryptoNative {

    UnpackResult unpackFile(byte[] encryptedFileBytes);

    byte[] packFile(List<byte[]> encryptedSegments, byte[] encryptedProjectInfoRsa, String formatVersion);

    List<byte[]> encryptSegments(byte[] plainBytes, byte[] keyBytes, int segmentSize);

    byte[] decryptSegments(List<byte[]> encryptedSegments, byte[] keyBytes);

    byte[] rsaEncrypt(byte[] plainBytes, String publicKey);

    byte[] rsaDecrypt(byte[] cipherBytes, String privateKey);

    String sha256(byte[] data);

    /**
     * 将 bidderPwdStr (32 bytes) 按 encryptOrder 层数均分。
     * C++ 侧实现后由 JNI 调用；当前 Stub 实现委托给 BidderPwdSegmentUtil。
     */
    List<byte[]> splitBidderPwd(byte[] bidderPwdBytes, int encryptOrder);
}
