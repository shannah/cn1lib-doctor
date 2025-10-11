package ca.weblite.cn1libDoctor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Cn1libAnalyzer {

    public static class NativeLibraryResult {
        public final String path;
        public final boolean supports16KB;
        public final long alignment;
        public final String message;

        public NativeLibraryResult(String path, boolean supports16KB, long alignment, String message) {
            this.path = path;
            this.supports16KB = supports16KB;
            this.alignment = alignment;
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("%s: %s (alignment: 0x%x)", path,
                supports16KB ? "✓ SUPPORTED" : "✗ NOT SUPPORTED", alignment);
        }
    }

    public static class AnalysisResult {
        public final List<NativeLibraryResult> libraries = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();

        public boolean hasUnsupportedLibraries() {
            return libraries.stream().anyMatch(lib -> !lib.supports16KB);
        }

        public boolean hasAnyLibraries() {
            return !libraries.isEmpty();
        }
    }

    public static AnalysisResult analyze(File cn1libFile) throws IOException {
        AnalysisResult result = new AnalysisResult();

        try (ZipInputStream cn1libZip = new ZipInputStream(new FileInputStream(cn1libFile))) {
            ZipEntry entry;

            while ((entry = cn1libZip.getNextEntry()) != null) {
                if (entry.getName().equals("nativeand.zip")) {
                    analyzeNativeAndZip(cn1libZip, result);
                    break;
                }
            }
        }

        if (!result.hasAnyLibraries()) {
            result.errors.add("No Android native libraries (.so files) found in cn1lib");
        }

        return result;
    }

    private static void analyzeNativeAndZip(InputStream nativeAndStream, AnalysisResult result) throws IOException {
        // Read the entire nativeand.zip into memory first
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = nativeAndStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }

        try (ZipInputStream nativeAndZip = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry;

            while ((entry = nativeAndZip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".aar")) {
                    analyzeAar(nativeAndZip, entry.getName(), result);
                } else if (entry.getName().endsWith(".so") && !entry.isDirectory()) {
                    analyzeSharedLibrary(nativeAndZip, entry.getName(), result);
                }
            }
        }
    }

    private static void analyzeAar(InputStream aarStream, String aarName, AnalysisResult result) throws IOException {
        // Read the entire AAR into memory first
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = aarStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }

        try (ZipInputStream aarZip = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            ZipEntry entry;

            while ((entry = aarZip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".so") && !entry.isDirectory()) {
                    String fullPath = aarName + "!" + entry.getName();
                    analyzeSharedLibrary(aarZip, fullPath, result);
                }
            }
        }
    }

    private static void analyzeSharedLibrary(InputStream soStream, String path, AnalysisResult result) {
        try {
            // Create a buffer to hold the stream data since we can't mark/reset zip streams
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = soStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ElfPageSizeChecker.ElfCheckResult elfResult = ElfPageSizeChecker.checkElf(bais);

            result.libraries.add(new NativeLibraryResult(
                    path,
                    elfResult.isSupported,
                    elfResult.maxAlignment,
                    elfResult.message
            ));
        } catch (IOException e) {
            result.errors.add("Error checking " + path + ": " + e.getMessage());
        }
    }
}
