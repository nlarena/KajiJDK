package java.awt.image;

import java.awt.Image;
import java.util.Hashtable;

/**
 * El puente de vuelta: convierte la tubería asíncrona de {@link ImageProducer} en un arreglo de
 * píxeles y una llamada que espera.
 *
 * <p>Todo lo demás en este paquete empuja píxeles hacia adelante. Esto es lo que los junta: se
 * registra como consumidor, deja que la imagen llegue, y {@link #grabPixels()} bloquea hasta que
 * terminó. Es la forma de sacar los píxeles de cualquier `Image`, venga de donde venga.
 *
 * <p>Hay dos maneras de usarlo y la diferencia importa. Si se le da un arreglo, escribe ahí y
 * respeta el modelo de color que traiga la imagen; si no, reserva el suyo cuando se entera del
 * tamaño. El constructor con `forceRGB` pide además que todo se convierta a ARGB de ocho bits por
 * canal, que es lo que se quiere cuando hay que mirar los colores y no sólo copiarlos.
 *
 * <p>Esa conversión puede pasar **a mitad de camino**: mientras todas las tandas vengan con el mismo
 * modelo de color se guardan los píxeles crudos, y en cuanto llega una con otro modelo hay que pasar
 * a ARGB todo lo que ya se había juntado, porque no hay un modelo que sirva para las dos.
 *
 * <p>Terminar sin error **no** garantiza una imagen completa: hay que mirar {@link #getStatus} y
 * comprobar que tenga `ALLBITS`. Una imagen abortada a mitad devuelve `true` de `grabPixels` con un
 * arreglo parcialmente lleno, y no darse cuenta es la equivocación clásica con esta clase.
 */
public class PixelGrabber implements ImageConsumer {

    private final ImageProducer producer;
    private final int dstX;
    private final int dstY;
    private int dstW;
    private int dstH;
    private ColorModel imageModel;
    private byte[] bytePixels;
    private int[] intPixels;
    private int dstOff;
    private int dstScan;
    private boolean grabbing;
    private int flags;

    private static final int GRABBEDBITS = ImageObserver.FRAMEBITS | ImageObserver.ALLBITS;
    private static final int DONEBITS = GRABBEDBITS | ImageObserver.ERROR;

    /**
     * Junta un rectángulo de una imagen en el arreglo dado, en ARGB.
     *
     * @throws NullPointerException si la imagen es `null`
     */
    public PixelGrabber(Image img, int x, int y, int w, int h, int[] pix, int off, int scansize) {
        this(img.getSource(), x, y, w, h, pix, off, scansize);
    }

    /**
     * Junta un rectángulo de un productor en el arreglo dado, en ARGB.
     *
     * <p>Con `pix` en `null` y las medidas en -1, el arreglo se reserva cuando el productor anuncia
     * el tamaño.
     */
    public PixelGrabber(ImageProducer ip, int x, int y, int w, int h, int[] pix, int off,
            int scansize) {
        this.producer = ip;
        this.dstX = x;
        this.dstY = y;
        this.dstW = w;
        this.dstH = h;
        this.dstOff = off;
        this.dstScan = scansize;
        this.intPixels = pix;
        this.imageModel = ColorModel.getRGBdefault();
    }

    /**
     * Junta un rectángulo de una imagen en un arreglo propio.
     *
     * <p>Con `forceRGB` en `false` se guardan los píxeles tal como vengan y el modelo de color queda
     * en {@link #getColorModel}; con `true` se convierte todo a ARGB.
     *
     * @throws NullPointerException si la imagen es `null`
     */
    public PixelGrabber(Image img, int x, int y, int w, int h, boolean forceRGB) {
        this.producer = img.getSource();
        this.dstX = x;
        this.dstY = y;
        this.dstW = w;
        this.dstH = h;
        if (forceRGB) {
            this.imageModel = ColorModel.getRGBdefault();
        }
    }

    /** Arranca la entrega sin esperarla. */
    public synchronized void startGrabbing() {
        if ((this.flags & DONEBITS) != 0) {
            return;
        }
        if (!this.grabbing) {
            this.grabbing = true;
            this.flags = this.flags & ~ImageObserver.ABORT;
            this.producer.startProduction(this);
        }
    }

    /** Corta la entrega. */
    public synchronized void abortGrabbing() {
        this.imageComplete(ImageConsumer.IMAGEABORTED);
    }

    /**
     * Arranca la entrega si hace falta y espera hasta que termine.
     *
     * @return `true` si se juntaron píxeles, lo que **no** quiere decir que la imagen esté completa
     * @throws InterruptedException si se interrumpe el hilo mientras espera
     */
    public boolean grabPixels() throws InterruptedException {
        return this.grabPixels(0);
    }

    /**
     * Como el anterior, con un plazo.
     *
     * @param ms cuánto esperar como máximo, o 0 para esperar sin plazo
     * @throws InterruptedException si se interrumpe el hilo mientras espera
     */
    public synchronized boolean grabPixels(long ms) throws InterruptedException {
        if ((this.flags & DONEBITS) != 0) {
            return (this.flags & GRABBEDBITS) != 0;
        }
        long end = ms + System.currentTimeMillis();
        if (!this.grabbing) {
            this.grabbing = true;
            this.flags = this.flags & ~ImageObserver.ABORT;
            this.producer.startProduction(this);
        }
        while (this.grabbing) {
            long timeout;
            if (ms == 0) {
                timeout = 0;
            } else {
                timeout = end - System.currentTimeMillis();
                if (timeout <= 0) {
                    break;
                }
            }
            this.wait(timeout);
        }
        return (this.flags & GRABBEDBITS) != 0;
    }

    /**
     * Las banderas de {@link ImageObserver} que describen cómo terminó.
     *
     * @deprecated el nombre no dice que devuelve banderas de estado. Usar {@link #getStatus}.
     */
    @Deprecated
    public synchronized int status() {
        return this.flags;
    }

    /** Las banderas de {@link ImageObserver} que describen cómo terminó. */
    public synchronized int getStatus() {
        return this.flags;
    }

    /** El ancho de lo que se juntó, o -1 si todavía no se sabe. */
    public synchronized int getWidth() {
        return this.dstW < 0 ? -1 : this.dstW;
    }

    /** El alto de lo que se juntó, o -1 si todavía no se sabe. */
    public synchronized int getHeight() {
        return this.dstH < 0 ? -1 : this.dstH;
    }

    /**
     * El arreglo con los píxeles: un `byte[]` o un `int[]`.
     *
     * <p>Hay que preguntarle a {@link #getColorModel} cómo interpretarlos, salvo que se haya pedido
     * ARGB.
     */
    public synchronized Object getPixels() {
        if (this.bytePixels == null) {
            return this.intPixels;
        }
        return this.bytePixels;
    }

    /**
     * El modelo de color de los píxeles juntados.
     *
     * <p>Puede no ser el de la imagen: si las tandas vinieron con modelos distintos, todo se
     * convirtió a ARGB y esto devuelve el modelo ARGB.
     */
    public synchronized ColorModel getColorModel() {
        return this.imageModel;
    }

    /** Anota el tamaño y reserva el arreglo si hace falta. */
    public void setDimensions(int width, int height) {
        if (this.dstW < 0) {
            this.dstW = width - this.dstX;
        }
        if (this.dstH < 0) {
            this.dstH = height - this.dstY;
        }
        if (this.dstW <= 0 || this.dstH <= 0) {
            this.imageComplete(ImageConsumer.STATICIMAGEDONE);
        } else if (this.intPixels == null && this.imageModel == ColorModel.getRGBdefault()) {
            this.intPixels = new int[this.dstW * this.dstH];
            this.dstScan = this.dstW;
            this.dstOff = 0;
        }
        this.flags = this.flags | ImageObserver.WIDTH | ImageObserver.HEIGHT;
    }

    /** No hace nada: este consumidor acepta cualquier orden. */
    public void setHints(int hints) {
    }

    /** No hace nada: este consumidor no guarda propiedades. */
    public void setProperties(Hashtable<?, ?> props) {
    }

    /**
     * No hace nada.
     *
     * <p>El modelo de color no se toma de acá sino de cada tanda: el productor anuncia el que va a
     * usar para la mayoría, pero puede mandar tandas con otro.
     */
    public void setColorModel(ColorModel model) {
    }

    /**
     * Pasa a ARGB todo lo que se haya juntado hasta ahora.
     *
     * <p>Hace falta cuando llega una tanda con un modelo de color distinto del de las anteriores: no
     * hay un modelo que describa a las dos, así que se pasa al único común.
     */
    private void convertToRGB() {
        int size = this.dstW * this.dstH;
        int[] newpixels = new int[size];
        if (this.bytePixels != null) {
            for (int i = 0; i < size; i++) {
                newpixels[i] = this.imageModel.getRGB(this.bytePixels[i] & 0xFF);
            }
        } else if (this.intPixels != null) {
            for (int i = 0; i < size; i++) {
                newpixels[i] = this.imageModel.getRGB(this.intPixels[i]);
            }
        }
        this.bytePixels = null;
        this.intPixels = newpixels;
        this.dstScan = this.dstW;
        this.dstOff = 0;
        this.imageModel = ColorModel.getRGBdefault();
    }

    /** Recorta la tanda al rectángulo pedido; devuelve `null` si no queda nada. */
    private int[] recortar(int srcX, int srcY, int srcW, int srcH, int srcOff, int srcScan) {
        int x = srcX;
        int y = srcY;
        int w = srcW;
        int h = srcH;
        int off = srcOff;
        if (y < this.dstY) {
            int diff = this.dstY - y;
            if (diff >= h) {
                return null;
            }
            off = off + srcScan * diff;
            y = y + diff;
            h = h - diff;
        }
        if (y + h > this.dstY + this.dstH) {
            h = this.dstY + this.dstH - y;
            if (h <= 0) {
                return null;
            }
        }
        if (x < this.dstX) {
            int diff = this.dstX - x;
            if (diff >= w) {
                return null;
            }
            off = off + diff;
            x = x + diff;
            w = w - diff;
        }
        if (x + w > this.dstX + this.dstW) {
            w = this.dstX + this.dstW - x;
            if (w <= 0) {
                return null;
            }
        }
        int[] r = new int[5];
        r[0] = x;
        r[1] = y;
        r[2] = w;
        r[3] = h;
        r[4] = off;
        return r;
    }

    /** Guarda una tanda de píxeles de un byte. */
    public void setPixels(int srcX, int srcY, int srcW, int srcH, ColorModel model, byte[] pixels,
            int srcOff, int srcScan) {
        int[] r = this.recortar(srcX, srcY, srcW, srcH, srcOff, srcScan);
        if (r == null) {
            return;
        }
        int x = r[0];
        int y = r[1];
        int w = r[2];
        int h = r[3];
        int off = r[4];
        if (this.intPixels == null) {
            if (this.bytePixels == null) {
                this.bytePixels = new byte[this.dstW * this.dstH];
                this.dstScan = this.dstW;
                this.dstOff = 0;
                this.imageModel = model;
            } else if (this.imageModel != model) {
                this.convertToRGB();
            }
        }
        int dstPtr = this.dstOff + (y - this.dstY) * this.dstScan + (x - this.dstX);
        if (this.intPixels == null) {
            int srcPtr = off;
            for (int fila = h; fila > 0; fila--) {
                System.arraycopy(pixels, srcPtr, this.bytePixels, dstPtr, w);
                srcPtr = srcPtr + srcScan;
                dstPtr = dstPtr + this.dstScan;
            }
        } else {
            int dstRem = this.dstScan - w;
            int srcRem = srcScan - w;
            int srcPtr = off;
            for (int fila = h; fila > 0; fila--) {
                for (int col = w; col > 0; col--) {
                    this.intPixels[dstPtr] = model.getRGB(pixels[srcPtr] & 0xFF);
                    dstPtr = dstPtr + 1;
                    srcPtr = srcPtr + 1;
                }
                srcPtr = srcPtr + srcRem;
                dstPtr = dstPtr + dstRem;
            }
        }
        this.flags = this.flags | ImageObserver.SOMEBITS;
    }

    /** Guarda una tanda de píxeles de un `int`. */
    public void setPixels(int srcX, int srcY, int srcW, int srcH, ColorModel model, int[] pixels,
            int srcOff, int srcScan) {
        int[] r = this.recortar(srcX, srcY, srcW, srcH, srcOff, srcScan);
        if (r == null) {
            return;
        }
        int x = r[0];
        int y = r[1];
        int w = r[2];
        int h = r[3];
        int off = r[4];
        if (this.intPixels == null) {
            this.convertToRGB();
        }
        boolean convertir = this.imageModel != model;
        int dstPtr = this.dstOff + (y - this.dstY) * this.dstScan + (x - this.dstX);
        int dstRem = this.dstScan - w;
        int srcRem = srcScan - w;
        int srcPtr = off;
        for (int fila = h; fila > 0; fila--) {
            for (int col = w; col > 0; col--) {
                if (convertir) {
                    this.intPixels[dstPtr] = model.getRGB(pixels[srcPtr]);
                } else {
                    this.intPixels[dstPtr] = pixels[srcPtr];
                }
                dstPtr = dstPtr + 1;
                srcPtr = srcPtr + 1;
            }
            srcPtr = srcPtr + srcRem;
            dstPtr = dstPtr + dstRem;
        }
        this.flags = this.flags | ImageObserver.SOMEBITS;
    }

    /** Anota cómo terminó la entrega y despierta a quien esté esperando. */
    public synchronized void imageComplete(int status) {
        this.grabbing = false;
        if (status == ImageConsumer.IMAGEABORTED) {
            this.flags = this.flags | ImageObserver.ABORT;
        } else if (status == ImageConsumer.STATICIMAGEDONE) {
            this.flags = this.flags | ImageObserver.ALLBITS;
        } else if (status == ImageConsumer.SINGLEFRAMEDONE) {
            this.flags = this.flags | ImageObserver.FRAMEBITS;
        } else {
            this.flags = this.flags | ImageObserver.ERROR | ImageObserver.ABORT;
        }
        this.producer.removeConsumer(this);
        this.notifyAll();
    }
}
