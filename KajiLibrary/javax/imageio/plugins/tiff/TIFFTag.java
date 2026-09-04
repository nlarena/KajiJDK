package javax.imageio.plugins.tiff;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFTag -- una etiqueta de metadato TIFF.
 *
 * <p>Un archivo TIFF es una lista de etiquetas numeradas. Esta clase describe <b>una</b> de ellas: que
 * numero tiene, como se llama, que tipos de dato admite y cuantos valores lleva.
 *
 * <h2>Los tipos van en una mascara de bits</h2>
 *
 * <p>{@link #getDataTypes} no devuelve un tipo sino un {@code int} donde el bit {@code n} indica que
 * el tipo {@code n} esta permitido. Se arma con {@code 1 << TIFF_SHORT}, y se consulta con
 * {@link #isDataTypeOK}.
 *
 * <p>La razon es que TIFF permite que la misma etiqueta venga en varios tipos: la altura de una imagen
 * puede ser {@code SHORT} o {@code LONG} segun el tamano. Un lector tiene que aceptar los dos.
 *
 * <h2>{@link #getCount} devuelve -1 seguido</h2>
 *
 * <p>Significa "cualquier cantidad", no "ninguno". Es lo normal en las etiquetas de texto y en las
 * tablas de color, donde la cantidad depende de la imagen.
 *
 * <h2>Las etiquetas que apuntan a otro directorio</h2>
 *
 * <p>El constructor de tres argumentos que toma un {@link TIFFTagSet} arma una etiqueta <b>puntero</b>:
 * su valor no es un dato sino la posicion de otro directorio de etiquetas. Es como TIFF anida
 * metadatos --Exif, GPS-- adentro de un archivo.
 *
 * <p>Una etiqueta asi se reconoce con {@link #isIFDPointer}, y su conjunto asociado dice que etiquetas
 * esperar del otro lado.
 *
 * <h2>Los nombres de valor</h2>
 *
 * <p>Muchas etiquetas guardan un numero que significa algo --1 es "sin comprimir", 5 es "LZW"--.
 * {@link #addValueName} deja registrar esa traduccion, y es una operacion <b>protegida</b>: solo una
 * subclase puede llamarla, tipicamente desde su constructor.
 *
 * <p>Es a proposito: los nombres son parte de la definicion de la etiqueta, no algo que se le agregue
 * despues.
 */
public class TIFFTag {

    /** Entero de 8 bits sin signo. */
    public static final int TIFF_BYTE = 1;

    /** Texto terminado en cero. */
    public static final int TIFF_ASCII = 2;

    /** Entero de 16 bits sin signo. */
    public static final int TIFF_SHORT = 3;

    /** Entero de 32 bits sin signo. */
    public static final int TIFF_LONG = 4;

    /** Dos enteros largos: numerador y denominador. */
    public static final int TIFF_RATIONAL = 5;

    /** Entero de 8 bits con signo. */
    public static final int TIFF_SBYTE = 6;

    /** Bytes sin interpretar. */
    public static final int TIFF_UNDEFINED = 7;

    /** Entero de 16 bits con signo. */
    public static final int TIFF_SSHORT = 8;

    /** Entero de 32 bits con signo. */
    public static final int TIFF_SLONG = 9;

    /** Dos enteros con signo: numerador y denominador. */
    public static final int TIFF_SRATIONAL = 10;

    /** Coma flotante de 32 bits. */
    public static final int TIFF_FLOAT = 11;

    /** Coma flotante de 64 bits. */
    public static final int TIFF_DOUBLE = 12;

    /** Puntero a otro directorio. Ver la nota de la clase. */
    public static final int TIFF_IFD_POINTER = 13;

    /** El tipo mas chico. */
    public static final int MIN_DATATYPE = 1;

    /** El tipo mas grande. */
    public static final int MAX_DATATYPE = 13;

    /** El nombre que se le pone a una etiqueta que no esta en ningun conjunto conocido. */
    public static final String UNKNOWN_TAG_NAME = "UnknownTag";

    /** Cuantos bytes ocupa un valor de cada tipo; la posicion 0 no se usa. */
    private static final int[] SIZE_OF_TYPE = {
        0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8, 4,
    };

    /** Como se llama. */
    private final String name;

    /** Que numero tiene. */
    private final int number;

    /** La mascara de tipos permitidos. Ver la nota de la clase. */
    private final int dataTypes;

    /** Cuantos valores, o -1. */
    private final int count;

    /** A que conjunto apunta, si es un puntero. */
    private final TIFFTagSet tagSet;

    /** La traduccion de valores a nombres, si la hay. */
    private SortedMap<Integer, String> valueNames = null;

    /**
     * Una etiqueta completa.
     *
     * @param dataTypes la mascara de tipos; ver la nota de la clase
     * @param count cuantos valores, o -1 para cualquiera
     * @throws NullPointerException si el nombre es null
     * @throws IllegalArgumentException si el numero es negativo, la mascara tiene bits fuera de
     *     rango, o la cantidad es negativa sin ser -1
     */
    public TIFFTag(String name, int number, int dataTypes, int count) {
        this(name, number, dataTypes, count, null);
    }

    /**
     * Una etiqueta puntero a otro directorio. Ver la nota de la clase.
     *
     * @throws NullPointerException si el nombre o el conjunto son null
     */
    public TIFFTag(String name, int number, TIFFTagSet tagSet) {
        this(name, number, 1 << TIFF_LONG | 1 << TIFF_IFD_POINTER, 1, checkSet(tagSet));
    }

    /**
     * Una etiqueta sin cantidad fija.
     *
     * @throws NullPointerException si el nombre es null
     */
    public TIFFTag(String name, int number, int dataTypes) {
        this(name, number, dataTypes, -1, null);
    }

    /** El unico constructor de verdad; los tres publicos delegan aca. */
    private TIFFTag(String name, int number, int dataTypes, int count, TIFFTagSet tagSet) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (number < 0) {
            throw new IllegalArgumentException("number (" + number + ") < 0");
        }
        if (dataTypes < 0 || (dataTypes & ~((1 << (MAX_DATATYPE + 1)) - 2)) != 0) {
            throw new IllegalArgumentException("dataTypes out of range");
        }
        if (count < -1) {
            throw new IllegalArgumentException("count (" + count + ") < -1");
        }
        this.name = name;
        this.number = number;
        this.dataTypes = dataTypes;
        this.count = count;
        this.tagSet = tagSet;
    }

    /**
     * Cuantos bytes ocupa un valor de ese tipo.
     *
     * @throws IllegalArgumentException si no es un tipo valido
     */
    public static int getSizeOfType(int dataType) {
        if (dataType < MIN_DATATYPE || dataType > MAX_DATATYPE) {
            throw new IllegalArgumentException("dataType out of range!");
        }
        return SIZE_OF_TYPE[dataType];
    }

    /** Como se llama. */
    public String getName() {
        return this.name;
    }

    /** Que numero tiene. */
    public int getNumber() {
        return this.number;
    }

    /** La mascara de tipos permitidos. Ver la nota de la clase. */
    public int getDataTypes() {
        return this.dataTypes;
    }

    /** Cuantos valores, o -1 para cualquiera. Ver la nota de la clase. */
    public int getCount() {
        return this.count;
    }

    /** Si ese tipo esta permitido. */
    public boolean isDataTypeOK(int dataType) {
        if (dataType < MIN_DATATYPE || dataType > MAX_DATATYPE) {
            return false;
        }
        return (this.dataTypes & (1 << dataType)) != 0;
    }

    /** A que conjunto apunta, o null si no es un puntero. */
    public TIFFTagSet getTagSet() {
        return this.tagSet;
    }

    /** Si su valor es la posicion de otro directorio. Ver la nota de la clase. */
    public boolean isIFDPointer() {
        return this.tagSet != null;
    }

    /** Si tiene nombres para sus valores. */
    public boolean hasValueNames() {
        return this.valueNames != null;
    }

    /**
     * Le da nombre a un valor. Protegido; ver la nota de la clase.
     *
     * @param value el numero que aparece en el archivo
     * @param name que significa
     */
    protected void addValueName(int value, String name) {
        if (this.valueNames == null) {
            this.valueNames = new TreeMap<Integer, String>();
        }
        this.valueNames.put(Integer.valueOf(value), name);
    }

    /** Que significa ese valor, o null si no tiene nombre. */
    public String getValueName(int value) {
        if (this.valueNames == null) {
            return null;
        }
        return this.valueNames.get(Integer.valueOf(value));
    }

    /** Los valores que tienen nombre, ordenados; null si no hay ninguno. */
    public int[] getNamedValues() {
        if (this.valueNames == null) {
            return null;
        }
        int[] out = new int[this.valueNames.size()];
        int i = 0;
        java.util.Iterator<Integer> it = this.valueNames.keySet().iterator();
        while (it.hasNext()) {
            out[i] = it.next().intValue();
            i = i + 1;
        }
        return out;
    }

    /** El control que el constructor de puntero necesita hacer antes de delegar. */
    private static TIFFTagSet checkSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new NullPointerException("tagSet == null");
        }
        return tagSet;
    }
}
