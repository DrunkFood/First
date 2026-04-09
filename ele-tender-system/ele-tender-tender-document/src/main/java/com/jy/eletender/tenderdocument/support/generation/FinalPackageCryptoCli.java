package com.jy.eletender.tenderdocument.support.generation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * 本地文件加解密工具。
 * <p>用法：
 * <pre>
 * encrypt <inputFile> <outputFile> <base64Key> [algorithm]
 * decrypt <inputFile> <outputFile> <base64Key> [algorithm]
 * </pre>
 * 说明：
 * 1) 默认算法是 AES/GCM/NoPadding；
 * 2) key 需要是 Base64 且解码后长度为 16/24/32 字节；
 * 3) 输出目录不存在时会自动创建。
 */
public final class FinalPackageCryptoCli {

    private static final String DEFAULT_ALGORITHM = "AES/GCM/NoPadding";

    private FinalPackageCryptoCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) {
        if (args == null || args.length < 4 || args.length > 5) {
            printUsage();
            return 1;
        }
        String mode = args[0].toLowerCase(Locale.ROOT);
        // 统一转换为绝对路径，便于本地测试时直接复制输出日志中的路径二次操作。
        Path inputPath = Paths.get(args[1]).toAbsolutePath();
        Path outputPath = Paths.get(args[2]).toAbsolutePath();
        String keyBase64 = args[3];
        String algorithm = args.length == 5 ? args[4] : DEFAULT_ALGORITHM;
        try {
            byte[] key = FinalPackageCryptoUtil.decodeBase64Key(keyBase64);
            byte[] input = Files.readAllBytes(inputPath);
            byte[] output;
            if ("encrypt".equals(mode)) {
                output = FinalPackageCryptoUtil.encrypt(input, algorithm, key);
            } else if ("decrypt".equals(mode)) {
                output = FinalPackageCryptoUtil.decrypt(input, algorithm, key);
            } else {
                System.err.println("Unknown mode: " + mode);
                printUsage();
                return 1;
            }
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(outputPath, output);
            System.out.println("OK " + mode + ": " + inputPath + " -> " + outputPath + ", bytes=" + output.length);
            return 0;
        } catch (Exception ex) {
            // 输出异常类型，便于快速区分“参数错误/密钥错误/算法错误/文件读写错误”。
            System.err.println("FAILED: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return 1;
        }
    }

    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  encrypt <inputFile> <outputFile> <base64Key> [algorithm]");
        System.err.println("  decrypt <inputFile> <outputFile> <base64Key> [algorithm]");
        System.err.println("Examples:");
        System.err.println("  encrypt ./plain.json ./plain.HzctZbs <base64Key>");
        System.err.println("  decrypt ./plain.HzctZbs ./plain.dec.json <base64Key>");
        System.err.println("  encrypt ./plain.json ./plain.bin <base64Key> AES/GCM/NoPadding");
        System.err.println("Key:");
        System.err.println("  base64 解码后必须是 16/24/32 字节（AES-128/192/256）");
        System.err.println("  可用 openssl 生成示例：openssl rand -base64 32");
        System.err.println("Default algorithm: " + DEFAULT_ALGORITHM);
    }
}
