package java.awt;

import java.io.Serializable;

/**
 * La grilla ya calculada de un {@link GridBagLayout}: cuántas filas y columnas hay, cuánto mide cada
 * una y cuánto peso tiene.
 *
 * <p>Existe porque el cálculo de la grilla es caro y se necesita tres veces —para la medida mínima,
 * para la preferida y para ubicar— así que se hace una vez y se guarda.
 *
 * <p>Todos sus campos son de paquete y no tiene ningún miembro público: es un resultado intermedio
 * de {@link GridBagLayout}, no algo con lo que se trabaje desde afuera. La clase es pública sólo
 * porque aparece como tipo de un campo protegido de la distribución, y una subclase tiene que poder
 * nombrarla.
 */
public final class GridBagLayoutInfo implements Serializable {

    private static final long serialVersionUID = -4899416460737170217L;

    /** Cuántas columnas tiene la grilla. */
    int width;

    /** Cuántas filas. */
    int height;

    /** Dónde arranca la grilla en X. */
    int startx;

    /** Dónde arranca en Y. */
    int starty;

    /** Lo que mide cada columna. */
    int[] minWidth;

    /** Lo que mide cada fila. */
    int[] minHeight;

    /** Cuánto del ancho sobrante se lleva cada columna. */
    double[] weightX;

    /** Cuánto del alto sobrante se lleva cada fila. */
    double[] weightY;

    /** Con la grilla de ese tamaño. */
    GridBagLayoutInfo(int width, int height) {
        this.width = width;
        this.height = height;
        this.minWidth = new int[width];
        this.minHeight = new int[height];
        this.weightX = new double[width];
        this.weightY = new double[height];
    }
}
