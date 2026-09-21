package com.sun.tools.javac;

import java.io.PrintWriter;

/**
 * The old door to the compiler.
 *
 * <h2>What it is historically</h2>
 *
 * <p>Before {@link javax.tools.JavaCompiler} existed, invoking the compiler from a program was
 * done by calling this class. It was left for compatibility and today it is no more than a
 * facade: in the real JDK it delegates to the same machinery the modern API uses. The supported
 * way of compiling from code is {@link javax.tools.ToolProvider#getSystemJavaCompiler}.
 *
 * <h2>Why it compiles nothing here</h2>
 *
 * <p>And it is not a lack that is fixed by writing more Java: in this project the compiler
 * <strong>is not written in Java</strong>. It is {@code bin/javac.exe}, a Rust binary, and that
 * decision is the project's design -- it breaks the bootstrap on purpose -- not a pending step.
 * {@link javax.tools.ToolProvider#getSystemJavaCompiler} returns {@code null} for the same
 * reason.
 *
 * <p>These methods could be made to launch an external process. It is not done: the contract of
 * {@code compile} is to return the exit code of <em>a</em> compiler, and firing a subprocess
 * whose location is guessed would give a result that is sometimes the right one and sometimes
 * "I did not find the executable", with the same return type. The house's criterion is that a
 * member that is missing is a legal subset and one that lies compiles and blows up afterwards;
 * to declare that it cannot be done is the honest version.
 */
public class Main {

    /** The JDK also leaves it instantiable, even though there is nothing to instantiate. */
    public Main() {
    }

    /**
     * The command line's entry point.
     *
     * @throws UnsupportedOperationException always, on this VM -- see the class note
     */
    public static void main(String[] args) throws Exception {
        throw new UnsupportedOperationException(
                "this project's javac is bin/javac.exe, a Rust binary, not this class");
    }

    /**
     * It compiles, and returns the code the command line would return.
     *
     * @throws UnsupportedOperationException always, on this VM
     */
    public static int compile(String[] args) {
        throw new UnsupportedOperationException(
                "this project's javac is bin/javac.exe, a Rust binary, not this class");
    }

    /**
     * The same, sending the diagnostics to {@code out} instead of to the standard error.
     *
     * @throws UnsupportedOperationException always, on this VM
     */
    public static int compile(String[] args, PrintWriter out) {
        throw new UnsupportedOperationException(
                "this project's javac is bin/javac.exe, a Rust binary, not this class");
    }
}
