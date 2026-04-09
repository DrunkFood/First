package com.jy.eletender.crypto.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.crypto.native_bridge.NativeTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeVectorExportCliTest {

    @BeforeAll
    static void buildNativeLibrary() throws Exception {
        NativeTestSupport.ensureNativeLibraryBuilt(Path.of(System.getProperty("user.dir")));
    }

    @Test
    void shouldExportReusableNativeVectors() throws Exception {
        Path outputDir = Path.of(System.getProperty("java.io.tmpdir"), "native-vectors-" + System.nanoTime());
        NativeVectorExporter.ExportResult result = new NativeVectorExporter().export(outputDir);

        assertTrue(Files.exists(outputDir.resolve("manifest.json")));
        assertTrue(Files.exists(outputDir.resolve("sample.HzctTbs")));
        assertTrue(Files.exists(outputDir.resolve("sample-bid-document.json")));
        assertTrue(Files.exists(outputDir.resolve("sample-project-info-rsa-plain.json")));
        assertTrue(Files.exists(outputDir.resolve("public-key.pem")));
        assertTrue(Files.exists(outputDir.resolve("private-key.pem")));

        assertArrayEquals(result.plainBytes(), result.decryptedPlainBytes());
        assertArrayEquals(result.projectInfoPlainBytes(), result.decryptedProjectInfoBytes());

        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> manifest = mapper.readValue(outputDir.resolve("manifest.json").toFile(), Map.class);
        assertEquals(NativeVectorExporter.SAMPLE_PASSWORD, manifest.get("passwordInput"));
        assertEquals(NativeVectorExporter.FORMAT_VERSION, manifest.get("formatVersion"));

        String readme = Files.readString(outputDir.resolve("README.md"), StandardCharsets.UTF_8);
        assertTrue(readme.contains("Electron / Node 与 Java / JNI 对拍"));

        String exportDir = System.getProperty("native.vector.export.dir");
        if (exportDir != null && !exportDir.isBlank()) {
            Path targetDir = Path.of(exportDir);
            Files.createDirectories(targetDir.getParent());
            copyDirectory(outputDir, targetDir);
        }
    }

    private void copyDirectory(Path sourceDir, Path targetDir) throws Exception {
        Files.createDirectories(targetDir);
        try (var paths = Files.list(sourceDir)) {
            for (Path path : paths.toList()) {
                Files.copy(path, targetDir.resolve(path.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
