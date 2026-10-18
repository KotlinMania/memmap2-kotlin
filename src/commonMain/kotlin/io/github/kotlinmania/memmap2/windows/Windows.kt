// port-lint: source windows.rs
package io.github.kotlinmania.memmap2.windows

import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem
import io.github.kotlinmania.memmap2.FileRegistry
import io.github.kotlinmania.memmap2.MemoryMapBuffer
import io.github.kotlinmania.memmap2.PlatformMmapInner
import io.github.kotlinmania.memmap2.RemapOptions

typealias BOOL = Int
typealias WORD = UShort
typealias DWORD = UInt
typealias WCHAR = UShort
typealias HANDLE = Long
typealias LPHANDLE = Long
typealias LPVOID = Long
typealias LPCVOID = Long
typealias ULONG_PTR = ULong
typealias SIZE_T = ULong
typealias LPCWSTR = String
typealias PDWORD = Long
typealias DWORD_PTR = ULong
typealias LPSECURITY_ATTRIBUTES = Long
typealias LPSYSTEM_INFO = Long

const val INVALID_HANDLE_VALUE: Long = -1L

const val DUPLICATE_SAME_ACCESS: UInt = 0x00000002u
const val STANDARD_RIGHTS_REQUIRED: UInt = 0x000F0000u
const val SECTION_QUERY: UInt = 0x0001u
const val SECTION_MAP_WRITE: UInt = 0x0002u
const val SECTION_MAP_READ: UInt = 0x0004u
const val SECTION_MAP_EXECUTE: UInt = 0x0008u
const val SECTION_EXTEND_SIZE: UInt = 0x0010u
const val SECTION_MAP_EXECUTE_EXPLICIT: UInt = 0x0020u
const val SECTION_ALL_ACCESS: UInt = 0x000F001Fu

const val PAGE_READONLY: UInt = 0x02u
const val PAGE_READWRITE: UInt = 0x04u
const val PAGE_WRITECOPY: UInt = 0x08u
const val PAGE_EXECUTE_READ: UInt = 0x20u
const val PAGE_EXECUTE_READWRITE: UInt = 0x40u
const val PAGE_EXECUTE_WRITECOPY: UInt = 0x80u

const val FILE_MAP_WRITE: UInt = SECTION_MAP_WRITE
const val FILE_MAP_READ: UInt = SECTION_MAP_READ
const val FILE_MAP_ALL_ACCESS: UInt = SECTION_ALL_ACCESS
const val FILE_MAP_EXECUTE: UInt = SECTION_MAP_EXECUTE_EXPLICIT
const val FILE_MAP_COPY: UInt = 0x00000001u

class SECURITY_ATTRIBUTES(
    val nLength: UInt = 0u,
    val lpSecurityDescriptor: Long = 0L,
    val bInheritHandle: Int = 0,
)

class SYSTEM_INFO(
    val wProcessorArchitecture: UShort = 0u,
    val wReserved: UShort = 0u,
    val dwPageSize: UInt = 4096u,
    val lpMinimumApplicationAddress: Long = 0L,
    val lpMaximumApplicationAddress: Long = 0L,
    val dwActiveProcessorMask: ULong = 0u,
    val dwNumberOfProcessors: UInt = 1u,
    val dwProcessorType: UInt = 0u,
    val dwAllocationGranularity: UInt = 65536u,
    val wProcessorLevel: UShort = 0u,
    val wProcessorRevision: UShort = 0u,
)

class FILETIME(
    val dwLowDateTime: UInt = 0u,
    val dwHighDateTime: UInt = 0u,
)

fun emptySlicePtr(): Long = 1L

fun protectionSupported(prot: UInt): Boolean = true

fun allocationGranularity(): Long = 65536L

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

class MmapInner(
    private var buffer: PlatformMmapInner,
) : PlatformMmapInner {
    fun virtualProtect(protect: UInt): Result<Unit> =
        runCatching {
            if (protect == PAGE_READONLY) {
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
            file: Long,
            protect: UInt,
            access: UInt,
            offset: Long,
            len: Long,
        ): Result<MmapInner> =
            runCatching {
                val pathStr = FileRegistry.getPath(file)
                val buffer =
                    if (pathStr != null) {
                        MemoryMapBuffer.createFileBacked(
                            path = Path(pathStr),
                            offset = offset,
                            len = len,
                            writable = (access and FILE_MAP_WRITE) != 0u,
                            copyOnWrite = (access and FILE_MAP_COPY) != 0u,
                        )
                    } else {
                        MemoryMapBuffer.createAnon(len)
                    }
                MmapInner(buffer)
            }

        fun map(file: Long, offset: Long, len: Long): Result<MmapInner> =
            new(file, PAGE_READONLY, FILE_MAP_READ, offset, len)

        fun mapExec(file: Long, offset: Long, len: Long): Result<MmapInner> =
            new(file, PAGE_EXECUTE_READ, FILE_MAP_READ or FILE_MAP_EXECUTE, offset, len)

        fun mapMut(file: Long, offset: Long, len: Long): Result<MmapInner> =
            new(file, PAGE_READWRITE, FILE_MAP_READ or FILE_MAP_WRITE, offset, len)

        fun mapCopy(file: Long, offset: Long, len: Long): Result<MmapInner> =
            new(file, PAGE_WRITECOPY, FILE_MAP_COPY, offset, len)

        fun mapCopyReadOnly(file: Long, offset: Long, len: Long): Result<MmapInner> =
            new(file, PAGE_WRITECOPY, FILE_MAP_READ, offset, len)

        fun mapAnon(len: Long): Result<MmapInner> =
            runCatching {
                MmapInner(MemoryMapBuffer.createAnon(len))
            }
    }
}
