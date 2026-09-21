package javax.sound.midi;

import java.util.List;

/**
 * KajiLibrary's javax.sound.midi.MidiDevice -- a MIDI device.
 *
 * <p>A physical port, a software synthesizer, a sequencer. They all open, close, and hand out
 * receivers and transmitters.
 *
 * <h2>Receiver and transmitter, from the device</h2>
 *
 * <p>It is the same inversion of names as in sampled audio, and for the same reason:
 *
 * <ul>
 *   <li>a {@link Receiver} of the device is where the program <b>writes</b>;
 *   <li>a {@link Transmitter} of the device is where the program <b>reads</b> from.
 * </ul>
 *
 * <p>A MIDI <b>output</b> port provides receivers; an <b>input</b> one provides transmitters. A
 * device that returns 0 from {@link #getMaxReceivers} does not accept being sent anything.
 *
 * <h2>{@link #getMaxReceivers} can return -1</h2>
 *
 * <p>It means "no limit", not "none". Confusing it leads to code that refuses to use a perfectly
 * good device.
 *
 * <h2>It closes when its last receiver closes</h2>
 *
 * <p>A device that was opened implicitly --by asking it for a receiver without having opened it--
 * closes by itself when the last one closes. One opened by hand with {@link #open} does not: that
 * one has to be closed by hand.
 */
public interface MidiDevice extends AutoCloseable {

    /** What it is called. */
    MidiDevice.Info getDeviceInfo();

    /**
     * Reserves the system resource.
     *
     * @throws MidiUnavailableException if it is busy
     */
    void open() throws MidiUnavailableException;

    /** Releases it, and closes everything it has handed out. */
    void close();

    /** Whether it is open. */
    boolean isOpen();

    /** The device's clock, in microseconds, or -1 if it keeps none. */
    long getMicrosecondPosition();

    /** How many receivers it can give at once; -1 is no limit. See the class note. */
    int getMaxReceivers();

    /** How many transmitters it can give at once; -1 is no limit. */
    int getMaxTransmitters();

    /**
     * A new receiver.
     *
     * @throws MidiUnavailableException if it cannot give more
     */
    Receiver getReceiver() throws MidiUnavailableException;

    /** The ones it already gave that are still open. */
    List<Receiver> getReceivers();

    /**
     * A new transmitter.
     *
     * @throws MidiUnavailableException if it cannot give more
     */
    Transmitter getTransmitter() throws MidiUnavailableException;

    /** The ones it already gave that are still open. */
    List<Transmitter> getTransmitters();

    /**
     * What a device is called.
     *
     * <p>Four strings for display. Equality is by <b>identity</b>: two devices with the same name
     * are still two.
     *
     * <p>{@link #toString} returns only the name, not the four things.
     */
    class Info {

        /** The name. */
        private final String name;

        /** Who made it. */
        private final String vendor;

        /** What it is. */
        private final String description;

        /** Which version. */
        private final String version;

        /** Protected: this data is defined by whoever implements the device. */
        protected Info(String name, String vendor, String description, String version) {
            this.name = name;
            this.vendor = vendor;
            this.description = description;
            this.version = version;
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
        public final String getName() {
            return this.name;
        }

        /** Who made it. */
        public final String getVendor() {
            return this.vendor;
        }

        /** What it is. */
        public final String getDescription() {
            return this.description;
        }

        /** Which version. */
        public final String getVersion() {
            return this.version;
        }

        /** Only the name. See the class note. */
        @Override
        public final String toString() {
            return this.name;
        }
    }
}
