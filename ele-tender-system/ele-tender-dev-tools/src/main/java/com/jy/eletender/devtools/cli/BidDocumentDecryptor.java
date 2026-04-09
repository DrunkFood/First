package com.jy.eletender.devtools.cli;

import com.jy.eletender.crypto.native_bridge.JniCryptoNative;
import com.jy.eletender.crypto.native_bridge.UnpackResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 解密投标文件（.HzctTbs → JSON）。
 *
 * <pre>
 * Usage: decrypt-bid -i inputFile -p password [-o outputFile] [--private-key keyFile]
 *
 * Options:
 *   -i &lt;inputFile&gt;         输入 .HzctTbs 文件 (必填)
 *   -p &lt;password&gt;          投标人密码 (必填)
 *   -o &lt;outputFile&gt;        输出 bid document JSON (默认 &lt;input&gt;.dec.json)
 *   --private-key &lt;file&gt;   RSA 私钥 PEM 文件 (默认使用内置测试私钥)
 * </pre>
 */
final class BidDocumentDecryptor {

    private BidDocumentDecryptor() {
    }

    static int run(String[] args) {
        String inputFile = null;
        String outputFile = null;
        String password = null;
        String privateKeyFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i" -> {
                    if (++i >= args.length) { return missingArg("-i"); }
                    inputFile = args[i];
                }
                case "-o" -> {
                    if (++i >= args.length) { return missingArg("-o"); }
                    outputFile = args[i];
                }
                case "-p" -> {
                    if (++i >= args.length) { return missingArg("-p"); }
                    password = args[i];
                }
                case "--private-key" -> {
                    if (++i >= args.length) { return missingArg("--private-key"); }
                    privateKeyFile = args[i];
                }
                default -> {
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    return 1;
                }
            }
        }

        if (inputFile == null) {
            System.err.println("Missing required option: -i");
            printUsage();
            return 1;
        }
        if (password == null) {
            System.err.println("Missing required option: -p");
            printUsage();
            return 1;
        }

        boolean nativeAvailable = BidDocumentGenerator.tryNativeLoad();
        if (!nativeAvailable) {
            System.out.println("[WARN] Native library not available, using stub mode.");
            System.out.println("[WARN] Stub decryption only works on files generated in stub mode.");
        }

        try {
            Path inPath = Paths.get(inputFile).toAbsolutePath();
            Path outPath = outputFile != null
                    ? Paths.get(outputFile).toAbsolutePath()
                    : Path.of(inPath.toString().replaceAll(java.util.regex.Pattern.quote(DevToolsConstants.BID_DOCUMENT_SUFFIX) + "$", "") + ".dec.json");
            Path projectInfoOutPath = Path.of(outPath.toString().replaceAll("\\.json$", "") + ".project-info.json");

            String privateKey = privateKeyFile != null
                    ? Files.readString(Paths.get(privateKeyFile).toAbsolutePath(), StandardCharsets.UTF_8)
                    : BidDocumentGenerator.PRIVATE_KEY;

            // 密码 → SHA256 hex → 32 字节 key
            String bidderPwdHex = sha256Hex(password.getBytes(StandardCharsets.UTF_8));
            byte[] bidderKeyBytes = HexFormat.of().parseHex(bidderPwdHex);

            JniCryptoNative cryptoNative = new JniCryptoNative();
            byte[] encryptedBytes = Files.readAllBytes(inPath);
            UnpackResult unpackResult = cryptoNative.unpackFile(encryptedBytes);

            // 解密 bid document（对称解密）
            byte[] plainBytes = cryptoNative.decryptSegments(unpackResult.getEncryptedSegments(), bidderKeyBytes);

            if (outPath.getParent() != null) {
                Files.createDirectories(outPath.getParent());
            }
            Files.write(outPath, plainBytes);

            // 解密 project info（RSA）
            byte[] projectInfoRsa = unpackResult.getEncryptedProjectInfoRsa();
            if (projectInfoRsa != null && projectInfoRsa.length > 0) {
                byte[] projectInfoBytes = cryptoNative.rsaDecrypt(projectInfoRsa, privateKey);
                Files.write(projectInfoOutPath, projectInfoBytes);
                System.out.println("     projectInfo: " + projectInfoOutPath);
            }

            System.out.println("[OK] decrypt-bid");
            System.out.println("     input     : " + inPath + " (" + encryptedBytes.length + " bytes)");
            System.out.println("     segments  : " + unpackResult.getEncryptedSegments().size());
            System.out.println("     output    : " + outPath + " (" + plainBytes.length + " bytes)");
            System.out.println("     plainSha256: " + sha256Hex(plainBytes));
            System.out.println("     preview   : " + preview(plainBytes, 120));
            return 0;
        } catch (Exception ex) {
            System.err.println("[FAILED] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return 1;
        }
    }

    private static String sha256Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static String preview(byte[] bytes, int maxLen) {
        String s = new String(bytes, StandardCharsets.UTF_8);
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    static void printUsage() {
        System.err.println("Usage: decrypt-bid -i inputFile -p password [-o outputFile] [--private-key keyFile]");
        System.err.println("  -i <inputFile>         输入 .HzctTbs 文件 (必填)");
        System.err.println("  -p <password>          投标人密码 (必填)");
        System.err.println("  -o <outputFile>        输出 JSON (默认 <input>.dec.json)");
        System.err.println("  --private-key <file>   RSA 私钥 PEM 文件 (默认内置测试密钥)");
        System.err.println("Example:");
        System.err.println("  decrypt-bid -i ./dev-output/sample.HzctTbs -p BidderPwd@2026!");
        System.err.println("  decrypt-bid -i ./dev-output/sample.HzctTbs -p MyPwd --private-key ./private-key.pem");
    }

    private static int missingArg(String opt) {
        System.err.println("Missing argument for " + opt);
        printUsage();
        return 1;
    }
}
