package com.sun.nio.file;

import java.nio.file.WatchEvent;

/**
 * How often to poll, when the {@link java.nio.file.WatchService} has no notifications from the
 * system and has to ask.
 *
 * <h2>Why something like this exists</h2>
 *
 * <p>The good implementation of a watching service uses the operating system's notification and
 * finds out at once. When that is not there --a network file system, a platform with no
 * support-- the JDK falls back to **polling**, and there a compromise appears that nobody can
 * resolve for the user: to poll often detects quickly and costs I/O; to poll spaced out is cheap
 * and arrives late.
 *
 * <p>These three constants are that compromise, said by whoever registers. Over an
 * implementation that does not poll, they do nothing -- and that is right: it is a hint, not a
 * requirement.
 *
 * @deprecated the JDK stopped looking at it: the implementations that polled were replaced, so
 *     today the modifier is accepted and ignored.
 */
@Deprecated(since = "23", forRemoval = true)
public enum SensitivityWatchEventModifier implements WatchEvent.Modifier {

    /** To poll often: every 2 seconds. */
    HIGH(2),
    /** The middle point: every 10 seconds. */
    MEDIUM(10),
    /** To poll little: every 30 seconds. */
    LOW(30);

    private final int sensitivity;

    SensitivityWatchEventModifier(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    /** How many seconds apart to poll. */
    public int sensitivityValueInSeconds() {
        return this.sensitivity;
    }
}
