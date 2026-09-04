package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MetaMessage -- un mensaje que solo existe en archivos.
 *
 * <p>El tempo, la armadura, el compas, el nombre de la pista, la letra. Nada de esto viaja por un
 * cable MIDI: son anotaciones del archivo.
 *
 * <p>El byte de estado es {@link #META}, que vale 0xFF -- el mismo que "reset del sistema" en el
 * cable. Por eso mandarle uno de estos a un dispositivo real no solo no sirve: lo resetea.
 *
 * <h2>El largo va codificado</h2>
 *
 * <p>Los bytes son {@code 0xFF}, el tipo, el <b>largo de los datos en cantidad de largo variable</b>,
 * y los datos. Esa codificacion --siete bits por byte, el bit alto indica que sigue-- es la que usa
 * todo el formato de archivo MIDI, y es la razon de que un archivo MIDI de tres minutos ocupe unos
 * pocos kilobytes.
 *
 * <p>{@link #getData} devuelve solo los datos, sin el encabezado. {@link #getLength} devuelve el
 * total, encabezado incluido, asi que los dos no coinciden y no tienen por que.
 *
 * <h2>El tipo 0x2F es el fin de pista</h2>
 *
 * <p>Es obligatorio y va al final de cada pista. {@link Track} lo mantiene solo; no hay que agregarlo
 * a mano.
 */
public class MetaMessage extends MidiMessage {

    /** El byte de estado de todos los meta mensajes. */
    public static final int META = 0xFF;

    /** Cuantos bytes ocupa el encabezado: 0xFF, el tipo, y el largo variable. */
    private int dataLength = 0;

    /** Un meta mensaje de tipo 0 sin datos. */
    public MetaMessage() {
        this(new byte[] { (byte) META, 0 });
    }

    /**
     * Un meta mensaje con tipo y datos.
     *
     * @param type de 0 a 127
     * @throws InvalidMidiDataException si el tipo esta fuera de rango
     */
    public MetaMessage(int type, byte[] data, int length) throws InvalidMidiDataException {
        super(null);
        setMessage(type, data, length);
    }

    /**
     * Para las subclases y los lectores.
     *
     * <p>Lee el largo variable del encabezado para saber donde empiezan los datos; no alcanza con
     * restar tres, porque el largo puede ocupar mas de un byte.
     */
    protected MetaMessage(byte[] data) {
        super(data);
        this.dataLength = 0;
        if (data != null && data.length >= 3) {
            int at = 2;
            while (at < data.length && (data[at] & 0x80) != 0) {
                at = at + 1;
            }
            this.dataLength = data.length - (at + 1);
            if (this.dataLength < 0) {
                this.dataLength = 0;
            }
        }
    }

    /**
     * Reemplaza tipo y datos.
     *
     * @param type de 0 a 127
     * @param length cuantos bytes de {@code data} usar
     * @throws InvalidMidiDataException si el tipo esta fuera de rango o el largo no cierra
     */
    public void setMessage(int type, byte[] data, int length) throws InvalidMidiDataException {
        if (type >= 128 || type < 0) {
            throw new InvalidMidiDataException("Invalid meta event with type " + type);
        }
        if (length > 0 && (data == null || length > data.length)) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }
        if (length < 0) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }
        byte[] lengthBytes = variableLength(length);
        this.dataLength = length;
        this.length = 2 + lengthBytes.length + length;
        this.data = new byte[this.length];
        this.data[0] = (byte) META;
        this.data[1] = (byte) type;
        System.arraycopy(lengthBytes, 0, this.data, 2, lengthBytes.length);
        if (length > 0) {
            System.arraycopy(data, 0, this.data, 2 + lengthBytes.length, length);
        }
    }

    /** El tipo, de 0 a 127. */
    public int getType() {
        if (this.length >= 2) {
            return this.data[1] & 0xFF;
        }
        return 0;
    }

    /** Una copia de los datos, sin el encabezado. Ver la nota de la clase. */
    public byte[] getData() {
        byte[] copy = new byte[this.dataLength];
        System.arraycopy(this.data, this.length - this.dataLength, copy, 0, this.dataLength);
        return copy;
    }

    /** Una copia independiente. */
    @Override
    public Object clone() {
        byte[] copy = new byte[this.length];
        System.arraycopy(this.data, 0, copy, 0, this.length);
        return new MetaMessage(copy);
    }

    /**
     * Codifica un numero en cantidad de largo variable.
     *
     * <p>Siete bits por byte, del mas significativo al menos; todos menos el ultimo llevan el bit alto
     * en uno. Ver la nota de la clase.
     */
    private static byte[] variableLength(int value) {
        int bytes = 1;
        int probe = value >> 7;
        while (probe > 0) {
            bytes = bytes + 1;
            probe = probe >> 7;
        }
        byte[] out = new byte[bytes];
        int i = bytes - 1;
        out[i] = (byte) (value & 0x7F);
        int rest = value >> 7;
        while (i > 0) {
            i = i - 1;
            out[i] = (byte) ((rest & 0x7F) | 0x80);
            rest = rest >> 7;
        }
        return out;
    }
}
