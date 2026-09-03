package java.awt.image;

import java.util.Hashtable;

/**
 * Un productor que toma los píxeles de otro y los pasa por un {@link ImageFilter}.
 *
 * <p>Es la pieza que arma la tubería: del lado del consumidor parece un productor común, y del lado
 * del productor original parece un consumidor común. Encadenar dos es envolver uno en otro.
 *
 * <p>Lleva una tabla de consumidor a filtro porque cada consumidor necesita **su** copia del filtro:
 * el filtro guarda el estado de una entrega, y dos entregas simultáneas se pisarían. Por eso el
 * filtro que se pasa al constructor es un molde y nunca se usa directamente.
 */
public class FilteredImageSource implements ImageProducer {

    private final ImageProducer src;
    private final ImageFilter filter;
    private Hashtable<ImageConsumer, ImageFilter> proxies;

    /**
     * Con el productor y el filtro dados.
     *
     * @throws NullPointerException si falta alguno de los dos
     */
    public FilteredImageSource(ImageProducer orig, ImageFilter imgf) {
        if (orig == null || imgf == null) {
            throw new NullPointerException();
        }
        this.src = orig;
        this.filter = imgf;
    }

    /** Suma un consumidor, con su propia copia del filtro. */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (this.proxies == null) {
            this.proxies = new Hashtable<ImageConsumer, ImageFilter>();
        }
        if (!this.proxies.containsKey(ic)) {
            ImageFilter imgf = this.filter.getFilterInstance(ic);
            this.proxies.put(ic, imgf);
            this.src.addConsumer(imgf);
        }
    }

    /** Si ese consumidor está registrado. */
    public synchronized boolean isConsumer(ImageConsumer ic) {
        return this.proxies != null && this.proxies.containsKey(ic);
    }

    /** Saca a ese consumidor y su copia del filtro. */
    public synchronized void removeConsumer(ImageConsumer ic) {
        if (this.proxies != null) {
            ImageFilter imgf = this.proxies.get(ic);
            if (imgf != null) {
                this.src.removeConsumer(imgf);
                this.proxies.remove(ic);
                if (this.proxies.isEmpty()) {
                    this.proxies = null;
                }
            }
        }
    }

    /** Lo registra si hace falta y arranca la entrega. */
    public synchronized void startProduction(ImageConsumer ic) {
        if (this.proxies == null) {
            this.proxies = new Hashtable<ImageConsumer, ImageFilter>();
        }
        ImageFilter imgf = this.proxies.get(ic);
        if (imgf == null) {
            imgf = this.filter.getFilterInstance(ic);
            this.proxies.put(ic, imgf);
        }
        this.src.startProduction(imgf);
    }

    /**
     * Pide la reentrega de arriba abajo.
     *
     * <p>El pedido se le hace al **filtro**, no al productor: un filtro que sepa reordenar por su
     * cuenta lo resuelve sin molestar a la fuente.
     */
    public synchronized void requestTopDownLeftRightResend(ImageConsumer ic) {
        if (this.proxies != null) {
            ImageFilter imgf = this.proxies.get(ic);
            if (imgf != null) {
                imgf.resendTopDownLeftRight(this.src);
            }
        }
    }
}
