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
 * KajiLibrary's javax.sound.midi.MidiSystem -- el punto de entrada del MIDI.
 *
 * <p>Solo metodos estaticos, y el mismo esquema que {@code javax.sound.sampled.AudioSystem}:
 * pregunta a los proveedores registrados y se queda con el primero que sepa. La clase no sabe MIDI.
 *
 * <p>Los cuatro tipos de proveedor son {@link MidiDeviceProvider}, {@link MidiFileReader},
 * {@link MidiFileWriter} y {@link SoundbankReader}. Se encuentran con {@link ServiceLoader}, y uno
 * roto no tumba la busqueda.
 *
 * <h2>{@link #getReceiver} no es de cualquier dispositivo</h2>
 *
 * <p>Devuelve el del dispositivo <b>por omision</b>, que normalmente es el sintetizador. Es el atajo
 * para "quiero que suene algo" sin elegir dispositivo.
 *
 * <h2>{@link #getSequencer(boolean)}</h2>
 *
 * <p>El booleano decide si el secuenciador viene <b>ya conectado</b> al sintetizador por omision.
 *
 * <p>Con {@code false} no suena nada hasta que uno conecte su transmisor a algo, y eso es
 * justamente lo que se quiere cuando el destino es un puerto MIDI externo: si viniera conectado,
 * todo sonaria dos veces.
 *
 * <h2>Los tipos de archivo son numeros</h2>
 *
 * <p>{@link #getMidiFileTypes} devuelve {@code int[]} con 0, 1 o 2. Ver {@link MidiFileFormat} sobre
 * que significa cada uno.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae proveedores: un puerto MIDI pide codigo nativo y un sintetizador pide un
 * motor de sintesis. La busqueda esta implementada de verdad y todo funciona sobre el conjunto vacio
 * -- arreglos vacios donde corresponde, y {@link MidiUnavailableException} o
 * {@link InvalidMidiDataException} donde el JDK las lanza en una maquina sin dispositivos.
 *
 * <p>Registrando proveedores como servicios, esto anda sin cambios.
 */
public class MidiSystem {

    /** No tiene estado; el constructor publico es el que el JDK dejo. */
    public MidiSystem() {
    }

    /** Los dispositivos que hay. */
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
                // Un proveedor roto no tumba la busqueda; ver la nota de la clase.
            }
        }
        return found.toArray(new MidiDevice.Info[found.size()]);
    }

    /**
     * Ese dispositivo.
     *
     * @throws MidiUnavailableException si nadie lo provee
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
                // Ver la nota de la clase.
            }
        }
        throw new IllegalArgumentException("Requested device not installed: " + info);
    }

    /**
     * Un receptor del dispositivo por omision. Ver la nota de la clase.
     *
     * @throws MidiUnavailableException si no hay ninguno
     */
    public static Receiver getReceiver() throws MidiUnavailableException {
        MidiDevice device = firstDeviceWith(true);
        if (device == null) {
            throw new MidiUnavailableException("Could not open a MIDI receiver");
        }
        return device.getReceiver();
    }

    /**
     * Un transmisor del dispositivo por omision.
     *
     * @throws MidiUnavailableException si no hay ninguno
     */
    public static Transmitter getTransmitter() throws MidiUnavailableException {
        MidiDevice device = firstDeviceWith(false);
        if (device == null) {
            throw new MidiUnavailableException("Could not open a MIDI transmitter");
        }
        return device.getTransmitter();
    }

    /**
     * El sintetizador por omision.
     *
     * @throws MidiUnavailableException si no hay ninguno
     */
    public static Synthesizer getSynthesizer() throws MidiUnavailableException {
        MidiDevice device = firstDeviceOfType(Synthesizer.class);
        if (device == null) {
            throw new MidiUnavailableException("No synthesizer installed");
        }
        return (Synthesizer) device;
    }

    /**
     * El secuenciador por omision, conectado al sintetizador.
     *
     * @throws MidiUnavailableException si no hay ninguno
     */
    public static Sequencer getSequencer() throws MidiUnavailableException {
        return getSequencer(true);
    }

    /**
     * Idem, decidiendo si viene conectado. Ver la nota de la clase.
     *
     * @throws MidiUnavailableException si no hay ninguno
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
     * Lee un banco de sonidos de un flujo.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
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
     * Idem, desde una direccion.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
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
     * Idem, desde un archivo.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
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
     * Que hay en ese flujo.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static MidiFileFormat getMidiFileFormat(InputStream stream)
        throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getMidiFileFormat(stream);
            } catch (InvalidMidiDataException e) {
                // Ese lector no lo reconoce; se prueba con el siguiente.
            }
        }
        throw new InvalidMidiDataException("input stream is not a supported file type");
    }

    /**
     * Idem, desde una direccion.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static MidiFileFormat getMidiFileFormat(URL url)
        throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getMidiFileFormat(url);
            } catch (InvalidMidiDataException e) {
                // Ver arriba.
            }
        }
        throw new InvalidMidiDataException("url is not a supported file type");
    }

    /**
     * Idem, desde un archivo.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static MidiFileFormat getMidiFileFormat(File file)
        throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getMidiFileFormat(file);
            } catch (InvalidMidiDataException e) {
                // Ver arriba.
            }
        }
        throw new InvalidMidiDataException("file is not a supported file type");
    }

    /**
     * La obra que hay en ese flujo.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static Sequence getSequence(InputStream stream)
        throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getSequence(stream);
            } catch (InvalidMidiDataException e) {
                // Ver arriba.
            }
        }
        throw new InvalidMidiDataException("could not get sequence from input stream");
    }

    /**
     * Idem, desde una direccion.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static Sequence getSequence(URL url) throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getSequence(url);
            } catch (InvalidMidiDataException e) {
                // Ver arriba.
            }
        }
        throw new InvalidMidiDataException("could not get sequence from URL");
    }

    /**
     * Idem, desde un archivo.
     *
     * @throws InvalidMidiDataException si nadie lo reconoce
     * @throws IOException si no se pudo leer
     */
    public static Sequence getSequence(File file) throws InvalidMidiDataException, IOException {
        Iterator<MidiFileReader> it = providers(MidiFileReader.class);
        while (it.hasNext()) {
            try {
                return it.next().getSequence(file);
            } catch (InvalidMidiDataException e) {
                // Ver arriba.
            }
        }
        throw new InvalidMidiDataException("could not get sequence from file");
    }

    /** Que tipos de archivo se pueden escribir. Ver la nota de la clase. */
    public static int[] getMidiFileTypes() {
        List<Integer> found = new ArrayList<Integer>();
        Iterator<MidiFileWriter> it = providers(MidiFileWriter.class);
        while (it.hasNext()) {
            try {
                collect(found, it.next().getMidiFileTypes());
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return toIntArray(found);
    }

    /** Si ese tipo se puede escribir. */
    public static boolean isFileTypeSupported(int fileType) {
        return contains(getMidiFileTypes(), fileType);
    }

    /** Que tipos se pueden escribir con esa obra. */
    public static int[] getMidiFileTypes(Sequence sequence) {
        List<Integer> found = new ArrayList<Integer>();
        Iterator<MidiFileWriter> it = providers(MidiFileWriter.class);
        while (it.hasNext()) {
            try {
                collect(found, it.next().getMidiFileTypes(sequence));
            } catch (Throwable e) {
                // Ver la nota de la clase.
            }
        }
        return toIntArray(found);
    }

    /** Si ese tipo se puede escribir con esa obra. */
    public static boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return contains(getMidiFileTypes(sequence), fileType);
    }

    /**
     * Escribe la obra.
     *
     * @return cuantos bytes se escribieron
     * @throws IOException si no se pudo escribir
     * @throws IllegalArgumentException si nadie sabe escribir ese tipo
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
     * Idem, a un archivo.
     *
     * @return cuantos bytes se escribieron
     * @throws IOException si no se pudo escribir
     * @throws IllegalArgumentException si nadie sabe escribir ese tipo
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

    /** El primer dispositivo que pueda dar receptores, o transmisores. */
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
                // -1 es "sin limite", no "ninguno"; ver MidiDevice.
                if (max != 0) {
                    return device;
                }
            }
            i = i + 1;
        }
        return null;
    }

    /** El primer dispositivo de esa clase. */
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

    /** Ese dispositivo, o null si no se pudo. */
    private static MidiDevice quietDevice(MidiDevice.Info info) {
        try {
            return getMidiDevice(info);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Los proveedores de ese tipo, saltandose los que no cargan. */
    private static <T> Iterator<T> providers(Class<T> type) {
        List<T> all = new ArrayList<T>();
        try {
            Iterator<T> it = ServiceLoader.load(type).iterator();
            while (it.hasNext()) {
                try {
                    all.add(it.next());
                } catch (Throwable e) {
                    // Ese proveedor no carga; se sigue con los demas.
                }
            }
        } catch (Throwable e) {
            // Ni siquiera se pudo abrir el cargador de servicios.
        }
        return all.iterator();
    }

    /** Agrega los que no esten repetidos. */
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

    /** La lista como arreglo de enteros. */
    private static int[] toIntArray(List<Integer> list) {
        int[] out = new int[list.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = list.get(i).intValue();
            i = i + 1;
        }
        return out;
    }

    /** Si ese valor esta en el arreglo. */
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
