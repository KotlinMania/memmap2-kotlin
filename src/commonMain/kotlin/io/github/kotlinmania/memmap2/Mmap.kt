package io.github.kotlinmania.memmap2

/**
 * A handle to an immutable memory mapped buffer.
 */
class Mmap internal constructor(
    internal val inner: PlatformMmapInner,
) : AutoCloseable {
    /**
     * Transition the memory map to be writable.
     */
    fun makeMut(): Result<MmapMut> =
        inner.makeMut().map { MmapMut(inner) }

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
     * Returns the byte at the given index.
     */
    operator fun get(index: Int): Byte =
        inner.get(index.toLong())

    operator fun get(index: Long): Byte =
        inner.get(index)

    /**
     * Returns a byte array copy or view of the mapped contents.
     */
    fun asByteArray(): ByteArray =
        inner.asByteArray()

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
        "Mmap(ptr=${asPtr()}, len=${len()})"

    companion object {
        /**
         * Creates a read-only memory map backed by a file.
         */
        fun <T : MmapAsRawDesc> map(file: T): Result<Mmap> =
            MmapOptions.new().map(file)
    }
}
