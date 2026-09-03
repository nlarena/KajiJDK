package java.awt.image;

import java.util.Vector;

/**
 * El productor que entrega los píxeles de una {@link BufferedImage}.
 *
 * <p>Es lo que hace que una imagen en memoria se pueda meter en la tubería de filtros: del lado del
 * consumidor no se nota la diferencia con una imagen que llega por la red, salvo que ésta llega
 * entera y de una.
 *
 * <p>Entrega en ARGB de ocho bits por canal y no en el formato de la imagen. Es una conversión de
 * más cuando los dos coinciden, pero es lo único que un consumidor cualquiera sabe leer sin
 * preguntar, y la alternativa —entregar en el modelo de la imagen— obligaría a cada filtro a saber
 * de modelos de color indexados y empaquetados.
 *
 * <p>No es pública: `java.awt.image` no declara esta clase, sólo la usa
 * {@link BufferedImage#getSource}.
 */
class BufferedImageSource implements ImageProducer {

    private final BufferedImage image;
    private final Vector<ImageConsumer> theConsumers = new Vector<ImageConsumer>();

    /** Con la imagen que va a entregar. */
    BufferedImageSource(BufferedImage image) {
        this.image = image;
    }

    /** Suma un consumidor y le entrega la imagen entera en el acto. */
    public synchronized void addConsumer(ImageConsumer ic) {
        if (this.theConsumers.contains(ic)) {
            return;
        }
        this.theConsumers.addElement(ic);
        try {
            this.entregar(ic);
        } finally {
            this.theConsumers.removeElement(ic);
        }
    }

    /** Si ese consumidor está recibiendo ahora mismo. */
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

    /** No hace falta: esta fuente ya entrega de arriba abajo. */
    public void requestTopDownLeftRightResend(ImageConsumer ic) {
    }

    /** Le manda el tamaño, el modelo, las pistas y los píxeles, fila por fila. */
    private void entregar(ImageConsumer ic) {
        int w = this.image.getWidth();
        int h = this.image.getHeight();
        ColorModel rgb = ColorModel.getRGBdefault();
        ic.setDimensions(w, h);
        ic.setColorModel(rgb);
        ic.setHints(ImageConsumer.TOPDOWNLEFTRIGHT | ImageConsumer.COMPLETESCANLINES
                | ImageConsumer.SINGLEPASS | ImageConsumer.SINGLEFRAME);
        // Fila por fila y no la imagen entera: el arreglo temporal se reusa, y una imagen grande no
        // necesita una copia completa de si misma en memoria para entregarse.
        int[] fila = new int[w];
        for (int y = 0; y < h; y++) {
            this.image.getRGB(0, y, w, 1, fila, 0, w);
            ic.setPixels(0, y, w, 1, rgb, fila, 0, w);
        }
        ic.imageComplete(ImageConsumer.STATICIMAGEDONE);
    }
}
