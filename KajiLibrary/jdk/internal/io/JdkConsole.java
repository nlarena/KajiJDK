package jdk.internal.io;

import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * KajiLibrary's jdk.internal.io.JdkConsole — la interfaz que hay **detrás** de {@link java.io.Console}.
 *
 * <p>La separación existe en el JDK por una razón concreta: la consola de verdad necesita cosas que
 * la biblioteca no puede hacer sola --leer una contraseña sin que se vea en la pantalla, saber si hay
 * una terminal conectada-- así que `java.io.Console` delega en una implementación que provee el
 * runtime. Esta interfaz es ese contrato.
 *
 * <p>Es una **declaración pura**: no promete comportamiento, lo describe. Por eso se puede escribir
 * entera y honesta aunque esta VM no tenga terminal. Quien la implemente decide qué puede cumplir;
 * {@link JdkConsoleImpl} lo dice explícitamente en su encabezado.
 *
 * <p>Los pares de métodos con y sin {@link Locale} no son azúcar: el que lleva locale formatea el
 * mensaje que se muestra antes de leer, y el que no lo lleva es la lectura pelada.
 */
public interface JdkConsole {

    /** El escritor de esta consola. */
    PrintWriter writer();

    /** El lector de esta consola. */
    Reader reader();

    /** Escribe el objeto y un salto de línea. */
    JdkConsole println(Object obj);

    /** Escribe el objeto, sin salto. */
    JdkConsole print(Object obj);

    /** Escribe una cadena formateada. */
    JdkConsole format(Locale locale, String format, Object... args);

    /** Muestra el mensaje formateado y lee una línea. */
    String readLine(Locale locale, String format, Object... args);

    /** Lee una línea. */
    String readLine();

    /**
     * Muestra el mensaje formateado y lee una contraseña **sin eco**.
     *
     * <p>Devuelve `char[]` y no `String` a propósito, y es la única parte de esta interfaz donde el
     * tipo lleva una intención: un arreglo se puede sobreescribir en cuanto se usó, y una cadena
     * queda en el pool hasta que el recolector la levante.
     */
    char[] readPassword(Locale locale, String format, Object... args);

    /** Lee una contraseña sin eco. */
    char[] readPassword();

    /** Vacía lo pendiente de escribir. */
    void flush();

    /** El juego de caracteres de esta consola. */
    Charset charset();
}
