package io.github.kotlinmania.memmap2

import io.github.kotlinmania.io.files.Path

/**
 * A handle to a raw memory mapped buffer.
 */
class MmapRaw internal constructor(
    internal val inner: PlatformMmapInner,
) : AutoCloseable {
    /**
     * Returns a raw pointer to the memory mapped file.
     */
    fun asPtr(): Long =
        inner.ptr()

    /**
     * Returns a mutable pointer to the memory mapped file.
     */
    fun asMutPtr(): Long =
        inner.mutPtr()

    /**
     * Returns the length in bytes of the memory map.
     */
    fun len(): Int =
        inner.len().toInt()

    val size: Int get() = len()

    operator fun get(index: Int): Byte =
        inner.get(index.toLong())

    operator fun get(index: Long): Byte =
        inner.get(index)

    fun readByte(offset: Long = 0L): Byte =
        inner.get(offset)

    fun asByteArray(): ByteArray =
        inner.asByteArray()

    /**
     * Flushes outstanding memory map modifications to disk.
     */
    fun flush(): Result<Unit> =
        inner.flush(0L, inner.len())

    /**
     * Asynchronously flushes outstanding memory map modifications to disk.
     */
    fun flushAsync(): Result<Unit> =
        inner.flushAsync(0L, inner.len())

    /**
     * Flushes outstanding memory map modifications in the range to disk.
     */
    fun flushRange(offset: Long, len: Long): Result<Unit> =
        inner.flush(offset, len)

    fun flushRange(offset: Int, len: Int): Result<Unit> =
        flushRange(offset.toLong(), len.toLong())

    /**
     * Asynchronously flushes outstanding memory map modifications in the range to disk.
     */
    fun flushAsyncRange(offset: Long, len: Long): Result<Unit> =
        inner.flushAsync(offset, len)

    fun flushAsyncRange(offset: Int, len: Int): Result<Unit> =
        flushAsyncRange(offset.toLong(), len.toLong())

    /**
     * Advise operating system how this memory map will be accessed.
     */
    fun advise(advice: Advice): Result<Unit> =
        inner.advise(advice.value, 0L, inner.len())

    /**
     * Advise operating system how this memory map will be accessed using unchecked flags.
     */
    fun uncheckedAdvise(advice: UncheckedAdvice): Result<Unit> =
        inner.advise(advice.value, 0L, inner.len())

    /**
     * Advise operating system how this range of memory map will be accessed.
     */
    fun adviseRange(advice: Advice, offset: Long, len: Long): Result<Unit> =
        inner.advise(advice.value, offset, len)

    fun adviseRange(advice: Advice, offset: Int, len: Int): Result<Unit> =
        adviseRange(advice, offset.toLong(), len.toLong())

    /**
     * Advise operating system how this range of memory map will be accessed using unchecked flags.
     */
    fun uncheckedAdviseRange(advice: UncheckedAdvice, offset: Long, len: Long): Result<Unit> =
        inner.advise(advice.value, offset, len)

    fun uncheckedAdviseRange(advice: UncheckedAdvice, offset: Int, len: Int): Result<Unit> =
        uncheckedAdviseRange(advice, offset.toLong(), len.toLong())

    /**
     * Lock the whole memory map into physical memory.
     */
    fun lock(): Result<Unit> =
        inner.lock()

    /**
     * Unlock the whole memory map.
     */
    fun unlock(): Result<Unit> =
        inner.unlock()

    /**
     * Adjust the size of the memory mapping.
     */
    fun remap(newLen: Long, options: RemapOptions): Result<Unit> =
        inner.remap(newLen, options)

    fun remap(newLen: Int, options: RemapOptions): Result<Unit> =
        remap(newLen.toLong(), options)

    override fun close() {
        inner.close()
    }

    override fun toString(): String =
        "MmapRaw(ptr=${asPtr()}, len=${len()})"

    companion object {
        /**
         * Creates a raw memory map backed by a file.
         */
        fun <T : MmapAsRawDesc> mapRaw(file: T): Result<MmapRaw> =
            MmapOptions.new().mapRaw(file)

        fun mapRaw(path: Path): Result<MmapRaw> =
            mapRaw(path.asRawDesc())

        /**
         * Creates a read-only raw memory map backed by a file.
         */
        fun <T : MmapAsRawDesc> mapRawReadOnly(file: T): Result<MmapRaw> =
            MmapOptions.new().mapRawReadOnly(file)

        fun mapRawReadOnly(path: Path): Result<MmapRaw> =
            mapRawReadOnly(path.asRawDesc())

        /**
         * Converts an immutable memory map into a raw memory map.
         */
        fun from(mmap: Mmap): MmapRaw =
            MmapRaw(mmap.inner)

        /**
         * Converts a mutable memory map into a raw memory map.
         */
        fun from(mmapMut: MmapMut): MmapRaw =
            MmapRaw(mmapMut.inner)
    }
}
