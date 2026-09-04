package java.awt.print;

/**
 * KajiLibrary's java.awt.print.PageFormat -- un {@link Paper} mas la orientacion.
 *
 * <p>La clase existe porque la orientacion no es una propiedad del papel: la hoja siempre entra en la
 * impresora igual, y lo que rota es el <b>dibujo</b>. Por eso todos los accesores de esta clase
 * traducen: {@link #getWidth} devuelve el ancho <i>tal como lo ve quien dibuja</i>, que en apaisado es
 * el alto de la hoja.
 *
 * <h2>Las dos orientaciones apaisadas</h2>
 *
 * <p>{@link #LANDSCAPE} y {@link #REVERSE_LANDSCAPE} giran para el lado contrario. Existen las dos
 * porque el borde de encuadernado queda en lados opuestos, y eso importa cuando el trabajo se va a
 * abrochar o a imprimir de los dos lados.
 *
 * <p>Los valores sorprenden: {@code LANDSCAPE} vale 0 y {@code PORTRAIT} vale 1. No hay razon, quedo
 * asi, y por eso nunca hay que asumir que el 0 es el vertical.
 *
 * <h2>{@link #getMatrix}</h2>
 *
 * <p>Devuelve los seis numeros de la transformacion que lleva del sistema de coordenadas de quien
 * dibuja al de la hoja. Es lo que un {@code Graphics2D} necesita para que dibujar en apaisado no exija
 * pensar en rotaciones.
 *
 * <h2>Copia lo que entra y lo que sale</h2>
 *
 * <p>{@link #getPaper} devuelve una copia y {@link #setPaper} guarda una copia. Cambiar el papel que
 * devolvio {@code getPaper} no cambia nada; hay que volver a pasarlo con {@code setPaper}. Es un
 * tropiezo clasico y es a proposito: sin eso, el formato de pagina de un trabajo en curso podria
 * cambiar por debajo.
 */
public class PageFormat implements Cloneable {

    /** Apaisado. Vale 0; ver la nota de la clase. */
    public static final int LANDSCAPE = 0;

    /** Vertical. Vale 1. */
    public static final int PORTRAIT = 1;

    /** Apaisado para el otro lado. */
    public static final int REVERSE_LANDSCAPE = 2;

    /** La hoja. */
    private Paper mPaper;

    /** Cual de las tres. */
    private int mOrientation = PORTRAIT;

    /** Una carta vertical. */
    public PageFormat() {
        this.mPaper = new Paper();
    }

    /** Una copia independiente, con su propio papel. */
    @Override
    public Object clone() {
        PageFormat copy;
        try {
            copy = (PageFormat) super.clone();
        } catch (CloneNotSupportedException e) {
            // PageFormat es Cloneable.
            throw new InternalError(e);
        }
        copy.mPaper = (Paper) this.mPaper.clone();
        return copy;
    }

    /** El ancho tal como lo ve quien dibuja. Ver la nota de la clase. */
    public double getWidth() {
        if (this.mOrientation == PORTRAIT) {
            return this.mPaper.getWidth();
        }
        return this.mPaper.getHeight();
    }

    /** El alto tal como lo ve quien dibuja. */
    public double getHeight() {
        if (this.mOrientation == PORTRAIT) {
            return this.mPaper.getHeight();
        }
        return this.mPaper.getWidth();
    }

    /** Borde izquierdo del area imprimible, ya rotado. */
    public double getImageableX() {
        if (this.mOrientation == LANDSCAPE) {
            return this.mPaper.getHeight()
                - (this.mPaper.getImageableY() + this.mPaper.getImageableHeight());
        }
        if (this.mOrientation == REVERSE_LANDSCAPE) {
            return this.mPaper.getImageableY();
        }
        return this.mPaper.getImageableX();
    }

    /** Borde superior, ya rotado. */
    public double getImageableY() {
        if (this.mOrientation == LANDSCAPE) {
            return this.mPaper.getImageableX();
        }
        if (this.mOrientation == REVERSE_LANDSCAPE) {
            return this.mPaper.getWidth()
                - (this.mPaper.getImageableX() + this.mPaper.getImageableWidth());
        }
        return this.mPaper.getImageableY();
    }

    /** Ancho del area imprimible, ya rotado. */
    public double getImageableWidth() {
        if (this.mOrientation == PORTRAIT) {
            return this.mPaper.getImageableWidth();
        }
        return this.mPaper.getImageableHeight();
    }

    /** Alto del area imprimible, ya rotado. */
    public double getImageableHeight() {
        if (this.mOrientation == PORTRAIT) {
            return this.mPaper.getImageableHeight();
        }
        return this.mPaper.getImageableWidth();
    }

    /** Una copia de la hoja. Ver la nota de la clase. */
    public Paper getPaper() {
        return (Paper) this.mPaper.clone();
    }

    /** Guarda una copia de esa hoja. */
    public void setPaper(Paper paper) {
        this.mPaper = (Paper) paper.clone();
    }

    /**
     * Cambia la orientacion.
     *
     * @throws IllegalArgumentException si no es una de las tres constantes
     */
    public void setOrientation(int orientation) throws IllegalArgumentException {
        if (orientation < LANDSCAPE || orientation > REVERSE_LANDSCAPE) {
            throw new IllegalArgumentException();
        }
        this.mOrientation = orientation;
    }

    /** Cual de las tres. */
    public int getOrientation() {
        return this.mOrientation;
    }

    /**
     * La transformacion de coordenadas de dibujo a coordenadas de hoja.
     *
     * <p>Seis numeros en el orden de {@code AffineTransform}: escala x, sesgo y, sesgo x, escala y,
     * traslacion x, traslacion y. Ver la nota de la clase.
     */
    public double[] getMatrix() {
        double[] matrix = new double[6];
        if (this.mOrientation == LANDSCAPE) {
            matrix[0] = 0.0;
            matrix[1] = -1.0;
            matrix[2] = 1.0;
            matrix[3] = 0.0;
            matrix[4] = 0.0;
            matrix[5] = this.mPaper.getHeight();
        } else if (this.mOrientation == REVERSE_LANDSCAPE) {
            matrix[0] = 0.0;
            matrix[1] = 1.0;
            matrix[2] = -1.0;
            matrix[3] = 0.0;
            matrix[4] = this.mPaper.getWidth();
            matrix[5] = 0.0;
        } else {
            matrix[0] = 1.0;
            matrix[1] = 0.0;
            matrix[2] = 0.0;
            matrix[3] = 1.0;
            matrix[4] = 0.0;
            matrix[5] = 0.0;
        }
        return matrix;
    }
}
