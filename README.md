# Cn1lib Doctor

A diagnostic tool for analyzing Codename One libraries (.cn1lib files) to check if they meet Google's Android requirement for 16KB page size support.

## Overview

Starting with Android 15, Google requires that all native libraries (.so files) support 16KB page sizes. This tool helps you diagnose whether a cn1lib file contains Android native libraries that do or don't meet this requirement.

## Features

- **File Selection**: Easy-to-use file dialog to select .cn1lib files
- **Deep Analysis**: Automatically extracts and analyzes nested archives:
  - Extracts nativeand.zip from the cn1lib
  - Scans for .aar files and standalone .so files
  - Analyzes all native libraries within .aar files
- **ELF Analysis**: Parses ELF headers to check LOAD segment alignment
- **Clear Results**: Shows detailed analysis with:
  - Full path to each native library
  - Support status (✓ SUPPORTED / ✗ NOT SUPPORTED)
  - Alignment values in hex and decimal
  - Overall summary

## Requirements

* Java 21 or later

## Usage

### Run the Application

```bash
./mvnw spring-boot:run
```

Or run the packaged JAR:

```bash
java -jar target/cn1lib-doctor-1.0-SNAPSHOT.jar
```

### Using the Tool

1. Click **"Select cn1lib file..."** to choose a .cn1lib file
2. Click **"Inspect"** to analyze the file
3. Review the results showing which native libraries support 16KB page sizes

### Example Output

```
=== 16KB Page Size Analysis Results ===

Found 14 native libraries:

✓ SUPPORTED
  Path: ZBarScannerLibrary.aar!jni/arm64-v8a/libiconv.so
  Alignment: 0x10000 (65536 bytes)

✗ NOT SUPPORTED
  Path: ZBarScannerLibrary.aar!jni/armeabi-v7a/libiconv.so
  Alignment: 0x1000 (4096 bytes)

...

=== Summary ===
✗ This cn1lib has libraries that DO NOT support 16KB page sizes.
  These libraries need to be recompiled with proper alignment.
```

## Build

```bash
./mvnw package
```

This creates an executable JAR at `target/cn1lib-doctor-1.0-SNAPSHOT.jar`

## Technical Details

### What is 16KB Page Size Support?

Google's requirement states that native libraries must have ELF LOAD segments aligned to at least 16KB (16384 bytes = 2^14). This tool checks the `p_align` field of PT_LOAD program headers in ELF files.

### How It Works

1. **ZIP Extraction**: cn1lib files are ZIP archives containing a `nativeand.zip` for Android native code
2. **Nested Analysis**: The tool recursively extracts:
   - nativeand.zip → .aar files and .so files
   - .aar files → jni/* .so files
3. **ELF Parsing**: For each .so file:
   - Reads ELF header (supports both 32-bit and 64-bit)
   - Locates program headers
   - Finds PT_LOAD segments
   - Checks if alignment >= 16384 (0x4000)

### Tested Architectures

The tool correctly identifies alignment for all Android architectures:
- arm64-v8a
- armeabi
- armeabi-v7a
- x86
- x86_64
- mips
- mips64

## Testing

Run the test suite:

```bash
./mvnw test
```

## License

See LICENSE file for details.

## Related Resources

- [Android 16KB Page Size Documentation](https://developer.android.com/guide/practices/page-sizes)
- [Codename One](https://www.codenameone.com/)
