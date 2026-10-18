// port-lint: source stub.rs
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
        when (never as Any) {
            else -> Result.failure(IllegalStateException("unreachable"))
        }

    fun flushAsync(offset: Long, len: Long): Result<Unit> =
        when (never as Any) {
            else -> Result.failure(IllegalStateException("unreachable"))
        }

    fun makeReadOnly(): Result<Unit> =
        when (never as Any) {
            else -> Result.failure(IllegalStateException("unreachable"))
        }

    fun makeExec(): Result<Unit> =
        when (never as Any) {
            else -> Result.failure(IllegalStateException("unreachable"))
        }

    fun makeMut(): Result<Unit> =
        when (never as Any) {
            else -> Result.failure(IllegalStateException("unreachable"))
        }

    fun ptr(): Long =
        when (never as Any) {
            else -> 0L
        }

    fun mutPtr(): Long =
        when (never as Any) {
            else -> 0L
        }

    fun len(): Long =
        when (never as Any) {
            else -> 0L
        }
}

fun fileLen(file: Any?): Result<Long> =
    Result.success(0L)
