package javax.imageio;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * KajiLibrary's javax.imageio.ImageReadParam -- como leer una imagen.
 *
 * <p>Agrega sobre {@link IIOParam} lo que solo tiene sentido al leer: donde escribir el resultado, que
 * bandas del destino usar, el tamano de renderizado, y hasta que pasada leer.
 *
 * <h2>{@link #setDestination} contra {@code setDestinationType}</h2>
 *
 * <p>Los dos dicen algo sobre el destino y no son lo mismo:
 *
 * <ul>
 *   <li>el <b>tipo</b> ({@link IIOParam#setDestinationType}) dice de que clase crear la imagen;
 *   <li>el <b>destino</b> ({@link #setDestination}) da una imagen <b>que ya existe</b> y en la que hay
 *       que escribir.
 * </ul>
 *
 * <p>Dar los dos es contradictorio, y por eso {@link #setDestinationType} redefinido aca lo permite:
 * el que gana es el destino. Reusar una imagen es lo que permite leer una animacion cuadro a cuadro
 * sin alocar uno nuevo cada vez.
 *
 * <h2>El tamano de renderizado</h2>
 *
 * <p>{@link #setSourceRenderSize} solo funciona en formatos que <b>escalan mientras decodifican</b>
 * --los vectoriales, o los progresivos con niveles--. Por eso hay que preguntar antes con
 * {@link #canSetSourceRenderSize}: pedirlo cuando no se puede lanza
 * {@link UnsupportedOperationException}.
 *
 * <p>No es lo mismo que submuestrear: el submuestreo tira pixeles, esto le pide al decodificador que
 * produzca directamente el tamano que se quiere, que suele salir bastante mejor.
 *
 * <h2>Las pasadas progresivas</h2>
 *
 * <p>Un JPEG progresivo se decodifica en pasadas, cada una mas nitida. {@link #setSourceProgressivePasses}
 * permite cortar antes: leer solo las primeras da una imagen borrosa en una fraccion del tiempo, que
 * es exactamente lo que sirve para una vista previa.
 */
public class ImageReadParam extends IIOParam {

    /** Si este lector sabe escalar mientras decodifica. */
    protected boolean canSetSourceRenderSize = false;

    /** A que tamano renderizar, o null. */
    protected Dimension sourceRenderSize = null;

    /** Donde escribir, o null para que se cree una. */
    protected BufferedImage destination = null;

    /** Que bandas del destino usar, o null para todas. */
    protected int[] destinationBands = null;

    /** Desde que pasada. */
    protected int minProgressivePass = 0;

    /** Cuantas pasadas leer; {@link Integer#MAX_VALUE} son todas. */
    protected int numProgressivePasses = Integer.MAX_VALUE;

    /** Todo por omision. */
    public ImageReadParam() {
    }

    /**
     * De que tipo crear el destino.
     *
     * <p>Redefinido solo para documentar que pierde contra {@link #setDestination}; ver la nota de la
     * clase.
     */
    @Override
    public void setDestinationType(ImageTypeSpecifier destinationType) {
        super.setDestinationType(destinationType);
        setDestination(null);
        setDestinationBands(null);
    }

    /** Donde escribir; null hace que el lector cree una imagen. */
    public void setDestination(BufferedImage destination) {
        this.destination = destination;
    }

    /** Donde escribir, o null. */
    public BufferedImage getDestination() {
        return this.destination;
    }

    /**
     * Que bandas del destino escribir; null son todas.
     *
     * <p>Mismas reglas que {@link IIOParam#setSourceBands}: sin repetidos y sin negativos.
     *
     * @throws IllegalArgumentException si esta vacio, tiene negativos o repite alguna
     */
    public void setDestinationBands(int[] destinationBands) {
        if (destinationBands == null) {
            this.destinationBands = null;
            return;
        }
        int numBands = destinationBands.length;
        if (numBands == 0) {
            throw new IllegalArgumentException("destinationBands.length == 0!");
        }
        int i = 0;
        while (i < numBands) {
            int band = destinationBands[i];
            if (band < 0) {
                throw new IllegalArgumentException("destinationBands[" + i + "] < 0!");
            }
            int j = i + 1;
            while (j < numBands) {
                if (band == destinationBands[j]) {
                    throw new IllegalArgumentException("Duplicate band value!");
                }
                j = j + 1;
            }
            i = i + 1;
        }
        this.destinationBands = new int[numBands];
        System.arraycopy(destinationBands, 0, this.destinationBands, 0, numBands);
    }

    /** Que bandas del destino, o null. Es una copia. */
    public int[] getDestinationBands() {
        if (this.destinationBands == null) {
            return null;
        }
        int[] copy = new int[this.destinationBands.length];
        System.arraycopy(this.destinationBands, 0, copy, 0, this.destinationBands.length);
        return copy;
    }

    /** Si este lector sabe escalar mientras decodifica. Ver la nota de la clase. */
    public boolean canSetSourceRenderSize() {
        return this.canSetSourceRenderSize;
    }

    /**
     * A que tamano renderizar; null vuelve al natural.
     *
     * @throws UnsupportedOperationException si este lector no sabe hacerlo
     * @throws IllegalArgumentException si el ancho o el alto no son positivos
     */
    public void setSourceRenderSize(Dimension size) throws UnsupportedOperationException {
        if (!canSetSourceRenderSize()) {
            throw new UnsupportedOperationException("Can't set source render size!");
        }
        if (size == null) {
            this.sourceRenderSize = null;
            return;
        }
        if (size.width <= 0 || size.height <= 0) {
            throw new IllegalArgumentException("width or height <= 0!");
        }
        this.sourceRenderSize = (Dimension) size.clone();
    }

    /** A que tamano, o null. Es una copia. */
    public Dimension getSourceRenderSize() {
        if (this.sourceRenderSize == null) {
            return null;
        }
        return (Dimension) this.sourceRenderSize.clone();
    }

    /**
     * Que pasadas progresivas leer. Ver la nota de la clase.
     *
     * @param minPass la primera, desde 0
     * @param numPasses cuantas; {@link Integer#MAX_VALUE} son todas las que haya
     * @throws IllegalArgumentException si la primera es negativa, si la cantidad no es positiva, o si
     *     la suma se desborda
     */
    public void setSourceProgressivePasses(int minPass, int numPasses) {
        if (minPass < 0) {
            throw new IllegalArgumentException("minPass < 0!");
        }
        if (numPasses <= 0) {
            throw new IllegalArgumentException("numPasses <= 0!");
        }
        if (numPasses != Integer.MAX_VALUE && minPass + numPasses - 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("minPass + numPasses - 1 > INTEGER.MAX_VALUE!");
        }
        this.minProgressivePass = minPass;
        this.numProgressivePasses = numPasses;
    }

    /** Desde que pasada. */
    public int getSourceMinProgressivePass() {
        return this.minProgressivePass;
    }

    /**
     * Hasta cual.
     *
     * <p>{@link Integer#MAX_VALUE} significa "todas las que haya", no una pasada numero dos mil
     * millones.
     */
    public int getSourceMaxProgressivePass() {
        if (this.numProgressivePasses == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return getSourceMinProgressivePass() + this.numProgressivePasses - 1;
    }

    /** Cuantas. */
    public int getSourceNumProgressivePasses() {
        return this.numProgressivePasses;
    }
}
