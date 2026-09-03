package java.awt.image;

/**
 * Una tabla de consulta: por cada componente de un pixel, que valor sale para cada valor que entra.
 *
 * <p>Es la forma de expresar cualquier transformacion **por componente y sin memoria** -- ajustar
 * el brillo, invertir, aplicar una curva-- de manera que aplicarla sea leer un arreglo y no evaluar
 * una funcion por pixel.
 *
 * <h2>El desplazamiento</h2>
 *
 * <p>{@link #getOffset} es lo que se le **resta** al valor de entrada antes de indexar. Una tabla
 * que solo cubre el rango 100..200 se guarda con 101 entradas y desplazamiento 100, en vez de con
 * 201 entradas de las cuales las primeras 100 no se usan. Olvidarse de restarlo da una imagen
 * corrida.
 *
 * <h2>Una tabla o una por componente</h2>
 *
 * <p>Las subclases aceptan las dos formas. Con **una sola** tabla, se aplica a todos los
 * componentes; con **varias**, una a cada uno. La segunda forma es la que permite, por ejemplo,
 * subir el rojo sin tocar el verde.
 */
public abstract class LookupTable {

    private final int numComponents;
    private final int offset;

    /**
     * Una tabla para `numComponents` componentes, con ese desplazamiento.
     *
     * @throws IllegalArgumentException si el desplazamiento es negativo o no hay componentes
     */
    protected LookupTable(int offset, int numComponents) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be greater than or equal to 0");
        }
        if (numComponents < 1) {
            throw new IllegalArgumentException("Number of components must be at least 1");
        }
        this.numComponents = numComponents;
        this.offset = offset;
    }

    /** Cuantos componentes cubre. */
    public int getNumComponents() {
        return this.numComponents;
    }

    /** Lo que se le resta a la entrada antes de indexar. Ver la nota de la clase. */
    public int getOffset() {
        return this.offset;
    }

    /**
     * Aplica la tabla a un pixel.
     *
     * @param src los componentes que entran
     * @param dst donde dejar los que salen, o nulo para que se reserve uno
     */
    public abstract int[] lookupPixel(int[] src, int[] dst);
}
