package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.ShortMessage -- un mensaje MIDI de uno a tres bytes.
 *
 * <p>Casi todo el MIDI es esto: notas, controladores, cambio de programa, rueda de tono, y los
 * mensajes de reloj.
 *
 * <h2>Comando y canal</h2>
 *
 * <p>El byte de estado lleva las dos cosas: los cuatro bits altos son el <b>comando</b> y los cuatro
 * bajos el <b>canal</b>. Por eso hay dieciseis canales y no diecisiete.
 *
 * <p>La trampa: en los mensajes de sistema --0xF0 para arriba-- esa division no existe, el byte
 * entero es el comando. {@link #getChannel} igual devuelve los cuatro bits bajos, y ahi el numero no
 * significa nada. Hay que mirar el comando primero.
 *
 * <h2>Una nota se apaga con volumen cero</h2>
 *
 * <p>{@link #NOTE_ON} con {@code data2} en 0 equivale a {@link #NOTE_OFF}. Es una optimizacion del
 * estandar --permite mandar una tira de notas sin repetir el byte de estado-- y todo lo que procese
 * MIDI tiene que contemplarla. Tratar solo {@code NOTE_OFF} deja notas colgadas sonando para siempre.
 */
public class ShortMessage extends MidiMessage {

    /** Cuadro de codigo de tiempo. */
    public static final int MIDI_TIME_CODE = 0xF1;

    /** Posicion en la cancion. */
    public static final int SONG_POSITION_POINTER = 0xF2;

    /** Elegir cancion. */
    public static final int SONG_SELECT = 0xF3;

    /** Pedido de afinacion. */
    public static final int TUNE_REQUEST = 0xF6;

    /** Fin de un mensaje exclusivo. */
    public static final int END_OF_EXCLUSIVE = 0xF7;

    /** Pulso de reloj; van veinticuatro por negra. */
    public static final int TIMING_CLOCK = 0xF8;

    /** Empezar a reproducir. */
    public static final int START = 0xFA;

    /** Seguir desde donde se paro. */
    public static final int CONTINUE = 0xFB;

    /** Parar. */
    public static final int STOP = 0xFC;

    /** Sensor de actividad; dice que el cable sigue vivo. */
    public static final int ACTIVE_SENSING = 0xFE;

    /** Reset del sistema. */
    public static final int SYSTEM_RESET = 0xFF;

    /** Soltar una nota. */
    public static final int NOTE_OFF = 0x80;

    /** Tocar una nota. Ver la nota de la clase sobre el volumen cero. */
    public static final int NOTE_ON = 0x90;

    /** Presion sobre una tecla ya pulsada. */
    public static final int POLY_PRESSURE = 0xA0;

    /** Mover un controlador. */
    public static final int CONTROL_CHANGE = 0xB0;

    /** Cambiar de sonido. */
    public static final int PROGRAM_CHANGE = 0xC0;

    /** Presion sobre el canal entero. */
    public static final int CHANNEL_PRESSURE = 0xD0;

    /** Rueda de tono. */
    public static final int PITCH_BEND = 0xE0;

    /**
     * Una nota central a maximo volumen.
     *
     * <p>Los bytes son {@code 0x90 0x40 0x7F}. Es un valor arbitrario que el JDK eligio para que un
     * mensaje recien construido sea valido y no vacio.
     */
    public ShortMessage() {
        this(new byte[3]);
        try {
            setMessage(NOTE_ON, 64, 127);
        } catch (InvalidMidiDataException e) {
            // Los valores son constantes y validos; no puede pasar.
        }
    }

    /**
     * Un mensaje de sistema sin datos.
     *
     * @throws InvalidMidiDataException si ese estado lleva datos
     */
    public ShortMessage(int status) throws InvalidMidiDataException {
        this(new byte[3]);
        setMessage(status);
    }

    /**
     * Un mensaje de canal con dos datos.
     *
     * @throws InvalidMidiDataException si algo esta fuera de rango
     */
    public ShortMessage(int status, int data1, int data2) throws InvalidMidiDataException {
        this(new byte[3]);
        setMessage(status, data1, data2);
    }

    /**
     * Idem, con el canal aparte.
     *
     * @param command el comando, sin el canal
     * @throws InvalidMidiDataException si algo esta fuera de rango
     */
    public ShortMessage(int command, int channel, int data1, int data2)
        throws InvalidMidiDataException {
        this(new byte[3]);
        setMessage(command, channel, data1, data2);
    }

    /** Para las subclases y los lectores. */
    protected ShortMessage(byte[] data) {
        super(data);
    }

    /**
     * Fija un mensaje sin datos.
     *
     * @throws InvalidMidiDataException si ese estado lleva datos
     */
    public void setMessage(int status) throws InvalidMidiDataException {
        int dataLength = getDataLength(status);
        if (dataLength != 0) {
            throw new InvalidMidiDataException("Status byte; " + status + " requires "
                + dataLength + " data bytes");
        }
        setMessage(status, 0, 0);
    }

    /**
     * Fija un mensaje con hasta dos datos.
     *
     * <p>Los datos que ese estado no use se ignoran, pero igual se validan.
     *
     * @throws InvalidMidiDataException si el estado o los datos estan fuera de rango
     */
    public void setMessage(int status, int data1, int data2) throws InvalidMidiDataException {
        int dataLength = getDataLength(status);
        if (dataLength > 0) {
            if (data1 < 0 || data1 > 127) {
                throw new InvalidMidiDataException("data1 out of range: " + data1);
            }
            if (dataLength > 1 && (data2 < 0 || data2 > 127)) {
                throw new InvalidMidiDataException("data2 out of range: " + data2);
            }
        }
        this.length = dataLength + 1;
        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[3];
        }
        this.data[0] = (byte) (status & 0xFF);
        if (this.length > 1) {
            this.data[1] = (byte) (data1 & 0xFF);
            if (this.length > 2) {
                this.data[2] = (byte) (data2 & 0xFF);
            }
        }
    }

    /**
     * Fija un mensaje de canal, con el comando y el canal por separado.
     *
     * @throws InvalidMidiDataException si el comando, el canal o los datos estan fuera de rango
     */
    public void setMessage(int command, int channel, int data1, int data2)
        throws InvalidMidiDataException {
        if (command >= 0xF0 || command < 0x80) {
            throw new InvalidMidiDataException("command out of range: 0x"
                + Integer.toHexString(command));
        }
        if ((channel & 0xFFFFFFF0) != 0) {
            throw new InvalidMidiDataException("channel out of range: " + channel);
        }
        setMessage((command & 0xF0) | (channel & 0x0F), data1, data2);
    }

    /**
     * Los cuatro bits bajos del estado.
     *
     * <p>Solo significa algo en los mensajes de canal; ver la nota de la clase.
     */
    public int getChannel() {
        return getStatus() & 0x0F;
    }

    /** Los cuatro bits altos del estado. */
    public int getCommand() {
        return getStatus() & 0xF0;
    }

    /** El primer byte de datos, o 0 si no hay. */
    public int getData1() {
        if (this.length > 1) {
            return this.data[1] & 0xFF;
        }
        return 0;
    }

    /** El segundo byte de datos, o 0 si no hay. */
    public int getData2() {
        if (this.length > 2) {
            return this.data[2] & 0xFF;
        }
        return 0;
    }

    /** Una copia independiente. */
    @Override
    public Object clone() {
        byte[] copy = new byte[this.length];
        System.arraycopy(this.data, 0, copy, 0, this.length);
        ShortMessage msg = new ShortMessage(copy);
        return msg;
    }

    /**
     * Cuantos bytes de datos lleva ese estado.
     *
     * <p>Los mensajes de canal siguen el patron de siempre: dos datos, salvo cambio de programa y
     * presion de canal, que llevan uno. Los de sistema no tienen patron y hay que saberlos de memoria.
     *
     * <p>{@code 0xF0} --el comienzo de un exclusivo-- lanza excepcion: no tiene un largo fijo, y por
     * eso no es un {@code ShortMessage}.
     *
     * @throws InvalidMidiDataException si ese byte no es un estado valido para esta clase
     */
    protected final int getDataLength(int status) throws InvalidMidiDataException {
        int cmd = status & 0xF0;
        if (cmd == NOTE_OFF || cmd == NOTE_ON || cmd == POLY_PRESSURE
            || cmd == CONTROL_CHANGE || cmd == PITCH_BEND) {
            return 2;
        }
        if (cmd == PROGRAM_CHANGE || cmd == CHANNEL_PRESSURE) {
            return 1;
        }
        if (cmd == 0xF0) {
            if (status == MIDI_TIME_CODE || status == SONG_SELECT) {
                return 1;
            }
            if (status == SONG_POSITION_POINTER) {
                return 2;
            }
            if (status == 0xF0 || status == 0xF4 || status == 0xF5) {
                throw new InvalidMidiDataException("Invalid status byte: " + status);
            }
            return 0;
        }
        throw new InvalidMidiDataException("Invalid status byte: " + status);
    }
}
