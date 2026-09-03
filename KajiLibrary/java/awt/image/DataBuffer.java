package java.awt.image;

/**
 * Los números crudos de una imagen, sin ninguna interpretación.
 *
 * <p>Es la capa más baja de `java.awt.image` y la más fácil de malentender, así que conviene decir
 * qué **no** sabe: no sabe de píxeles, ni de ancho, ni de color. Es un arreglo de números y nada
 * más. Quién es un píxel lo dice un {@link SampleModel}; qué color es lo dice un
 * {@link ColorModel}. Separar las tres cosas es lo que permite que la misma memoria se lea como
 * escala de grises o como RGB sin copiarla.
 *
 * <h2>Los bancos</h2>
 *
 * <p>Un buffer puede tener varios **bancos**, que son arreglos independientes. Sirven para guardar
 * cada componente por separado --todos los rojos juntos, todos los verdes juntos-- en vez de
 * intercalados. Los métodos que no dicen banco trabajan sobre el banco 0.
 *
 * <p>Cada banco tiene su propio desplazamiento inicial: {@link #getOffsets} los devuelve todos y
 * {@link #getOffset} el del banco 0. Un buffer de un solo banco tiene un solo desplazamiento, y por
 * eso los dos métodos parecen redundantes hasta que hay más de uno.
 *
 * <h2>Los seis tipos y sus rangos</h2>
 *
 * <p>{@link #TYPE_USHORT} y {@link #TYPE_SHORT} usan los dos un `short[]` y se diferencian sólo en
 * cómo se lee: el primero como 0..65535 y el segundo como -32768..32767. Es la misma memoria con
 * dos interpretaciones, y confundirlos da imágenes con los tonos claros dados vuelta.
 *
 * <h2>Los tres pares de accesores</h2>
 *
 * <p>`getElem`, `getElemFloat` y `getElemDouble` leen **el mismo dato** con distinta conversión. Las
 * subclases de enteros redefinen sólo el primero y heredan los otros dos, que convierten; las de
 * punto flotante hacen al revés. Por eso `getElemFloat` sobre un `DataBufferInt` no pierde nada
 * --un `int` entra en un `float` con pérdida sólo por encima de 2^24-- y `getElem` sobre un
 * `DataBufferDouble` **sí** trunca. Está en el contrato y no es un defecto.
 */
public abstract class DataBuffer {

    /** Enteros de 8 bits sin signo, guardados en un `byte[]`. */
    public static final int TYPE_BYTE = 0;
    /** Enteros de 16 bits **sin** signo, en un `short[]`. */
    public static final int TYPE_USHORT = 1;
    /** Enteros de 16 bits **con** signo, en un `short[]`. */
    public static final int TYPE_SHORT = 2;
    /** Enteros de 32 bits con signo. */
    public static final int TYPE_INT = 3;
    /** Coma flotante de 32 bits. */
    public static final int TYPE_FLOAT = 4;
    /** Coma flotante de 64 bits. */
    public static final int TYPE_DOUBLE = 5;
    /** Ninguno de los anteriores. */
    public static final int TYPE_UNDEFINED = 32;

    /** El tipo de los datos: una de las constantes `TYPE_`. */
    protected int dataType;

    /** Cuántos bancos tiene. */
    protected int banks;

    /** El desplazamiento del banco 0. */
    protected int offset;

    /** Cuántos elementos utiliza cada banco a partir de su desplazamiento. */
    protected int size;

    /** El desplazamiento de cada banco. */
    protected int[] offsets;

    /**
     * Cuántos bits ocupa un elemento de ese tipo.
     *
     * @throws IllegalArgumentException si el tipo no es uno de los seis
     */
    public static int getDataTypeSize(int type) {
        if (type < TYPE_BYTE || type > TYPE_DOUBLE) {
            throw new IllegalArgumentException("Unknown data type " + type);
        }
        int[] tamanos = { 8, 16, 16, 32, 32, 64 };
        return tamanos[type];
    }

    /** Un banco, sin desplazamiento. */
    protected DataBuffer(int dataType, int size) {
        this(dataType, size, 1, 0);
    }

    /** `numBanks` bancos, sin desplazamiento. */
    protected DataBuffer(int dataType, int size, int numBanks) {
        this(dataType, size, numBanks, 0);
    }

    /** `numBanks` bancos, todos con el mismo desplazamiento. */
    protected DataBuffer(int dataType, int size, int numBanks, int offset) {
        this.dataType = dataType;
        this.size = size;
        this.banks = numBanks;
        this.offset = offset;
        this.offsets = new int[numBanks];
        for (int i = 0; i < numBanks; i++) {
            this.offsets[i] = offset;
        }
    }

    /**
     * `numBanks` bancos, cada uno con su desplazamiento.
     *
     * @throws ArrayIndexOutOfBoundsException si hay menos desplazamientos que bancos
     */
    protected DataBuffer(int dataType, int size, int numBanks, int[] offsets) {
        if (offsets.length < numBanks) {
            throw new ArrayIndexOutOfBoundsException("Number of banks does not match number of "
                    + "band offsets");
        }
        this.dataType = dataType;
        this.size = size;
        this.banks = numBanks;
        this.offsets = new int[numBanks];
        for (int i = 0; i < numBanks; i++) {
            this.offsets[i] = offsets[i];
        }
        this.offset = offsets[0];
    }

    /** El tipo de los datos. */
    public int getDataType() {
        return this.dataType;
    }

    /** Cuántos elementos usa cada banco. */
    public int getSize() {
        return this.size;
    }

    /** El desplazamiento del banco 0. */
    public int getOffset() {
        return this.offset;
    }

    /** Una copia de los desplazamientos de todos los bancos. */
    public int[] getOffsets() {
        int[] out = new int[this.offsets.length];
        System.arraycopy(this.offsets, 0, out, 0, this.offsets.length);
        return out;
    }

    /** Cuántos bancos. */
    public int getNumBanks() {
        return this.banks;
    }

    /** El elemento `i` del banco 0. */
    public int getElem(int i) {
        return this.getElem(0, i);
    }

    /** El elemento `i` del banco dado. */
    public abstract int getElem(int bank, int i);

    /** Escribe el elemento `i` del banco 0. */
    public void setElem(int i, int val) {
        this.setElem(0, i, val);
    }

    /** Escribe el elemento `i` del banco dado. */
    public abstract void setElem(int bank, int i, int val);

    /** El elemento `i` del banco 0, como `float`. */
    public float getElemFloat(int i) {
        return this.getElem(i);
    }

    /** El elemento `i` del banco dado, como `float`. */
    public float getElemFloat(int bank, int i) {
        return this.getElem(bank, i);
    }

    /** Escribe un `float` en el banco 0. En un buffer entero se trunca. */
    public void setElemFloat(int i, float val) {
        this.setElem(i, (int) val);
    }

    /** Escribe un `float` en el banco dado. En un buffer entero se trunca. */
    public void setElemFloat(int bank, int i, float val) {
        this.setElem(bank, i, (int) val);
    }

    /** El elemento `i` del banco 0, como `double`. */
    public double getElemDouble(int i) {
        return this.getElem(i);
    }

    /** El elemento `i` del banco dado, como `double`. */
    public double getElemDouble(int bank, int i) {
        return this.getElem(bank, i);
    }

    /** Escribe un `double` en el banco 0. En un buffer entero se trunca. */
    public void setElemDouble(int i, double val) {
        this.setElem(i, (int) val);
    }

    /** Escribe un `double` en el banco dado. En un buffer entero se trunca. */
    public void setElemDouble(int bank, int i, double val) {
        this.setElem(bank, i, (int) val);
    }
}
