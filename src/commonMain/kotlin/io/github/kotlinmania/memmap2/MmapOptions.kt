package io.github.kotlinmania.memmap2

import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem

/**
 * Registry to resolve raw file descriptors to filesystem paths.
 */
object FileRegistry {
    private val paths = HashMap<Long, String>()

    fun register(fd: Long, path: String) {
        paths[fd] = path
    }

    fun getPath(fd: Long): String? = paths[fd]

    fun unregister(fd: Long) {
        paths.remove(fd)
    }
}

/**
 * A memory map builder, providing advanced options and flags for specifying memory map behavior.
 */
class MmapOptions(
    private var offsetValue: Long = 0L,
    private var lenValue: Long? = null,
    private var hugeValue: UByte? = null,
    private var stackValue: Boolean = false,
    private var populateValue: Boolean = false,
) {
    /**
     * Configures the memory map to start at byte offset from the beginning of the file.
     */
    fun offset(offset: Long): MmapOptions {
        offsetValue = offset
        return this
    }

    fun offset(offset: Int): MmapOptions = offset(offset.toLong())

    /**
     * Configures the created memory mapped buffer to be len bytes long.
     */
    fun len(len: Long): MmapOptions {
        lenValue = len
        return this
    }

    fun len(len: Int): MmapOptions = len(len.toLong())

    /**
     * Configures the anonymous memory map to be suitable for a process or thread stack.
     */
    fun stack(): MmapOptions {
        stackValue = true
        return this
    }

    /**
     * Configures the anonymous memory map to be allocated using huge pages.
     */
    fun huge(pageBits: UByte?): MmapOptions {
        hugeValue = pageBits ?: 0u
        return this
    }

    /**
     * Populate page tables for a mapping.
     */
    fun populate(): MmapOptions {
        populateValue = true
        return this
    }

    /**
     * Returns the configured length, or the length of the provided file.
     */
    fun <T : MmapAsRawDesc> getLen(file: T): Result<Long> {
        val configured = lenValue
        if (configured != null) {
            return Result.success(configured)
        }
        val desc = file.asRawDesc()
        val fileLength = queryFileLen(desc)
        if (fileLength < offsetValue) {
            return Result.failure(
                IllegalArgumentException("memory map offset is larger than length"),
            )
        }
        return Result.success(fileLength - offsetValue)
    }

    /**
     * Creates a read-only memory map backed by a file.
     */
    fun <T : MmapAsRawDesc> map(file: T): Result<Mmap> =
        runCatching {
            val desc = file.asRawDesc()
            val length = getLen(desc).getOrThrow()
            val path = resolvePath(desc)
            val inner =
                MemoryMapBuffer.createFileBacked(
                    path = path,
                    offset = offsetValue,
                    len = length,
                    writable = false,
                    copyOnWrite = false,
                )
            Mmap(inner)
        }

    /**
     * Creates a readable and executable memory map backed by a file.
     */
    fun <T : MmapAsRawDesc> mapExec(file: T): Result<Mmap> =
        runCatching {
            val desc = file.asRawDesc()
            val length = getLen(desc).getOrThrow()
            val path = resolvePath(desc)
            val inner =
                MemoryMapBuffer.createFileBacked(
                    path = path,
                    offset = offsetValue,
                    len = length,
                    writable = false,
                    copyOnWrite = false,
                )
            Mmap(inner)
        }

    /**
     * Creates a writeable memory map backed by a file.
     */
    fun <T : MmapAsRawDesc> mapMut(file: T): Result<MmapMut> =
        runCatching {
            val desc = file.asRawDesc()
            val length = getLen(desc).getOrThrow()
            val path = resolvePath(desc)
            val inner =
                MemoryMapBuffer.createFileBacked(
                    path = path,
                    offset = offsetValue,
                    len = length,
                    writable = true,
                    copyOnWrite = false,
                )
            MmapMut(inner)
        }

    /**
     * Creates a copy-on-write memory map backed by a file.
     */
    fun <T : MmapAsRawDesc> mapCopy(file: T): Result<MmapMut> =
        runCatching {
            val desc = file.asRawDesc()
            val length = getLen(desc).getOrThrow()
            val path = resolvePath(desc)
            val inner =
                MemoryMapBuffer.createFileBacked(
                    path = path,
                    offset = offsetValue,
                    len = length,
                    writable = true,
                    copyOnWrite = true,
                )
            MmapMut(inner)
        }

    /**
     * Creates a copy-on-write read-only memory map backed by a file.
     */
    fun <T : MmapAsRawDesc> mapCopyReadOnly(file: T): Result<Mmap> =
        runCatching {
            val desc = file.asRawDesc()
            val length = getLen(desc).getOrThrow()
            val path = resolvePath(desc)
            val inner =
                MemoryMapBuffer.createFileBacked(
                    path = path,
                    offset = offsetValue,
                    len = length,
                    writable = false,
                    copyOnWrite = true,
                )
            Mmap(inner)
        }

    /**
     * Creates an anonymous memory map.
     */
    fun mapAnon(): Result<MmapMut> =
        runCatching {
            val length = lenValue ?: 0L
            val inner = MemoryMapBuffer.createAnon(length)
            MmapMut(inner)
        }

    /**
     * Creates a raw memory map.
     */
    fun <T : MmapAsRawDesc> mapRaw(file: T): Result<MmapRaw> =
        runCatching {
            val desc = file.asRawDesc()
            val length = getLen(desc).getOrThrow()
            val path = resolvePath(desc)
            val inner =
                MemoryMapBuffer.createFileBacked(
                    path = path,
                    offset = offsetValue,
                    len = length,
                    writable = true,
                    copyOnWrite = false,
                )
            MmapRaw(inner)
        }

    /**
     * Creates a read-only raw memory map.
     */
    fun <T : MmapAsRawDesc> mapRawReadOnly(file: T): Result<MmapRaw> =
        runCatching {
            val desc = file.asRawDesc()
            val length = getLen(desc).getOrThrow()
            val path = resolvePath(desc)
            val inner =
                MemoryMapBuffer.createFileBacked(
                    path = path,
                    offset = offsetValue,
                    len = length,
                    writable = false,
                    copyOnWrite = false,
                )
            MmapRaw(inner)
        }

    fun map(path: Path): Result<Mmap> = map(path.asRawDesc())

    fun mapExec(path: Path): Result<Mmap> = mapExec(path.asRawDesc())

    fun mapMut(path: Path): Result<MmapMut> = mapMut(path.asRawDesc())

    fun mapCopy(path: Path): Result<MmapMut> = mapCopy(path.asRawDesc())

    fun mapCopyReadOnly(path: Path): Result<Mmap> = mapCopyReadOnly(path.asRawDesc())

    fun mapRaw(path: Path): Result<MmapRaw> = mapRaw(path.asRawDesc())

    fun mapRawReadOnly(path: Path): Result<MmapRaw> = mapRawReadOnly(path.asRawDesc())

    private fun resolvePath(desc: MmapRawDescriptor): Path {
        val pathStr = desc.path ?: FileRegistry.getPath(desc.raw)
        requireNotNull(pathStr) { "Unable to resolve filesystem path for descriptor $desc" }
        return Path(pathStr)
    }

    private fun queryFileLen(desc: MmapRawDescriptor): Long {
        val pathStr = desc.path ?: FileRegistry.getPath(desc.raw)
        if (pathStr != null) {
            val path = Path(pathStr)
            if (SystemFileSystem.exists(path)) {
                return SystemFileSystem.metadataOrNull(path)?.size ?: 0L
            }
        }
        return 0L
    }

    companion object {
        /**
         * Creates a new set of options for configuring and creating a memory map.
         */
        fun new(): MmapOptions = MmapOptions()
    }
}
