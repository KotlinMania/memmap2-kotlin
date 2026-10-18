// port-lint: source src/stub.rs
package io.github.kotlinmania.memmap2.stub

/**
 * An uninstantiable type representing an impossible state.
 */
enum class Never

/**
 * Platform stub for unsupported targets.
 */
class MmapInner private constructor(
    private val never: Never,
) {
    companion object {
        fun new(): Result<MmapInner> =
            Result.failure(IllegalStateException("platform not supported"))

        fun map(len: Long, file: Any?, offset: Long, populate: Boolean): Result<MmapInner> =
            new()

        fun mapExec(len: Long, file: Any?, offset: Long, populate: Boolean): Result<MmapInner> =
            new()

        fun mapMut(len: Long, file: Any?, offset: Long, populate: Boolean): Result<MmapInner> =
            new()

        fun mapCopy(len: Long, file: Any?, offset: Long, populate: Boolean): Result<MmapInner> =
            new()

        fun mapCopyReadOnly(len: Long, file: Any?, offset: Long, populate: Boolean): Result<MmapInner> =
            new()

        fun mapAnon(len: Long, stack: Boolean, populate: Boolean, huge: UByte?): Result<MmapInner> =
            new()
    }

    fun flush(offset: Long, len: Long): Result<Unit> =
        when (never) {}

    fun flushAsync(offset: Long, len: Long): Result<Unit> =
        when (never) {}

    fun makeReadOnly(): Result<Unit> =
        when (never) {}

    fun makeExec(): Result<Unit> =
        when (never) {}

    fun makeMut(): Result<Unit> =
        when (never) {}

    fun ptr(): Long =
        when (never) {}

    fun mutPtr(): Long =
        when (never) {}

    fun len(): Long =
        when (never) {}
}

fun fileLen(file: Any?): Result<Long> =
    Result.success(0L)
