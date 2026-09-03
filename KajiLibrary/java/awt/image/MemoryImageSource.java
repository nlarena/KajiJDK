package java.awt.image;

import java.util.Hashtable;
import java.util.Vector;

/**
 * Un productor cuya imagen **ya está** en un arreglo en memoria.
 *
 * <p>Es el extremo simple de la tubería: no hay nada que descargar ni que decodificar, así que la
 * entrega es una sola llamada con el arreglo entero. Sirve para hacer una imagen a partir de píxeles
 * calculados.
 *
 * <p>También sirve para lo contrario de lo que su nombre sugiere: con {@link #setAnimated} el mismo
 * objeto pasa a ser una imagen **viva**. Los consumidores no se dan de baja al terminar y cada
 * {@link #newPixels()} les vuelve a mandar lo que haya en el arreglo, así que escribir en el arreglo
 * y avisar es todo lo que hace falta para animar.
 *
 * <p>{@link #setFullBufferUpdates} decide si cada actualización manda la imagen entera o sólo el
 * rectángulo que cambió. Mandar de más cuesta ancho de banda; mandar de menos obliga al consumidor a
 * aceptar píxeles en cualquier orden, y eso le impide a un filtro como
 * {@link AreaAveragingScaleFilter} hacer su trabajo. Por eso la decisión se declara: cambia las
 * pistas que reciben los consumidores.
 *
 * <p>El arreglo no se copia. Escribirlo cambia lo que van a ver los próximos consumidores, que es
 * justamente lo que hace posible la animación, y también lo que hace que compartirlo entre hilos sin
 * cuidado sea un problema.
 */
public class MemoryImageSource implements ImageProducer {

    private int width;
    private int height;
    private ColorModel model;
    private Object pixels;
    private int pixeloffset;
    private int pixelscan;
    private Hashtable<?, ?> properties;
    private final Vector<ImageConsumer> theConsumers = new Vector<ImageConsumer>();
    private boolean animating;
    private boolean fullbuffers;

    /** Con píxeles de un byte y el modelo de color dado. */
    public MemoryImageSource(int w, int h, ColorModel cm, byte[] pix, int off, int scan) {
        this.initialize(w, h, cm, pix, off, scan, null);
    }

    /** Como el anterior, con propiedades. */
    public MemoryImageSource(int w, int h, ColorModel cm, byte[] pix, int off, int scan,
            Hashtable<?, ?> props) {
        this.initialize(w, h, cm, pix, off, scan, props);
    }

    /** Con píxeles de un `int` y el modelo de color dado. */
    public MemoryImageSource(int w, int h, ColorModel cm, int[] pix, int off, int scan) {
        this.initialize(w, h, cm, pix, off, scan, null);
    }

    /** Como el anterior, con propiedades. */
    public MemoryImageSource(int w, int h, ColorModel cm, int[] pix, int off, int scan,
            Hashtable<?, ?> props) {
        this.initialize(w, h, cm, pix, off, scan, props);
    }

    /** Con píxeles ARGB de ocho bits por canal. */
    public MemoryImageSource(int w, int h, int[] pix, int off, int scan) {
        this.initialize(w, h, ColorModel.getRGBdefault(), pix, off, scan, null);
    }

    /** Como el anterior, con propiedades. */
    public MemoryImageSource(int w, int h, int[] pix, int off, int scan, Hashtable<?, ?> props) {
        this.initialize(w, h, ColorModel.getRGBdefault(), pix, off, scan, props);
    }

    /** Guarda todo lo que describe a la imagen. */
    private void initialize(int w, int h, ColorModel cm, Object pix, int off, int scan,
            Hashtable<?, ?> props) {
        this.width = w;
        this.height = h;
        this.model = cm;
        this.pixels = pix;
        this.pixeloffset = off;
        this.pixelscan = scan;
        this.properties = props;
    }

    /**
     * Suma un consumidor y le entrega la imagen entera en el acto.
     *
     * <p>Si no está animada, además se lo da de baja al terminar: no va a haber más nada que
     * mandarle.
     */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (this.theConsumers.contains(ic)) {
            return;
        }
        this.theConsumers.addElement(ic);
        try {
            this.initConsumer(ic);
            this.sendPixels(ic, 0, 0, this.width, this.height);
            if (this.isConsumer(ic)) {
                if (this.animating) {
                    ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
                } else {
                    ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
                    this.removeConsumer(ic);
                }
            }
        } catch (RuntimeException e) {
            if (this.isConsumer(ic)) {
                ic.imageComplete(ImageConsumer.IMAGEERROR);
            }
        }
    }

    /** Si ese consumidor está registrado. */
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return this.theConsumers.contains(ic);
    }

    /** Saca a ese consumidor. */
    public synchronized void removeConsumer(ImageConsumer ic) {
        this.theConsumers.removeElement(ic);
    }

    /** Lo registra y le entrega la imagen. */
    public void startProduction(ImageConsumer ic) {
        this.addConsumer(ic);
    }

    /**
     * Vuelve a mandarle la imagen de arriba abajo.
     *
     * <p>Es gratis: los píxeles ya están en memoria y siempre se mandan en ese orden.
     */
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
        // No hace falta nada: esta fuente ya entrega de arriba abajo y de una sola vez.
    }

    /**
     * Declara si la imagen va a cambiar con el tiempo.
     *
     * <p>Hay que llamarlo **antes** de que se registre el primer consumidor: los que ya recibieron
     * una imagen estática se dieron de baja y no van a ver los cambios.
     */
    public synchronized void setAnimated(boolean animated) {
        this.animating = animated;
        if (!this.animating) {
            // Los consumidores que quedan estaban esperando mas cuadros; hay que cerrarles la
            // entrega antes de soltarlos, o se quedan esperando para siempre.
            int n = this.theConsumers.size();
            for (int i = 0; i < n; i++) {
                ImageConsumer ic = this.theConsumers.elementAt(i);
                ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
            }
            this.theConsumers.removeAllElements();
        }
    }

    /**
     * Declara si cada actualización manda la imagen entera.
     *
     * <p>Sólo tiene efecto sobre una imagen animada.
     */
    public synchronized void setFullBufferUpdates(boolean fullbuffers) {
        if (this.fullbuffers == fullbuffers) {
            return;
        }
        this.fullbuffers = fullbuffers;
        if (this.animating) {
            int n = this.theConsumers.size();
            for (int i = 0; i < n; i++) {
                ImageConsumer ic = this.theConsumers.elementAt(i);
                ic.setHints(fullbuffers
                        ? ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                        : ImageConsumer.RANDOMPIXELORDER);
            }
        }
    }

    /** Manda la imagen entera de nuevo. */
    public void newPixels() {
        this.newPixels(0, 0, this.width, this.height, true);
    }

    /** Manda ese rectángulo de nuevo. */
    public synchronized void newPixels(int x, int y, int w, int h) {
        this.newPixels(x, y, w, h, true);
    }

    /**
     * Manda ese rectángulo de nuevo, avisando o no que se completó un cuadro.
     *
     * <p>No avisar sirve para mandar varios pedazos y recién después declarar el cuadro completo,
     * para que el consumidor no dibuje una imagen a medio actualizar.
     */
    public synchronized void newPixels(int x, int y, int w, int h, boolean framenotify) {
        if (!this.animating) {
            return;
        }
        int x1 = x;
        int y1 = y;
        int w1 = w;
        int h1 = h;
        if (this.fullbuffers) {
            x1 = 0;
            y1 = 0;
            w1 = this.width;
            h1 = this.height;
        } else {
            if (x1 < 0) {
                w1 = w1 + x1;
                x1 = 0;
            }
            if (x1 + w1 > this.width) {
                w1 = this.width - x1;
            }
            if (y1 < 0) {
                h1 = h1 + y1;
                y1 = 0;
            }
            if (y1 + h1 > this.height) {
                h1 = this.height - y1;
            }
        }
        if ((w1 <= 0 || h1 <= 0) && !framenotify) {
            return;
        }
        int n = this.theConsumers.size();
        for (int i = 0; i < n; i++) {
            ImageConsumer ic = this.theConsumers.elementAt(i);
            if (w1 > 0 && h1 > 0) {
                this.sendPixels(ic, x1, y1, w1, h1);
            }
            if (framenotify && this.isConsumer(ic)) {
                ic.imageComplete(ImageConsumer.SINGLEFRAMEDONE);
            }
        }
    }

    /** Cambia el arreglo de píxeles por uno de bytes y manda la imagen entera. */
    public synchronized void newPixels(byte[] newpix, ColorModel newmodel, int offset,
            int scansize) {
        this.pixels = newpix;
        this.model = newmodel;
        this.pixeloffset = offset;
        this.pixelscan = scansize;
        this.newPixels();
    }

    /** Cambia el arreglo de píxeles por uno de enteros y manda la imagen entera. */
    public synchronized void newPixels(int[] newpix, ColorModel newmodel, int offset,
            int scansize) {
        this.pixels = newpix;
        this.model = newmodel;
        this.pixeloffset = offset;
        this.pixelscan = scansize;
        this.newPixels();
    }

    /** Le anuncia al consumidor el tamaño, las propiedades, el modelo y las pistas. */
    private void initConsumer(ImageConsumer ic) {
        if (this.isConsumer(ic)) {
            ic.setDimensions(this.width, this.height);
        }
        if (this.isConsumer(ic) && this.properties != null) {
            ic.setProperties(this.properties);
        }
        if (this.isConsumer(ic)) {
            ic.setColorModel(this.model);
        }
        if (this.isConsumer(ic)) {
            int hints;
            if (this.animating) {
                if (this.fullbuffers) {
                    hints = ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES;
                } else {
                    hints = ImageConsumer.RANDOMPIXELORDER;
                }
            } else {
                hints = ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                        | ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME;
            }
            ic.setHints(hints);
        }
    }

    /** Le manda un rectángulo de píxeles, sin copiar el arreglo. */
    private void sendPixels(ImageConsumer ic, int x, int y, int w, int h) {
        int off = this.pixeloffset + this.pixelscan * y + x;
        int w1 = w < 0 ? this.width - x : w;
        int h1 = h < 0 ? this.height - y : h;
        if (this.pixels instanceof byte[]) {
            ic.setPixels(x, y, w1, h1, this.model, (byte[]) this.pixels, off, this.pixelscan);
        } else {
            ic.setPixels(x, y, w1, h1, this.model, (int[]) this.pixels, off, this.pixelscan);
        }
    }
}
