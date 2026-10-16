# memmap2-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Fmemmap2-kotlin-blue.svg)](https://github.com/KotlinMania/memmap2-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/memmap2-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/memmap2-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/memmap2-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/memmap2-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`memmap2`](https://github.com/danburkert/memmap2-rs).

**Original Project:** This port is based on [`memmap2`](https://github.com/danburkert/memmap2-rs). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `memmap2`

> The text below is reproduced from [`https://github.com/danburkert/memmap2-rs`](https://github.com/danburkert/memmap2-rs). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs where appropriate so they continue to resolve from this repository.

# memmap2
![Build Status](https://github.com/RazrFalcon/memmap2-rs/workflows/Rust/badge.svg)
[![Crates.io](https://img.shields.io/crates/v/memmap2.svg)](https://crates.io/crates/memmap2)
[![Documentation](https://docs.rs/memmap2/badge.svg)](https://docs.rs/memmap2)
[![Rust 1.36+](https://img.shields.io/badge/rust-1.36+-orange.svg)](https://www.rust-lang.org)

A Rust library for cross-platform memory mapped IO.

This is a **fork** of the [memmap-rs](https://github.com/danburkert/memmap-rs) crate.

## Features

- [x] file-backed memory maps
- [x] anonymous memory maps
- [x] synchronous and asynchronous flushing
- [x] copy-on-write memory maps
- [x] read-only memory maps
- [x] stack support (`MAP_STACK` on unix)
- [x] executable memory maps
- [x] huge page support (linux only)

A list of supported/tested targets can be found in [Actions](https://github.com/RazrFalcon/memmap2-rs/actions).

## License

`memmap2` is primarily distributed under the terms of both the MIT license and the
Apache License (Version 2.0).

See [LICENSE-APACHE](LICENSE-APACHE), [LICENSE-MIT](LICENSE-MIT) for details.

Copyright (c) 2020 Yevhenii Reizner

Copyright (c) 2015 Dan Burkert

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:memmap2-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same license terms as the upstream [`memmap2`](https://github.com/danburkert/memmap2-rs). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the memmap2 authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`memmap2`](https://github.com/danburkert/memmap2-rs) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
