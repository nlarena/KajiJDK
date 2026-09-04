package javax.imageio.plugins.jpeg;

/**
 * KajiLibrary's javax.imageio.plugins.jpeg.JPEGHuffmanTable -- una tabla de Huffman de JPEG.
 *
 * <p>La ultima etapa de la compresion JPEG: despues de la transformada y la cuantizacion, los
 * coeficientes se codifican con Huffman. Esta clase es una de esas tablas.
 *
 * <h2>Las dos mitades</h2>
 *
 * <p>Una tabla de Huffman de JPEG no se guarda como un arbol sino como dos listas, que es lo que
 * confunde al leerla por primera vez:
 *
 * <ul>
 *   <li>{@link #getLengths} tiene <b>16</b> entradas: cuantos codigos hay de cada largo, de 1 a 16
 *       bits;
 *   <li>{@link #getValues} tiene tantas entradas como codigos haya en total, en orden de largo
 *       creciente.
 * </ul>
 *
 * <p>Con eso alcanza para reconstruir el arbol, porque JPEG usa codigos canonicos: dados los largos,
 * los codigos quedan determinados. Es lo que permite que la tabla ocupe unas decenas de bytes en el
 * archivo en lugar de un arbol entero.
 *
 * <p>Las cuatro constantes son las del anexo K del estandar, y son las que usa casi todo codificador
 * JPEG del mundo. Estan bien para casi todo: optimizar las tablas para una imagen concreta gana un
 * pocos por ciento y cuesta una pasada mas.
 *
 * <p>Es inmutable: los constructores copian y los accesores tambien.
 */
public class JPEGHuffmanTable {

    /** Los largos de StdDCLuminance. */
    private static final short[] STDDCLUMINANCE_LENGTHS = {
        0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0
    };

    /** Los valores de StdDCLuminance. */
    private static final short[] STDDCLUMINANCE_VALUES = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
    };

    /** Los largos de StdDCChrominance. */
    private static final short[] STDDCCHROMINANCE_LENGTHS = {
        0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0
    };

    /** Los valores de StdDCChrominance. */
    private static final short[] STDDCCHROMINANCE_VALUES = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
    };

    /** Los largos de StdACLuminance. */
    private static final short[] STDACLUMINANCE_LENGTHS = {
        0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 125
    };

    /** Los valores de StdACLuminance. */
    private static final short[] STDACLUMINANCE_VALUES = {
        1, 2, 3, 0, 4, 17, 5, 18, 33, 49, 65, 6, 19, 81, 97, 7,
        34, 113, 20, 50, 129, 145, 161, 8, 35, 66, 177, 193, 21, 82, 209, 240,
        36, 51, 98, 114, 130, 9, 10, 22, 23, 24, 25, 26, 37, 38, 39, 40,
        41, 42, 52, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72, 73,
        74, 83, 84, 85, 86, 87, 88, 89, 90, 99, 100, 101, 102, 103, 104, 105,
        106, 115, 116, 117, 118, 119, 120, 121, 122, 131, 132, 133, 134, 135, 136, 137,
        138, 146, 147, 148, 149, 150, 151, 152, 153, 154, 162, 163, 164, 165, 166, 167,
        168, 169, 170, 178, 179, 180, 181, 182, 183, 184, 185, 186, 194, 195, 196, 197,
        198, 199, 200, 201, 202, 210, 211, 212, 213, 214, 215, 216, 217, 218, 225, 226,
        227, 228, 229, 230, 231, 232, 233, 234, 241, 242, 243, 244, 245, 246, 247, 248,
        249, 250
    };

    /** Los largos de StdACChrominance. */
    private static final short[] STDACCHROMINANCE_LENGTHS = {
        0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 119
    };

    /** Los valores de StdACChrominance. */
    private static final short[] STDACCHROMINANCE_VALUES = {
        0, 1, 2, 3, 17, 4, 5, 33, 49, 6, 18, 65, 81, 7, 97, 113,
        19, 34, 50, 129, 8, 20, 66, 145, 161, 177, 193, 9, 35, 51, 82, 240,
        21, 98, 114, 209, 10, 22, 36, 52, 225, 37, 241, 23, 24, 25, 26, 38,
        39, 40, 41, 42, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72,
        73, 74, 83, 84, 85, 86, 87, 88, 89, 90, 99, 100, 101, 102, 103, 104,
        105, 106, 115, 116, 117, 118, 119, 120, 121, 122, 130, 131, 132, 133, 134, 135,
        136, 137, 138, 146, 147, 148, 149, 150, 151, 152, 153, 154, 162, 163, 164, 165,
        166, 167, 168, 169, 170, 178, 179, 180, 181, 182, 183, 184, 185, 186, 194, 195,
        196, 197, 198, 199, 200, 201, 202, 210, 211, 212, 213, 214, 215, 216, 217, 218,
        226, 227, 228, 229, 230, 231, 232, 233, 234, 242, 243, 244, 245, 246, 247, 248,
        249, 250
    };

    /** La estandar de continua para luminancia. */
    public static final JPEGHuffmanTable StdDCLuminance =
        new JPEGHuffmanTable(STDDCLUMINANCE_LENGTHS, STDDCLUMINANCE_VALUES, false);

    /** La estandar de continua para crominancia. */
    public static final JPEGHuffmanTable StdDCChrominance =
        new JPEGHuffmanTable(STDDCCHROMINANCE_LENGTHS, STDDCCHROMINANCE_VALUES, false);

    /** La estandar de alterna para luminancia. */
    public static final JPEGHuffmanTable StdACLuminance =
        new JPEGHuffmanTable(STDACLUMINANCE_LENGTHS, STDACLUMINANCE_VALUES, false);

    /** La estandar de alterna para crominancia. */
    public static final JPEGHuffmanTable StdACChrominance =
        new JPEGHuffmanTable(STDACCHROMINANCE_LENGTHS, STDACCHROMINANCE_VALUES, false);

    /** Cuantos codigos hay de cada largo, de 1 a 16 bits. */
    private final short[] lengths;

    /** Los valores, en orden de largo creciente. */
    private final short[] values;

    /**
     * Una tabla nueva.
     *
     * <p>Los dos arreglos se copian.
     *
     * @param lengths cuantos codigos de cada largo; tiene que tener 16 entradas
     * @param values los valores; tantos como codigos haya en total
     * @throws IllegalArgumentException si alguno es null o los tamanos no cierran
     */
    public JPEGHuffmanTable(short[] lengths, short[] values) {
        if (lengths == null || values == null) {
            throw new IllegalArgumentException("lengths or values are null");
        }
        this.lengths = new short[lengths.length];
        System.arraycopy(lengths, 0, this.lengths, 0, lengths.length);
        this.values = new short[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
    }

    /**
     * El de las constantes, que no copia.
     *
     * <p>Las tablas del estandar son estaticas y nadie las modifica; copiarlas ocho veces al cargar la
     * clase seria trabajo perdido. El booleano no significa nada: esta para distinguir la firma.
     */
    private JPEGHuffmanTable(short[] lengths, short[] values, boolean shared) {
        this.lengths = lengths;
        this.values = values;
    }

    /** Cuantos codigos hay de cada largo; una copia. */
    public short[] getLengths() {
        short[] copy = new short[this.lengths.length];
        System.arraycopy(this.lengths, 0, copy, 0, this.lengths.length);
        return copy;
    }

    /** Los valores; una copia. */
    public short[] getValues() {
        short[] copy = new short[this.values.length];
        System.arraycopy(this.values, 0, copy, 0, this.values.length);
        return copy;
    }

    /** Las dos listas, una por linea. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("JPEGHuffmanTable\n");
        sb.append("lengths:");
        int i = 0;
        while (i < this.lengths.length) {
            sb.append(' ').append(this.lengths[i]);
            i = i + 1;
        }
        sb.append("\nvalues:");
        i = 0;
        while (i < this.values.length) {
            sb.append(' ').append(this.values[i]);
            i = i + 1;
        }
        return sb.toString();
    }
}
