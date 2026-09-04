package javax.imageio;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * KajiLibrary's javax.imageio.IIOParam -- que parte de la imagen y como.
 *
 * <p>Lo que comparten {@link ImageReadParam} y {@link ImageWriteParam}: recortar, submuestrear, elegir
 * bandas y desplazar el destino.
 *
 * <h2>El submuestreo tiene desplazamiento, y por eso son cuatro numeros</h2>
 *
 * <p>{@link #setSourceSubsampling} toma el paso en X e Y <b>y ademas</b> desde que pixel arrancar. Con
 * un paso de 2 y desplazamiento 0 se toman los pixeles pares; con desplazamiento 1, los impares.
 *
 * <p>Eso es lo que permite leer una imagen enorme en cuatro pasadas que juntas la cubren entera, o
 * generar una miniatura sin cargar todo. Los desplazamientos tienen que ser <b>menores</b> que el
 * paso: si no, se saltearia el primer bloque entero.
 *
 * <h2>Recortar y submuestrear se combinan en ese orden</h2>
 *
 * <p>Primero se recorta a la region, y despues se submuestrea dentro del recorte -- y el
 * desplazamiento del submuestreo se cuenta desde el borde de la region, no de la imagen.
 *
 * <p>{@link #setSourceRegion} con null quita el recorte, igual que {@link #setSourceBands} con null
 * vuelve a todas las bandas. Es el convenio de toda la clase: null significa "lo de siempre".
 *
 * <h2>{@link #setDestinationOffset}</h2>
 *
 * <p>Donde poner lo leido dentro de la imagen destino. Es como se arma un mosaico leyendo pedazos de
 * varias imagenes en una sola.
 */
public abstract class IIOParam {

    /** Que pedazo de la fuente, o null para toda. */
    protected Rectangle sourceRegion = null;

    /** Cada cuantos pixeles en X. */
    protected int sourceXSubsampling = 1;

    /** Cada cuantos en Y. */
    protected int sourceYSubsampling = 1;

    /** Desde que pixel arrancar en X. Ver la nota de la clase. */
    protected int subsamplingXOffset = 0;

    /** Idem en Y. */
    protected int subsamplingYOffset = 0;

    /** Que bandas, o null para todas. */
    protected int[] sourceBands = null;

    /** De que tipo tiene que ser el destino, o null. */
    protected ImageTypeSpecifier destinationType = null;

    /** Donde ubicar lo leido en el destino. */
    protected Point destinationOffset = new Point(0, 0);

    /** El controlador de fabrica, o null. */
    protected IIOParamController defaultController = null;

    /** El que esta puesto ahora. */
    protected IIOParamController controller = null;

    /** Para las subclases. */
    protected IIOParam() {
    }

    /**
     * Fija el recorte de la fuente; null lo quita.
     *
     * @throws IllegalArgumentException si el ancho o el alto son cero o negativos, o si la esquina es
     *     negativa
     */
    public void setSourceRegion(Rectangle sourceRegion) {
        if (sourceRegion != null) {
            if (sourceRegion.x < 0) {
                throw new IllegalArgumentException("sourceRegion.x < 0!");
            }
            if (sourceRegion.y < 0) {
                throw new IllegalArgumentException("sourceRegion.y < 0!");
            }
            if (sourceRegion.width <= 0) {
                throw new IllegalArgumentException("sourceRegion.width <= 0!");
            }
            if (sourceRegion.height <= 0) {
                throw new IllegalArgumentException("sourceRegion.height <= 0!");
            }
            // Un recorte tan chico que el submuestreo no alcanzaria a tomar ni un pixel no es un
            // recorte valido: el resultado seria una imagen vacia.
            if (sourceRegion.width <= this.subsamplingXOffset) {
                throw new IllegalArgumentException("sourceRegion.width <= subsamplingXOffset!");
            }
            if (sourceRegion.height <= this.subsamplingYOffset) {
                throw new IllegalArgumentException("sourceRegion.height <= subsamplingYOffset!");
            }
            this.sourceRegion = (Rectangle) sourceRegion.clone();
        } else {
            this.sourceRegion = null;
        }
    }

    /** El recorte, o null. Es una copia. */
    public Rectangle getSourceRegion() {
        if (this.sourceRegion == null) {
            return null;
        }
        return (Rectangle) this.sourceRegion.clone();
    }

    /**
     * Fija el submuestreo. Ver la nota de la clase.
     *
     * @param sourceXSubsampling cada cuantos pixeles en X; al menos 1
     * @param subsamplingXOffset desde cual arrancar; menor que el paso
     * @throws IllegalArgumentException si los pasos no son positivos, si los desplazamientos son
     *     negativos o no menores que su paso, o si el recorte no da para tanto
     */
    public void setSourceSubsampling(int sourceXSubsampling, int sourceYSubsampling,
                                     int subsamplingXOffset, int subsamplingYOffset) {
        if (sourceXSubsampling <= 0) {
            throw new IllegalArgumentException("sourceXSubsampling <= 0!");
        }
        if (sourceYSubsampling <= 0) {
            throw new IllegalArgumentException("sourceYSubsampling <= 0!");
        }
        if (subsamplingXOffset < 0 || subsamplingXOffset >= sourceXSubsampling) {
            throw new IllegalArgumentException("subsamplingXOffset out of range!");
        }
        if (subsamplingYOffset < 0 || subsamplingYOffset >= sourceYSubsampling) {
            throw new IllegalArgumentException("subsamplingYOffset out of range!");
        }
        if (this.sourceRegion != null) {
            if (subsamplingXOffset >= this.sourceRegion.width
                || subsamplingYOffset >= this.sourceRegion.height) {
                throw new IllegalArgumentException("region contains no pixels!");
            }
        }
        this.sourceXSubsampling = sourceXSubsampling;
        this.sourceYSubsampling = sourceYSubsampling;
        this.subsamplingXOffset = subsamplingXOffset;
        this.subsamplingYOffset = subsamplingYOffset;
    }

    /** Cada cuantos pixeles en X. */
    public int getSourceXSubsampling() {
        return this.sourceXSubsampling;
    }

    /** Cada cuantos en Y. */
    public int getSourceYSubsampling() {
        return this.sourceYSubsampling;
    }

    /** Desde cual arrancar en X. */
    public int getSubsamplingXOffset() {
        return this.subsamplingXOffset;
    }

    /** Idem en Y. */
    public int getSubsamplingYOffset() {
        return this.subsamplingYOffset;
    }

    /**
     * Que bandas de la fuente usar; null son todas.
     *
     * <p>El arreglo se copia, y se comprueba que no tenga repetidos: una banda pedida dos veces no
     * significa nada y casi siempre es un error de indice.
     *
     * <p>Un arreglo vacio se acepta, aunque no signifique nada util: es lo que hace el JDK.
     *
     * @throws IllegalArgumentException si tiene negativos o repite alguna
     */
    public void setSourceBands(int[] sourceBands) {
        if (sourceBands == null) {
            this.sourceBands = null;
            return;
        }
        int numBands = sourceBands.length;
        int i = 0;
        while (i < numBands) {
            int band = sourceBands[i];
            if (band < 0) {
                throw new IllegalArgumentException("sourceBands[" + i + "] < 0!");
            }
            int j = i + 1;
            while (j < numBands) {
                if (band == sourceBands[j]) {
                    throw new IllegalArgumentException("Duplicate band value!");
                }
                j = j + 1;
            }
            i = i + 1;
        }
        this.sourceBands = new int[numBands];
        System.arraycopy(sourceBands, 0, this.sourceBands, 0, numBands);
    }

    /** Que bandas, o null. Es una copia. */
    public int[] getSourceBands() {
        if (this.sourceBands == null) {
            return null;
        }
        int[] copy = new int[this.sourceBands.length];
        System.arraycopy(this.sourceBands, 0, copy, 0, this.sourceBands.length);
        return copy;
    }

    /** De que tipo tiene que ser el destino; null lo deja a criterio del lector. */
    public void setDestinationType(ImageTypeSpecifier destinationType) {
        this.destinationType = destinationType;
    }

    /** De que tipo, o null. */
    public ImageTypeSpecifier getDestinationType() {
        return this.destinationType;
    }

    /**
     * Donde ubicar lo leido en el destino. Ver la nota de la clase.
     *
     * <p>Las coordenadas negativas se permiten: es como se descarta la parte de arriba o de la
     * izquierda de lo leido.
     *
     * @throws IllegalArgumentException si es null
     */
    public void setDestinationOffset(Point destinationOffset) {
        if (destinationOffset == null) {
            throw new IllegalArgumentException("destinationOffset == null!");
        }
        this.destinationOffset = (Point) destinationOffset.clone();
    }

    /** Donde ubicarlo. Es una copia. */
    public Point getDestinationOffset() {
        return (Point) this.destinationOffset.clone();
    }

    /** Quien completa este parametro; null usa el de fabrica. */
    public void setController(IIOParamController controller) {
        this.controller = controller;
    }

    /** El que esta puesto. */
    public IIOParamController getController() {
        return this.controller;
    }

    /** El de fabrica, o null. */
    public IIOParamController getDefaultController() {
        return this.defaultController;
    }

    /** Si hay alguno puesto. */
    public boolean hasController() {
        return getController() != null;
    }

    /**
     * Le pide al controlador que complete este parametro.
     *
     * @return si el usuario acepto
     * @throws IllegalStateException si no hay controlador
     */
    public boolean activateController() {
        if (!hasController()) {
            throw new IllegalStateException("hasController() == false!");
        }
        return getController().activate(this);
    }
}
