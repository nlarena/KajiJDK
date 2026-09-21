package com.sun.source.util;

/**
 * A plugin that is hooked to the compiler from outside.
 *
 * <h2>How it comes to run</h2>
 *
 * <p>It is found by {@link java.util.ServiceLoader} and chosen by name with the
 * {@code -Xplugin} option. The difference with an annotation processor is the
 * <strong>moment</strong>: a processor runs in its round and sees elements that are already
 * resolved; a plugin receives the {@link JavacTask} and may register a {@link TaskListener},
 * that is, get into each phase -- before parsing, after analysing, on generating.
 *
 * <p>{@link #autoStart} arrived later, with a body, so as not to break those that already
 * existed: by default a plugin starts only if it is named, and returning {@code false} requires
 * it to be named explicitly.
 */
public interface Plugin {

    /** The name it is named with in {@code -Xplugin}. */
    String getName();

    /** It is called once, with the compilation task and the arguments it was passed. */
    void init(JavacTask task, String... args);

    /** Whether it starts without being named explicitly. */
    default boolean autoStart() {
        return true;
    }
}
