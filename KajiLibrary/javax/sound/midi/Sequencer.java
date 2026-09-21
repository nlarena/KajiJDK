package javax.sound.midi;

import java.io.IOException;
import java.io.InputStream;

/**
 * KajiLibrary's javax.sound.midi.Sequencer -- plays and records MIDI sequences.
 *
 * <p>A {@link MidiDevice} that knows how to walk a {@link Sequence} in time and send its events to
 * whoever it has connected.
 *
 * <h2>It makes no noise by itself</h2>
 *
 * <p>It is the first thing that confuses. A sequencer <b>sends messages</b>; to hear something its
 * transmitter has to be connected to the receiver of a {@link Synthesizer}. The sequencer that
 * {@code MidiSystem.getSequencer()} returns comes connected to the default synthesizer, and that is
 * why it seems to sound on its own.
 *
 * <h2>{@link #start} does not block</h2>
 *
 * <p>It returns right away and playback goes on in another thread. To know when it finished a
 * {@link MetaEventListener} has to be registered and the meta event 0x2F awaited. Sleeping a while
 * is what almost everybody does and it always comes out wrong.
 *
 * <h2>The three ways of changing the speed</h2>
 *
 * <ul>
 *   <li>{@link #setTempoInBPM} and {@link #setTempoInMPQ} are the same thing in two units: quarter
 *       notes per minute, or microseconds per quarter note. They are inverses;
 *   <li>{@link #setTempoFactor} is <b>a multiplier</b> over whatever the piece asks for. It holds
 *       even if the piece has tempo changes written in it.
 * </ul>
 *
 * <p>The difference matters: setting the tempo is lost as soon as the piece reaches its next tempo
 * change; the factor is not. For "play at half speed" the factor is wanted.
 *
 * <p>None of this has any effect on an SMPTE sequence; see {@link Sequence}.
 *
 * <h2>Looping</h2>
 *
 * <p>{@link #setLoopCount} with {@link #LOOP_CONTINUOUSLY} repeats forever the stretch between
 * {@link #setLoopStartPoint} and {@link #setLoopEndPoint}. The end point -1 means the end of the
 * piece.
 */
public interface Sequencer extends MidiDevice {

    /** Repeat forever. */
    int LOOP_CONTINUOUSLY = -1;

    /**
     * What to play.
     *
     * @throws InvalidMidiDataException if it does not support that sequence
     */
    void setSequence(Sequence sequence) throws InvalidMidiDataException;

    /**
     * Likewise, reading it from a stream.
     *
     * @throws IOException if it could not be read
     * @throws InvalidMidiDataException if it is not a valid MIDI file
     */
    void setSequence(InputStream stream) throws IOException, InvalidMidiDataException;

    /** What is loaded, or null. */
    Sequence getSequence();

    /** Starts. It does not block; see the class note. */
    void start();

    /** Stops, without going back to the beginning. */
    void stop();

    /** Whether it is playing. */
    boolean isRunning();

    /** Starts recording on the enabled tracks. */
    void startRecording();

    /** Stops recording; it keeps playing. */
    void stopRecording();

    /** Whether it is recording. */
    boolean isRecording();

    /**
     * Enables a track for recording.
     *
     * @param channel which channel to record there; -1 is all
     */
    void recordEnable(Track track, int channel);

    /** Disables it. */
    void recordDisable(Track track);

    /** The tempo, in quarter notes per minute. See the class note. */
    float getTempoInBPM();

    /** Changes it. It is lost at the piece's next tempo change. */
    void setTempoInBPM(float bpm);

    /** The tempo, in microseconds per quarter note. */
    float getTempoInMPQ();

    /** Changes it, in the other units. */
    void setTempoInMPQ(float mpq);

    /** A multiplier over whatever the piece asks for. See the class note. */
    void setTempoFactor(float factor);

    /** How much that multiplier is. */
    float getTempoFactor();

    /** How long the piece lasts, in ticks. */
    long getTickLength();

    /** At which tick it is. */
    long getTickPosition();

    /** Jumps to that tick. */
    void setTickPosition(long tick);

    /** How long it lasts, in microseconds. */
    long getMicrosecondLength();

    /** At which microsecond it is. */
    long getMicrosecondPosition();

    /** Jumps to that microsecond. */
    void setMicrosecondPosition(long microseconds);

    /**
     * Where it takes its time from.
     *
     * @throws IllegalArgumentException if it does not support that mode
     */
    void setMasterSyncMode(SyncMode sync);

    /** Where it takes it from. */
    SyncMode getMasterSyncMode();

    /** The modes it supports as master. */
    SyncMode[] getMasterSyncModes();

    /**
     * What it sends for others to follow.
     *
     * @throws IllegalArgumentException if it does not support that mode
     */
    void setSlaveSyncMode(SyncMode sync);

    /** What it sends. */
    SyncMode getSlaveSyncMode();

    /** The modes it supports as slave. */
    SyncMode[] getSlaveSyncModes();

    /** Mutes a track. */
    void setTrackMute(int track, boolean mute);

    /** Whether it is muted; false also if it does not support it. */
    boolean getTrackMute(int track);

    /** Lets only that track sound. */
    void setTrackSolo(int track, boolean solo);

    /** Whether it is soloed; false also if it does not support it. */
    boolean getTrackSolo(int track);

    /**
     * Registers a meta event listener.
     *
     * @return whether it could
     */
    boolean addMetaEventListener(MetaEventListener listener);

    /** Unregisters it. */
    void removeMetaEventListener(MetaEventListener listener);

    /**
     * Registers a listener for those controllers.
     *
     * @return the ones that really got registered; see {@link ControllerEventListener}
     */
    int[] addControllerEventListener(ControllerEventListener listener, int[] controllers);

    /**
     * Unregisters those controllers from that listener.
     *
     * @param controllers null removes them all
     * @return the ones it has left
     */
    int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers);

    /** Where the stretch that repeats begins. */
    void setLoopStartPoint(long tick);

    /** Where it begins. */
    long getLoopStartPoint();

    /** Where it ends; -1 is the end of the piece. */
    void setLoopEndPoint(long tick);

    /** Where it ends. */
    long getLoopEndPoint();

    /** How many times to repeat, or {@link #LOOP_CONTINUOUSLY}. */
    void setLoopCount(int count);

    /** How many times. */
    int getLoopCount();

    /**
     * Where a sequencer's time comes from.
     *
     * <p>It is used in both directions and that is why there are two sets of methods: as
     * <b>master</b> --where this sequencer takes its time from-- and as <b>slave</b> --what it
     * sends for others to follow--.
     *
     * <p>{@link #NO_SYNC} as slave means it sends nothing, not that it does not work.
     *
     * <p>It is not an enum, for the same reason as the rest of these APIs: they are from 1999.
     * Equality is by identity.
     */
    class SyncMode {

        /** Its own clock. It is the normal one. */
        public static final SyncMode INTERNAL_CLOCK = new SyncMode("Internal Clock");

        /** The MIDI clock ticks that arrive from outside. */
        public static final SyncMode MIDI_SYNC = new SyncMode("MIDI Sync");

        /** MIDI timecode, which also carries absolute position. */
        public static final SyncMode MIDI_TIME_CODE = new SyncMode("MIDI Time Code");

        /** Nothing. See the class note. */
        public static final SyncMode NO_SYNC = new SyncMode("No Timing");

        /** The name, for display. */
        private final String name;

        /** Protected: the modes are defined by the platform. */
        protected SyncMode(String name) {
            this.name = name;
        }

        /** By identity. See the class note. */
        @Override
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /** The identity one. */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        /** The name. */
        @Override
        public final String toString() {
            return this.name;
        }
    }
}
