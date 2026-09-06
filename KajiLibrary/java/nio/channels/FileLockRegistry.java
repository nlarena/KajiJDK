package java.nio.channels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// The file locks this virtual machine holds.
//
// ===============================================================================================
// WHY A TABLE OF OUR OWN WHEN THE SYSTEM ALREADY HAS ONE
// ===============================================================================================
//
// Because the system and Java do not count the same thing. To the system a lock belongs to the
// process, so asking twice for the same range from the same process **succeeds** -- and on Windows
// the two even stack, and have to be released twice. To Java that is an error:
// `OverlappingFileLockException`.
//
// The JDK's reason is a good one. A file lock is taken on behalf of the whole virtual machine, so
// two parts of one program that each believe they hold a range are not protected from each other,
// and the system will never say so. This table is what turns that silence into an exception.
//
// **Any overlap counts, even between two shared locks.** Measured against JDK 25: two
// `tryLock(0, 8, true)` in a row give `OverlappingFileLockException`, not two locks. That is
// stricter than the system requires, and deliberately so.
//
// Keyed by canonical path and not by channel: two channels over one file are two objects and one
// file, and it is the file that gets locked.
final class FileLockRegistry {

    private static final List<KajiFileLock> HELD = new ArrayList<KajiFileLock>();

    private FileLockRegistry() {
    }

    /**
     * Checks that nobody in this VM already holds a range overlapping this one.
     *
     * @throws OverlappingFileLockException if somebody does
     */
    static synchronized void check(String file, long position, long size) {
        for (int i = 0; i < HELD.size(); i++) {
            final KajiFileLock l = HELD.get(i);
            if (l.blocksOverlap() && l.file().equals(file) && l.overlaps(position, size)) {
                throw new OverlappingFileLockException();
            }
        }
    }

    /** Records a lock that was just taken. */
    static synchronized void add(KajiFileLock l) {
        HELD.add(l);
    }

    /** Drops one that was released. */
    static synchronized void remove(KajiFileLock l) {
        HELD.remove(l);
    }

    /**
     * Releases every lock taken on that channel.
     *
     * <p>This is what `close()` does: closing a channel invalidates its locks. Without it a lock
     * would stay held in the system until the process died, which is the most expensive way there
     * is to lose a file.
     *
     * @param channel the channel being closed
     * @throws IOException if a release fails
     */
    static void releaseAllOn(Channel channel) throws IOException {
        final List<KajiFileLock> copy;
        synchronized (FileLockRegistry.class) {
            copy = new ArrayList<KajiFileLock>(HELD);
        }
        for (int i = 0; i < copy.size(); i++) {
            if (copy.get(i).acquiredBy() == channel) {
                copy.get(i).release();
            }
        }
    }
}
