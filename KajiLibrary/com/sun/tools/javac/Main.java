package com.sun.tools.javac;

import java.io.PrintWriter;

/**
 * La puerta vieja al compilador.
 *
 * <h2>Qué es históricamente</h2>
 *
 * <p>Antes de que existiera {@link javax.tools.JavaCompiler}, invocar al compilador desde un
 * programa se hacía llamando a esta clase. Quedó por compatibilidad y hoy no es más que una fachada:
 * en el JDK real delega en la misma maquinaria que usa la API moderna. La forma soportada de
 * compilar desde código es {@link javax.tools.ToolProvider#getSystemJavaCompiler}.
 *
 * <h2>Por qué acá no compila nada</h2>
 *
 * <p>Y no es una carencia que se arregle escribiendo más Java: en este proyecto el compilador
 * <strong>no está escrito en Java</strong>. Es {@code bin/javac.exe}, un binario Rust, y esa
 * decisión es del diseño del proyecto —rompe el bootstrap a propósito— no un paso pendiente.
 * {@link javax.tools.ToolProvider#getSystemJavaCompiler} devuelve {@code null} por lo mismo.
 *
 * <p>Podría hacerse que estos métodos lancen un proceso externo. No se hace: el contrato de
 * {@code compile} es devolver el código de salida de <em>un</em> compilador, y disparar un
 * subproceso cuya ubicación se adivina daría un resultado que a veces es el correcto y a veces es
 * "no encontré el ejecutable", con el mismo tipo de retorno. El criterio de la casa es que un
 * miembro que falta es un subconjunto legal y uno que miente compila y revienta después; declarar
 * que no se puede es la versión honesta.
 */
public class Main {

    /** El JDK también la deja instanciable, aunque no haya nada que instanciar. */
    public Main() {
    }

    /**
     * El punto de entrada de la línea de comandos.
     *
     * @throws UnsupportedOperationException siempre, en esta VM — ver la nota de la clase
     */
    public static void main(String[] args) throws Exception {
        throw new UnsupportedOperationException(
                "el javac de este proyecto es bin/javac.exe, un binario Rust, no esta clase");
    }

    /**
     * Compila, y devuelve el código que devolvería la línea de comandos.
     *
     * @throws UnsupportedOperationException siempre, en esta VM
     */
    public static int compile(String[] args) {
        throw new UnsupportedOperationException(
                "el javac de este proyecto es bin/javac.exe, un binario Rust, no esta clase");
    }

    /**
     * Igual, mandando los diagnósticos a {@code out} en vez de al error estándar.
     *
     * @throws UnsupportedOperationException siempre, en esta VM
     */
    public static int compile(String[] args, PrintWriter out) {
        throw new UnsupportedOperationException(
                "el javac de este proyecto es bin/javac.exe, un binario Rust, no esta clase");
    }
}
