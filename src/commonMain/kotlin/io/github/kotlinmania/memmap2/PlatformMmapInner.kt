package io.github.kotlinmania.memmap2

/**
 * Common internal interface for platform memory map implementations.
 */
interface PlatformMmapInner : AutoCloseable {
    fun len(): Long

    fun ptr(): Long

    fun mutPtr(): Long

    fun get(index: Long): Byte

    fun set(index: Long, value: Byte)

    fun asByteArray(): ByteArray

    fun copyFrom(src: ByteArray, offset: Long, srcOffset: Int, len: Int)

    fun copyTo(dest: ByteArray, destOffset: Int, startIndex: Long, endIndex: Long)

    fun flush(offset: Long, len: Long): Result<Unit>

    fun flushAsync(offset: Long, len: Long): Result<Unit>

    fun makeReadOnly(): Result<Unit>

    fun makeExec(): Result<Unit>

    fun makeMut(): Result<Unit>

    fun advise(advice: Int, offset: Long, len: Long): Result<Unit>

    fun lock(): Result<Unit>

    fun unlock(): Result<Unit>

    fun remap(newLen: Long, options: RemapOptions): Result<Unit>

    override fun close()
}
