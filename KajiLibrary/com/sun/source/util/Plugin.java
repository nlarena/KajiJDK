package com.sun.source.util;

/**
 * Un complemento que se engancha al compilador desde afuera.
 *
 * <h2>Como llega a correr</h2>
 *
 * <p>Se lo encuentra por {@link java.util.ServiceLoader} y se lo elige por nombre con la opcion
 * {@code -Xplugin}. La diferencia con un procesador de anotaciones es el <strong>momento</strong>:
 * un procesador corre en su ronda y ve elementos ya resueltos; un plugin recibe el
 * {@link JavacTask} y puede registrar un {@link TaskListener}, o sea meterse en cada fase — antes de
 * parsear, despues de analizar, al generar.
 *
 * <p>{@link #autoStart} llego despues, con cuerpo, para no romper a los que ya existian: por omision
 * un plugin arranca solo si lo nombran, y devolver {@code false} exige que lo hagan explicitamente.
 */
public interface Plugin {

    /** El nombre con el que se lo nombra en {@code -Xplugin}. */
    String getName();

    /** Se lo llama una vez, con la tarea de compilacion y los argumentos que le hayan pasado. */
    void init(JavacTask task, String... args);

    /** Si arranca sin que lo nombren explicitamente. */
    default boolean autoStart() {
        return true;
    }
}
