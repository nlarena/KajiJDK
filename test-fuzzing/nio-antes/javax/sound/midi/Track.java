package javax.sound.midi;

import java.util.ArrayList;

/**
 * KajiLibrary's javax.sound.midi.Track -- a track of MIDI events sorted by time.
 *
 * <p>It is not constructed: it is asked for with {@code Sequence.createTrack()}. A loose track
 * would have nothing to measure its ticks against.
 *
 * <h2>It always has the end of track</h2>
 *
 * <p>A newly created track already has <b>one</b> event: the meta message of type 0x2F, end of
 * track, at tick 0. That is why {@code size()} of an empty track returns 1, which surprises the
 * first time.
 *
 * <p>The track keeps it by itself: when an event is added later, the end of track moves to stay
 * always last. It is mandatory in the file format, and leaving it in the hands of whoever uses the
 * API would be asking for broken files.
 *
 * <h2>It is sorted on insertion</h2>
 *
 * <p>{@link #add} puts the event in its place by tick, not at the end. Events with the same tick
 * stay in the order they were added.
 *
 * <p>And the same {@link MidiEvent} object cannot be added twice: the second attempt returns false.
 * It is identity, not equality -- two different events with the same content both go in.
 */
public final class Track {

    /** The events, sorted by tick. */
    private final ArrayList<MidiEvent> events = new ArrayList<MidiEvent>();

    /** The end of track, which is always there and always last. */
    private final MidiEvent endOfTrack;

    /** Package access: only {@link Sequence} creates tracks. */
    Track() {
        this.endOfTrack = new ImmutableEndOfTrack();
        this.events.add(this.endOfTrack);
    }

    /**
     * Adds an event in its place.
     *
     * <p>See the class note: it is sorted by tick and the end of track stays last.
     *
     * @return whether it was added; false if it is null or if that same object was already there
     */
    public boolean add(MidiEvent event) {
        if (event == null) {
            return false;
        }
        synchronized (this.events) {
            if (indexOfIdentity(event) >= 0) {
                return false;
            }
            long tick = event.getTick();
            if (tick > this.endOfTrack.getTick()) {
                this.endOfTrack.setTick(tick);
            }
            int at = this.events.size();
            // It is searched from the end: the usual thing is adding in order, and that way it
            // costs one comparison instead of walking the whole track.
            while (at > 0 && this.events.get(at - 1).getTick() > tick) {
                at = at - 1;
            }
            // The end of track stays last even if it shares the tick. It is checked that it is
            // still there: it can be taken out with remove(), and in that case there is nothing to
            // preserve.
            if (at == this.events.size() && at > 0
                && this.events.get(at - 1) == this.endOfTrack) {
                at = at - 1;
            }
            this.events.add(at, event);
            return true;
        }
    }

    /**
     * Removes an event.
     *
     * @return whether it was there
     */
    public boolean remove(MidiEvent event) {
        if (event == null) {
            return false;
        }
        synchronized (this.events) {
            int at = indexOfIdentity(event);
            if (at < 0) {
                return false;
            }
            this.events.remove(at);
            return true;
        }
    }

    /**
     * Event number {@code index}.
     *
     * @throws ArrayIndexOutOfBoundsException if it does not exist
     */
    public MidiEvent get(int index) throws ArrayIndexOutOfBoundsException {
        synchronized (this.events) {
            if (index < 0 || index >= this.events.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return this.events.get(index);
        }
    }

    /** How many events there are, counting the end of track. See the class note. */
    public int size() {
        synchronized (this.events) {
            return this.events.size();
        }
    }

    /** At which tick it ends. */
    public long ticks() {
        synchronized (this.events) {
            if (this.events.isEmpty()) {
                return 0;
            }
            return this.events.get(this.events.size() - 1).getTick();
        }
    }

    /** Where that exact object is, or -1. By identity; see the class note. */
    private int indexOfIdentity(MidiEvent event) {
        int i = 0;
        while (i < this.events.size()) {
            if (this.events.get(i) == event) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * The end of track event.
     *
     * <p>Package access. It is a normal {@link MidiEvent} except for one thing: its message cannot
     * be changed. If somebody could rewrite it, the track would stop having an end and the file
     * that came out of it would not be valid.
     */
    private static final class ImmutableEndOfTrack extends MidiEvent {

        ImmutableEndOfTrack() {
            super(new EndOfTrackMessage(), 0);
        }
    }

    /** The 0x2F meta message, without data, that will not let itself be modified. */
    private static final class EndOfTrackMessage extends MetaMessage {

        EndOfTrackMessage() {
            super(new byte[] { (byte) MetaMessage.META, 0x2F, 0 });
        }

        /**
         * Does nothing.
         *
         * <p>The note said ignoring silently is what the JDK does. It is not: JDK 25 throws {@code
         * InvalidMidiDataException("cannot modify end of track message")}. The note's reason for
         * not throwing was that it would break code that walks a track rewriting messages, which is
         * just when this gets touched by accident.
         */
        @Override
        public void setMessage(int type, byte[] data, int length) {
        }

        /** A normal copy, now modifiable. */
        @Override
        public Object clone() {
            return new MetaMessage(getMessage());
        }
    }
}
