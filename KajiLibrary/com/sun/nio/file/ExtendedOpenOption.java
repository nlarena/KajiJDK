package com.sun.nio.file;

import java.nio.file.OpenOption;

/**
 * File opening options the JDK offers outside the standard set.
 *
 * <p>They are here and not in {@link java.nio.file.StandardOpenOption} because **not every
 * platform can honour them**: the three {@code NOSHARE_*} are a mode of mandatory locking that
 * Windows has and POSIX does not, and {@link #DIRECT} depends on the file system's admitting
 * that its cache should be skipped. To put in the standard set something that on half the
 * platforms throws {@link UnsupportedOperationException} would be promising too much.
 */
public enum ExtendedOpenOption implements OpenOption {

    /** Nobody else may open the file to read while this channel has it. */
    NOSHARE_READ,
    /** Nobody else may open it to write. */
    NOSHARE_WRITE,
    /** Nobody else may delete it. */
    NOSHARE_DELETE,
    /**
     * To skip the file system's cache.
     *
     * <p>It is not a free optimization: it forces the reads and writes to be aligned to the
     * device's block size. It serves whoever administers a cache of their own --a database-- and
     * gets in the way of everybody else.
     */
    DIRECT
}
