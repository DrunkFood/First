package com.jy.eletender.devtools.cli;

import java.util.Arrays;

/**
 * 统一开发联调 CLI 工具。
 *
 * <pre>
 * Usage: EleTenderDevCli &lt;command&gt; [options]
 *
 * Commands:
 *   generate-tender   生成 mock 招标文件 (.HzctZbs)
 *   generate-bid      生成 mock 投标文件 (.HzctTbs)
 *   decrypt-tender    解密招标文件 (.HzctZbs → JSON)
 *   decrypt-bid       解密投标文件 (.HzctTbs → JSON)
 * </pre>
 */
public final class EleTenderDevCli {

    private EleTenderDevCli() {
    }

    public static void main(String[] args) {
        int exitCode = run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args) {
        if (args == null || args.length == 0) {
            printUsage();
            return 1;
        }
        String command = args[0];
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return switch (command) {
            case "generate-tender" -> TenderDocumentGenerator.run(rest);
            case "generate-bid" -> BidDocumentGenerator.run(rest);
            case "decrypt-tender" -> TenderDocumentDecryptor.run(rest);
            case "decrypt-bid" -> BidDocumentDecryptor.run(rest);
            default -> {
                System.err.println("Unknown command: " + command);
                printUsage();
                yield 1;
            }
        };
    }

    static void printUsage() {
        System.err.println("Usage: EleTenderDevCli <command> [options]");
        System.err.println();
        System.err.println("Commands:");
        System.err.println("  generate-tender   生成 mock 招标文件 (.HzctZbs)");
        System.err.println("  generate-bid      生成 mock 投标文件 (.HzctTbs)");
        System.err.println("  decrypt-tender    解密招标文件 (.HzctZbs → JSON)");
        System.err.println("  decrypt-bid       解密投标文件 (.HzctTbs → JSON)");
        System.err.println();
        System.err.println("Run 'EleTenderDevCli <command>' without options for command-specific help.");
    }
}
