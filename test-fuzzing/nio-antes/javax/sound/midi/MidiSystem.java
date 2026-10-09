package javax.sound.midi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import javax.sound.midi.spi.MidiDeviceProvider;
import javax.sound.midi.spi.MidiFileReader;
import javax.sound.midi.spi.MidiFileWriter;
import javax.sound.midi.spi.SoundbankReader;

/**
 * KajiLibrary's javax.sound.midi.MidiSystem -- the entry point to MIDI.
 *
 * <p>Only static methods, and the same scheme as {@code javax.sound.sampled.AudioSystem}: it asks
 * the registered providers and keeps the first one that knows how. The class knows no MIDI.
 *
 * <p>The four kinds of provider are {@link MidiDeviceProvider}, {@link MidiFileReader}, {@link
 * MidiFileWriter} and {@link SoundbankReader}. They are found with {@link ServiceLoader}, and a
 * broken one does not bring the search down.
 *
 * <h2>{@link #getReceiver} is not from just any device</h2>
 *
 * <p>It returns the one of the <b>default</b> device, which is normally the synthesizer. It is the
 * shortcut for "I want something to sound" without choosing a device.
 *
 * <h2>{@link #getSequencer(boolean)}</h2>
 *
 * <p>The boolean decides whether the sequencer comes <b>already connected</b> to the default
 * synthesizer.
 *
 * <p>With {@code false} nothing sounds until one connects its transmitter to something, and that is
 * precisely what is wanted when the destination is an external MIDI port: if it came connected,
 * everything would sound twice.
 *
 * <h2>File types are numbers</h2>
 *
 * <p>{@link #getMidiFileTypes} returns {@code int[]} with 0, 1 or 2. See {@link MidiFileFormat} on
 * what each one means.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library comes with no providers: a MIDI port needs native code and a synthesizer needs a
 * synthesis engine. The search is really implemented and everything works over the empty set --
 * empty arrays where they belong, and {@link MidiUnavailableException} or
 * {@link InvalidMidiDataException} where the JDK throws them on a machine without devices.
 *
 * <p>Registering providers as services, this works unchanged.
 */
public class MidiSystem {

    /** It has no state; the public constructor is the one the JDK left. */
    public MidiSystem() {
    }

    /** The devices there are. */
    public static MidiDevice.Info[] getMidiDeviceInfo() {
        List<MidiDevice.Info> found = new ArrayList<MidiDevice.Info>();
        Iterator<MidiDeviceProvider> it = providers(MidiDeviceProvider.class);
        while (it.hasNext()) {
            try {
                MidiDevice.Info[] some = it.next().getDeviceInfo();
                int i = 0;
                while (some != null && i < some.length) {
                    found.add(some[i]);
                    i = i + 1;
                }
            } catch (Throwable e) {
                // A broken provider does not bring the search down; see the class note.
            }
        }
        return found.toArray(new MidiDevice.Info[found.size()]);
    }

    /**
     * That device.
     *
     * @throws MidiUnavailableException if nobody provides it
     */
    public static MidiDevice getMidiDevice(MidiDevice.Info info) throws MidiUnavailableException {
        if (info == null) {
            throw new NullPointerException();
        }
        Iterator<MidiDeviceProvider> it = providers(MidiDeviceProvider.class);
        while (it.hasNext()) {
            MidiDeviceProvider p = it.next();
            try {
                if (p.isDeviceSupported(info)) {
                    return p.getDevice(info);
                }
            } catch (Throwable e) {
                // See the class note.
            }
        }
        throw new IllegalArgumentException("Requested device not installed: " + info);
    }

    /**
     * A receiver of the default device. See the class note.
     *
     * @throws MidiUnavailableException if there is none
     */
    public static Receiver getReceiver() throws MidiUnavailableException {
        MidiDevice device = firstDeviceWith(true);
        if (device == null) {
            throw new MidiUnavailableException("Could not open a MIDI receiver");
        }
        return device.getReceiver();
    }

    /**
     * A transmitter of the default device.
     *
     * @throws MidiUnavailableException if there is none
     */
    public static Transmitter getTransmitter() throws MidiUnavailableException {
        MidiDevice device = firstDeviceWith(false);
        if (device == null) {
            throw new MidiUnavailableException("Could not open a MIDI transmitter");
        }
        return device.getTransmitter();
    }

    /**
     * The default synthesizer.
     *
     * @throws MidiUnavailableException if there is none
     */
    public static Synthesizer getSynthesizer() throws MidiUnavailableException {
        MidiDevice device = firstDeviceOfType(Synthesizer.class);
        if (device == null) {
            throw new MidiUnavailableException("No synthesizer installed");
        }
        return (Synthesizer) device;
    }

    /**
     * The default sequencer, connected to the synthesizer.
     *
     * @throws MidiUnavailableException if there is none
     */
    public static Sequencer getSequencer() throws MidiUnavailableException {
        return getSequencer(true);
    }

    /**
     * Likewise, deciding whether it comes connected. See the class note.
     *
     * @throws MidiUnavailableException if there is none
     */
    public static Sequencer getSequencer(boolean connected) throws MidiUnavailableException {
        MidiDevice device = firstDeviceOfType(Sequencer.class);
        if (device == null) {
            throw new MidiUnavailableException("No sequencer installed");
        }
        Sequencer sequencer = (Sequencer) device;
        if (connected) {
            Synthesizer synth = getSynthesizer();
            sequencer.open();
            synth.open();
            sequencer.getTransmitter().setReceiver(synth.getReceiver());
        }
        return sequencer;
    }

    /**
     * Reads a sound bank from a stream.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static Soundbank getSoundbank(InputStream stream)
        throws InvalidMidiDataException, IOException {
        Iterator<SoundbankReader> it = providers(SoundbankReader.class);
        while (it.hasNext()) {
            Soundbank bank = it.next().getSoundbank(stream);
            if (bank != null) {
                return bank;
            }
        }
        throw new InvalidMidiDataException("cannot get soundbank from stream");
    }

    /**
     * Likewise, from a URL.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException {
        Iterator<SoundbankReader> it = providers(SoundbankReader.class);
        while (it.hasNext()) {
            Soundbank bank = it.next().getSoundbank(url);
            if (bank != null) {
                return bank;
            }
        }
        throw new InvalidMidiDataException("cannot get soundbank from stream");
    }

    /**
     * Likewise, from a file.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        Iterator<SoundbankReader> it = providers(SoundbankReader.class);
        while (it.hasNext()) {
            Soundbank bank = it.next().getSoundbank(file);
            if (bank != null) {
                return bank;
            }
        }
        throw new InvalidMidiDataException("cannot get soundbank from stream");
    }

    /**
     * What there is in that stream.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static MidiFileFormat getMidiFileFormat(InputStream stream)
        throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getMidiFileFormat(stream);
            } catch (InvalidMidiDataException e) {
                // That reader does not recognize it; try the next one.
            }
        }
        throw new InvalidMidiDataException("input stream is not a supported file type");
    }

    /**
     * Likewise, from a URL.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static MidiFileFormat getMidiFileFormat(URL url)
        throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getMidiFileFormat(url);
            } catch (InvalidMidiDataException e) {
                // See above.
            }
        }
        throw new InvalidMidiDataException("url is not a supported file type");
    }

    /**
     * Likewise, from a file.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static MidiFileFormat getMidiFileFormat(File file)
        throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getMidiFileFormat(file);
            } catch (InvalidMidiDataException e) {
                // See above.
            }
        }
        throw new InvalidMidiDataException("file is not a supported file type");
    }

    /**
     * The piece there is in that stream.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static Sequence getSequence(InputStream stream)
        throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getSequence(stream);
            } catch (InvalidMidiDataException e) {
                // See above.
            }
        }
        throw new InvalidMidiDataException("could not get sequence from input stream");
    }

    /**
     * Likewise, from a URL.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static Sequence getSequence(URL url) throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getSequence(url);
            } catch (InvalidMidiDataException e) {
                // See above.
            }
        }
        throw new InvalidMidiDataException("could not get sequence from URL");
    }

    /**
     * Likewise, from a file.
     *
     * @throws InvalidMidiDataException if nobody recognizes it
     * @throws IOException if it could not be read
     */
    public static Sequence getSequence(File file) throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getSequence(file);
            } catch (InvalidMidiDataException e) {
                // See above.
            }
        }
        throw new InvalidMidiDataException("could not get sequence from file");
    }

    /** Which file types can be written. See the class note. */
    public static int[] getMidiFileTypes() {
        List<Integer> found = new ArrayList<Integer>();
        Iterator<MidiFileWriter> it = providers(MidiFileWriter.class);
        while (it.hasNext()) {
            try {
                collect(found, it.next().getMidiFileTypes());
            } catch (Throwable e) {
                // See the class note.
            }
        }
        return toIntArray(found);
    }

    /** Whether that type can be written. */
    public static boolean isFileTypeSupported(int fileType) {
        return contains(getMidiFileTypes(), fileType);
    }

    /** Which types can be written with that piece. */
    public static int[] getMidiFileTypes(Sequence sequence) {
        List<Integer> found = new ArrayList<Integer>();
        Iterator<MidiFileWriter> it = providers(MidiFileWriter.class);
        while (it.hasNext()) {
            try {
                collect(found, it.next().getMidiFileTypes(sequence));
            } catch (Throwable e) {
                // See the class note.
            }
        }
        return toIntArray(found);
    }

    /** Whether that type can be written with that piece. */
    public static boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return contains(getMidiFileTypes(sequence), fileType);
    }

    /**
     * Writes the piece.
     *
     * @return how many bytes were written
     * @throws IOException if it could not be written
     * @throws IllegalArgumentException if nobody knows how to write that type
     */
    public static int write(Sequence in, int fileType, OutputStream out) throws IOException {
        Iterator<MidiFileWriter> it = providers(MidiFileWriter.class);
        while (it.hasNext()) {
            MidiFileWriter w = it.next();
            if (w.isFileTypeSupported(fileType, in)) {
                return w.write(in, fileType, out);
            }
        }
        throw new IllegalArgumentException("MIDI file type is not supported");
    }

    /**
     * Likewise, to a file.
     *
     * @return how many bytes were written
     * @throws IOException if it could not be written
     * @throws IllegalArgumentException if nobody knows how to write that type
     */
    public static int write(Sequence in, int fileType, File out) throws IOException {
        Iterator<MidiFileWriter> it = providers(MidiFileWriter.class);
        while (it.hasNext()) {
            MidiFileWriter w = it.next();
            if (w.isFileTypeSupported(fileType, in)) {
                return w.write(in, fileType, out);
            }
        }
        throw new IllegalArgumentException("MIDI file type is not supported");
    }

    /** The first device that can give receivers, or transmitters. */
    private static MidiDevice firstDeviceWith(boolean wantReceiver) {
        MidiDevice.Info[] infos = getMidiDeviceInfo();
        int i = 0;
        while (i < infos.length) {
            MidiDevice device = quietDevice(infos[i]);
            if (device != null) {
                int max;
                if (wantReceiver) {
                    max = device.getMaxReceivers();
                } else {
                    max = device.getMaxTransmitters();
                }
                // -1 is "no limit", not "none"; see MidiDevice.
                if (max != 0) {
                    return device;
                }
            }
            i = i + 1;
        }
        return null;
    }

    /** The first device of that class. */
    private static MidiDevice firstDeviceOfType(Class<?> type) {
        MidiDevice.Info[] infos = getMidiDeviceInfo();
        int i = 0;
        while (i < infos.length) {
            MidiDevice device = quietDevice(infos[i]);
            if (device != null && type.isInstance(device)) {
                return device;
            }
            i = i + 1;
        }
        return null;
    }

    /** That device, or null if it could not be had. */
    private static MidiDevice quietDevice(MidiDevice.Info info) {
        try {
            return getMidiDevice(info);
        } catch (Throwable e) {
            return null;
        }
    }

    /** The providers of that kind, skipping the ones that do not load. */
    private static <T> Iterator<T> providers(Class<T> type) {
        List<T> all = new ArrayList<T>();
        try {
            Iterator<T> it = ServiceLoader.load(type).iterator();
            while (it.hasNext()) {
                try {
                    all.add(it.next());
                } catch (Throwable e) {
                    // That provider does not load; carry on with the others.
                }
            }
        } catch (Throwable e) {
            // Not even the service loader could be opened.
        }
        return all.iterator();
    }

    /** Adds the ones that are not repeated. */
    private static void collect(List<Integer> into, int[] some) {
        int i = 0;
        while (some != null && i < some.length) {
            Integer boxed = Integer.valueOf(some[i]);
            if (!into.contains(boxed)) {
                into.add(boxed);
            }
            i = i + 1;
        }
    }

    /** The list as an array of ints. */
    private static int[] toIntArray(List<Integer> list) {
        int[] out = new int[list.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = list.get(i).intValue();
            i = i + 1;
        }
        return out;
    }

    /** Whether that value is in the array. */
    private static boolean contains(int[] all, int one) {
        int i = 0;
        while (i < all.length) {
            if (all[i] == one) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
