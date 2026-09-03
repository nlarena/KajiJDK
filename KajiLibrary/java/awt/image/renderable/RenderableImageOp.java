package java.awt.image.renderable;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.Vector;

/**
 * KajiLibrary's java.awt.image.renderable.RenderableImageOp -- un nodo del arbol de operaciones.
 *
 * <p>Junta una operacion --{@link ContextualRenderedImageFactory}-- con sus argumentos --un
 * {@link ParameterBlock}-- y se presenta como una {@link RenderableImage} mas. Con eso, una cadena
 * de operaciones es simplemente un arbol de estos, y cada uno no sabe nada de los demas salvo que
 * son fuentes.
 *
 * <h2>Nada se calcula hasta que alguien pide pixeles</h2>
 *
 * <p>El constructor no renderiza. Ni siquiera {@link #getWidth} renderiza: el tamano se lo pregunta
 * a la operacion, que lo sabe calcular sin producir un solo pixel. Recien
 * {@link #createRendering} dispara trabajo, y lo hace <b>hacia atras</b>: le pregunta a la operacion
 * que necesita de cada fuente, se lo pide a la fuente, y con lo que vuelve arma el resultado.
 *
 * <p>Esa es la parte que hay que entender del paquete: el pedido baja por el arbol traduciendose en
 * cada nivel, y los pixeles suben. Sin la traduccion --{@link ContextualRenderedImageFactory#mapRenderContext}--
 * habria que calcular cada nivel entero.
 *
 * <h2>El bloque se copia al renderizar</h2>
 *
 * <p>Al renderizar, las fuentes que son renderizables se reemplazan por lo que devolvieron, y eso se
 * hace sobre una <b>copia</b> del bloque. Tiene que ser asi: el mismo nodo se puede renderizar dos
 * veces a resoluciones distintas, y pisar sus fuentes en la primera dejaria la segunda mirando
 * pixeles de la anterior.
 */
public class RenderableImageOp implements RenderableImage {

    /** La operacion. */
    private ContextualRenderedImageFactory crif;

    /** Sus argumentos. */
    private ParameterBlock paramBlock;

    /** El rectangulo en coordenadas de usuario; se pide una sola vez y se recuerda. */
    private Rectangle2D boundingBox;

    /**
     * @param crif la operacion
     * @param paramBlock sus argumentos; se guarda una copia, para que cambiarlo despues no cambie
     *     este nodo por atras
     */
    public RenderableImageOp(ContextualRenderedImageFactory crif, ParameterBlock paramBlock) {
        this.crif = crif;
        this.paramBlock = (ParameterBlock) paramBlock.clone();
    }

    /** Las fuentes que son renderizables; las que no, no entran. */
    public Vector<RenderableImage> getSources() {
        return getRenderableSources();
    }

    /** El recorrido comun; ver {@link #getSources}. */
    private Vector<RenderableImage> getRenderableSources() {
        Vector<RenderableImage> sources = null;
        if (this.paramBlock.getNumSources() > 0) {
            sources = new Vector<RenderableImage>();
            int i = 0;
            while (i < this.paramBlock.getNumSources()) {
                Object o = this.paramBlock.getSource(i);
                if (o instanceof RenderableImage) {
                    sources.addElement((RenderableImage) o);
                }
                i = i + 1;
            }
            if (sources.size() == 0) {
                sources = null;
            }
        }
        return sources;
    }

    /** Se la pregunta a la operacion, que la calcula sin renderizar. */
    public Object getProperty(String name) {
        return this.crif.getProperty(this.paramBlock, name);
    }

    /** Ver {@link #getProperty}. */
    public String[] getPropertyNames() {
        return this.crif.getPropertyNames();
    }

    /** Lo que conteste la operacion. */
    public boolean isDynamic() {
        return this.crif.isDynamic();
    }

    /** El ancho en coordenadas de usuario, sin renderizar nada. */
    public float getWidth() {
        if (this.boundingBox == null) {
            this.boundingBox = this.crif.getBounds2D(this.paramBlock);
        }
        return (float) this.boundingBox.getWidth();
    }

    /** El alto en coordenadas de usuario. */
    public float getHeight() {
        if (this.boundingBox == null) {
            this.boundingBox = this.crif.getBounds2D(this.paramBlock);
        }
        return (float) this.boundingBox.getHeight();
    }

    /** El borde izquierdo en coordenadas de usuario. */
    public float getMinX() {
        if (this.boundingBox == null) {
            this.boundingBox = this.crif.getBounds2D(this.paramBlock);
        }
        return (float) this.boundingBox.getMinX();
    }

    /** El borde superior en coordenadas de usuario. */
    public float getMinY() {
        if (this.boundingBox == null) {
            this.boundingBox = this.crif.getBounds2D(this.paramBlock);
        }
        return (float) this.boundingBox.getMinY();
    }

    /**
     * Cambia los argumentos.
     *
     * @return los que habia antes
     */
    public ParameterBlock setParameterBlock(ParameterBlock paramBlock) {
        ParameterBlock previous = this.paramBlock;
        this.paramBlock = (ParameterBlock) paramBlock.clone();
        // El rectangulo dependia de los argumentos viejos: hay que volver a preguntarlo.
        this.boundingBox = null;
        return previous;
    }

    /** Una copia de los argumentos. */
    public ParameterBlock getParameterBlock() {
        return this.paramBlock;
    }

    /**
     * Renderiza a un tamano en pixeles.
     *
     * <p>Un 0 en ancho o alto significa "el que salga manteniendo la proporcion". Los dos en 0 no
     * significa nada y se rechaza: no hay resolucion que deducir.
     *
     * @throws IllegalArgumentException si los dos son 0, o si alguno es negativo
     */
    public RenderedImage createScaledRendering(int w, int h, RenderingHints hints) {
        if (w < 0 || h < 0) {
            throw new IllegalArgumentException("width and height cannot be negative");
        }
        if (w == 0 && h == 0) {
            throw new IllegalArgumentException("width and height cannot both be zero");
        }
        float width = getWidth();
        float height = getHeight();
        int targetWidth = w;
        int targetHeight = h;
        if (targetWidth == 0) {
            targetWidth = Math.round(h * (width / height));
        }
        if (targetHeight == 0) {
            targetHeight = Math.round(w * (height / width));
        }
        double sx = targetWidth / (double) width;
        double sy = targetHeight / (double) height;
        AffineTransform usr2dev = AffineTransform.getScaleInstance(sx, sy);
        return createRendering(new RenderContext(usr2dev, hints));
    }

    /** Renderiza sin escalar: una unidad de usuario, un pixel. */
    public RenderedImage createDefaultRendering() {
        return createRendering(new RenderContext(new AffineTransform()));
    }

    /**
     * Renderiza con control completo.
     *
     * <p>Aca es donde el pedido baja por el arbol; ver la nota de la clase.
     *
     * @return null si alguna fuente no pudo producir lo que se le pidio
     */
    public RenderedImage createRendering(RenderContext renderContext) {
        // Copia: el mismo nodo se puede renderizar dos veces. Ver la nota de la clase.
        ParameterBlock rendered = (ParameterBlock) this.paramBlock.clone();
        Vector<Object> sources = this.paramBlock.getSources();
        if (sources != null && sources.size() > 0) {
            Vector<Object> renderedSources = new Vector<Object>();
            int i = 0;
            while (i < sources.size()) {
                Object o = sources.elementAt(i);
                if (o instanceof RenderableImage) {
                    RenderContext forSource =
                        this.crif.mapRenderContext(i, renderContext, this.paramBlock, this);
                    RenderedImage produced = ((RenderableImage) o).createRendering(forSource);
                    if (produced == null) {
                        return null;
                    }
                    renderedSources.addElement(produced);
                } else {
                    renderedSources.addElement(o);
                }
                i = i + 1;
            }
            rendered.setSources(renderedSources);
        }
        return this.crif.create(renderContext, rendered);
    }
}
