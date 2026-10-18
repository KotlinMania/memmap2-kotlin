package io.github.kotlinmania.memmap2

/**
 * Options for remapping a memory mapped buffer.
 */
class RemapOptions(
    private var mayMoveValue: Boolean = false,
) {
    /**
     * If the mapping cannot be resized in place, move it.
     */
    fun mayMove(): RemapOptions {
        mayMoveValue = true
        return this
    }

    internal fun intoFlags(): Int =
        if (mayMoveValue) {
            1 // MREMAP_MAYMOVE
        } else {
            0
        }

    companion object {
        /**
         * Creates a new set of remap options.
         */
        fun new(): RemapOptions = RemapOptions()
    }
}
