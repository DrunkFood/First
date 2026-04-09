package com.jy.eletender.crypto.support;

import java.util.*;

/**
 * bidderPwdStr 信封切段与拼接工具。
 * <p>
 * 切段：将 32 字节 bidderPwdStr 按 encryptOrder 层数均分，末段取余。
 * 拼接：按 order 升序拼接各段，还原 32 字节 bidderPwdStr。
 */
public final class BidderPwdSegmentUtil {

    private static final int BIDDER_PWD_LENGTH = 32;
    private static final int MAX_ENCRYPT_ORDER = 32;

    private BidderPwdSegmentUtil() {
    }

    /**
     * 将 bidderPwdStr 按层数均分。
     *
     * @param bidderPwdBytes 32 字节原始密钥
     * @param encryptOrder   层数，1~32
     * @return 按顺序排列的各层段，index 0 对应 order 最小的层
     */
    public static List<byte[]> split(byte[] bidderPwdBytes, int encryptOrder) {
        if (bidderPwdBytes == null || bidderPwdBytes.length != BIDDER_PWD_LENGTH) {
            throw new IllegalArgumentException(
                    "bidderPwdBytes 长度必须为 " + BIDDER_PWD_LENGTH + " 字节");
        }
        if (encryptOrder < 1 || encryptOrder > MAX_ENCRYPT_ORDER) {
            throw new IllegalArgumentException(
                    "encryptOrder 必须在 1~" + MAX_ENCRYPT_ORDER + " 之间，当前值: " + encryptOrder);
        }

        int baseSize = BIDDER_PWD_LENGTH / encryptOrder;
        int remainder = BIDDER_PWD_LENGTH % encryptOrder;

        List<byte[]> segments = new ArrayList<>(encryptOrder);
        int offset = 0;
        for (int i = 0; i < encryptOrder; i++) {
            // 末段取余：最后一段包含余数字节
            int segSize = (i == encryptOrder - 1) ? baseSize + remainder : baseSize;
            byte[] seg = Arrays.copyOfRange(bidderPwdBytes, offset, offset + segSize);
            segments.add(seg);
            offset += segSize;
        }
        return segments;
    }

    /**
     * 按 order 升序拼接各段，还原 bidderPwdStr。
     *
     * @param orderToSegment order → 解密后的段（raw bytes）
     * @return 还原的 32 字节 bidderPwdStr
     */
    public static byte[] join(Map<Integer, byte[]> orderToSegment) {
        if (orderToSegment == null || orderToSegment.isEmpty()) {
            throw new IllegalArgumentException("orderToSegment 不能为空");
        }

        List<Integer> sortedOrders = new ArrayList<>(orderToSegment.keySet());
        Collections.sort(sortedOrders);

        int totalLength = 0;
        for (byte[] seg : orderToSegment.values()) {
            if (seg == null) {
                throw new IllegalArgumentException("段数据不能为 null");
            }
            totalLength += seg.length;
        }
        if (totalLength != BIDDER_PWD_LENGTH) {
            throw new IllegalArgumentException(
                    "拼接后总长度为 " + totalLength + " 字节，期望 " + BIDDER_PWD_LENGTH + " 字节");
        }

        byte[] result = new byte[BIDDER_PWD_LENGTH];
        int offset = 0;
        for (int order : sortedOrders) {
            byte[] seg = orderToSegment.get(order);
            System.arraycopy(seg, 0, result, offset, seg.length);
            offset += seg.length;
        }
        return result;
    }
}
