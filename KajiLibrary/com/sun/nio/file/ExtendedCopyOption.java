package com.sun.nio.file;

import java.nio.file.CopyOption;

/**
 * Copy options outside the standard set.
 */
public enum ExtendedCopyOption implements CopyOption {

    /**
     * The copy may be interrupted.
     *
     * <p>Copying a big file is a long operation and, by default, a deaf one: interrupting the
     * thread does not stop it. With this option the copy attends to the interruption, aborts and
     * throws {@link java.nio.file.FileSystemException}. The price is that the thread's state has to
     * be checked every so often, and that is why it is not the default behaviour.
     */
    INTERRUPTIBLE
}
