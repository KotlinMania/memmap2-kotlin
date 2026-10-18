package io.github.kotlinmania.memmap2

import io.github.kotlinmania.io.buffered
import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem
import io.github.kotlinmania.io.readByteArray

/**
 * Backing storage implementation for memory mapped buffers across Kotlin Multiplatform targets.
 */
internal class MemoryMapBuffer(
    private var data: ByteArray,
    private val filePath: Path? = null,
    private val fileOffset: Long = 0L,
    private var isWritable: Boolean = true,
    private val isCopyOnWrite: Boolean = false,
) : PlatformMmapInner {
    private var isClosed = false

    override fun len(): Long = data.size.toLong()

    override fun ptr(): Long = (data.hashCode().toLong() and 0xFFFFFFFFL) or 0x100000000L

    override fun mutPtr(): Long = ptr()

    override fun get(index: Long): Byte {
        checkNotClosed()
        require(index in 0 until len()) { "Index out of bounds: $index for len ${len()}" }
        return data[index.toInt()]
    }

    override fun set(index: Long, value: Byte) {
        checkNotClosed()
        check(isWritable) { "Memory map is read-only" }
        require(index in 0 until len()) { "Index out of bounds: $index for len ${len()}" }
        data[index.toInt()] = value
        syncToSharedCache()
    }

    override fun asByteArray(): ByteArray {
        checkNotClosed()
        return data
    }

    override fun copyFrom(src: ByteArray, offset: Long, srcOffset: Int, len: Int) {
        checkNotClosed()
        check(isWritable) { "Memory map is read-only" }
        require(offset >= 0L && offset + len <= this.len()) {
            "Destination range [offset=$offset, len=$len] exceeds buffer len ${this.len()}"
        }
        require(srcOffset >= 0 && srcOffset + len <= src.size) {
            "Source range [srcOffset=$srcOffset, len=$len] exceeds source size ${src.size}"
        }
        src.copyInto(data, offset.toInt(), srcOffset, srcOffset + len)
        syncToSharedCache()
    }

    override fun copyTo(dest: ByteArray, destOffset: Int, startIndex: Long, endIndex: Long) {
        checkNotClosed()
        require(startIndex >= 0L && endIndex <= len() && startIndex <= endIndex) {
            "Invalid range [$startIndex, $endIndex] for len ${len()}"
        }
        val copyLen = (endIndex - startIndex).toInt()
        require(destOffset >= 0 && destOffset + copyLen <= dest.size) {
            "Destination range [$destOffset, ${destOffset + copyLen}] exceeds size ${dest.size}"
        }
        data.copyInto(dest, destOffset, startIndex.toInt(), endIndex.toInt())
    }

    override fun flush(offset: Long, len: Long): Result<Unit> =
        runCatching {
            checkNotClosed()
            if (isCopyOnWrite || filePath == null || !isWritable) {
                return@runCatching
            }
            syncToSharedCache()
            val pathKey = filePath.toString()
            val merged = sharedCache[pathKey] ?: ByteArray(0)
            val sink = SystemFileSystem.sink(filePath).buffered()
            sink.use { it.write(merged) }
        }

    override fun flushAsync(offset: Long, len: Long): Result<Unit> =
        flush(offset, len)

    override fun makeReadOnly(): Result<Unit> =
        runCatching {
            checkNotClosed()
            isWritable = false
        }

    override fun makeExec(): Result<Unit> =
        runCatching {
            checkNotClosed()
            isWritable = false
        }

    override fun makeMut(): Result<Unit> =
        runCatching {
            checkNotClosed()
            isWritable = true
        }

    override fun advise(advice: Int, offset: Long, len: Long): Result<Unit> =
        runCatching {
            checkNotClosed()
        }

    override fun lock(): Result<Unit> =
        runCatching {
            checkNotClosed()
        }

    override fun unlock(): Result<Unit> =
        runCatching {
            checkNotClosed()
        }

    override fun remap(newLen: Long, options: RemapOptions): Result<Unit> =
        runCatching {
            checkNotClosed()
            val newIntLen = newLen.toInt()
            data = data.copyOf(newIntLen)
            syncToSharedCache()
        }

    override fun close() {
        if (!isClosed) {
            flush(0L, len())
            isClosed = true
        }
    }

    private fun syncToSharedCache() {
        if (!isCopyOnWrite && filePath != null) {
            val key = filePath.toString()
            val current = sharedCache[key] ?: ByteArray(0)
            val neededSize = maxOf(current.size, (fileOffset + len()).toInt())
            val updated = if (current.size < neededSize) current.copyOf(neededSize) else current
            data.copyInto(updated, fileOffset.toInt(), 0, data.size)
            sharedCache[key] = updated
        }
    }

    private fun checkNotClosed() {
        check(!isClosed) { "Memory map is closed" }
    }

    companion object {
        private val sharedCache = HashMap<String, ByteArray>()

        fun createAnon(len: Long): MemoryMapBuffer =
            MemoryMapBuffer(
                data = ByteArray(len.toInt()),
                filePath = null,
                fileOffset = 0L,
                isWritable = true,
                isCopyOnWrite = false,
            )

        fun createFileBacked(
            path: Path,
            offset: Long,
            len: Long,
            writable: Boolean,
            copyOnWrite: Boolean,
        ): MemoryMapBuffer {
            val pathKey = path.toString()
            val fileBytes =
                sharedCache[pathKey] ?: if (SystemFileSystem.exists(path)) {
                    val source = SystemFileSystem.source(path).buffered()
                    val bytes = source.use { it.readByteArray() }
                    sharedCache[pathKey] = bytes
                    bytes
                } else {
                    val bytes = ByteArray(0)
                    sharedCache[pathKey] = bytes
                    bytes
                }

            val bufferSize = len.toInt()
            val slice = ByteArray(bufferSize)
            val availableFromOffset = maxOf(0, fileBytes.size - offset.toInt())
            val copyCount = minOf(bufferSize, availableFromOffset)
            if (copyCount > 0) {
                fileBytes.copyInto(slice, 0, offset.toInt(), offset.toInt() + copyCount)
            }

            return MemoryMapBuffer(
                data = slice,
                filePath = path,
                fileOffset = offset,
                isWritable = writable,
                isCopyOnWrite = copyOnWrite,
            )
        }
    }
}
