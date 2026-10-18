package io.github.kotlinmania.memmap2

import io.github.kotlinmania.io.files.Path

/**
 * A raw platform descriptor for creating memory maps.
 */
class MmapRawDescriptor(
    val raw: Long = -1L,
    val path: String? = null,
) : MmapAsRawDesc {
    constructor(fd: Int) : this(fd.toLong(), null)
    constructor(path: String) : this(-1L, path)

    override fun asRawDesc(): MmapRawDescriptor = this

    override fun toString(): String =
        if (path != null) "MmapRawDescriptor(path=$path)" else "MmapRawDescriptor(raw=$raw)"
}

/**
 * A trait for types that can be borrowed as a raw descriptor.
 */
interface MmapAsRawDesc {
    fun asRawDesc(): MmapRawDescriptor
}

fun Long.asRawDesc(): MmapRawDescriptor = MmapRawDescriptor(this)

fun Int.asRawDesc(): MmapRawDescriptor = MmapRawDescriptor(this.toLong())

fun String.asRawDesc(): MmapRawDescriptor = MmapRawDescriptor(this)

fun Path.asRawDesc(): MmapRawDescriptor = MmapRawDescriptor(this.toString())
