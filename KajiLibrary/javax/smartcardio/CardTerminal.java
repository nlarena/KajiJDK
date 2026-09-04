package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.CardTerminal -- un lector.
 *
 * <p>Se consigue por {@link CardTerminals}. Lo que se hace con el es esperar a que aparezca una
 * tarjeta y conectarse.
 *
 * <h2>Los dos {@code waitFor}</h2>
 *
 * <p>{@link #waitForCardPresent} y {@link #waitForCardAbsent} bloquean hasta que cambie el estado o se
 * acabe el tiempo. Un tiempo de cero significa <b>esperar para siempre</b>, no "no esperar"; es la
 * convencion de {@code Object.wait} y equivocarse cuelga el programa.
 */
public abstract class CardTerminal {

    /** Para las subclases. */
    protected CardTerminal() {
    }

    /** Como se llama el lector. */
    public abstract String getName();

    /**
     * Se conecta a la tarjeta que este puesta.
     *
     * @param protocol {@code "T=0"}, {@code "T=1"} o {@code "*"} para negociar
     * @throws CardNotPresentException si no hay tarjeta
     * @throws CardException si no se pudo conectar
     */
    public abstract Card connect(String protocol) throws CardException;

    /**
     * Si hay una tarjeta puesta.
     *
     * @throws CardException si no se pudo averiguar
     */
    public abstract boolean isCardPresent() throws CardException;

    /**
     * Espera a que pongan una. Ver la nota de la clase.
     *
     * @param timeout milisegundos, o cero para esperar para siempre
     * @return si hay tarjeta al volver
     * @throws IllegalArgumentException si el tiempo es negativo
     * @throws CardException si no se pudo esperar
     */
    public abstract boolean waitForCardPresent(long timeout) throws CardException;

    /**
     * Espera a que la saquen. Ver la nota de la clase.
     *
     * @param timeout milisegundos, o cero para esperar para siempre
     * @return si no hay tarjeta al volver
     * @throws IllegalArgumentException si el tiempo es negativo
     * @throws CardException si no se pudo esperar
     */
    public abstract boolean waitForCardAbsent(long timeout) throws CardException;
}
