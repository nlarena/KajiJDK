package jdk.internal.io;

import java.nio.charset.Charset;

/**
 * KajiLibrary's jdk.internal.io.JdkConsoleProvider — quien fabrica la {@link JdkConsole}.
 *
 * <p>En el JDK esto es un punto de extensión de verdad: se busca por `ServiceLoader`, de modo que un
 * runtime con terminal --o `jshell`, que tiene la suya-- pueda entregar una consola distinta sin que
 * `java.io.Console` sepa nada del asunto. `DEFAULT_PROVIDER_MODULE_NAME` nombra al proveedor que se
 * usa cuando no hay ninguno mejor.
 *
 * <p>La interfaz es una declaración pura y por eso está completa. Lo que esta biblioteca no tiene es
 * un proveedor **registrado**: `System.console()` devuelve `null` sin consultar a nadie, porque no
 * hay terminal. El punto de extensión existe y funciona; lo que no hay es quién se enchufe.
 */
public interface JdkConsoleProvider {

    /**
     * El módulo del proveedor por omisión.
     *
     * <p>Es `"java.base"` y no el nombre de una clase: lo que se busca es el módulo donde vive la
     * implementación, no la implementación misma.
     */
    String DEFAULT_PROVIDER_MODULE_NAME = "java.base";

    /**
     * La consola, o `null` si no hay.
     *
     * @param isTTY si la entrada y la salida estan conectadas a una terminal
     * @param inCharset el juego de caracteres de la entrada
     * @param outCharset el de la salida
     */
    JdkConsole console(boolean isTTY, Charset inCharset, Charset outCharset);
}
