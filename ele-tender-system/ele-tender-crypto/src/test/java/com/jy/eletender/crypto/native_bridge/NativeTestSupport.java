package com.jy.eletender.crypto.native_bridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

public final class NativeTestSupport {

    private NativeTestSupport() {
    }

    public static void ensureNativeLibraryBuilt(Path moduleDir) throws Exception {
        run(moduleDir, "cmake", "-S", "src/main/cpp", "-B", "target/native");
        run(moduleDir, "cmake", "--build", "target/native", "--target", "eletender_crypto_native");
        String libraryName = System.getProperty("os.name").toLowerCase().contains("mac")
                ? "libeletender_crypto_native.dylib"
                : "libeletender_crypto_native.so";
        System.setProperty("eletender.crypto.native.lib.path",
                moduleDir.resolve("target/native").resolve(libraryName).toAbsolutePath().toString());
    }

    public static void run(Path workingDirectory, String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            output = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + System.lineSeparator() + output);
        }
    }
}
