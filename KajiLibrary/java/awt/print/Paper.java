package java.awt.print;

/**
 * KajiLibrary's java.awt.print.Paper -- la hoja fisica y su area imprimible.
 *
 * <p>Dos rectangulos: el tamano de la hoja, y adentro el pedazo donde la impresora puede poner tinta.
 * El resto es el margen mecanico, que existe porque los rodillos tienen que agarrar el papel de algun
 * lado.
 *
 * <p>Todo esta en <b>puntos</b>: 1/72 de pulgada. Una carta es 612 por 792, y eso es lo que trae por
 * omision, con una pulgada de margen por lado.
 *
 * <h2>No valida nada</h2>
 *
 * <p>{@link #setImageableArea} acepta un area que se sale de la hoja, o negativa. Es deliberado en el
 * JDK y lo respetamos: quien corrige es {@code PrinterJob.validatePage}, que sabe contra que impresora
 * validar. Un {@code Paper} suelto no tiene con que.
 *
 * <p>Es mutable y {@link Cloneable}; por eso {@link PageFormat#getPaper} devuelve una copia.
 */
public class Paper implements Cloneable {

    /** Una pulgada en puntos. */
    private static final int INCH = 72;

    /** Ancho de la hoja. */
    private double mHeight;

    /** Alto de la hoja. */
    private double mWidth;

    /** El area imprimible. */
    private double mImageableX;

    private double mImageableY;

    private double mImageableWidth;

    private double mImageableHeight;

    /** Una carta con una pulgada de margen. */
    public Paper() {
        this.mHeight = 11.0 * INCH;
        this.mWidth = 8.5 * INCH;
        this.mImageableX = INCH;
        this.mImageableY = INCH;
        this.mImageableWidth = this.mWidth - 2.0 * INCH;
        this.mImageableHeight = this.mHeight - 2.0 * INCH;
    }

    /** Una copia independiente. */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Paper es Cloneable, asi que esto no puede pasar.
            throw new InternalError(e);
        }
    }

    /** El ancho de la hoja, en puntos. */
    public double getWidth() {
        return this.mWidth;
    }

    /** El alto de la hoja, en puntos. */
    public double getHeight() {
        return this.mHeight;
    }

    /** Cambia el tamano de la hoja. No toca el area imprimible. */
    public void setSize(double width, double height) {
        this.mWidth = width;
        this.mHeight = height;
    }

    /** Cambia el area imprimible. No valida; ver la nota de la clase. */
    public void setImageableArea(double x, double y, double width, double height) {
        this.mImageableX = x;
        this.mImageableY = y;
        this.mImageableWidth = width;
        this.mImageableHeight = height;
    }

    /** Borde izquierdo del area imprimible. */
    public double getImageableX() {
        return this.mImageableX;
    }

    /** Borde superior. */
    public double getImageableY() {
        return this.mImageableY;
    }

    /** Ancho del area imprimible. */
    public double getImageableWidth() {
        return this.mImageableWidth;
    }

    /** Alto del area imprimible. */
    public double getImageableHeight() {
        return this.mImageableHeight;
    }
}
