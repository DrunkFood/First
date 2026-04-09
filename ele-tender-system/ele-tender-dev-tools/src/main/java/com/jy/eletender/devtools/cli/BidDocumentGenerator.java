package com.jy.eletender.devtools.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.crypto.native_bridge.ICryptoNative;
import com.jy.eletender.crypto.native_bridge.JniCryptoNative;
import com.jy.eletender.crypto.support.BidderPwdSegmentUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 生成 mock 投标文件（.HzctTbs）。
 * <p>无 native library 时自动降级到 StubCryptoNative（分段拼接，非真实加密）。
 *
 * <pre>
 * Usage: generate-bid [-o outputDir] [-p password]
 *
 * Options:
 *   -o &lt;outputDir&gt;  输出目录 (默认 ./dev-output)
 *   -p &lt;password&gt;   投标人密码 (默认 "BidderPwd@2026!")
 * </pre>
 */
final class BidDocumentGenerator {

    private static final String DEFAULT_OUTPUT_DIR = "dev-output";
    static final String DEFAULT_PASSWORD = "BidderPwd@2026!";
    static final String FORMAT_VERSION = "2.0.0";

    // 测试用 RSA 密钥对（与 NativeVectorExporter 相同）
    static final String PUBLIC_KEY = """
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
    static final String PRIVATE_KEY = """
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BidDocumentGenerator() {
    }

    static int run(String[] args) {
        String outputDir = DEFAULT_OUTPUT_DIR;
        String password = DEFAULT_PASSWORD;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o" -> {
                    if (++i >= args.length) { return missingArg("-o"); }
                    outputDir = args[i];
                }
                case "-p" -> {
                    if (++i >= args.length) { return missingArg("-p"); }
                    password = args[i];
                }
                default -> {
                    System.err.println("Unknown option: " + args[i]);
                    printUsage();
                    return 1;
                }
            }
        }

        boolean nativeAvailable = tryNativeLoad();
        if (!nativeAvailable) {
            System.out.println("[WARN] Native library not available, using stub mode.");
            System.out.println("[WARN] Generated .HzctTbs uses stub encryption (no real crypto).");
        }

        try {
            Path outDir = Paths.get(outputDir).toAbsolutePath();
            Files.createDirectories(outDir);

            export(outDir, password, new JniCryptoNative());

            System.out.println("[OK] generate-bid");
            System.out.println("     encrypted  : " + outDir.resolve("sample.HzctTbs"));
            System.out.println("     plaintext  : " + outDir.resolve("sample-bid-document.json"));
            System.out.println("     manifest   : " + outDir.resolve("bid-manifest.json"));
            System.out.println("     password   : " + password);
            System.out.println("     stub mode  : " + !nativeAvailable);
            System.out.println("     round-trip : PASS");
            return 0;
        } catch (Exception ex) {
            System.err.println("[FAILED] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return 1;
        }
    }

    static void export(Path outDir, String password, ICryptoNative cryptoNative) throws Exception {
        // mock 版本信息（招标文件侧 + 投标文件侧，v8 规范要求两者都填）
        // 格式：(格式版本号)_(系统版本号)_(自定义)
        String tenderDocFormatVersion   = "2_1_DEV";
        String tenderDocAppVersion      = "2";
        String tenderDocUniqueCode      = "TD-DEV-" + System.currentTimeMillis();
        String tenderDocSha256          = "mock-tender-document-sha256-placeholder";
        String bidDocFormatVersion      = "2_1_DEV";
        String bidDocAppVersion         = "1";
        String bidDocUniqueCode         = UUID.randomUUID().toString();

        // ── 投标文件明文 JSON（v9 规范：招投标大字段保持一致，投标填招标预留字段） ──

        // baseInfo：项目/标段基本信息
        Map<String, Object> baseInfo = new LinkedHashMap<>();
        baseInfo.put("projectId", "P-DEV-2026-001");
        baseInfo.put("projectName", "dev联调项目");
        baseInfo.put("projectNo", "PN-2026-001");
        baseInfo.put("tenderId", "T-DEV-2026-001");
        baseInfo.put("tenderName", "dev联调一标段");
        baseInfo.put("tenderNo", "TN-2026-001");

        // tender：所投标段的编辑内容（对应招标文件 tenders[] 中单个标段）
        // bidForms：开标标录（标录方案 + 投标方填写的标录数据）
        Map<String, Object> bidForms = new LinkedHashMap<>();
        bidForms.put("schemeContent", DevToolsConstants.MOCK_BID_FORMS_SCHEME_CONTENT);
        Map<String, Object> bidFormData = new LinkedHashMap<>();
        bidFormData.put("bidPrice", "888888.00");
        bidFormData.put("quality", "合格");
        bidFormData.put("time", "60");
        bidForms.put("bidFormData", bidFormData);

        // bidEvalRules：投标方响应的评审规则（从招标文件复制，填写对应响应内容）
        Map<String, Object> qualificationSection = new LinkedHashMap<>();
        qualificationSection.put("reviewMode", "PASS");
        qualificationSection.put("scoreRules", List.of());
        Map<String, Object> qualificationFile = new LinkedHashMap<>();
        qualificationFile.put("fileName", "资格证明文件.pdf");
        qualificationFile.put("sha256", "mock-qualification-sha256");
        qualificationFile.put("size", "102400");
        qualificationFile.put("sourceFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_QUALIFICATION_FILE".getBytes(StandardCharsets.UTF_8)));
        qualificationFile.put("signedFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_QUALIFICATION_FILE_SIGNED".getBytes(StandardCharsets.UTF_8)));
        qualificationSection.put("file", qualificationFile);

        Map<String, Object> conformitySection = new LinkedHashMap<>();
        conformitySection.put("reviewMode", "PASS");
        conformitySection.put("scoreRules", List.of());
        Map<String, Object> conformityFile = new LinkedHashMap<>();
        conformityFile.put("fileName", "符合性文件.pdf");
        conformityFile.put("sha256", "mock-conformity-sha256");
        conformityFile.put("size", "81920");
        conformityFile.put("sourceFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_CONFORMITY_FILE".getBytes(StandardCharsets.UTF_8)));
        conformityFile.put("signedFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_CONFORMITY_FILE_SIGNED".getBytes(StandardCharsets.UTF_8)));
        conformitySection.put("file", conformityFile);

        Map<String, Object> creditSection = new LinkedHashMap<>();
        creditSection.put("reviewMode", "SCORE");
        creditSection.put("totalScore", 100);
        creditSection.put("percentage", 30);
        creditSection.put("scoreRules", List.of());

        Map<String, Object> technicalSection = new LinkedHashMap<>();
        technicalSection.put("reviewMode", "SCORE");
        technicalSection.put("totalScore", 100);
        technicalSection.put("percentage", 30);
        technicalSection.put("scoreRules", List.of());
        Map<String, Object> technicalFile = new LinkedHashMap<>();
        technicalFile.put("fileName", "技术文件.pdf");
        technicalFile.put("sha256", "mock-technical-sha256");
        technicalFile.put("size", "204800");
        technicalFile.put("sourceFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_TECHNICAL_FILE".getBytes(StandardCharsets.UTF_8)));
        technicalFile.put("signedFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_TECHNICAL_FILE_SIGNED".getBytes(StandardCharsets.UTF_8)));
        technicalSection.put("file", technicalFile);

        Map<String, Object> businessSection = new LinkedHashMap<>();
        businessSection.put("reviewMode", "SCORE");
        businessSection.put("totalScore", 100);
        businessSection.put("percentage", 40);
        businessSection.put("scoreRules", List.of());
        Map<String, Object> businessFile = new LinkedHashMap<>();
        businessFile.put("fileName", "商务文件.pdf");
        businessFile.put("sha256", "mock-business-sha256");
        businessFile.put("size", "153600");
        businessFile.put("sourceFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_BUSINESS_FILE".getBytes(StandardCharsets.UTF_8)));
        businessFile.put("signedFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_BUSINESS_FILE_SIGNED".getBytes(StandardCharsets.UTF_8)));
        businessSection.put("file", businessFile);

        Map<String, Object> otherSection = new LinkedHashMap<>();
        Map<String, Object> otherFile = new LinkedHashMap<>();
        otherFile.put("fileName", "其他附件.pdf");
        otherFile.put("sha256", "mock-other-sha256");
        otherFile.put("size", "61440");
        otherFile.put("sourceFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_OTHER_FILE".getBytes(StandardCharsets.UTF_8)));
        otherFile.put("signedFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_OTHER_FILE_SIGNED".getBytes(StandardCharsets.UTF_8)));
        otherSection.put("file", otherFile);

        Map<String, Object> evalRulesInfo = new LinkedHashMap<>();
        evalRulesInfo.put("bidEvalMethod", "综合评分法");
        evalRulesInfo.put("reviewMode", "ACTUAL");

        Map<String, Object> bidEvalRules = new LinkedHashMap<>();
        bidEvalRules.put("info", evalRulesInfo);
        bidEvalRules.put("QUALIFICATION", qualificationSection);
        bidEvalRules.put("CONFORMITY", conformitySection);
        bidEvalRules.put("CREDIT", creditSection);
        bidEvalRules.put("TECHNICAL", technicalSection);
        bidEvalRules.put("BUSINESS", businessSection);
        bidEvalRules.put("OTHER", otherSection);

        // bidLetter：投标函（v8 新增）
        Map<String, Object> bidLetterFile = new LinkedHashMap<>();
        bidLetterFile.put("signedFileBufferBase64", Base64.getEncoder().encodeToString("MOCK_BID_LETTER_SIGNED".getBytes(StandardCharsets.UTF_8)));
        bidLetterFile.put("sha256", "mock-bid-letter-sha256");
        bidLetterFile.put("size", "51200");

        Map<String, Object> bidLetter = new LinkedHashMap<>();
        bidLetter.put("bidderName", "dev联调投标企业有限公司");
        bidLetter.put("legalRepresentative", "张三");
        bidLetter.put("legalIdCard", "330100199001011234");
        bidLetter.put("contactPhone", "13800138000");
        bidLetter.put("fax", "0571-88888888");
        bidLetter.put("address", "浙江省杭州市西湖区XX路XX号");
        bidLetter.put("postalCode", "310000");
        bidLetter.put("bankName", "中国银行杭州分行");
        bidLetter.put("bankAccount", "1234567890123456789");
        bidLetter.put("representativeFlag", false);
        bidLetter.put("representativeName", "");
        bidLetter.put("representativeIdentityCode", "");
        bidLetter.put("representativePhone", "");
        bidLetter.put("remarks", "");
        bidLetter.put("file", bidLetterFile);

        Map<String, Object> tender = new LinkedHashMap<>();
        tender.put("tenderId", "T-DEV-2026-001");
        tender.put("tenderName", "dev联调一标段");
        tender.put("tenderNo", "TN-2026-001");
        tender.put("bidLetter", bidLetter);
        tender.put("bidForms", bidForms);
        tender.put("bidEvalRules", bidEvalRules);

        // versionInfo：招标文件版本信息 + 投标文件版本信息 + 投标端机器指纹
        Map<String, Object> macDiskCpuEntry = new LinkedHashMap<>();
        macDiskCpuEntry.put("cpu", "MOCK_CPU_ID_DEV");
        macDiskCpuEntry.put("disk", "MOCK_DISK_ID_DEV");
        macDiskCpuEntry.put("sysdisk", "MOCK_SYSDISK_ID_DEV");
        macDiskCpuEntry.put("mac", "00:00:00:00:00:01");
        macDiskCpuEntry.put("Ip", "127.0.0.1");
        macDiskCpuEntry.put("computername", "DEV-MACHINE");

        Map<String, Object> versionInfo = new LinkedHashMap<>();
        versionInfo.put("tenderDocumentFormatVersion", tenderDocFormatVersion);
        versionInfo.put("tenderDocumentAppVersion", tenderDocAppVersion);
        versionInfo.put("tenderDocumentSha256", tenderDocSha256);
        versionInfo.put("tenderDocumentUniqueCode", tenderDocUniqueCode);
        versionInfo.put("bidDocumentFormatVersion", bidDocFormatVersion);
        versionInfo.put("bidDocumentAppVersion", bidDocAppVersion);
        versionInfo.put("bidDocumentUniqueCode", bidDocUniqueCode);
        versionInfo.put("macDiskCpu", List.of(macDiskCpuEntry));
        versionInfo.put("creatorCaNo", "MOCK_CA_NO_DEV");
        versionInfo.put("creatorName", "dev联调投标企业有限公司");
        versionInfo.put("creatorEnterpriceCode", "91330100MA2XXXXX0X");
        versionInfo.put("createTime", System.currentTimeMillis());

        Map<String, Object> plain = new LinkedHashMap<>();
        plain.put("baseInfo", baseInfo);
        plain.put("tender", tender);
        plain.put("versionInfo", versionInfo);
        byte[] plainBytes = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(plain);

        // 密码 → SHA256 hex → 32 字节 key
        String bidderPwdHex = sha256Hex(password.getBytes(StandardCharsets.UTF_8));
        byte[] bidderKeyBytes = HexFormat.of().parseHex(bidderPwdHex);
        String bidderPwdBase64 = Base64.getEncoder().encodeToString(bidderKeyBytes);

        // 加密
        List<byte[]> encryptedSegments = cryptoNative.encryptSegments(plainBytes, bidderKeyBytes, 2 * 1024 * 1024);

        // 计算密文 SHA256（各段 base64 用 ||| 连接后整体做 sha256）
        String encryptedContent = encryptedSegments.stream()
                .map(seg -> Base64.getEncoder().encodeToString(seg))
                .reduce((a, b) -> a + "|||" + b)
                .orElse("");
        String encryptedSha256 = sha256Hex(encryptedContent.getBytes(StandardCharsets.UTF_8));

        // 构建 projectInfoRSA（v8 规范：versionInfo 含双版本信息）
        Map<String, Object> projectInfoBaseInfo = new LinkedHashMap<>();
        projectInfoBaseInfo.put("projectId", "P-DEV-2026-001");
        projectInfoBaseInfo.put("projectName", "dev联调项目");
        projectInfoBaseInfo.put("projectNo", "PN-2026-001");
        projectInfoBaseInfo.put("tenderId", "T-DEV-2026-001");
        projectInfoBaseInfo.put("tenderName", "dev联调一标段");
        projectInfoBaseInfo.put("tenderNo", "TN-2026-001");
        projectInfoBaseInfo.put("totalStage", 1);
        projectInfoBaseInfo.put("currentStage", 1);

        Map<String, Object> projectInfoVersionInfo = new LinkedHashMap<>();
        projectInfoVersionInfo.put("tenderDocumentFormatVersion", tenderDocFormatVersion);
        projectInfoVersionInfo.put("tenderDocumentAppVersion", tenderDocAppVersion);
        projectInfoVersionInfo.put("tenderDocumentSha256", tenderDocSha256);
        projectInfoVersionInfo.put("tenderDocumentUniqueCode", tenderDocUniqueCode);
        projectInfoVersionInfo.put("bidDocumentFormatVersion", bidDocFormatVersion);
        projectInfoVersionInfo.put("bidDocumentAppVersion", bidDocAppVersion);
        projectInfoVersionInfo.put("bidDocumentUniqueCode", bidDocUniqueCode);

        Map<String, Object> projectInfo = new LinkedHashMap<>();
        projectInfo.put("baseInfo", projectInfoBaseInfo);
        projectInfo.put("versionInfo", projectInfoVersionInfo);
        projectInfo.put("originSha256", sha256Hex(plainBytes));
        projectInfo.put("encryptedSha256", encryptedSha256);
        // 使用 BidderPwdSegmentUtil 切段生成 hashKeyList（encryptOrder=1 即不切段）
        int encryptOrder = 1;
        List<byte[]> pwdSegments = BidderPwdSegmentUtil.split(bidderKeyBytes, encryptOrder);
        List<Map<String, Object>> hashKeyList = new java.util.ArrayList<>();
        for (int i = 0; i < pwdSegments.size(); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("order", i + 1);
            item.put("caId", "stub-ca-001");
            item.put("hashKeyE", Base64.getEncoder().encodeToString(pwdSegments.get(i)));
            hashKeyList.add(item);
        }
        projectInfo.put("hashKeyList", hashKeyList);
        byte[] projectInfoPlainBytes = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(projectInfo);
        byte[] projectInfoCipherBytes = cryptoNative.rsaEncrypt(projectInfoPlainBytes, PUBLIC_KEY);

        // 打包
        byte[] packedFileBytes = cryptoNative.packFile(encryptedSegments, projectInfoCipherBytes, FORMAT_VERSION);

        // round-trip 验证
        var unpackResult = cryptoNative.unpackFile(packedFileBytes);
        byte[] decryptedPlain = cryptoNative.decryptSegments(unpackResult.getEncryptedSegments(), bidderKeyBytes);
        if (!java.util.Arrays.equals(plainBytes, decryptedPlain)) {
            throw new IllegalStateException("Round-trip verification FAILED");
        }

        // 写出文件
        Files.write(outDir.resolve("sample" + DevToolsConstants.BID_DOCUMENT_SUFFIX), packedFileBytes);
        Files.writeString(outDir.resolve("sample-bid-document.json"),
                new String(plainBytes, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        Files.writeString(outDir.resolve("sample-project-info-rsa-plain.json"),
                new String(projectInfoPlainBytes, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        Files.writeString(outDir.resolve("public-key.pem"), PUBLIC_KEY, StandardCharsets.UTF_8);
        Files.writeString(outDir.resolve("private-key.pem"), PRIVATE_KEY, StandardCharsets.UTF_8);

        // manifest
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("formatVersion", FORMAT_VERSION);
        manifest.put("passwordInput", password);
        manifest.put("bidderPwdHex", bidderPwdHex);
        manifest.put("bidderPwdBase64", bidderPwdBase64);
        manifest.put("plainFile", "sample-bid-document.json");
        manifest.put("projectInfoPlainFile", "sample-project-info-rsa-plain.json");
        manifest.put("encryptedFile", "sample" + DevToolsConstants.BID_DOCUMENT_SUFFIX);
        manifest.put("publicKeyFile", "public-key.pem");
        manifest.put("privateKeyFile", "private-key.pem");
        manifest.put("plainSha256", sha256Hex(plainBytes));
        manifest.put("encryptedFileSha256", sha256Hex(packedFileBytes));
        manifest.put("segmentCount", encryptedSegments.size());
        Files.writeString(outDir.resolve("bid-manifest.json"),
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(manifest),
                StandardCharsets.UTF_8);
    }

    private static String sha256Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    static void printUsage() {
        System.err.println("Usage: generate-bid [-o outputDir] [-p password]");
        System.err.println("  -o <outputDir>   输出目录 (默认 ./dev-output)");
        System.err.println("  -p <password>    投标人密码 (默认 \"BidderPwd@2026!\")");
        System.err.println("Example:");
        System.err.println("  generate-bid -o /tmp/dev-output -p MyBidPwd123");
    }

    private static int missingArg(String opt) {
        System.err.println("Missing argument for " + opt);
        printUsage();
        return 1;
    }

    /**
     * 尝试加载 native 库，检测是否运行在真实加密模式。
     * NativeLibraryLoader 为 package-private，通过系统属性和 loadLibrary 直接检测。
     */
    static boolean tryNativeLoad() {
        String explicitPath = System.getProperty("eletender.crypto.native.lib.path");
        try {
            if (explicitPath != null && !explicitPath.isBlank()) {
                java.nio.file.Path p = java.nio.file.Path.of(explicitPath);
                if (java.nio.file.Files.exists(p)) {
                    System.load(p.toAbsolutePath().toString());
                    return true;
                }
                return false;
            }
            System.loadLibrary("eletender_crypto_native");
            return true;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }
}
