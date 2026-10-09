package javax.sound.midi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.sound.midi.MidiFileFormat -- what there is in a MIDI file.
 *
 * <p>What can be known without loading the whole piece: the file type, how time is counted, how
 * much it takes and how long it lasts.
 *
 * <h2>The three file types</h2>
 *
 * <ul>
 *   <li><b>0</b>: a single track with everything mixed;
 *   <li><b>1</b>: several tracks that sound <b>at the same time</b>. It is the normal one;
 *   <li><b>2</b>: several <b>independent</b> tracks, which do not share a timeline. It is hardly
 *       used and many programs will not even open it.
 * </ul>
 *
 * <p>The classic confusion is between 1 and 2: both have several tracks, and only in 1 are those
 * tracks simultaneous.
 *
 * <p>{@link #getByteLength} and {@link #getMicrosecondLength} can be {@link #UNKNOWN_LENGTH}
 * --which is -1-- when the file arrives through a stream with no known end.
 *
 * <p>See {@link Sequence} on {@link #getDivisionType} and {@link #getResolution}.
 */
public class MidiFileFormat {

    /** Not known. */
    public static final int UNKNOWN_LENGTH = -1;

    /** 0, 1 or 2. See the class note. */
    protected int type;

    /** How time is counted; see {@link Sequence}. */
    protected float divisionType;

    /** Ticks per quarter note, or per frame. */
    protected int resolution;

    /** How many bytes it takes, or {@link #UNKNOWN_LENGTH}. */
    protected int byteLength;

    /** How long it lasts, or {@link #UNKNOWN_LENGTH}. */
    protected long microsecondLength;

    /** What does not fit in the fixed fields. */
    private HashMap<String, Object> properties;

    /** The usual one. */
    public MidiFileFormat(int type, float divisionType, int resolution, int bytes,
                          long microseconds) {
        this.type = type;
        this.divisionType = divisionType;
        this.resolution = resolution;
        this.byteLength = bytes;
        this.microsecondLength = microseconds;
        this.properties = null;
    }

    /**
     * Likewise, with properties.
     *
     * @throws NullPointerException if the map is null
     */
    public MidiFileFormat(int type, float divisionType, int resolution, int bytes,
                          long microseconds, Map<String, Object> properties) {
        this(type, divisionType, resolution, bytes, microseconds);
        this.properties = new HashMap<String, Object>(properties);
    }

    /** 0, 1 or 2. See the class note. */
    public int getType() {
        return this.type;
    }

    /** How time is counted. */
    public float getDivisionType() {
        return this.divisionType;
    }

    /** Ticks per quarter note, or per frame. */
    public int getResolution() {
        return this.resolution;
    }

    /** How many bytes, or {@link #UNKNOWN_LENGTH}. */
    public int getByteLength() {
        return this.byteLength;
    }

    /** How long it lasts, or {@link #UNKNOWN_LENGTH}. */
    public long getMicrosecondLength() {
        return this.microsecondLength;
    }

    /** The properties, read-only. */
    public Map<String, Object> properties() {
        Map<String, Object> ret;
        if (this.properties == null) {
            ret = new HashMap<String, Object>(0);
        } else {
            ret = new HashMap<String, Object>(this.properties);
        }
        return Collections.unmodifiableMap(ret);
    }

    /** A property, or null. */
    public Object getProperty(String key) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(key);
    }
}
