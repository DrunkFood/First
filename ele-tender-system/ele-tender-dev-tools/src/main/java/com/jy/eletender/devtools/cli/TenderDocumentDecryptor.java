package com.jy.eletender.devtools.cli;

import com.jy.eletender.tenderdocument.support.generation.FinalPackageCryptoUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 解密招标文件（.HzctZbs → JSON）。
 *
 * <pre>
 * Usage: decrypt-tender -i inputFile -k base64Key [-o outputFile]
 *
 * Options:
 *   -i &lt;inputFile&gt;    输入 .HzctZbs 文件 (必填)
 *   -k &lt;base64Key&gt;   AES 密钥 Base64 (必填)
 *   -o &lt;outputFile&gt;  输出 JSON 文件 (默认 &lt;input&gt;.dec.json)
 * </pre>
 */
final class TenderDocumentDecryptor {

    private static final String DEFAULT_ALGORITHM = "AES/GCM/NoPadding";

    private TenderDocumentDecryptor() {
    }

    static int run(String[] args) {
        String inputFile = null;
        String outputFile = null;
        String keyBase64 = null;

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
                case "-k" -> {
                    if (++i >= args.length) { return missingArg("-k"); }
                    keyBase64 = args[i];
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
        if (keyBase64 == null) {
            System.err.println("Missing required option: -k");
            printUsage();
            return 1;
        }

        try {
            Path inPath = Paths.get(inputFile).toAbsolutePath();
            Path outPath = outputFile != null
                    ? Paths.get(outputFile).toAbsolutePath()
                    : Path.of(inPath.toString().replaceAll(java.util.regex.Pattern.quote(DevToolsConstants.TENDER_DOCUMENT_SUFFIX) + "$", "") + ".dec.json");

            byte[] aesKey = FinalPackageCryptoUtil.decodeBase64Key(keyBase64);
            byte[] encryptedBytes = Files.readAllBytes(inPath);
            byte[] plainBytes = FinalPackageCryptoUtil.decrypt(encryptedBytes, DEFAULT_ALGORITHM, aesKey);

            if (outPath.getParent() != null) {
                Files.createDirectories(outPath.getParent());
            }
            Files.write(outPath, plainBytes);

            System.out.println("[OK] decrypt-tender");
            System.out.println("     input     : " + inPath + " (" + encryptedBytes.length + " bytes)");
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
        System.err.println("Usage: decrypt-tender -i inputFile -k base64Key [-o outputFile]");
        System.err.println("  -i <inputFile>    输入 .HzctZbs 文件 (必填)");
        System.err.println("  -k <base64Key>    AES 密钥 Base64 (必填)");
        System.err.println("  -o <outputFile>   输出 JSON 文件 (默认 <input>.dec.json)");
        System.err.println("Example:");
        System.err.println("  decrypt-tender -i ./dev-output/sample-tender-document.HzctZbs -k <base64Key>");
    }

    private static int missingArg(String opt) {
        System.err.println("Missing argument for " + opt);
        printUsage();
        return 1;
    }
}
