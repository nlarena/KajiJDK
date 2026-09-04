package javax.smartcardio;

/**
 * KajiLibrary's javax.smartcardio.Card -- una tarjeta conectada.
 *
 * <p>Se consigue con {@link CardTerminal#connect} y se suelta con {@link #disconnect}. Mientras dure,
 * las ordenes van por un {@link CardChannel}.
 *
 * <h2>El canal basico y los logicos</h2>
 *
 * <p>{@link #getBasicChannel} es el canal 0, el que existe siempre. {@link #openLogicalChannel} abre
 * otro, con su propio estado de seleccion de archivos, para que dos partes del programa puedan usar la
 * tarjeta sin pisarse. No todas las tarjetas los soportan.
 *
 * <h2>{@link #beginExclusive}</h2>
 *
 * <p>Bloquea el lector para este hilo. Va con {@link #endExclusive} en un {@code finally}: si no, el
 * lector queda tomado hasta que el proceso termine y ningun otro programa puede usar la tarjeta.
 */
public abstract class Card {

    /** Para las subclases. */
    protected Card() {
    }

    /** Lo que la tarjeta contesto al encenderse. */
    public abstract ATR getATR();

    /** Con que protocolo se negocio: {@code "T=0"} o {@code "T=1"}. */
    public abstract String getProtocol();

    /** El canal 0. Ver la nota de la clase. */
    public abstract CardChannel getBasicChannel();

    /**
     * Abre un canal logico.
     *
     * @throws CardException si la tarjeta no puede abrirlo
     */
    public abstract CardChannel openLogicalChannel() throws CardException;

    /**
     * Toma el lector para este hilo. Ver la nota de la clase.
     *
     * @throws CardException si no se pudo
     */
    public abstract void beginExclusive() throws CardException;

    /**
     * Lo suelta.
     *
     * @throws CardException si este hilo no lo tenia
     */
    public abstract void endExclusive() throws CardException;

    /**
     * Una orden para el <b>lector</b>, no para la tarjeta.
     *
     * @throws CardException si el lector la rechaza
     */
    public abstract byte[] transmitControlCommand(int controlCode, byte[] command)
        throws CardException;

    /**
     * Suelta la tarjeta.
     *
     * @param reset si ademas hay que reiniciarla
     * @throws CardException si no se pudo
     */
    public abstract void disconnect(boolean reset) throws CardException;
}
