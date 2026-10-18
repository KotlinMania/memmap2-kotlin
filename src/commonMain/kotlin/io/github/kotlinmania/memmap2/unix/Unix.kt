// port-lint: source unix.rs
package io.github.kotlinmania.memmap2.unix

import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem
import io.github.kotlinmania.memmap2.FileRegistry
import io.github.kotlinmania.memmap2.MemoryMapBuffer
import io.github.kotlinmania.memmap2.PlatformMmapInner
import io.github.kotlinmania.memmap2.RemapOptions

const val MAP_STACK: Int = 0x20000
const val MAP_POPULATE: Int = 0x08000
const val MAP_HUGETLB: Int = 0x40000
const val MAP_HUGE_MASK: Int = 0x3f
const val MAP_HUGE_SHIFT: Int = 26

class MmapInner(
    private var buffer: PlatformMmapInner,
) : PlatformMmapInner {
    fun adjustMmapParams(len: Long, alignment: Long): Result<Pair<Long, Long>> =
        runCatching {
            val mapLen = (len + alignment).coerceAtLeast(1L)
            val mapOffset = alignment
            Pair(mapLen, mapOffset)
        }

    fun asMmapParams(): Triple<Long, Long, Long> {
        val offset = ptr() % pageSize()
        val total = len() + offset
        return if (total == 0L) {
            Triple(ptr(), 1L, 0L)
        } else {
            Triple(ptr() - offset, total, offset)
        }
    }

    fun fromRawParts(ptr: Long, len: Long, offset: Long): MmapInner =
        MmapInner(MemoryMapBuffer.createAnon(len))

    fun mprotect(prot: Int): Result<Unit> =
        runCatching {
            if (prot == 1) { // PROT_READ
                buffer.makeReadOnly().getOrThrow()
            } else {
                buffer.makeMut().getOrThrow()
            }
        }

    fun drop() {
        close()
    }

    override fun flush(offset: Long, len: Long): Result<Unit> =
        buffer.flush(offset, len)

    override fun flushAsync(offset: Long, len: Long): Result<Unit> =
        buffer.flushAsync(offset, len)

    override fun makeReadOnly(): Result<Unit> =
        buffer.makeReadOnly()

    override fun makeExec(): Result<Unit> =
        buffer.makeExec()

    override fun makeMut(): Result<Unit> =
        buffer.makeMut()

    override fun ptr(): Long =
        buffer.ptr()

    override fun mutPtr(): Long =
        buffer.mutPtr()

    override fun len(): Long =
        buffer.len()

    override fun advise(advice: Int, offset: Long, len: Long): Result<Unit> =
        buffer.advise(advice, offset, len)

    override fun remap(newLen: Long, options: RemapOptions): Result<Unit> =
        buffer.remap(newLen, options)

    override fun lock(): Result<Unit> =
        buffer.lock()

    override fun unlock(): Result<Unit> =
        buffer.unlock()

    override fun get(index: Long): Byte =
        buffer.get(index)

    override fun set(index: Long, value: Byte) =
        buffer.set(index, value)

    override fun asByteArray(): ByteArray =
        buffer.asByteArray()

    override fun copyFrom(src: ByteArray, offset: Long, srcOffset: Int, len: Int) =
        buffer.copyFrom(src, offset, srcOffset, len)

    override fun copyTo(dest: ByteArray, destOffset: Int, startIndex: Long, endIndex: Long) =
        buffer.copyTo(dest, destOffset, startIndex, endIndex)

    override fun close() =
        buffer.close()

    companion object {
        fun new(
            len: Long,
            prot: Int,
            flags: Int,
            file: Long,
            offset: Long,
        ): Result<MmapInner> =
            runCatching {
                val pathStr = FileRegistry.getPath(file)
                val buffer =
                    if (pathStr != null) {
                        MemoryMapBuffer.createFileBacked(
                            path = Path(pathStr),
                            offset = offset,
                            len = len,
                            writable = (prot and 2) != 0,
                            copyOnWrite = (flags and 1) != 0,
                        )
                    } else {
                        MemoryMapBuffer.createAnon(len)
                    }
                MmapInner(buffer)
            }

        fun map(len: Long, file: Long, offset: Long, populate: Boolean): Result<MmapInner> =
            new(len, 1, 0, file, offset)

        fun mapExec(len: Long, file: Long, offset: Long, populate: Boolean): Result<MmapInner> =
            new(len, 1 or 4, 0, file, offset)

        fun mapMut(len: Long, file: Long, offset: Long, populate: Boolean): Result<MmapInner> =
            new(len, 1 or 2, 0, file, offset)

        fun mapCopy(len: Long, file: Long, offset: Long, populate: Boolean): Result<MmapInner> =
            new(len, 1 or 2, 1, file, offset)

        fun mapCopyReadOnly(len: Long, file: Long, offset: Long, populate: Boolean): Result<MmapInner> =
            new(len, 1, 1, file, offset)

        fun mapAnon(len: Long, stack: Boolean, populate: Boolean, huge: UByte?): Result<MmapInner> =
            runCatching {
                MmapInner(MemoryMapBuffer.createAnon(len))
            }
    }
}

fun pageSize(): Long = 4096L

fun fileLen(file: Long): Result<Long> =
    runCatching {
        val pathStr = FileRegistry.getPath(file)
        if (pathStr != null) {
            val path = Path(pathStr)
            if (SystemFileSystem.exists(path)) {
                return@runCatching SystemFileSystem.metadataOrNull(path)?.size ?: 0L
            }
        }
        0L
    }
