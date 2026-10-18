package io.github.kotlinmania.memmap2

/**
 * A handle to a mutable memory mapped buffer.
 */
class MmapMut internal constructor(
    internal val inner: PlatformMmapInner,
) : AutoCloseable {
    /**
     * Transition the memory map to be read-only.
     */
    fun makeReadOnly(): Result<Mmap> =
        inner.makeReadOnly().map { Mmap(inner) }

    /**
     * Transition the memory map to be executable.
     */
    fun makeExec(): Result<Mmap> =
        inner.makeExec().map { Mmap(inner) }

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

    /**
     * Returns the length in bytes of the memory map.
     */
    fun len(): Int =
        inner.len().toInt()

    val size: Int get() = len()

    /**
     * Returns true if the memory map is empty.
     */
    fun isEmpty(): Boolean =
        inner.len() == 0L

    /**
     * Returns a pointer to the memory mapped buffer.
     */
    fun asPtr(): Long =
        inner.ptr()

    /**
     * Returns a mutable pointer to the memory mapped buffer.
     */
    fun asMutPtr(): Long =
        inner.mutPtr()

    /**
     * Returns the byte at the given index.
     */
    operator fun get(index: Int): Byte =
        inner.get(index.toLong())

    operator fun get(index: Long): Byte =
        inner.get(index)

    /**
     * Sets the byte at the given index.
     */
    operator fun set(index: Int, value: Byte) {
        inner.set(index.toLong(), value)
    }

    operator fun set(index: Long, value: Byte) {
        inner.set(index, value)
    }

    /**
     * Returns a byte array copy or view of the mapped contents.
     */
    fun asByteArray(): ByteArray =
        inner.asByteArray()

    /**
     * Copies all bytes from source slice into this mapped buffer.
     */
    fun copyFromSlice(src: ByteArray, offset: Int = 0) {
        inner.copyFrom(src, offset.toLong(), 0, src.size)
    }

    /**
     * Copies a range of bytes from source into this mapped buffer.
     */
    fun copyFrom(src: ByteArray, offset: Long = 0L, srcOffset: Int = 0, len: Int = src.size) {
        inner.copyFrom(src, offset, srcOffset, len)
    }

    /**
     * Copies data from this mapped buffer into the destination array.
     */
    fun copyTo(
        destination: ByteArray,
        destinationOffset: Int = 0,
        startIndex: Int = 0,
        endIndex: Int = size,
    ) {
        inner.copyTo(destination, destinationOffset, startIndex.toLong(), endIndex.toLong())
    }

    /**
     * Returns a slice of the mapped buffer as a byte array.
     */
    fun slice(offset: Int, length: Int): ByteArray {
        val dest = ByteArray(length)
        copyTo(dest, 0, offset, offset + length)
        return dest
    }

    override fun close() {
        inner.close()
    }

    override fun toString(): String =
        "MmapMut(ptr=${asPtr()}, len=${len()})"

    companion object {
        /**
         * Creates a writeable memory map backed by a file.
         */
        fun <T : MmapAsRawDesc> mapMut(file: T): Result<MmapMut> =
            MmapOptions.new().mapMut(file)

        /**
         * Creates an anonymous memory map of given length.
         */
        fun mapAnon(len: Long): Result<MmapMut> =
            MmapOptions.new().len(len).mapAnon()

        fun mapAnon(len: Int): Result<MmapMut> =
            mapAnon(len.toLong())

        /**
         * Creates a copy-on-write memory map backed by a file.
         */
        fun <T : MmapAsRawDesc> mapCopy(file: T): Result<MmapMut> =
            MmapOptions.new().mapCopy(file)
    }
}
