package javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import javax.sound.midi.Sequence;

/**
 * KajiLibrary's javax.sound.midi.spi.MidiFileWriter -- escribe archivos MIDI.
 *
 * <p>El espejo de {@link MidiFileReader}. Los tipos son numeros --0, 1 o 2-- y no objetos; ver
 * {@code MidiFileFormat} sobre que significa cada uno.
 *
 * <p>Los dos {@code getMidiFileTypes} tienen la misma distincion que en el audio muestreado: el sin
 * argumentos dice que sabe escribir en general, el que toma una obra dice que puede escribir <b>con
 * esa</b>. Una obra de varias pistas no entra en un archivo de tipo 0 sin mezclarlas, y hay
 * implementaciones que se niegan a mezclar.
 */
public abstract class MidiFileWriter {

    /** Para las subclases. */
    protected MidiFileWriter() {
    }

    /** Que tipos sabe escribir en general. */
    public abstract int[] getMidiFileTypes();

    /** Que tipos puede escribir con esa obra. Ver la nota de la clase. */
    public abstract int[] getMidiFileTypes(Sequence sequence);

    /** Si sabe escribir ese tipo. */
    public boolean isFileTypeSupported(int fileType) {
        return contains(getMidiFileTypes(), fileType);
    }

    /** Si puede escribir ese tipo con esa obra. */
    public boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return contains(getMidiFileTypes(sequence), fileType);
    }

    /**
     * Escribe.
     *
     * @return cuantos bytes se escribieron
     * @throws IOException si no se pudo escribir
     * @throws IllegalArgumentException si no soporta ese tipo con esa obra
     */
    public abstract int write(Sequence in, int fileType, OutputStream out) throws IOException;

    /**
     * Idem, a un archivo.
     *
     * @return cuantos bytes se escribieron
     * @throws IOException si no se pudo escribir
     * @throws IllegalArgumentException si no soporta ese tipo con esa obra
     */
    public abstract int write(Sequence in, int fileType, File out) throws IOException;

    /** Si ese valor esta en el arreglo. */
    private static boolean contains(int[] all, int one) {
        int i = 0;
        while (all != null && i < all.length) {
            if (all[i] == one) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
