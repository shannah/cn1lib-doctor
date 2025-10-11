package ca.weblite.cn1libDoctor;

import junit.framework.TestCase;
import java.io.File;

public class Cn1libAnalyzerTest extends TestCase {

    public void testAnalyzeQRScanner() throws Exception {
        File testFile = new File("/Users/shannah/Downloads/QRScanner.cn1lib");
        if (!testFile.exists()) {
            System.out.println("Test file not found, skipping test");
            return;
        }

        Cn1libAnalyzer.AnalysisResult result = Cn1libAnalyzer.analyze(testFile);

        assertNotNull(result);
        assertTrue("Should find native libraries", result.hasAnyLibraries());

        System.out.println("\n=== Test Results ===");
        System.out.println("Found " + result.libraries.size() + " libraries:");
        for (Cn1libAnalyzer.NativeLibraryResult lib : result.libraries) {
            System.out.println("  " + lib);
        }

        if (result.hasUnsupportedLibraries()) {
            System.out.println("\nSome libraries do not support 16KB page sizes");
        } else {
            System.out.println("\nAll libraries support 16KB page sizes!");
        }
    }
}
