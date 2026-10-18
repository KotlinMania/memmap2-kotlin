// port-lint: source src/advice.rs
package io.github.kotlinmania.memmap2

/**
 * Values supported by advise functions.
 */
enum class Advice(val value: Int) {
    /**
     * No special treatment. This is the default.
     */
    Normal(0),

    /**
     * Expect page references in random order.
     */
    Random(1),

    /**
     * Expect page references in sequential order.
     */
    Sequential(2),

    /**
     * Expect access in the near future.
     */
    WillNeed(3),

    /**
     * Do not make the pages in this range available to the child after a fork.
     */
    DontFork(10),

    /**
     * Undo the effect of DontFork, restoring the default behavior.
     */
    DoFork(11),

    /**
     * Enable Kernel Samepage Merging for the pages in the range.
     */
    Mergeable(12),

    /**
     * Undo the effect of an earlier Mergeable operation.
     */
    Unmergeable(13),

    /**
     * Enable Transparent Huge Pages for pages in the range.
     */
    HugePage(14),

    /**
     * Ensures that memory in the address range will not be backed by transparent hugepages.
     */
    NoHugePage(15),

    /**
     * Exclude from a core dump those pages in the range.
     */
    DontDump(16),

    /**
     * Undo the effect of an earlier DontDump.
     */
    DoDump(17),

    /**
     * Poison the pages in the range and handle subsequent references like corruption.
     */
    HwPoison(100),

    /**
     * Populate page tables readable, faulting in all pages in the range.
     */
    PopulateRead(22),

    /**
     * Populate page tables writable, faulting in all pages in the range.
     */
    PopulateWrite(23),

    /**
     * Darwin only: indicates that the application would like the wired pages zeroed out.
     */
    ZeroWiredPages(6),
}

/**
 * Values supported by unchecked advise functions.
 */
enum class UncheckedAdvice(val value: Int) {
    /**
     * Do not expect access in the near future.
     */
    DontNeed(4),

    /**
     * The application no longer requires the pages in the range.
     */
    Free(8),

    /**
     * Free up a given range of pages and its associated backing store.
     */
    Remove(9),

    /**
     * Darwin only: behaves like Free, but the freed pages are accounted for in process RSS.
     */
    FreeReusable(7),

    /**
     * Darwin only: marks a memory region previously freed as non-reusable.
     */
    FreeReuse(8),
}
