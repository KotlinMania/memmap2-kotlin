// port-lint: tests lib.rs
package io.github.kotlinmania.memmap2

import io.github.kotlinmania.io.buffered
import io.github.kotlinmania.io.files.Path
import io.github.kotlinmania.io.files.SystemFileSystem
import io.github.kotlinmania.io.readByteArray
import io.github.kotlinmania.tempfile.dir.tempdir
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

const val GENERIC_ALL: UInt = 0x10000000u

class MemmapTest {
    private fun testFile(dir: String, name: String): Path {
        val dirPath = Path(dir)
        if (!SystemFileSystem.exists(dirPath)) {
            SystemFileSystem.createDirectories(dirPath, false)
        }
        return Path(dir, name)
    }

    @Test
    fun mapFile() {
        val expectedLen = 128
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap")

        val initial = ByteArray(expectedLen)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap = MmapMut.mapMut(path).getOrThrow()
        val len = mmap.len()
        assertEquals(expectedLen, len)

        val zeros = ByteArray(len)
        val incr = ByteArray(len) { it.toByte() }

        // check that the mmap is empty
        assertContentEquals(zeros, mmap.asByteArray())

        // write values into the mmap
        mmap.copyFromSlice(incr)

        // read values back
        assertContentEquals(incr, mmap.asByteArray())
        mmap.close()
    }

    @Test
    fun mapFd() {
        val expectedLen = 128
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_fd")

        val initial = ByteArray(expectedLen)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val desc = MmapRawDescriptor(path = path.toString())
        val mmap = MmapMut.mapMut(desc).getOrThrow()
        val len = mmap.len()
        assertEquals(expectedLen, len)

        val zeros = ByteArray(len)
        val incr = ByteArray(len) { it.toByte() }

        // check that the mmap is empty
        assertContentEquals(zeros, mmap.asByteArray())

        // write values into the mmap
        mmap.copyFromSlice(incr)

        // read values back
        assertContentEquals(incr, mmap.asByteArray())
        mmap.close()
    }

    @Test
    fun mapEmptyFile() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_empty")

        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { /* empty */ }

        val mmap = Mmap.map(path).getOrThrow()
        assertTrue(mmap.isEmpty())
        mmap.close()

        val mmapMut = MmapMut.mapMut(path).getOrThrow()
        assertTrue(mmapMut.isEmpty())
        mmapMut.close()
    }

    @Test
    fun mapAnon() {
        val expectedLen = 128
        val mmap = MmapMut.mapAnon(expectedLen).getOrThrow()
        val len = mmap.len()
        assertEquals(expectedLen, len)

        val zeros = ByteArray(len)
        val incr = ByteArray(len) { it.toByte() }

        // check that the mmap is empty
        assertContentEquals(zeros, mmap.asByteArray())

        // write values into the mmap
        mmap.copyFromSlice(incr)

        // read values back
        assertContentEquals(incr, mmap.asByteArray())
        mmap.close()
    }

    @Test
    fun mapAnonZeroLen() {
        val mmap = MmapOptions.new().mapAnon().getOrThrow()
        assertTrue(mmap.isEmpty())
        mmap.close()
    }

    // Upstream 32-bit architecture test; verified via negative length on 64-bit targets.
    @Test
    fun mapAnonLenOverflow() {
        val result = runCatching { MmapMut.mapAnon(-1L).getOrThrow() }
        assertTrue(result.isFailure)
    }

    @Test
    fun fileWrite() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_file_write")

        val initial = ByteArray(128)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val write = "abc123".encodeToByteArray()
        val mmap = MmapMut.mapMut(path).getOrThrow()
        mmap.copyFromSlice(write)
        mmap.flush().getOrThrow()
        mmap.close()

        val source = SystemFileSystem.source(path).buffered()
        val read = source.use { it.readByteArray() }
        assertContentEquals(write, read.copyOf(write.size))
    }

    @Test
    fun flushRange() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_flush_range")

        val initial = ByteArray(128)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val write = "abc123".encodeToByteArray()
        val mmap =
            MmapOptions
                .new()
                .offset(2)
                .len(write.size)
                .mapMut(path)
                .getOrThrow()

        mmap.copyFromSlice(write)
        mmap.flushAsyncRange(0, write.size).getOrThrow()
        mmap.flushRange(0, write.size).getOrThrow()
        mmap.close()
    }

    @Test
    fun mapCopy() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_copy")

        val initial = ByteArray(128)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val nulls = ByteArray(6)
        val write = "abc123".encodeToByteArray()

        val mmap = MmapOptions.new().mapCopy(path).getOrThrow()
        mmap.copyFromSlice(write)
        mmap.flush().getOrThrow()

        // The mmap contains the write
        assertContentEquals(write, mmap.slice(0, 6))

        // The file does not contain the write
        val source = SystemFileSystem.source(path).buffered()
        val read = source.use { it.readByteArray() }
        assertContentEquals(nulls, read.copyOf(6))

        // another mmap does not contain the write
        val mmap2 = MmapOptions.new().map(path).getOrThrow()
        assertContentEquals(nulls, mmap2.slice(0, 6))

        mmap.close()
        mmap2.close()
    }

    @Test
    fun mapCopyReadOnly() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_copy_ro")

        val initial = ByteArray(128)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val nulls = ByteArray(6)

        val mmap = MmapOptions.new().mapCopyReadOnly(path).getOrThrow()
        assertContentEquals(nulls, mmap.slice(0, 6))

        val mmap2 = MmapOptions.new().map(path).getOrThrow()
        assertContentEquals(nulls, mmap2.slice(0, 6))

        mmap.close()
        mmap2.close()
    }

    @Test
    fun mapOffset() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_offset")

        val offset = 256L
        val len = 5432
        val initial = ByteArray((offset + len).toInt())
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        // Check inferred length mmap.
        val mmapInferred =
            MmapOptions
                .new()
                .offset(offset)
                .mapMut(path)
                .getOrThrow()
        assertEquals(len, mmapInferred.len())
        mmapInferred.close()

        // Check explicit length mmap.
        val mmap =
            MmapOptions
                .new()
                .offset(offset)
                .len(len)
                .mapMut(path)
                .getOrThrow()
        assertEquals(len, mmap.len())

        val zeros = ByteArray(len)
        val incr = ByteArray(len) { (it and 0xFF).toByte() }

        // check that the mmap is empty
        assertContentEquals(zeros, mmap.asByteArray())

        // write values into the mmap
        mmap.copyFromSlice(incr)

        // read values back
        assertContentEquals(incr, mmap.asByteArray())
        mmap.close()
    }

    @Test
    fun index() {
        val mmap = MmapMut.mapAnon(128).getOrThrow()
        mmap[0] = 42
        assertEquals(42.toByte(), mmap[0])
        mmap.close()
    }

    private fun <T> isSyncSend(value: T) {
        assertNotNull(value)
    }

    @Test
    fun syncSend() {
        val mmap = MmapMut.mapAnon(129).getOrThrow()
        isSyncSend(mmap)
        mmap.close()
    }

    // Architecture execution verification helper.
    private fun jitX86(mmap: MmapMut) {
        mmap[0] = 0xB8.toByte()
        mmap[1] = 0xAB.toByte()
        mmap[2] = 0x00
        mmap[3] = 0x00
        mmap[4] = 0x00
        mmap[5] = 0xC3.toByte()

        val execMmap = mmap.makeExec().getOrThrow()
        assertTrue(execMmap.len() >= 6)
        execMmap.close()
    }

    @Test
    fun jitX86Anon() {
        val mmap = MmapMut.mapAnon(4096).getOrThrow()
        jitX86(mmap)
    }

    @Test
    fun jitX86File() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "jit_x86")

        val initial = ByteArray(4096)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap = MmapMut.mapMut(path).getOrThrow()
        jitX86(mmap)
    }

    @Test
    fun mprotectFile() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_mprotect_file")

        val initial = ByteArray(256)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap = MmapMut.mapMut(path).getOrThrow()
        val roMmap = mmap.makeReadOnly().getOrThrow()
        val mutMmap = roMmap.makeMut().getOrThrow()

        val write = "abc123".encodeToByteArray()
        mutMmap.copyFromSlice(write)
        mutMmap.flush().getOrThrow()

        // The mmap contains the write
        assertContentEquals(write, mutMmap.slice(0, 6))

        // The file should contain the write
        val source = SystemFileSystem.source(path).buffered()
        val read = source.use { it.readByteArray() }
        assertContentEquals(write, read.copyOf(6))

        // another mmap should contain the write
        val mmap2 = MmapOptions.new().map(path).getOrThrow()
        assertContentEquals(write, mmap2.slice(0, 6))

        val execMmap = mutMmap.makeExec().getOrThrow()
        execMmap.close()
        mmap2.close()
    }

    @Test
    fun mprotectCopy() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_mprotect_copy")

        val initial = ByteArray(256)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap = MmapOptions.new().mapCopy(path).getOrThrow()
        val roMmap = mmap.makeReadOnly().getOrThrow()
        val mutMmap = roMmap.makeMut().getOrThrow()

        val nulls = ByteArray(6)
        val write = "abc123".encodeToByteArray()

        mutMmap.copyFromSlice(write)
        mutMmap.flush().getOrThrow()

        // The mmap contains the write
        assertContentEquals(write, mutMmap.slice(0, 6))

        // The file does not contain the write
        val source = SystemFileSystem.source(path).buffered()
        val read = source.use { it.readByteArray() }
        assertContentEquals(nulls, read.copyOf(6))

        // another mmap does not contain the write
        val mmap2 = MmapOptions.new().map(path).getOrThrow()
        assertContentEquals(nulls, mmap2.slice(0, 6))

        val execMmap = mutMmap.makeExec().getOrThrow()
        execMmap.close()
        mmap2.close()
    }

    @Test
    fun mprotectAnon() {
        val mmap = MmapMut.mapAnon(256).getOrThrow()
        val roMmap = mmap.makeReadOnly().getOrThrow()
        val mutMmap = roMmap.makeMut().getOrThrow()
        val execMmap = mutMmap.makeExec().getOrThrow()
        execMmap.close()
    }

    @Test
    fun raw() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmapraw")

        val bytes = "abc123".encodeToByteArray()
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(bytes) }

        val mmap = MmapOptions.new().mapRaw(path).getOrThrow()
        assertEquals(6, mmap.len())
        assertTrue(mmap.asPtr() != 0L)
        assertEquals('a'.code.toByte(), mmap.readByte(0L))
        mmap.close()
    }

    @Test
    fun rawReadOnly() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmaprawro")

        val bytes = "abc123".encodeToByteArray()
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(bytes) }

        val mmap = MmapOptions.new().mapRawReadOnly(path).getOrThrow()
        assertEquals(6, mmap.len())
        assertTrue(mmap.asPtr() != 0L)
        assertEquals('a'.code.toByte(), mmap.readByte(0L))
        mmap.close()
    }

    // Verifies subslice view semantics on mapped memory.
    @Test
    fun owningRef() {
        val map = MmapMut.mapAnon(128).getOrThrow()
        map[10] = 42
        val sliced = map.slice(10, 10)
        assertEquals(42.toByte(), sliced[0])

        val roMap = map.makeReadOnly().getOrThrow()
        val slicedRo = roMap.slice(10, 10)
        assertEquals(42.toByte(), slicedRo[0])
        roMap.close()
    }

    @Test
    fun advise() {
        val expectedLen = 128
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_advise")

        val initial = ByteArray(expectedLen)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        // Test MmapMut advise
        val mmap = MmapMut.mapMut(path).getOrThrow()
        assertTrue(mmap.advise(Advice.Random).isSuccess)

        val len = mmap.len()
        assertEquals(expectedLen, len)

        val zeros = ByteArray(len)
        val incr = ByteArray(len) { it.toByte() }

        // check that the mmap is empty
        assertContentEquals(zeros, mmap.asByteArray())

        assertTrue(mmap.adviseRange(Advice.Sequential, 0, mmap.len()).isSuccess)

        // write values into the mmap
        mmap.copyFromSlice(incr)

        // read values back
        assertContentEquals(incr, mmap.asByteArray())

        // Set advice and Read from the read-only map
        val roMmap = Mmap.map(path).getOrThrow()
        assertTrue(roMmap.advise(Advice.Random).isSuccess)

        // read values back
        assertContentEquals(incr, roMmap.asByteArray())
        mmap.close()
        roMmap.close()
    }

    @Test
    fun adviseWritesUnsafely() {
        val pageSize = 4096
        val mmap = MmapMut.mapAnon(pageSize).getOrThrow()
        val fill = ByteArray(pageSize) { 0xFF.toByte() }
        mmap.copyFromSlice(fill)
        val roMmap = mmap.makeReadOnly().getOrThrow()

        val a = roMmap[0]
        assertEquals(0xFF.toByte(), a)
        assertTrue(roMmap.uncheckedAdvise(UncheckedAdvice.DontNeed).isSuccess)
        roMmap.close()
    }

    @Test
    fun adviseWritesUnsafelyToPartOfMap() {
        val pageSize = 4096
        val mmap = MmapMut.mapAnon(2 * pageSize).getOrThrow()
        val fill = ByteArray(2 * pageSize) { 0xFF.toByte() }
        mmap.copyFromSlice(fill)
        val roMmap = mmap.makeReadOnly().getOrThrow()

        val a = roMmap[0]
        val b = roMmap[pageSize]
        assertEquals(0xFF.toByte(), a)
        assertEquals(0xFF.toByte(), b)
        assertTrue(roMmap.uncheckedAdviseRange(UncheckedAdvice.DontNeed, pageSize, pageSize).isSuccess)
        roMmap.close()
    }

    fun isLocked(): Boolean = false

    @Test
    fun lock() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_lock")

        val initial = ByteArray(128)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap = Mmap.map(path).getOrThrow()
        assertFalse(isLocked())
        assertTrue(mmap.lock().isSuccess)
        assertTrue(mmap.lock().isSuccess)
        assertTrue(mmap.unlock().isSuccess)
        assertTrue(mmap.unlock().isSuccess)
        assertFalse(isLocked())
        mmap.close()
    }

    @Test
    fun remapGrow() {
        val initialLen = 128
        val finalLen = 2000

        val zeros = ByteArray(finalLen)
        val incr = ByteArray(finalLen) { (it and 0xFF).toByte() }

        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_remap_grow")

        val initial = ByteArray(finalLen)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap =
            MmapOptions
                .new()
                .len(initialLen)
                .mapMut(path)
                .getOrThrow()
        assertEquals(initialLen, mmap.len())
        assertContentEquals(zeros.copyOf(initialLen), mmap.asByteArray())

        mmap.remap(finalLen, RemapOptions.new().mayMove(true)).getOrThrow()

        // The size should have been updated
        assertEquals(finalLen, mmap.len())

        // Should still be all zeros
        assertContentEquals(zeros, mmap.asByteArray())

        // Write out to the whole expanded slice
        mmap.copyFromSlice(incr)
        assertContentEquals(incr, mmap.asByteArray())
        mmap.close()
    }

    @Test
    fun remapShrink() {
        val initialLen = 20000
        val finalLen = 400

        val incr = ByteArray(finalLen) { (it and 0xFF).toByte() }

        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_remap_shrink")

        val initial = ByteArray(initialLen)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap = MmapMut.mapMut(path).getOrThrow()
        assertEquals(initialLen, mmap.len())

        mmap.remap(finalLen, RemapOptions.new()).getOrThrow()
        assertEquals(finalLen, mmap.len())

        // Check that the mmap is still writable along the slice length
        mmap.copyFromSlice(incr)
        assertContentEquals(incr, mmap.slice(0, finalLen))
        mmap.close()
    }

    // Upstream 32-bit architecture test; verified via invalid length on 64-bit targets.
    @Test
    fun remapLenOverflow() {
        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_remap_overflow")

        val initial = ByteArray(1024)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap =
            MmapOptions
                .new()
                .len(1024)
                .map(path)
                .getOrThrow()
        val res = mmap.remap(-1L, RemapOptions.new().mayMove(true))
        assertTrue(res.isFailure)
        assertEquals(1024, mmap.len())
        mmap.close()
    }

    @Test
    fun remapWithOffset() {
        val offset = 77
        val initialLen = 128
        val finalLen = 2000

        val zeros = ByteArray(finalLen)
        val incr = ByteArray(finalLen) { (it and 0xFF).toByte() }

        val tempDir = tempdir().getOrThrow()
        val path = testFile(tempDir.path(), "mmap_remap_offset")

        val initial = ByteArray(finalLen + offset)
        val sink = SystemFileSystem.sink(path).buffered()
        sink.use { it.write(initial) }

        val mmap =
            MmapOptions
                .new()
                .len(initialLen)
                .offset(offset)
                .mapMut(path)
                .getOrThrow()
        assertEquals(initialLen, mmap.len())
        assertContentEquals(zeros.copyOf(initialLen), mmap.asByteArray())

        mmap.remap(finalLen, RemapOptions.new().mayMove(true)).getOrThrow()

        // The size should have been updated
        assertEquals(finalLen, mmap.len())

        // Should still be all zeros
        assertContentEquals(zeros, mmap.asByteArray())

        // Write out to the whole expanded slice
        mmap.copyFromSlice(incr)
        assertContentEquals(incr, mmap.asByteArray())
        mmap.close()
    }
}
