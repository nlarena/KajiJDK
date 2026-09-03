package java.awt.image;

/**
 * La matriz de una convolucion: los pesos con los que cada pixel vecino aporta al resultado.
 *
 * <p>Lo unico que hay que entender de esta clase es **donde esta el centro**, porque de ahi sale
 * todo lo demas. El origen es el pixel que se esta calculando, y se toma en el medio:
 * `(ancho - 1) / 2` y `(alto - 1) / 2`. Para un kernel de 3x3 es (1,1), o sea el del medio; para uno
 * de 4x4 es (1,1) tambien -- con lado par no hay centro exacto y la division entera lo corre hacia
 * arriba y a la izquierda. Eso desplaza la imagen medio pixel, y es la razon por la que los kernels
 * se hacen de lado impar.
 *
 * <p>Los datos van **por filas**: `data[y * ancho + x]`.
 *
 * <p>Es inmutable: el arreglo se copia al entrar y {@link #getKernelData} devuelve otra copia. Un
 * kernel que alguien pudiera cambiar despues de configurar el filtro daria un resultado distinto en
 * cada franja de la imagen.
 */
public class Kernel implements Cloneable {

    private final int width;
    private final int height;
    private final int xOrigin;
    private final int yOrigin;
    private final float[] data;

    /**
     * Un kernel de `width` por `height` con esos pesos.
     *
     * @throws IllegalArgumentException si el arreglo tiene menos de `width * height` pesos
     */
    public Kernel(int width, int height, float[] data) {
        this.width = width;
        this.height = height;
        this.xOrigin = (width - 1) >> 1;
        this.yOrigin = (height - 1) >> 1;
        int n = width * height;
        if (data == null || data.length < n) {
            throw new IllegalArgumentException("Data array too small (is " +
                    (data == null ? 0 : data.length) + " and should be " + n);
        }
        this.data = new float[n];
        System.arraycopy(data, 0, this.data, 0, n);
    }

    /** La columna del origen. Ver la nota de la clase. */
    public final int getXOrigin() {
        return this.xOrigin;
    }

    /** La fila del origen. */
    public final int getYOrigin() {
        return this.yOrigin;
    }

    /** El ancho. */
    public final int getWidth() {
        return this.width;
    }

    /** El alto. */
    public final int getHeight() {
        return this.height;
    }

    /**
     * Los pesos, por filas.
     *
     * @param data donde dejarlos, o nulo para que se reserve uno
     * @throws IllegalArgumentException si el arreglo dado es mas chico que el kernel
     */
    public final float[] getKernelData(float[] data) {
        if (data == null) {
            float[] out = new float[this.data.length];
            System.arraycopy(this.data, 0, out, 0, this.data.length);
            return out;
        }
        if (data.length < this.data.length) {
            throw new IllegalArgumentException(
                    "Data array too small (should be " + this.data.length + ")");
        }
        System.arraycopy(this.data, 0, data, 0, this.data.length);
        return data;
    }

    /** Una copia. Superficial alcanza: un kernel es inmutable. */
    public Object clone() {
        return new Kernel(this.width, this.height, this.data);
    }
}
