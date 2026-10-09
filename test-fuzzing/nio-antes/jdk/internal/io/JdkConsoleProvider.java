package jdk.internal.io;

import java.nio.charset.Charset;

/**
 * KajiLibrary's jdk.internal.io.JdkConsoleProvider -- the one that manufactures the
 * {@link JdkConsole}.
 *
 * <p>In the JDK this is a real point of extension: it is looked up with `ServiceLoader`, so that a
 * runtime with a terminal --or `jshell`, which has its own-- can hand over a different console
 * without `java.io.Console` knowing anything about the matter.
 * `DEFAULT_PROVIDER_MODULE_NAME` names the provider that is used when there is no better one.
 *
 * <p>The interface is a pure declaration and that is why it is complete. What this library does not
 * have is a **registered** provider: `System.console()` returns `null` without consulting anybody,
 * because there is no terminal. The point of extension exists and works; what there is not is
 * somebody to plug into it.
 */
public interface JdkConsoleProvider {

    /**
     * The module of the default provider.
     *
     * <p>It is `"java.base"` and not the name of a class: what is looked up is the module where the
     * implementation lives, not the implementation itself.
     */
    String DEFAULT_PROVIDER_MODULE_NAME = "java.base";

    /**
     * The console, or `null` if there is none.
     *
     * @param isTTY whether the input and the output are connected to a terminal
     * @param inCharset the character set of the input
     * @param outCharset that of the output
     */
    JdkConsole console(boolean isTTY, Charset inCharset, Charset outCharset);
}
