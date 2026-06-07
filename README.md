# nio-filecopy

Console utility to copy files using 3 strategies. An opportunity to compare .

The goal — not just only copy files with three different approaches, but а **representative comparing** their
performance, CPU usage, throughput.

## Strategies

| Name       | How it works                               | Data stream                              |
|------------|--------------------------------------------|------------------------------------------|
| `stream`   | classic `java.io` (`Buffered*Stream`)      | disk → core → JVM-array → core → disk    |
| `buffer`   | NIO через `ByteBuffer` + `FileChannel`     | disk → core → `ByteBuffer` → core → disk |
| `transfer` | zero-copy через `FileChannel.transferTo()` | dis → core → disk (bypass user space)    |

`transfer` on Linux turns into system call `sendfile()` — data don't run into application's code.

## Build

```bash
mvn clean package
# as result:  target/filecopy.jar
```

## Usage

```bash
# Copy with specific strategy
java -jar target/filecopy.jar <src> <dst> --strategy=transfer

# by default buffer strategy
java -jar target/filecopy.jar big.bin copy.bin

# Benchmark mode: runs all three strategies and prints summary table
java -jar target/filecopy.jar big.bin copy.bin --benchmark
```

Create big test file:

```bash
# Linux
fallocate -l 1G big.bin
# macOS
mkfile 1g big.bin
```

## Summary

**Env:** _CPU: **AMD Ryzen 5 5600X (12) @ 3.700GHz** /
RAM: **DDR4** / HDD / ОС: **Linux Mint 22 x86_64**/ ver JDK_ 17.0.17
**File:** 500 MB

| Strategy | Time, ms | Throughput, MB/s |
|----------|----------|------------------|
| stream   | _3909,7_ | _122,0_          |
| buffer   | _3817,4_ | _124,9_          |
| transfer | _3746,4_ | _127,3_          |

## Test

```bash
mvn test
```

Test checks **correctness**: copy equals to original byte per byte (check on SHA-256), and — rewriting file doesn't
leave tail from previous file (check on `TRUNCATE_EXISTING`).

