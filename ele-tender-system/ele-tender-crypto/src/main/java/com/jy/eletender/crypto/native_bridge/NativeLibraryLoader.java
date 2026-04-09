package com.jy.eletender.crypto.native_bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

final class NativeLibraryLoader {

    static final String LIB_PATH_PROPERTY = "eletender.crypto.native.lib.path";
    private static final Logger log = LoggerFactory.getLogger(NativeLibraryLoader.class);
    private static volatile boolean loaded;

    private NativeLibraryLoader() {
    }

    static boolean isLoaded() {
        return loaded;
    }

    static synchronized boolean tryLoad() {
        if (loaded) {
            return true;
        }
        String explicitPath = System.getProperty(LIB_PATH_PROPERTY);
        try {
            if (explicitPath != null && !explicitPath.isBlank()) {
                Path libraryPath = Path.of(explicitPath);
                if (Files.exists(libraryPath)) {
                    System.load(libraryPath.toAbsolutePath().toString());
                    loaded = true;
                    return true;
                }
            }
            System.loadLibrary("eletender_crypto_native");
            loaded = true;
            return true;
        } catch (UnsatisfiedLinkError ex) {
            log.warn("Native crypto library not available, fallback to stub: {}", ex.getMessage());
            return false;
        }
    }
}
