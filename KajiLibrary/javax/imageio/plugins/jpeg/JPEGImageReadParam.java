package javax.imageio.plugins.jpeg;

import javax.imageio.ImageReadParam;

/**
 * KajiLibrary's javax.imageio.plugins.jpeg.JPEGImageReadParam -- las tablas para decodificar un JPEG
 * al que le faltan.
 *
 * <p>Existe por un caso concreto y poco frecuente: los JPEG <b>abreviados</b>. El estandar permite un
 * flujo que trae los datos comprimidos pero <b>no</b> las tablas con las que se comprimieron -- porque
 * viajan aparte, en un flujo de solo tablas, o porque el emisor y el receptor las acordaron de
 * antemano.
 *
 * <p>Un JPEG asi no se puede decodificar solo. {@link #setDecodeTables} es como se le pasan las tablas
 * que faltan.
 *
 * <h2>Los tres arreglos van juntos</h2>
 *
 * <p>Cuantizacion, Huffman de continua y Huffman de alterna. Se ponen los tres de una y se sacan los
 * tres de una: no hay forma de poner solo uno, porque un juego incompleto no sirve para decodificar
 * nada.
 *
 * <p>Pasar null en cualquiera de los tres lanza {@link IllegalArgumentException}; para sacarlos esta
 * {@link #unsetDecodeTables}.
 *
 * <p>Para un JPEG normal --que es el noventa y nueve por ciento-- esta clase no hace falta: las tablas
 * vienen en el archivo.
 */
public class JPEGImageReadParam extends ImageReadParam {

    /** Las de cuantizacion, o null. */
    private JPEGQTable[] qTables = null;

    /** Las de Huffman de continua, o null. */
    private JPEGHuffmanTable[] DCHuffmanTables = null;

    /** Las de alterna, o null. */
    private JPEGHuffmanTable[] ACHuffmanTables = null;

    /** Sin tablas puestas. */
    public JPEGImageReadParam() {
    }

    /** Si hay tablas puestas. */
    public boolean areTablesSet() {
        return this.qTables != null;
    }

    /**
     * Pone los tres juegos. Ver la nota de la clase: van juntos.
     *
     * <p>Los arreglos se copian.
     *
     * @throws IllegalArgumentException si alguno es null, si estan vacios, o si los dos de Huffman no
     *     tienen el mismo largo
     */
    public void setDecodeTables(JPEGQTable[] qTables, JPEGHuffmanTable[] DCHuffmanTables,
                                JPEGHuffmanTable[] ACHuffmanTables) {
        if (qTables == null || DCHuffmanTables == null || ACHuffmanTables == null
            || qTables.length == 0 || DCHuffmanTables.length == 0 || ACHuffmanTables.length == 0
            || DCHuffmanTables.length != ACHuffmanTables.length) {
            throw new IllegalArgumentException("Invalid JPEG table arrays");
        }
        this.qTables = new JPEGQTable[qTables.length];
        System.arraycopy(qTables, 0, this.qTables, 0, qTables.length);
        this.DCHuffmanTables = new JPEGHuffmanTable[DCHuffmanTables.length];
        System.arraycopy(DCHuffmanTables, 0, this.DCHuffmanTables, 0, DCHuffmanTables.length);
        this.ACHuffmanTables = new JPEGHuffmanTable[ACHuffmanTables.length];
        System.arraycopy(ACHuffmanTables, 0, this.ACHuffmanTables, 0, ACHuffmanTables.length);
    }

    /** Los saca. */
    public void unsetDecodeTables() {
        this.qTables = null;
        this.DCHuffmanTables = null;
        this.ACHuffmanTables = null;
    }

    /** Las de cuantizacion, o null; una copia. */
    public JPEGQTable[] getQTables() {
        if (this.qTables == null) {
            return null;
        }
        JPEGQTable[] copy = new JPEGQTable[this.qTables.length];
        System.arraycopy(this.qTables, 0, copy, 0, this.qTables.length);
        return copy;
    }

    /** Las de continua, o null; una copia. */
    public JPEGHuffmanTable[] getDCHuffmanTables() {
        if (this.DCHuffmanTables == null) {
            return null;
        }
        JPEGHuffmanTable[] copy = new JPEGHuffmanTable[this.DCHuffmanTables.length];
        System.arraycopy(this.DCHuffmanTables, 0, copy, 0, this.DCHuffmanTables.length);
        return copy;
    }

    /** Las de alterna, o null; una copia. */
    public JPEGHuffmanTable[] getACHuffmanTables() {
        if (this.ACHuffmanTables == null) {
            return null;
        }
        JPEGHuffmanTable[] copy = new JPEGHuffmanTable[this.ACHuffmanTables.length];
        System.arraycopy(this.ACHuffmanTables, 0, copy, 0, this.ACHuffmanTables.length);
        return copy;
    }
}
