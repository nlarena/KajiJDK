package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.SysexMessage -- un mensaje exclusivo del fabricante.
 *
 * <p>La valvula de escape del estandar: lo que MIDI no define, cada fabricante lo manda por aca.
 * Cargar un sonido en un sintetizador, volcar su configuracion, actualizar su firmware.
 *
 * <p>Arranca con {@link #SYSTEM_EXCLUSIVE} (0xF0), sigue con el identificador del fabricante y lo que
 * el quiera, y termina con 0xF7. Es el unico mensaje de largo arbitrario.
 *
 * <h2>Las dos constantes de estado</h2>
 *
 * <p>{@link #SPECIAL_SYSTEM_EXCLUSIVE} vale 0xF7 y no es "el final": es el estado de una
 * <b>continuacion</b>. Un exclusivo muy largo puede mandarse partido, y los pedazos que no son el
 * primero llevan ese estado.
 *
 * <p>Es la parte que se malinterpreta: recibir un mensaje con estado 0xF7 no significa que termino
 * uno, significa que llego un pedazo del medio o del final.
 *
 * <p>{@link #getData} devuelve todo menos el byte de estado, asi que el 0xF7 de cierre <b>si</b> entra.
 */
public class SysexMessage extends MidiMessage {

    /** El comienzo de un exclusivo. */
    public static final int SYSTEM_EXCLUSIVE = 0xF0;

    /** La continuacion de uno partido. Ver la nota de la clase. */
    public static final int SPECIAL_SYSTEM_EXCLUSIVE = 0xF7;

    /** Un exclusivo vacio: {@code 0xF0 0xF7}. */
    public SysexMessage() {
        this(new byte[] { (byte) SYSTEM_EXCLUSIVE, (byte) SPECIAL_SYSTEM_EXCLUSIVE });
    }

    /**
     * Un exclusivo con esos bytes, incluido el de estado.
     *
     * @throws InvalidMidiDataException si el primer byte no es 0xF0 ni 0xF7
     */
    public SysexMessage(byte[] data, int length) throws InvalidMidiDataException {
        super(null);
        setMessage(data, length);
    }

    /**
     * Idem, con el estado aparte.
     *
     * @throws InvalidMidiDataException si el estado no es 0xF0 ni 0xF7
     */
    public SysexMessage(int status, byte[] data, int length) throws InvalidMidiDataException {
        super(null);
        setMessage(status, data, length);
    }

    /** Para las subclases y los lectores. */
    protected SysexMessage(byte[] data) {
        super(data);
    }

    /**
     * Reemplaza los bytes; el primero tiene que ser el estado.
     *
     * @throws InvalidMidiDataException si el primer byte no es 0xF0 ni 0xF7
     */
    @Override
    public void setMessage(byte[] data, int length) throws InvalidMidiDataException {
        int status = 0;
        if (data != null && length > 0) {
            status = data[0] & 0xFF;
        }
        if (status != SYSTEM_EXCLUSIVE && status != SPECIAL_SYSTEM_EXCLUSIVE) {
            throw new InvalidMidiDataException("Invalid status byte for sysex message: 0x"
                + Integer.toHexString(status));
        }
        super.setMessage(data, length);
    }

    /**
     * Reemplaza los bytes, con el estado aparte.
     *
     * @throws InvalidMidiDataException si el estado no es 0xF0 ni 0xF7
     */
    public void setMessage(int status, byte[] data, int length) throws InvalidMidiDataException {
        if (status != SYSTEM_EXCLUSIVE && status != SPECIAL_SYSTEM_EXCLUSIVE) {
            throw new InvalidMidiDataException("Invalid status byte for sysex message: 0x"
                + Integer.toHexString(status));
        }
        if (length < 0 || (length > 0 && length > data.length)) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }
        this.length = length + 1;
        this.data = new byte[this.length];
        this.data[0] = (byte) (status & 0xFF);
        if (length > 0) {
            System.arraycopy(data, 0, this.data, 1, length);
        }
    }

    /** Una copia de todo menos el byte de estado. Ver la nota de la clase. */
    public byte[] getData() {
        int n = this.length - 1;
        if (n < 0) {
            n = 0;
        }
        byte[] copy = new byte[n];
        System.arraycopy(this.data, 1, copy, 0, n);
        return copy;
    }

    /** Una copia independiente. */
    @Override
    public Object clone() {
        byte[] copy = new byte[this.length];
        System.arraycopy(this.data, 0, copy, 0, this.length);
        return new SysexMessage(copy);
    }
}
