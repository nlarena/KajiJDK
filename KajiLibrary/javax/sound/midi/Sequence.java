package javax.sound.midi;

import java.util.Vector;

/**
 * KajiLibrary's javax.sound.midi.Sequence -- una obra MIDI completa, con sus pistas.
 *
 * <p>Es lo que se carga de un archivo y lo que se le da a un {@link Sequencer} para que suene.
 *
 * <h2>Las dos formas de contar el tiempo</h2>
 *
 * <p>Es lo unico que hay que entender de esta clase, y decide todo lo demas:
 *
 * <ul>
 *   <li>{@link #PPQ}: los pulsos son <b>musicales</b>. La resolucion son pulsos por negra, y cuanto
 *       dura un pulso depende del tempo actual. Cambiar el tempo acelera la obra sin tocar un solo
 *       evento;
 *   <li>{@code SMPTE_*}: los pulsos son <b>de reloj</b>. La division es cuadros por segundo y la
 *       resolucion pulsos por cuadro, asi que un pulso dura siempre lo mismo. El tempo no lo afecta.
 * </ul>
 *
 * <p>Para musica se usa PPQ; para sincronizar con video o pelicula, SMPTE. Elegir mal se descubre
 * tarde: una obra en SMPTE ignora los cambios de tempo que uno le escriba.
 *
 * <p>{@link #SMPTE_30DROP} vale 29.97 y no 30. Es la tasa real de la television en color de Norteamerica,
 * y esos 0.03 de diferencia son la razon de que exista el codigo de tiempo con salto de cuadro.
 *
 * <h2>{@link #getPatchList}</h2>
 *
 * <p>Devuelve un arreglo vacio. No es una omision de esta biblioteca: el JDK tampoco lo implementa
 * --se comprobo contra el JDK 25-- y su documentacion ya avisa que no esta terminado.
 */
public class Sequence {

    /** Pulsos por negra: tiempo musical. Ver la nota de la clase. */
    public static final float PPQ = 0.0f;

    /** Veinticuatro cuadros por segundo, el del cine. */
    public static final float SMPTE_24 = 24.0f;

    /** Veinticinco, el de la television europea. */
    public static final float SMPTE_25 = 25.0f;

    /** 29.97, con salto de cuadro. Ver la nota de la clase. */
    public static final float SMPTE_30DROP = 29.97f;

    /** Treinta justos. */
    public static final float SMPTE_30 = 30.0f;

    /** Cual de las cinco. */
    protected float divisionType;

    /** Pulsos por negra, o pulsos por cuadro. */
    protected int resolution;

    /** Las pistas. */
    protected Vector<Track> tracks = new Vector<Track>();

    /**
     * Una secuencia vacia.
     *
     * @throws InvalidMidiDataException si la division no es una de las cinco
     */
    public Sequence(float divisionType, int resolution) throws InvalidMidiDataException {
        this(divisionType, resolution, 0);
    }

    /**
     * Idem, con esa cantidad de pistas vacias.
     *
     * @throws InvalidMidiDataException si la division no es una de las cinco
     */
    public Sequence(float divisionType, int resolution, int numTracks)
        throws InvalidMidiDataException {
        if (divisionType != PPQ && divisionType != SMPTE_24 && divisionType != SMPTE_25
            && divisionType != SMPTE_30DROP && divisionType != SMPTE_30) {
            throw new InvalidMidiDataException("Unsupported division type: " + divisionType);
        }
        this.divisionType = divisionType;
        this.resolution = resolution;
        int i = 0;
        while (i < numTracks) {
            this.tracks.addElement(new Track());
            i = i + 1;
        }
    }

    /** Cual de las cinco. Ver la nota de la clase. */
    public float getDivisionType() {
        return this.divisionType;
    }

    /** Pulsos por negra, o pulsos por cuadro. */
    public int getResolution() {
        return this.resolution;
    }

    /** Una pista nueva, vacia, ya agregada. */
    public Track createTrack() {
        synchronized (this.tracks) {
            Track track = new Track();
            this.tracks.addElement(track);
            return track;
        }
    }

    /**
     * Saca esa pista.
     *
     * @return si estaba
     */
    public boolean deleteTrack(Track track) {
        synchronized (this.tracks) {
            return this.tracks.removeElement(track);
        }
    }

    /** Las pistas, en un arreglo nuevo. */
    public Track[] getTracks() {
        synchronized (this.tracks) {
            return this.tracks.toArray(new Track[this.tracks.size()]);
        }
    }

    /**
     * Cuanto dura, en microsegundos.
     *
     * <p>En SMPTE es exacto. En PPQ supone el tempo por omision --120 negras por minuto, medio millon
     * de microsegundos por negra-- e <b>ignora los cambios de tempo</b> que la obra tenga escritos.
     * Es lo que hace el JDK, y significa que para una obra con cambios de tempo este numero es una
     * estimacion.
     */
    public long getMicrosecondLength() {
        long ticks = getTickLength();
        if (this.divisionType == PPQ) {
            if (this.resolution == 0) {
                return 0;
            }
            return (long) (ticks * 500000.0 / this.resolution);
        }
        double ticksPerSecond = this.divisionType * this.resolution;
        if (ticksPerSecond == 0.0) {
            return 0;
        }
        return (long) (ticks * 1000000.0 / ticksPerSecond);
    }

    /** El pulso mas alto de todas las pistas. */
    public long getTickLength() {
        long longest = 0;
        synchronized (this.tracks) {
            int i = 0;
            while (i < this.tracks.size()) {
                long ticks = this.tracks.get(i).ticks();
                if (ticks > longest) {
                    longest = ticks;
                }
                i = i + 1;
            }
        }
        return longest;
    }

    /** Vacio. Ver la nota de la clase: el JDK tampoco lo implementa. */
    public Patch[] getPatchList() {
        return new Patch[0];
    }
}
