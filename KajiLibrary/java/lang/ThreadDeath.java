package java.lang;

/**
 * KajiLibrary's java.lang.ThreadDeath — thrown into a thread by the withdrawn Thread.stop().
 *
 * It extends Error and not Exception on purpose: a thread being killed should unwind without
 * some `catch (Exception)` along the way swallowing it. It is deprecated together with the
 * mechanism that threw it — stopping a thread from outside leaves whatever it was mutating
 * half-written, which is why the whole idea was withdrawn.
 */
@Deprecated(since = "1.2", forRemoval = true)
public class ThreadDeath extends Error {

    public ThreadDeath() {
    }
}
