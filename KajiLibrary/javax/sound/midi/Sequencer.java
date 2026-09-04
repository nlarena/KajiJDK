package javax.sound.midi;

import java.io.IOException;
import java.io.InputStream;

/**
 * KajiLibrary's javax.sound.midi.Sequencer -- reproduce y graba secuencias MIDI.
 *
 * <p>Un {@link MidiDevice} que sabe recorrer una {@link Sequence} en el tiempo y mandar sus eventos a
 * quien tenga conectado.
 *
 * <h2>No hace ruido por si mismo</h2>
 *
 * <p>Es lo primero que confunde. Un secuenciador <b>manda mensajes</b>; para oir algo hay que conectar
 * su transmisor al receptor de un {@link Synthesizer}. El secuenciador que devuelve
 * {@code MidiSystem.getSequencer()} viene conectado al sintetizador por omision, y por eso parece que
 * suena solo.
 *
 * <h2>{@link #start} no bloquea</h2>
 *
 * <p>Vuelve enseguida y la reproduccion sigue en otro hilo. Para saber cuando termino hay que
 * registrar un {@link MetaEventListener} y esperar el meta evento 0x2F. Dormir un rato es lo que hace
 * casi todo el mundo y siempre queda mal.
 *
 * <h2>Las tres formas de cambiar la velocidad</h2>
 *
 * <ul>
 *   <li>{@link #setTempoInBPM} y {@link #setTempoInMPQ} son la misma cosa en dos unidades: negras por
 *       minuto, o microsegundos por negra. Son inversas;
 *   <li>{@link #setTempoFactor} es <b>un multiplicador</b> sobre lo que la obra pida. Se mantiene
 *       aunque la obra tenga cambios de tempo escritos.
 * </ul>
 *
 * <p>La diferencia importa: fijar el tempo se pierde en cuanto la obra llegue a su proximo cambio de
 * tempo; el factor no. Para "reproducir a la mitad de velocidad" se quiere el factor.
 *
 * <p>Nada de esto tiene efecto en una secuencia SMPTE; ver {@link Sequence}.
 *
 * <h2>La repeticion</h2>
 *
 * <p>{@link #setLoopCount} con {@link #LOOP_CONTINUOUSLY} repite para siempre el tramo entre
 * {@link #setLoopStartPoint} y {@link #setLoopEndPoint}. El punto final -1 significa el final de la
 * obra.
 */
public interface Sequencer extends MidiDevice {

    /** Repetir para siempre. */
    int LOOP_CONTINUOUSLY = -1;

    /**
     * Que reproducir.
     *
     * @throws InvalidMidiDataException si no soporta esa secuencia
     */
    void setSequence(Sequence sequence) throws InvalidMidiDataException;

    /**
     * Idem, leyendola de un flujo.
     *
     * @throws IOException si no se pudo leer
     * @throws InvalidMidiDataException si no es un archivo MIDI valido
     */
    void setSequence(InputStream stream) throws IOException, InvalidMidiDataException;

    /** Que esta cargado, o null. */
    Sequence getSequence();

    /** Arranca. No bloquea; ver la nota de la clase. */
    void start();

    /** Para, sin volver al principio. */
    void stop();

    /** Si esta reproduciendo. */
    boolean isRunning();

    /** Empieza a grabar en las pistas habilitadas. */
    void startRecording();

    /** Deja de grabar; sigue reproduciendo. */
    void stopRecording();

    /** Si esta grabando. */
    boolean isRecording();

    /**
     * Habilita una pista para grabar.
     *
     * @param channel que canal grabar ahi; -1 son todos
     */
    void recordEnable(Track track, int channel);

    /** La deshabilita. */
    void recordDisable(Track track);

    /** El tempo, en negras por minuto. Ver la nota de la clase. */
    float getTempoInBPM();

    /** Lo cambia. Se pierde en el proximo cambio de tempo de la obra. */
    void setTempoInBPM(float bpm);

    /** El tempo, en microsegundos por negra. */
    float getTempoInMPQ();

    /** Lo cambia, en las otras unidades. */
    void setTempoInMPQ(float mpq);

    /** Un multiplicador sobre lo que la obra pida. Ver la nota de la clase. */
    void setTempoFactor(float factor);

    /** Cuanto es ese multiplicador. */
    float getTempoFactor();

    /** Cuanto dura la obra, en pulsos. */
    long getTickLength();

    /** En que pulso va. */
    long getTickPosition();

    /** Salta a ese pulso. */
    void setTickPosition(long tick);

    /** Cuanto dura, en microsegundos. */
    long getMicrosecondLength();

    /** En que microsegundo va. */
    long getMicrosecondPosition();

    /** Salta a ese microsegundo. */
    void setMicrosecondPosition(long microseconds);

    /**
     * De donde toma el tiempo.
     *
     * @throws IllegalArgumentException si no soporta ese modo
     */
    void setMasterSyncMode(SyncMode sync);

    /** De donde lo toma. */
    SyncMode getMasterSyncMode();

    /** Los modos que soporta como maestro. */
    SyncMode[] getMasterSyncModes();

    /**
     * Que manda para que otros lo sigan.
     *
     * @throws IllegalArgumentException si no soporta ese modo
     */
    void setSlaveSyncMode(SyncMode sync);

    /** Que manda. */
    SyncMode getSlaveSyncMode();

    /** Los modos que soporta como esclavo. */
    SyncMode[] getSlaveSyncModes();

    /** Silencia una pista. */
    void setTrackMute(int track, boolean mute);

    /** Si esta silenciada; false tambien si no lo soporta. */
    boolean getTrackMute(int track);

    /** Deja sonar solo esa pista. */
    void setTrackSolo(int track, boolean solo);

    /** Si esta en solo; false tambien si no lo soporta. */
    boolean getTrackSolo(int track);

    /**
     * Registra un escucha de meta eventos.
     *
     * @return si se pudo
     */
    boolean addMetaEventListener(MetaEventListener listener);

    /** Lo da de baja. */
    void removeMetaEventListener(MetaEventListener listener);

    /**
     * Registra un escucha de esos controladores.
     *
     * @return los que quedaron registrados de verdad; ver {@link ControllerEventListener}
     */
    int[] addControllerEventListener(ControllerEventListener listener, int[] controllers);

    /**
     * Da de baja esos controladores de ese escucha.
     *
     * @param controllers null los saca todos
     * @return los que le quedaron
     */
    int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers);

    /** Donde empieza el tramo que se repite. */
    void setLoopStartPoint(long tick);

    /** Donde empieza. */
    long getLoopStartPoint();

    /** Donde termina; -1 es el final de la obra. */
    void setLoopEndPoint(long tick);

    /** Donde termina. */
    long getLoopEndPoint();

    /** Cuantas veces repetir, o {@link #LOOP_CONTINUOUSLY}. */
    void setLoopCount(int count);

    /** Cuantas veces. */
    int getLoopCount();

    /**
     * De donde sale el tiempo de un secuenciador.
     *
     * <p>Se usa en los dos sentidos y por eso hay dos juegos de metodos: como <b>maestro</b> --de donde
     * este secuenciador toma el tiempo-- y como <b>esclavo</b> --que manda para que otros lo sigan--.
     *
     * <p>{@link #NO_SYNC} como esclavo significa que no manda nada, no que no funciona.
     *
     * <p>No es un enum, por la misma razon que el resto de estas APIs: son de 1999. La igualdad es por
     * identidad.
     */
    class SyncMode {

        /** Su propio reloj. Es lo normal. */
        public static final SyncMode INTERNAL_CLOCK = new SyncMode("Internal Clock");

        /** Los pulsos de reloj MIDI que llegan de afuera. */
        public static final SyncMode MIDI_SYNC = new SyncMode("MIDI Sync");

        /** Codigo de tiempo MIDI, que ademas lleva posicion absoluta. */
        public static final SyncMode MIDI_TIME_CODE = new SyncMode("MIDI Time Code");

        /** Nada. Ver la nota de la clase. */
        public static final SyncMode NO_SYNC = new SyncMode("No Timing");

        /** El nombre, para mostrar. */
        private final String name;

        /** Protegido: los modos los define la plataforma. */
        protected SyncMode(String name) {
            this.name = name;
        }

        /** Por identidad. Ver la nota de la clase. */
        @Override
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /** El de identidad. */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        /** El nombre. */
        @Override
        public final String toString() {
            return this.name;
        }
    }
}
