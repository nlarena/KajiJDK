package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiMessage -- un mensaje MIDI crudo.
 *
 * <p>Guarda los bytes tal como viajan por el cable y nada mas. Las tres subclases interpretan esos
 * bytes segun las tres familias del estandar:
 *
 * <ul>
 *   <li>{@link ShortMessage}: uno, dos o tres bytes. Las notas, los controladores, el reloj. Es el
 *       noventa y nueve por ciento del trafico;
 *   <li>{@link SysexMessage}: exclusivo del fabricante, de largo arbitrario;
 *   <li>{@link MetaMessage}: no existe en el cable. Solo aparece en archivos, y lleva el tempo, la
 *       armadura, el nombre de la pista.
 * </ul>
 *
 * <p>Esa ultima distincion importa y se olvida: mandarle un {@code MetaMessage} a un dispositivo real
 * no tiene sentido, porque el 0xFF que lo encabeza significa "reset del sistema" en el cable.
 *
 * <h2>El byte de estado</h2>
 *
 * <p>El primero, y siempre tiene el bit alto en uno. Los bytes de datos nunca lo tienen, y por eso un
 * receptor puede resincronizarse en medio de un flujo: encuentra el proximo byte mayor o igual a 0x80
 * y sabe que ahi arranca un mensaje.
 *
 * <p>{@link #getStatus} lo devuelve <b>sin signo</b>, de 0 a 255. Es lo que hay que usar: leer
 * {@code getMessage()[0]} da un {@code byte} negativo y comparar eso contra {@code 0x90} falla.
 */
public abstract class MidiMessage implements Cloneable {

    /** Los bytes crudos; puede ser mas largo que {@link #length}. */
    protected byte[] data;

    /** Cuantos de esos bytes valen. */
    protected int length = 0;

    /** Para las subclases. */
    protected MidiMessage(byte[] data) {
        this.data = data;
        if (data == null) {
            this.length = 0;
        } else {
            this.length = data.length;
        }
    }

    /**
     * Reemplaza los bytes.
     *
     * @throws InvalidMidiDataException si el largo es negativo o mayor que el arreglo
     */
    protected void setMessage(byte[] data, int length) throws InvalidMidiDataException {
        if (length < 0 || (length > 0 && length > data.length)) {
            throw new InvalidMidiDataException("length out of bounds: " + length);
        }
        this.length = length;
        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[this.length];
        }
        System.arraycopy(data, 0, this.data, 0, length);
    }

    /** Una copia de los bytes que valen. */
    public byte[] getMessage() {
        byte[] copy = new byte[this.length];
        if (this.data != null) {
            System.arraycopy(this.data, 0, copy, 0, this.length);
        }
        return copy;
    }

    /** El byte de estado, de 0 a 255. Ver la nota de la clase. */
    public int getStatus() {
        if (this.length > 0) {
            return this.data[0] & 0xFF;
        }
        return 0;
    }

    /** Cuantos bytes tiene. */
    public int getLength() {
        return this.length;
    }

    /** Una copia independiente, con su propio arreglo. */
    @Override
    public abstract Object clone();
}
