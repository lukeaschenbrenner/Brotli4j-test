# Brotli4j

[![Maven Central](https://img.shields.io/maven-central/v/com.aayushatharva.brotli4j/brotli4j-parent.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.aayushatharva.brotli4j%22%20AND%20a:%22brotli4j-parent%22)

Brotli4j provides Brotli compression and decompression for Java.

## Supported Platforms:

Windows (Windows Server 2022) x64  
Linux (CentOS 6) x64  
Linux (CentOS 6) Aarch64  
Linux (Ubuntu 22.04) ARMv7
macOS (Catalina) x64  
macOS (Catalina) Aarch64 (Apple Silicon: M1)

#### *Install [Microsoft Visual C++ Redistributable](https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vc-redist?view=msvc-170) before running this library on Windows

## Download

### Maven

For maven, the natives will
[import automatically by your system family and architecture](https://github.com/hyperxpro/Brotli4j/blob/main/natives/pom.xml#L38-L114).

```xml
<dependency>
    <groupId>com.aayushatharva.brotli4j</groupId>
    <artifactId>brotli4j</artifactId>
    <version>1.9.0</version>
</dependency>
```

### Gradle
For gradle, we have to write some logic to import native automatically. 
Of course, you can add native(s) as dependency manually also.

#### Kotlin DSL

```kotlin
val brotliVersion = "1.8.0"
val operatingSystem: OperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.aayushatharva.brotli4j:brotli4j:$brotliVersion")
    runtimeOnly(
        "com.aayushatharva.brotli4j:native-${
            if (operatingSystem.isWindows) "windows-x86_64"
            else if (operatingSystem.isMacOsX)
                if (DefaultNativePlatform.getCurrentArchitecture().isArm) "osx-aarch64"
                else "osx-x86_64"
            else if (operatingSystem.isLinux)
                if (DefaultNativePlatform.getCurrentArchitecture().isArm) "linux-aarch64"
                else "linux-x86_64"
            else ""
        }:$brotliVersion"
    )
}
```

#### Groovy

```groovy
def brotliVersion = "1.8.0"
def operatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

repositories {
    mavenCentral()
}

dependencies {
    implementation "com.aayushatharva.brotli4j:brotli4j:$brotliVersion"
    runtimeOnly("com.aayushatharva.brotli4j:native-${
        if (operatingSystem.isWindows()) "windows-x86_64"
        else if (operatingSystem.isMacOsX())
            if (DefaultNativePlatform.getCurrentArchitecture().isArm()) "osx-aarch64"
            else "osx-x86_64"
        else if (operatingSystem.isLinux())
            if (DefaultNativePlatform.getCurrentArchitecture().isArm()) "linux-aarch64"
            else "linux-x86_64"
    }:$brotliVersion")
}
```

## Usage

### Loading native library:

Call `Brotli4jLoader.ensureAvailability()` in your application once before using Brotli4j.

### Direct API

```java
public class Example {
    public static void main(String[] args) {
        // Load the native library
        Brotli4jLoader.ensureAvailability();

        // Compress data and get output in byte array
        byte[] compressed = Encoder.compress("Meow".getBytes());

        // Decompress data and get output in DirectDecompress
        DirectDecompress directDecompress = Decoder.decompress(compressed); // or DirectDecompress.decompress(compressed);

        if (directDecompress.getResultStatus() == DecoderJNI.Status.DONE) {
            System.out.println("Decompression Successful: " + new String(directDecompress.getDecompressedData()));
        } else {
            System.out.println("Some Error Occurred While Decompressing");
        }
    }
}
```

### Compressing a stream:

```java
public class Example {
    public static void main(String[] args) {
        // Load the native library
        Brotli4jLoader.ensureAvailability();

        FileInputStream inFile = new FileInputStream(filePath);
        FileOutputStream outFile = new FileOutputStream(filePath + ".br");

        Encoder.Parameters params = new Encoder.Parameters().setQuality(4);

        BrotliOutputStream brotliOutputStream = new BrotliOutputStream(outFile, params);

        int read = inFile.read();
        while (read > -1) {
            brotliOutputStream.write(read);
            read = inFile.read();
        }

        // Close the BrotliOutputStream. This also closes the FileOutputStream.
        brotliOutputStream.close();
        inFile.close();
    }
}
```

### Decompressing a stream:

```java
public class Example {
    public static void main(String[] args) {
        // Load the native library
        Brotli4jLoader.ensureAvailability();

        FileInputStream inFile = new FileInputStream(filePath);
        FileOutputStream outFile = new FileOutputStream(decodedfilePath);

        BrotliInputStream brotliInputStream = new BrotliInputStream(inFile);

        int read = brotliInputStream.read();
        while (read > -1) {
            outFile.write(read);
            read = brotliInputStream.read();
        }

        // Close the BrotliInputStream. This also closes the FileInputStream.
        brotliInputStream.close();
        outFile.close();
    }
}
```

__________________________________________________________________

## Sponsors

JProfiler is supporting Brotli4J with its full-featured Java Profiler. JProfiler's intuitive UI helps you resolve
performance bottlenecks, pin down memory leaks and understand threading issues. Click below to know more:

<a href="https://www.ej-technologies.com/products/jprofiler/overview.html" target="_blank" title="File Management">
  <img src="https://www.ej-technologies.com/images/product_banners/jprofiler_large.png" alt="File Management">
</a>
