package javax.imageio.plugins.jpeg;

/**
 * KajiLibrary's javax.imageio.plugins.jpeg.JPEGQTable -- una tabla de cuantizacion de JPEG.
 *
 * <p>Aca es donde JPEG <b>pierde informacion</b>, y es lo unico que hay que entender de esta clase.
 * Cada uno de los 64 coeficientes de un bloque se divide por su entrada de la tabla y se redondea; lo
 * que se pierde en ese redondeo no vuelve.
 *
 * <p>Por eso los numeros crecen hacia el final: las primeras entradas son las frecuencias bajas --las
 * formas grandes, que el ojo ve-- y las ultimas las altas --el detalle fino, que el ojo casi no ve--.
 * Dividir las altas por 99 y las bajas por 16 es exactamente la apuesta que hace JPEG.
 *
 * <p>Los 64 valores estan en <b>orden natural</b>, por filas: la entrada {@code i} es la fila
 * {@code i / 8}, columna {@code i % 8}. En el archivo van en zigzag; la conversion no es asunto de
 * esta clase.
 *
 * <h2>{@link #getScaledInstance}</h2>
 *
 * <p>Es como se implementa un control de calidad: multiplicar la tabla entera por un factor. Menos de
 * uno da mas calidad y mas tamano; mas de uno, al reves.
 *
 * <p>El booleano decide el techo: {@code true} recorta a 255 --lo que exige JPEG de 8 bits-- y
 * {@code false} a 32767. El piso siempre es 1, porque dividir por cero no es una opcion.
 *
 * <p>Las cuatro constantes son las del anexo K del estandar. Las {@code Div2} son las mismas a la
 * mitad, que es aproximadamente calidad 75 contra calidad 50.
 *
 * <p>Es inmutable.
 */
public class JPEGQTable {

    /** Los 64 valores de K1Luminance, en orden natural. */
    private static final int[] K1LUMINANCE_TABLE = {
        16, 11, 10, 16, 24, 40, 51, 61,
        12, 12, 14, 19, 26, 58, 60, 55,
        14, 13, 16, 24, 40, 57, 69, 56,
        14, 17, 22, 29, 51, 87, 80, 62,
        18, 22, 37, 56, 68, 109, 103, 77,
        24, 35, 55, 64, 81, 104, 113, 92,
        49, 64, 78, 87, 103, 121, 120, 101,
        72, 92, 95, 98, 112, 100, 103, 99
    };

    /** Los 64 valores de K1Div2Luminance, en orden natural. */
    private static final int[] K1DIV2LUMINANCE_TABLE = {
        8, 6, 5, 8, 12, 20, 26, 31,
        6, 6, 7, 10, 13, 29, 30, 28,
        7, 7, 8, 12, 20, 29, 35, 28,
        7, 9, 11, 15, 26, 44, 40, 31,
        9, 11, 19, 28, 34, 55, 52, 39,
        12, 18, 28, 32, 41, 52, 57, 46,
        25, 32, 39, 44, 52, 61, 60, 51,
        36, 46, 48, 49, 56, 50, 52, 50
    };

    /** Los 64 valores de K2Chrominance, en orden natural. */
    private static final int[] K2CHROMINANCE_TABLE = {
        17, 18, 24, 47, 99, 99, 99, 99,
        18, 21, 26, 66, 99, 99, 99, 99,
        24, 26, 56, 99, 99, 99, 99, 99,
        47, 66, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99,
        99, 99, 99, 99, 99, 99, 99, 99
    };

    /** Los 64 valores de K2Div2Chrominance, en orden natural. */
    private static final int[] K2DIV2CHROMINANCE_TABLE = {
        9, 9, 12, 24, 50, 50, 50, 50,
        9, 11, 13, 33, 50, 50, 50, 50,
        12, 13, 28, 50, 50, 50, 50, 50,
        24, 33, 50, 50, 50, 50, 50, 50,
        50, 50, 50, 50, 50, 50, 50, 50,
        50, 50, 50, 50, 50, 50, 50, 50,
        50, 50, 50, 50, 50, 50, 50, 50,
        50, 50, 50, 50, 50, 50, 50, 50
    };

    /** La tabla K.1 del estandar, para luminancia. */
    public static final JPEGQTable K1Luminance = new JPEGQTable(K1LUMINANCE_TABLE, false);

    /** La K.1 a la mitad: el doble de calidad y de tamano. */
    public static final JPEGQTable K1Div2Luminance = new JPEGQTable(K1DIV2LUMINANCE_TABLE, false);

    /** La tabla K.2 del estandar, para crominancia. */
    public static final JPEGQTable K2Chrominance = new JPEGQTable(K2CHROMINANCE_TABLE, false);

    /** La K.2 a la mitad. */
    public static final JPEGQTable K2Div2Chrominance = new JPEGQTable(K2DIV2CHROMINANCE_TABLE, false);

    /** Los 64 valores, en orden natural. */
    private final int[] qTable;

    /**
     * Una tabla nueva; el arreglo se copia.
     *
     * @param table 64 valores en orden natural
     * @throws IllegalArgumentException si es null o no tiene 64 entradas
     */
    public JPEGQTable(int[] table) {
        if (table == null) {
            throw new IllegalArgumentException("table must not be null.");
        }
        if (table.length != 64) {
            throw new IllegalArgumentException("table.length != 64");
        }
        this.qTable = new int[64];
        System.arraycopy(table, 0, this.qTable, 0, 64);
    }

    /** El de las constantes, que no copia. Ver {@link JPEGHuffmanTable}. */
    private JPEGQTable(int[] table, boolean shared) {
        this.qTable = table;
    }

    /** Los 64 valores; una copia. */
    public int[] getTable() {
        int[] copy = new int[64];
        System.arraycopy(this.qTable, 0, copy, 0, 64);
        return copy;
    }

    /**
     * Una tabla con todos los valores multiplicados por ese factor.
     *
     * <p>Ver la nota de la clase: el piso es 1 y el techo depende del booleano.
     *
     * @param scaleFactor por cuanto multiplicar
     * @param forceBaseline si recortar a 255 en lugar de a 32767
     */
    public JPEGQTable getScaledInstance(float scaleFactor, boolean forceBaseline) {
        int max;
        if (forceBaseline) {
            max = 255;
        } else {
            max = 32767;
        }
        int[] scaled = new int[64];
        int i = 0;
        while (i < 64) {
            int value = Math.round(this.qTable[i] * scaleFactor);
            if (value < 1) {
                value = 1;
            }
            if (value > max) {
                value = max;
            }
            scaled[i] = value;
            i = i + 1;
        }
        return new JPEGQTable(scaled, false);
    }

    /** Los 64 valores en ocho filas de ocho, con una tabulacion adelante. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("JPEGQTable:\n");
        int row = 0;
        while (row < 8) {
            sb.append('\t');
            int col = 0;
            while (col < 8) {
                sb.append(this.qTable[row * 8 + col]);
                if (col < 7) {
                    sb.append(' ');
                }
                col = col + 1;
            }
            sb.append('\n');
            row = row + 1;
        }
        return sb.toString();
    }
}
