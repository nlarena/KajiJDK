package java.awt.image.renderable;

import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Vector;

/**
 * KajiLibrary's java.awt.image.renderable.RenderableImageProducer -- el puente al AWT viejo.
 *
 * <p>Convierte una {@link RenderableImage} en un {@link ImageProducer}, que es lo que entiende el
 * {@code java.awt.Image} de 1995. Existe para que una cadena de operaciones se pueda mostrar con las
 * herramientas que ya habia, y por eso el paquete lo trae aunque los dos modelos no se parezcan en
 * nada.
 *
 * <p>Los dos modelos empujan la imagen en direcciones opuestas y ahi esta la incomodidad: el
 * renderizable es <b>por pedido</b> --nadie calcula nada hasta que se pide-- y el productor es
 * <b>por empuje</b>, le manda los pixeles a quien escuche. Esta clase junta los dos renderizando
 * todo de una y mandando el resultado linea por linea.
 *
 * <h2>Corre en el hilo de quien llama</h2>
 *
 * <p>Implementa {@link Runnable} y {@link #startProduction} no arranca ningun hilo: llama a
 * {@link #run} directo. Es una decision y hay que saberla --renderizar puede tardar mucho, y quien
 * quiera que sea en otro hilo tiene que armarlo por su cuenta con este mismo objeto--. Hacerlo al
 * reves seria peor: un hilo que nadie pidio y que nadie sabe esperar.
 *
 * <p>{@link #requestTopDownLeftRightResend} no hace nada, y no es una omision: la produccion ya es
 * de arriba hacia abajo y de una sola pasada, asi que no hay nada que reenviar de otra forma.
 */
public class RenderableImageProducer implements ImageProducer, Runnable {

    /** La imagen a producir. */
    private RenderableImage rdblImage;

    /** Con que contexto renderizarla; null para el de por omision. */
    private RenderContext rc;

    /** Quienes escuchan. */
    private Vector<ImageConsumer> ics = new Vector<ImageConsumer>();

    /**
     * @param rdblImage la imagen
     * @param rc el contexto, o null para {@link RenderableImage#createDefaultRendering}
     */
    public RenderableImageProducer(RenderableImage rdblImage, RenderContext rc) {
        this.rdblImage = rdblImage;
        this.rc = rc;
    }

    /** Cambia el contexto. Afecta a la proxima produccion, no a una en curso. */
    public synchronized void setRenderContext(RenderContext rc) {
        this.rc = rc;
    }

    /** Agrega un consumidor, si no estaba. */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (!this.ics.contains(ic)) {
            this.ics.addElement(ic);
        }
    }

    /** Si ese consumidor esta anotado. */
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return this.ics.contains(ic);
    }

    /** Lo saca. Si no estaba, no hace nada. */
    public synchronized void removeConsumer(ImageConsumer ic) {
        this.ics.removeElement(ic);
    }

    /**
     * Lo agrega y produce.
     *
     * <p>Produce para <b>todos</b> los anotados y no solo para este; ver {@link #run}.
     */
    public synchronized void startProduction(ImageConsumer ic) {
        addConsumer(ic);
        // En el hilo de quien llama; ver la nota de la clase.
        run();
    }

    /** No hace nada: la produccion ya es de arriba abajo y de una pasada. */
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
    }

    /**
     * Renderiza y manda el resultado, linea por linea.
     *
     * <p>Linea por linea y no de una: un consumidor puede ir dibujando lo que le llega, y mandar
     * todo junto obligaria a tener dos copias de la imagen en memoria.
     */
    public void run() {
        RenderedImage rendered;
        if (this.rc != null) {
            rendered = this.rdblImage.createRendering(this.rc);
        } else {
            rendered = this.rdblImage.createDefaultRendering();
        }
        if (rendered == null) {
            // No se pudo renderizar: se avisa el fallo en vez de quedarse callado.
            int k = 0;
            while (k < this.ics.size()) {
                this.ics.elementAt(k).imageComplete(ImageConsumer.IMAGEERROR);
                k = k + 1;
            }
            return;
        }
        ColorModel colorModel = rendered.getColorModel();
        Raster raster = rendered.getData();
        SampleModel sampleModel = raster.getSampleModel();
        DataBuffer dataBuffer = raster.getDataBuffer();
        if (colorModel == null) {
            colorModel = ColorModel.getRGBdefault();
        }
        int width = raster.getWidth();
        int height = raster.getHeight();

        int i = 0;
        while (i < this.ics.size()) {
            ImageConsumer ic = this.ics.elementAt(i);
            ic.setDimensions(width, height);
            ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                | ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME);
            i = i + 1;
        }

        int[] line = new int[width];
        int[] pixel = new int[sampleModel.getNumBands()];
        int y = 0;
        while (y < height) {
            int x = 0;
            while (x < width) {
                sampleModel.getPixel(x, y, pixel, dataBuffer);
                line[x] = colorModel.getDataElement(pixel, 0);
                x = x + 1;
            }
            int j = 0;
            while (j < this.ics.size()) {
                this.ics.elementAt(j).setPixels(0, y, width, 1, colorModel, line, 0, width);
                j = j + 1;
            }
            y = y + 1;
        }

        int k = 0;
        while (k < this.ics.size()) {
            this.ics.elementAt(k).imageComplete(ImageConsumer.STATICIMAGEDONE);
            k = k + 1;
        }
    }
}
