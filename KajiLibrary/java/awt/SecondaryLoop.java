package java.awt;

/**
 * Una espera que no bloquea el hilo de eventos.
 *
 * <p>Resuelve la contradicción de un diálogo modal: la llamada que lo abre tiene que **no volver**
 * hasta que se cierre, pero si esa llamada vino del hilo de eventos y se queda esperando, la interfaz
 * entera se congela y el diálogo nunca se puede cerrar.
 *
 * <p>La salida es un segundo bucle de eventos anidado: {@link #enter} sigue atendiendo eventos
 * mientras espera, y {@link #exit} lo corta. Por eso el bucle es un objeto y no un método — hay que
 * poder terminarlo desde adentro del propio despacho.
 *
 * <p>Un bucle se usa **una sola vez**: {@link #enter} sobre uno que ya está corriendo devuelve
 * `false` en vez de anidarse.
 */
public interface SecondaryLoop {

    /**
     * Entra al bucle y no vuelve hasta que alguien llame a {@link #exit}.
     *
     * @return `true` si el bucle terminó normalmente, `false` si ya estaba corriendo
     */
    boolean enter();

    /**
     * Corta el bucle.
     *
     * @return `true` si había un bucle que cortar
     */
    boolean exit();
}
