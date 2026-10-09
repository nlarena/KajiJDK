package javax.sound.midi;

import java.util.Vector;

/**
 * KajiLibrary's javax.sound.midi.Sequence -- a complete MIDI piece, with its tracks.
 *
 * <p>It is what is loaded from a file and what is given to a {@link Sequencer} to be played.
 *
 * <h2>The two ways of counting time</h2>
 *
 * <p>It is the only thing to understand about this class, and it decides everything else:
 *
 * <ul>
 *   <li>{@link #PPQ}: the ticks are <b>musical</b>. The resolution is ticks per quarter note, and
 *       how long a tick lasts depends on the current tempo. Changing the tempo speeds the piece up
 *       without touching a single event;
 *   <li>{@code SMPTE_*}: the ticks are <b>clock</b> ticks. The division is frames per second and
 *       the resolution ticks per frame, so a tick always lasts the same. The tempo does not affect
 *       it.
 * </ul>
 *
 * <p>For music PPQ is used; to synchronize with video or film, SMPTE. Choosing wrong is discovered
 * late: a piece in SMPTE ignores the tempo changes one writes into it.
 *
 * <p>{@link #SMPTE_30DROP} is 29.97 and not 30. It is the real rate of North American colour
 * television, and those 0.03 of difference are the reason drop-frame timecode exists.
 *
 * <h2>{@link #getPatchList}</h2>
 *
 * <p>It returns an empty array. It is not an omission of this library: the JDK does not implement
 * it either --checked against JDK 25-- and its documentation already warns that it is not finished.
 */
public class Sequence {

    /** Ticks per quarter note: musical time. See the class note. */
    public static final float PPQ = 0.0f;

    /** Twenty-four frames per second, the cinema's. */
    public static final float SMPTE_24 = 24.0f;

    /** Twenty-five, European television's. */
    public static final float SMPTE_25 = 25.0f;

    /** 29.97, with drop frame. See the class note. */
    public static final float SMPTE_30DROP = 29.97f;

    /** Exactly thirty. */
    public static final float SMPTE_30 = 30.0f;

    /** Which of the five. */
    protected float divisionType;

    /** Ticks per quarter note, or ticks per frame. */
    protected int resolution;

    /** The tracks. */
    protected Vector<Track> tracks = new Vector<Track>();

    /**
     * An empty sequence.
     *
     * @throws InvalidMidiDataException if the division is not one of the five
     */
    public Sequence(float divisionType, int resolution) throws InvalidMidiDataException {
        this(divisionType, resolution, 0);
    }

    /**
     * Likewise, with that many empty tracks.
     *
     * @throws InvalidMidiDataException if the division is not one of the five
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

    /** Which of the five. See the class note. */
    public float getDivisionType() {
        return this.divisionType;
    }

    /** Ticks per quarter note, or ticks per frame. */
    public int getResolution() {
        return this.resolution;
    }

    /** A new, empty track, already added. */
    public Track createTrack() {
        synchronized (this.tracks) {
            Track track = new Track();
            this.tracks.addElement(track);
            return track;
        }
    }

    /**
     * Removes that track.
     *
     * @return whether it was there
     */
    public boolean deleteTrack(Track track) {
        synchronized (this.tracks) {
            return this.tracks.removeElement(track);
        }
    }

    /** The tracks, in a new array. */
    public Track[] getTracks() {
        synchronized (this.tracks) {
            return this.tracks.toArray(new Track[this.tracks.size()]);
        }
    }

    /**
     * How long it lasts, in microseconds.
     *
     * <p>In SMPTE it is exact. In PPQ it assumes the default tempo --120 quarter notes per minute,
     * half a million microseconds per quarter note-- and <b>ignores the tempo changes</b> the piece
     * has written in it, so for a piece with tempo changes this number is an estimate.
     *
     * <p>The note said this is what the JDK does. It is not: JDK 25 follows the tempo events, and a
     * sequence at PPQ 480 with a 60 bpm tempo event at tick 0 and 480 ticks lasts 1,000,000
     * microseconds there and 500,000 here.
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

    /** The highest tick of all the tracks. */
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

    /** Empty. See the class note: the JDK does not implement it either. */
    public Patch[] getPatchList() {
        return new Patch[0];
    }
}
