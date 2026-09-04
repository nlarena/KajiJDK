package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.InvalidMidiDataException -- esos bytes no son MIDI valido.
 *
 * <p>La lanzan los constructores y los {@code setMessage} de los mensajes, y los lectores de archivo.
 *
 * <p>Es comprobada, y por eso armar un mensaje MIDI obliga a un {@code try}. Es incomodo y esta bien
 * asi: un byte de estado invalido silenciosamente aceptado se convierte en un dispositivo que se
 * cuelga o en un archivo que nadie mas puede leer.
 */
public class InvalidMidiDataException extends Exception {

    private static final long serialVersionUID = 2780771756789932067L;

    /** Sin detalle. */
    public InvalidMidiDataException() {
        super();
    }

    /** Con mensaje. */
    public InvalidMidiDataException(String message) {
        super(message);
    }
}
