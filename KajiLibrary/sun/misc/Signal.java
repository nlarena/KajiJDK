package sun.misc;

/**
 * Una senal del sistema operativo, para poder atenderla desde Java.
 *
 * <h2>Para que se usa</h2>
 *
 * <p>Casi siempre para lo mismo: enterarse de que alguien pidio terminar el proceso —{@code
 * SIGTERM} de un {@code kill}, {@code SIGINT} de un Ctrl-C— y cerrar ordenadamente. Tambien para
 * {@code SIGHUP}, que por convencion significa "reelee tu configuracion".
 *
 * <p>Es {@code sun.misc} y nunca fue API publica; lo que hay para esto en la API oficial es
 * {@code Runtime.addShutdownHook}, que cubre el caso comun y no permite distinguir que senal llego
 * ni ignorarla.
 *
 * <h2>Por que el numero no se puede inventar</h2>
 *
 * <p>El nombre de una senal es portable y su numero no. {@code SIGUSR1} es 10 en Linux sobre x86 y
 * 30 en macOS; en Windows la mayoria directamente no existe. El numero sale de preguntarle al
 * sistema operativo por el nombre, y es lo primero que hace el constructor del JDK.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>El constructor lanza {@link UnsupportedOperationException}. Es la misma decision que en
 * {@code com.sun.security.auth.module.UnixSystem}: cuando el dato solo lo tiene el sistema
 * operativo y esta VM no puede preguntarselo, inventarlo es peor que fallar.
 *
 * <p>Aca el dano seria concreto. Un numero de senal equivocado no da un error: instala el manejador
 * sobre <strong>otra</strong> senal. Un programa que cree estar atendiendo {@code SIGTERM} y en
 * realidad atiende {@code SIGSEGV} se comporta de forma inexplicable, y el sintoma no apunta ni de
 * cerca a la causa.
 *
 * @since 1.2
 */
public final class Signal {

    private static final String NO_HAY =
            "el numero de una senal se lo asigna el sistema operativo al nombre, y esta VM no puede "
            + "preguntarselo; un numero inventado instalaria el manejador sobre otra senal";

    /**
     * La senal con ese nombre, sin el prefijo {@code SIG}.
     *
     * @param name el nombre, por ejemplo {@code "TERM"} o {@code "INT"}
     * @throws IllegalArgumentException si el sistema operativo no conoce esa senal
     * @throws UnsupportedOperationException en esta VM siempre; ver la nota de la clase
     */
    public Signal(String name) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * El numero que el sistema operativo le da a esta senal.
     *
     * @return el numero
     */
    public int getNumber() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * El nombre de la senal, sin el prefijo {@code SIG}.
     *
     * @return el nombre
     */
    public String getName() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Dos senales son iguales si tienen el mismo nombre y el mismo numero.
     *
     * @param other la otra
     * @return si son la misma senal
     */
    public boolean equals(Object other) {
        return this == other;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return System.identityHashCode(this);
    }

    /** {@inheritDoc} */
    public String toString() {
        return "Signal";
    }

    /**
     * Instala un manejador para esa senal y devuelve el que estaba.
     *
     * <p>Devolver el anterior no es un detalle: es lo que permite encadenar. Un manejador educado
     * hace lo suyo y despues llama al que habia, porque puede haber sido la VM la que lo instalo
     * —para {@code SIGQUIT}, por ejemplo, que es lo que produce el volcado de hilos—.
     *
     * @param sig la senal
     * @param handler el manejador, o {@link SignalHandler#SIG_DFL} / {@link SignalHandler#SIG_IGN}
     * @return el manejador que estaba
     * @throws IllegalArgumentException si la senal no se puede atender
     * @throws UnsupportedOperationException en esta VM siempre
     */
    public static synchronized SignalHandler handle(Signal sig, SignalHandler handler)
            throws IllegalArgumentException {
        throw new UnsupportedOperationException(
                "instalar un manejador de senales necesita que la VM se registre ante el sistema "
                + "operativo, y esta VM no lo hace");
    }

    /**
     * Le manda esa senal al propio proceso.
     *
     * @param sig la senal
     * @throws IllegalArgumentException si la senal no se puede levantar
     * @throws UnsupportedOperationException en esta VM siempre
     */
    public static void raise(Signal sig) throws IllegalArgumentException {
        throw new UnsupportedOperationException(
                "levantar una senal necesita el sistema operativo, y esta VM no lo alcanza");
    }
}
