package com.jy.eletender.crypto.cli;

import java.nio.file.Path;

public final class NativeVectorExportCli {

    private NativeVectorExportCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: NativeVectorExportCli <outputDir>");
            System.exit(1);
        }
        Path outputDir = Path.of(args[0]);
        new NativeVectorExporter().export(outputDir);
        System.out.println("Native vectors exported to " + outputDir.toAbsolutePath());
    }
}
