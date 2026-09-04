package javax.smartcardio;

import java.nio.ByteBuffer;

/**
 * KajiLibrary's javax.smartcardio.CardChannel -- por donde van las ordenes.
 *
 * <p>El canal 0 es el basico y no se cierra; los logicos se abren con
 * {@link Card#openLogicalChannel} y se cierran con {@link #close}.
 *
 * <p>Las dos formas de {@link #transmit} hacen lo mismo: una con objetos, otra con buffers para
 * quien quiera evitar la copia.
 */
public abstract class CardChannel {

    /** Para las subclases. */
    protected CardChannel() {
    }

    /** De que tarjeta es. */
    public abstract Card getCard();

    /** Que numero de canal es; 0 es el basico. */
    public abstract int getChannelNumber();

    /**
     * Manda la orden y espera la respuesta.
     *
     * @throws CardException si la tarjeta no contesta
     */
    public abstract ResponseAPDU transmit(CommandAPDU command) throws CardException;

    /**
     * Lo mismo, con buffers.
     *
     * @return cuantos bytes se escribieron en el buffer de respuesta
     * @throws CardException si la tarjeta no contesta
     */
    public abstract int transmit(ByteBuffer command, ByteBuffer response) throws CardException;

    /**
     * Cierra el canal.
     *
     * @throws CardException si es el basico, que no se cierra, o si fallo
     */
    public abstract void close() throws CardException;
}
