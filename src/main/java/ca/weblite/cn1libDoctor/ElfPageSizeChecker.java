package ca.weblite.cn1libDoctor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ElfPageSizeChecker {

    private static final int REQUIRED_ALIGNMENT = 16384; // 16KB = 2^14

    public static class ElfCheckResult {
        public final boolean isSupported;
        public final long maxAlignment;
        public final String message;

        public ElfCheckResult(boolean isSupported, long maxAlignment, String message) {
            this.isSupported = isSupported;
            this.maxAlignment = maxAlignment;
            this.message = message;
        }
    }

    public static ElfCheckResult checkElf(InputStream is) throws IOException {
        byte[] header = new byte[64]; // ELF header is 64 bytes for 64-bit
        int bytesRead = is.read(header);

        if (bytesRead < 16) {
            return new ElfCheckResult(false, 0, "File too small to be an ELF");
        }

        // Check ELF magic
        if (header[0] != 0x7f || header[1] != 'E' || header[2] != 'L' || header[3] != 'F') {
            return new ElfCheckResult(false, 0, "Not an ELF file");
        }

        int elfClass = header[4]; // 1 = 32-bit, 2 = 64-bit
        int elfData = header[5];  // 1 = little endian, 2 = big endian

        ByteOrder byteOrder = (elfData == 1) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer bb = ByteBuffer.wrap(header).order(byteOrder);

        long phoff; // Program header offset
        int phentsize; // Program header entry size
        int phnum; // Number of program headers

        if (elfClass == 2) { // 64-bit
            phoff = bb.getLong(32);
            phentsize = bb.getShort(54) & 0xFFFF;
            phnum = bb.getShort(56) & 0xFFFF;
        } else { // 32-bit
            phoff = bb.getInt(28) & 0xFFFFFFFFL;
            phentsize = bb.getShort(42) & 0xFFFF;
            phnum = bb.getShort(44) & 0xFFFF;
        }

        // Read all program headers
        is.reset();
        is.skip(phoff);

        long maxAlignment = 0;
        boolean hasLoadSegment = false;

        for (int i = 0; i < phnum; i++) {
            byte[] phEntry = new byte[phentsize];
            int read = is.read(phEntry);
            if (read < phentsize) break;

            bb = ByteBuffer.wrap(phEntry).order(byteOrder);

            int pType = bb.getInt(0);

            // PT_LOAD = 1
            if (pType == 1) {
                hasLoadSegment = true;
                long align;

                if (elfClass == 2) { // 64-bit
                    align = bb.getLong(48); // p_align at offset 48 for 64-bit
                } else { // 32-bit
                    align = bb.getInt(28) & 0xFFFFFFFFL; // p_align at offset 28 for 32-bit
                }

                if (align > maxAlignment) {
                    maxAlignment = align;
                }
            }
        }

        if (!hasLoadSegment) {
            return new ElfCheckResult(false, 0, "No LOAD segments found");
        }

        boolean supported = maxAlignment >= REQUIRED_ALIGNMENT;
        String message = String.format("Max alignment: %d (0x%x), Required: %d - %s",
                maxAlignment, maxAlignment, REQUIRED_ALIGNMENT,
                supported ? "SUPPORTED" : "NOT SUPPORTED");

        return new ElfCheckResult(supported, maxAlignment, message);
    }

    public static ElfCheckResult checkElfFile(File file) throws IOException {
        try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            fis.mark(10000000); // Mark for reset
            return checkElf(fis);
        }
    }
}
