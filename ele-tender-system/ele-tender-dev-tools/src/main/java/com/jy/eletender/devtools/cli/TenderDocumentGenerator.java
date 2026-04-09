package com.jy.eletender.devtools.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.tenderdocument.model.generation.FinalPackagePayload;
import com.jy.eletender.tenderdocument.model.generation.FinalPackageRulePayload;
import com.jy.eletender.tenderdocument.model.generation.FinalPackageTenderPayload;
import com.jy.eletender.tenderdocument.support.generation.FinalPackageCryptoUtil;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成 mock 招标文件（.HzctZbs）。
 *
 * <pre>
 * Usage: generate-tender [-o outputDir] [-k base64Key]
 *
 * Options:
 *   -o &lt;outputDir&gt;   输出目录 (默认 ./dev-output)
 *   -k &lt;base64Key&gt;   AES 密钥 Base64 (默认自动生成 32 字节)
 * </pre>
 */
final class TenderDocumentGenerator {

    private static final String DEFAULT_OUTPUT_DIR = "dev-output";
    private static final String DEFAULT_ALGORITHM = "AES/GCM/NoPadding";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TenderDocumentGenerator() {
    }

    static int run(String[] args) {
        String outputDir = DEFAULT_OUTPUT_DIR;
        String keyBase64 = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o" -> {
                    if (++i >= args.length) { return missingArg("-o"); }
                    outputDir = args[i];
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

        try {
            Path outDir = Paths.get(outputDir).toAbsolutePath();
            Files.createDirectories(outDir);

            // 生成或解析 AES key
            byte[] aesKey;
            if (keyBase64 == null) {
                aesKey = new byte[32];
                new SecureRandom().nextBytes(aesKey);
                keyBase64 = Base64.getEncoder().encodeToString(aesKey);
                System.out.println("[INFO] Generated AES key (Base64): " + keyBase64);
            } else {
                aesKey = FinalPackageCryptoUtil.decodeBase64Key(keyBase64);
            }

            // 构造 mock payload
            FinalPackagePayload payload = buildMockPayload();
            byte[] plainBytes = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);

            // 加密
            byte[] encryptedBytes = FinalPackageCryptoUtil.encrypt(plainBytes, DEFAULT_ALGORITHM, aesKey);

            // 写出文件
            Path plainFile = outDir.resolve("sample-tender-document.json");
            Path encFile = outDir.resolve("sample-tender-document" + DevToolsConstants.TENDER_DOCUMENT_SUFFIX);
            Files.write(plainFile, plainBytes);
            Files.write(encFile, encryptedBytes);

            // 写 manifest
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("algorithm", DEFAULT_ALGORITHM);
            manifest.put("aesKeyBase64", keyBase64);
            manifest.put("plainFile", "sample-tender-document.json");
            manifest.put("encryptedFile", "sample-tender-document" + DevToolsConstants.TENDER_DOCUMENT_SUFFIX);
            manifest.put("plainSha256", sha256Hex(plainBytes));
            manifest.put("encryptedSha256", sha256Hex(encryptedBytes));
            manifest.put("plainBytes", plainBytes.length);
            manifest.put("encryptedBytes", encryptedBytes.length);
            Files.writeString(outDir.resolve("tender-manifest.json"),
                    MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(manifest),
                    StandardCharsets.UTF_8);

            // round-trip 验证
            byte[] decrypted = FinalPackageCryptoUtil.decrypt(encryptedBytes, DEFAULT_ALGORITHM, aesKey);
            if (!java.util.Arrays.equals(plainBytes, decrypted)) {
                System.err.println("[ERROR] Round-trip verification FAILED!");
                return 1;
            }

            System.out.println("[OK] generate-tender");
            System.out.println("     plaintext  : " + plainFile);
            System.out.println("     encrypted  : " + encFile);
            System.out.println("     manifest   : " + outDir.resolve("tender-manifest.json"));
            System.out.println("     AES key    : " + keyBase64);
            System.out.println("     round-trip : PASS");
            return 0;
        } catch (Exception ex) {
            System.err.println("[FAILED] " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return 1;
        }
    }

    private static FinalPackagePayload buildMockPayload() {
        FinalPackagePayload payload = new FinalPackagePayload();

        // --- baseInfo ---
        FinalPackagePayload.TenderInfo tenderInfo1 = new FinalPackagePayload.TenderInfo();
        tenderInfo1.setTenderId("T-DEV-2026-001");
        tenderInfo1.setTenderName("dev联调一标段");
        tenderInfo1.setTenderNo("TN-2026-001");
        tenderInfo1.setTenderAmount(new BigDecimal("1000000.00"));
        tenderInfo1.setPurchaseContext("一标段：办公设备采购及安装");

        FinalPackagePayload.TenderInfo tenderInfo2 = new FinalPackagePayload.TenderInfo();
        tenderInfo2.setTenderId("T-DEV-2026-002");
        tenderInfo2.setTenderName("dev联调二标段");
        tenderInfo2.setTenderNo("TN-2026-002");
        tenderInfo2.setTenderAmount(new BigDecimal("2000000.00"));
        tenderInfo2.setPurchaseContext("二标段：网络设备采购及部署");

        FinalPackagePayload.BaseInfo baseInfo = new FinalPackagePayload.BaseInfo();
        baseInfo.setProjectId("P-DEV-2026-001");
        baseInfo.setProjectName("dev联调项目");
        baseInfo.setProjectNo("PN-2026-001");
        baseInfo.setPurchaserName("dev联调采购单位（政府采购中心）");
        baseInfo.setPurchaseMethod("公开招标");
        baseInfo.setBidEndTime("2026-06-01 10:00:00");
        baseInfo.setTendersInfo(List.of(tenderInfo1, tenderInfo2));
        payload.setBaseInfo(baseInfo);

        // --- tenders ---
        FinalPackageTenderPayload tenderPayload1 = new FinalPackageTenderPayload();
        tenderPayload1.setTenderId("T-DEV-2026-001");
        tenderPayload1.setTenderName("dev联调一标段");
        tenderPayload1.setTenderNo("TN-2026-001");
        tenderPayload1.setBidForms(buildMockBidForms());
        tenderPayload1.setBidEvalRules(buildMockBidEvalRules("综合评分法"));

        FinalPackageTenderPayload tenderPayload2 = new FinalPackageTenderPayload();
        tenderPayload2.setTenderId("T-DEV-2026-002");
        tenderPayload2.setTenderName("dev联调二标段");
        tenderPayload2.setTenderNo("TN-2026-002");
        tenderPayload2.setBidForms(buildMockBidForms());
        tenderPayload2.setBidEvalRules(buildMockBidEvalRules("最低评标价法"));

        payload.setTenders(List.of(tenderPayload1, tenderPayload2));

        // --- tenderDocumentSignPdf ---
        FinalPackagePayload.TenderDocumentSignPdf signPdf = new FinalPackagePayload.TenderDocumentSignPdf();
        signPdf.setFileName("招标文件.pdf");
        signPdf.setSize(1024L);
        signPdf.setSignedFileBase64(Base64.getEncoder().encodeToString("MOCK_PDF_CONTENT".getBytes(StandardCharsets.UTF_8)));
        signPdf.setSha256("mock-pdf-sha256");
        payload.setTenderDocumentSignPdf(signPdf);

        // --- caKeysInfo ---
        FinalPackagePayload.CaKeyInfo caKey = new FinalPackagePayload.CaKeyInfo();
        caKey.setEncryptOrder(1);
        caKey.setUserId("dev-user-001");
        caKey.setCaId("stub-ca-001");
        caKey.setCaNo("CA-2026-001");
        caKey.setPublicKey("MOCK_PUBLIC_KEY");
        payload.setCaKeysInfo(List.of(caKey));

        // --- settings ---
        FinalPackagePayload.SignPosition signPos = new FinalPackagePayload.SignPosition();
        signPos.setX(100);
        signPos.setY(100);
        FinalPackagePayload.Settings settings = new FinalPackagePayload.Settings();
        settings.setSignPosition(signPos);
        payload.setSettings(settings);

        // --- versionInfo ---
        FinalPackagePayload.VersionInfo versionInfo = new FinalPackagePayload.VersionInfo();
        versionInfo.setTenderDocumentFormatVersion("2_1_DEV");
        versionInfo.setTenderDocumentAppVersion("2");
        versionInfo.setTenderDocumentUniqueCode("DEV-" + System.currentTimeMillis());
        payload.setVersionInfo(versionInfo);

        return payload;
    }

    private static Map<String, Object> buildMockBidForms() {
        Map<String, Object> bidForms = new LinkedHashMap<>();
        bidForms.put("schemeContent", DevToolsConstants.MOCK_BID_FORMS_SCHEME_CONTENT);
        return bidForms;
    }

    private static FinalPackageRulePayload buildMockBidEvalRules(String bidEvalMethod) {
        FinalPackageRulePayload rules = new FinalPackageRulePayload();

        FinalPackageRulePayload.Info info = new FinalPackageRulePayload.Info();
        info.setBidEvalMethod(bidEvalMethod);
        info.setReviewMode("ACTUAL");
        rules.setInfo(info);

        rules.setQualification(buildPassSection("资格审查条件：营业执照、资质证书齐全有效"));
        rules.setConformity(buildPassSection("符合性审查：投标文件格式、签章符合招标文件要求"));

        if ("最低评标价法".equals(bidEvalMethod)) {
            rules.setDetail(buildPassSection("详细评审：价格核实无重大偏差"));
        } else {
            rules.setCredit(buildScoreSection("SCORE", 100, 30,
                    "信用分：企业信用等级 AAA 得满分，AA 得 80 分，A 得 60 分"));
            rules.setTechnical(buildScoreSection("SCORE", 100, 30,
                    "技术分：技术方案完整性、先进性评分"));
            rules.setBusiness(buildScoreSection("SCORE", 100, 40,
                    "商务分：报价合理性，最低报价得满分"));
        }

        return rules;
    }

    private static FinalPackageRulePayload.RuleSection buildPassSection(String standard) {
        FinalPackageRulePayload.RuleSection section = new FinalPackageRulePayload.RuleSection();
        section.setReviewMode("PASS");
        FinalPackageRulePayload.RuleNode node = buildRuleNode("r-" + standard.hashCode(), standard);
        node.setIsPass(Boolean.TRUE);
        section.setScoreRules(List.of(node));
        return section;
    }

    private static FinalPackageRulePayload.ScoreRuleSection buildScoreSection(
            String reviewMode, int totalScore, int percentage, String standard) {
        FinalPackageRulePayload.ScoreRuleSection section = new FinalPackageRulePayload.ScoreRuleSection();
        section.setReviewMode(reviewMode);
        section.setTotalScore(totalScore);
        section.setPercentage(percentage);
        section.setScoreRules(List.of(buildRuleNode("r-" + standard.hashCode(), standard)));
        return section;
    }

    private static FinalPackageRulePayload.RuleNode buildRuleNode(String id, String standard) {
        FinalPackageRulePayload.RuleNode node = new FinalPackageRulePayload.RuleNode();
        node.setId(id);
        node.setOrder(1);
        node.setKey("1");
        node.setName(standard.length() > 10 ? standard.substring(0, 10) : standard);
        node.setStandard(standard);
        node.setLowest(BigDecimal.ZERO);
        node.setHighest(BigDecimal.TEN);
        node.setObjectiveType("OBJECTIVE");
        node.setChildren(List.of());
        node.setIsParent(Boolean.FALSE);
        return node;
    }

    private static String sha256Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    static void printUsage() {
        System.err.println("Usage: generate-tender [-o outputDir] [-k base64Key]");
        System.err.println("  -o <outputDir>   输出目录 (默认 ./dev-output)");
        System.err.println("  -k <base64Key>   AES 密钥 Base64，解码后 16/24/32 字节 (默认自动生成)");
        System.err.println("Example:");
        System.err.println("  generate-tender -o /tmp/dev-output");
        System.err.println("  generate-tender -o /tmp/dev-output -k $(openssl rand -base64 32)");
    }

    private static int missingArg(String opt) {
        System.err.println("Missing argument for " + opt);
        printUsage();
        return 1;
    }
}
