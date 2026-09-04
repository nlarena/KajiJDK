package javax.imageio;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.event.IIOWriteWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageOutputStream;

/**
 * KajiLibrary's javax.imageio.ImageWriter -- codifica imagenes a un formato.
 *
 * <p>El espejo de {@link ImageReader}. Una subclase concreta tiene que dar cinco metodos --los cuatro
 * de metadatos y {@link #write(IIOMetadata, IIOImage, ImageWriteParam)}-- y hereda todo lo demas.
 *
 * <h2>Casi todo es opcional, y hay que preguntar</h2>
 *
 * <p>Es lo que define esta clase. Diecisiete de sus metodos vienen de a pares {@code canXxx} /
 * {@code xxx}, y el {@code can} devuelve false por omision mientras el otro lanza
 * {@link UnsupportedOperationException}.
 *
 * <p>La razon es que los formatos son muy distintos entre si: TIFF puede insertar una imagen en el
 * medio de un archivo y reemplazar pixeles en el lugar; PNG no puede hacer ninguna de las dos. Un
 * escritor generico tiene que preguntar antes de cada cosa.
 *
 * <h2>Las tres formas de escribir varias imagenes</h2>
 *
 * <ul>
 *   <li><b>secuencia</b> ({@link #canWriteSequence}): {@code prepareWriteSequence}, varios
 *       {@code writeToSequence}, {@code endWriteSequence}. Es la normal;
 *   <li><b>insercion</b> ({@link #canInsertImage}): meter una imagen en una posicion de un archivo que
 *       ya existe;
 *   <li><b>vacia</b> ({@link #canWriteEmpty}): reservar el espacio primero y llenar los pixeles
 *       despues con {@link #replacePixels}. Es como se escribe una imagen enorme que no entra en
 *       memoria.
 * </ul>
 *
 * <h2>{@link #getOutput} suele necesitar un {@link ImageOutputStream}</h2>
 *
 * <p>Igual que en la lectura: acepta {@link Object} para dejar lugar a escritores especializados, pero
 * lo normal es que solo acepte {@code ImageOutputStream}. {@code ImageIO.createImageOutputStream} es
 * el que envuelve.
 *
 * <h2>Implementa {@link ImageTranscoder}</h2>
 *
 * <p>Por eso {@link #convertStreamMetadata} y {@link #convertImageMetadata} son abstractos: un
 * escritor <b>tiene</b> que saber traducir metadatos de otro formato al suyo, porque es lo que pasa
 * cada vez que alguien convierte una imagen y quiere conservar lo que tenia.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>La clase esta entera; lo que falta son <b>subclases</b>. Codificar PNG o JPEG pide los
 * codificadores. Registrando un escritor como servicio, todo esto funciona sin cambios.
 */
public abstract class ImageWriter implements ImageTranscoder {

    /** Quien lo creo, o null. */
    protected ImageWriterSpi originatingProvider;

    /** A donde escribe, o null. */
    protected Object output = null;

    /** En que idiomas sabe dar sus mensajes, o null. */
    protected Locale[] availableLocales = null;

    /** En cual los da, o null. */
    protected Locale locale = null;

    /** Los escuchas de advertencia, o null. */
    protected List<IIOWriteWarningListener> warningListeners = null;

    /** El idioma de cada uno cuando se registro. */
    protected List<Locale> warningLocales = null;

    /** Los escuchas de avance, o null. */
    protected List<IIOWriteProgressListener> progressListeners = null;

    /** Si alguien pidio cancelar. */
    private boolean abortFlag = false;

    /** Para las subclases. */
    protected ImageWriter(ImageWriterSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    /** Quien lo creo, o null. */
    public ImageWriterSpi getOriginatingProvider() {
        return this.originatingProvider;
    }

    /**
     * A donde escribir.
     *
     * @param output tipicamente un {@link ImageOutputStream}; null lo desconecta
     * @throws IllegalArgumentException si ese tipo de salida no se soporta
     */
    public void setOutput(Object output) {
        if (output != null) {
            boolean found = false;
            if (this.originatingProvider != null) {
                Class<?>[] classes = this.originatingProvider.getOutputTypes();
                int i = 0;
                while (i < classes.length) {
                    if (classes[i].isInstance(output)) {
                        found = true;
                    }
                    i = i + 1;
                }
            } else if (output instanceof ImageOutputStream) {
                found = true;
            }
            if (!found) {
                throw new IllegalArgumentException("Illegal output type!");
            }
        }
        this.output = output;
    }

    /** A donde escribe, o null. */
    public Object getOutput() {
        return this.output;
    }

    /** En que idiomas sabe dar sus mensajes; una copia, o null. */
    public Locale[] getAvailableLocales() {
        if (this.availableLocales == null) {
            return null;
        }
        Locale[] copy = new Locale[this.availableLocales.length];
        System.arraycopy(this.availableLocales, 0, copy, 0, this.availableLocales.length);
        return copy;
    }

    /**
     * En cual darlos; null vuelve al del sistema.
     *
     * @throws IllegalArgumentException si no es uno de los disponibles
     */
    public void setLocale(Locale locale) {
        if (locale != null) {
            Locale[] locales = getAvailableLocales();
            boolean found = false;
            if (locales != null) {
                int i = 0;
                while (i < locales.length) {
                    if (locale.equals(locales[i])) {
                        found = true;
                    }
                    i = i + 1;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Invalid locale!");
            }
        }
        this.locale = locale;
    }

    /** En cual los da, o null. */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * Un objeto de parametros vacio, del tipo que este escritor entiende.
     *
     * <p>Una subclase con parametros propios lo redefine.
     */
    public ImageWriteParam getDefaultWriteParam() {
        return new ImageWriteParam(getLocale());
    }

    /** Los metadatos de flujo por omision para esos parametros, o null si no lleva. */
    public abstract IIOMetadata getDefaultStreamMetadata(ImageWriteParam param);

    /** Los de una imagen de ese tipo. */
    public abstract IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType,
                                                        ImageWriteParam param);

    /** Traduce metadatos de flujo de otro formato. Ver la nota de la clase. */
    public abstract IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param);

    /** Idem, de una imagen. */
    public abstract IIOMetadata convertImageMetadata(IIOMetadata inData,
                                                     ImageTypeSpecifier imageType,
                                                     ImageWriteParam param);

    /**
     * Cuantas miniaturas puede incrustar; 0 si ninguna, -1 si no se sabe todavia.
     *
     * <p>Los cuatro argumentos existen porque la respuesta puede depender de todo: hay formatos que
     * solo admiten miniatura en ciertos modos de compresion.
     */
    public int getNumThumbnailsSupported(ImageTypeSpecifier imageType, ImageWriteParam param,
                                         IIOMetadata streamMetadata, IIOMetadata imageMetadata) {
        return 0;
    }

    /**
     * Que tamanos de miniatura prefiere, de a pares minimo y maximo; null si no opina.
     *
     * @throws IllegalArgumentException si se piden miniaturas y no soporta ninguna
     */
    public Dimension[] getPreferredThumbnailSizes(ImageTypeSpecifier imageType,
                                                  ImageWriteParam param,
                                                  IIOMetadata streamMetadata,
                                                  IIOMetadata imageMetadata) {
        return null;
    }

    /** Si sabe escribir pixeles crudos sin modelo de color. Ver {@link ImageReader#canReadRaster}. */
    public boolean canWriteRasters() {
        return false;
    }

    /**
     * Escribe una imagen con sus metadatos.
     *
     * <p>Es el metodo que hace el trabajo, y el unico que una subclase <b>tiene</b> que escribir para
     * codificar.
     *
     * @throws IllegalStateException si no hay salida
     * @throws UnsupportedOperationException si la imagen trae un raster y no sabe escribirlos
     * @throws IOException si fallo la escritura
     */
    public abstract void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param)
        throws IOException;

    /**
     * Idem, sin metadatos de flujo ni parametros.
     *
     * @throws IOException si fallo la escritura
     */
    public void write(IIOImage image) throws IOException {
        write(null, image, null);
    }

    /**
     * Idem, desde una imagen pelada.
     *
     * @throws IOException si fallo la escritura
     */
    public void write(RenderedImage image) throws IOException {
        write(null, new IIOImage(image, null, null), null);
    }

    /** Si sabe escribir varias imagenes en secuencia. Ver la nota de la clase. */
    public boolean canWriteSequence() {
        return false;
    }

    /**
     * Abre una secuencia.
     *
     * @throws UnsupportedOperationException si no sabe
     * @throws IOException si fallo la escritura
     */
    public void prepareWriteSequence(IIOMetadata streamMetadata) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Agrega una imagen a la secuencia abierta.
     *
     * @throws IllegalStateException si no hay secuencia abierta
     * @throws IOException si fallo la escritura
     */
    public void writeToSequence(IIOImage image, ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * La cierra.
     *
     * @throws IllegalStateException si no hay secuencia abierta
     * @throws IOException si fallo la escritura
     */
    public void endWriteSequence() throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Si sabe reemplazar los metadatos de flujo de un archivo ya escrito.
     *
     * @throws IOException si fallo la lectura de la salida
     */
    public boolean canReplaceStreamMetadata() throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Los reemplaza.
     *
     * @throws UnsupportedOperationException si no sabe
     * @throws IOException si fallo la escritura
     */
    public void replaceStreamMetadata(IIOMetadata streamMetadata) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Si sabe reemplazar los metadatos de esa imagen.
     *
     * @throws IOException si fallo la lectura de la salida
     */
    public boolean canReplaceImageMetadata(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Los reemplaza.
     *
     * @throws UnsupportedOperationException si no sabe
     * @throws IOException si fallo la escritura
     */
    public void replaceImageMetadata(int imageIndex, IIOMetadata imageMetadata)
        throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Si sabe insertar una imagen en esa posicion. Ver la nota de la clase.
     *
     * @param imageIndex donde; -1 significa al final
     * @throws IOException si fallo la lectura de la salida
     */
    public boolean canInsertImage(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * La inserta.
     *
     * @throws UnsupportedOperationException si no sabe
     * @throws IOException si fallo la escritura
     */
    public void writeInsert(int imageIndex, IIOImage image, ImageWriteParam param)
        throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Si sabe borrar una imagen del archivo.
     *
     * @throws IOException si fallo la lectura de la salida
     */
    public boolean canRemoveImage(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * La borra.
     *
     * @throws UnsupportedOperationException si no sabe
     * @throws IOException si fallo la escritura
     */
    public void removeImage(int imageIndex) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Si sabe reservar una imagen vacia para llenarla despues. Ver la nota de la clase.
     *
     * @throws IOException si fallo la lectura de la salida
     */
    public boolean canWriteEmpty() throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * La reserva.
     *
     * @throws UnsupportedOperationException si no sabe
     * @throws IOException si fallo la escritura
     */
    public void prepareWriteEmpty(IIOMetadata streamMetadata, ImageTypeSpecifier imageType,
                                  int width, int height, IIOMetadata imageMetadata,
                                  List<? extends BufferedImage> thumbnails,
                                  ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Cierra la imagen vacia.
     *
     * @throws IllegalStateException si no hay ninguna abierta
     * @throws IOException si fallo la escritura
     */
    public void endWriteEmpty() throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Si sabe insertar una imagen vacia en esa posicion.
     *
     * @throws IOException si fallo la lectura de la salida
     */
    public boolean canInsertEmpty(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * La inserta.
     *
     * @throws UnsupportedOperationException si no sabe
     * @throws IOException si fallo la escritura
     */
    public void prepareInsertEmpty(int imageIndex, ImageTypeSpecifier imageType, int width,
                                   int height, IIOMetadata imageMetadata,
                                   List<? extends BufferedImage> thumbnails,
                                   ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * La cierra.
     *
     * @throws IllegalStateException si no hay ninguna abierta
     * @throws IOException si fallo la escritura
     */
    public void endInsertEmpty() throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Si sabe reescribir pixeles de una imagen ya escrita.
     *
     * @throws IOException si fallo la lectura de la salida
     */
    public boolean canReplacePixels(int imageIndex) throws IOException {
        if (getOutput() == null) {
            throw new IllegalStateException("getOutput() == null!");
        }
        return false;
    }

    /**
     * Abre una region para reescribir.
     *
     * @param region que rectangulo, o null para toda la imagen
     * @throws UnsupportedOperationException si no sabe
     * @throws IOException si fallo la escritura
     */
    public void prepareReplacePixels(int imageIndex, Rectangle region) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Reescribe esos pixeles.
     *
     * @throws IllegalStateException si no hay region abierta
     * @throws IOException si fallo la escritura
     */
    public void replacePixels(RenderedImage image, ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Idem, desde pixeles crudos.
     *
     * @throws IOException si fallo la escritura
     */
    public void replacePixels(Raster raster, ImageWriteParam param) throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /**
     * Cierra la region.
     *
     * @throws IllegalStateException si no hay ninguna abierta
     * @throws IOException si fallo la escritura
     */
    public void endReplacePixels() throws IOException {
        throw new UnsupportedOperationException("Unsupported write variant!");
    }

    /** Pide cancelar. Se llama desde otro hilo; ver {@link ImageReader#abort}. */
    public synchronized void abort() {
        this.abortFlag = true;
    }

    /** Si alguien pidio cancelar. */
    protected synchronized boolean abortRequested() {
        return this.abortFlag;
    }

    /** Limpia el pedido. La subclase lo llama al empezar cada operacion. */
    protected synchronized void clearAbortRequest() {
        this.abortFlag = false;
    }

    /** Registra un escucha de advertencias; null no hace nada. */
    public void addIIOWriteWarningListener(IIOWriteWarningListener listener) {
        if (listener == null) {
            return;
        }
        if (this.warningListeners == null) {
            this.warningListeners = new ArrayList<IIOWriteWarningListener>();
            this.warningLocales = new ArrayList<Locale>();
        }
        this.warningListeners.add(listener);
        // El idioma se guarda al registrar; ver ImageReader.
        this.warningLocales.add(getLocale());
    }

    /** Lo da de baja. */
    public void removeIIOWriteWarningListener(IIOWriteWarningListener listener) {
        if (listener == null || this.warningListeners == null) {
            return;
        }
        int at = this.warningListeners.indexOf(listener);
        if (at >= 0) {
            this.warningListeners.remove(at);
            this.warningLocales.remove(at);
            if (this.warningListeners.isEmpty()) {
                this.warningListeners = null;
                this.warningLocales = null;
            }
        }
    }

    /** Los da de baja a todos. */
    public void removeAllIIOWriteWarningListeners() {
        this.warningListeners = null;
        this.warningLocales = null;
    }

    /** Registra un escucha de avance. */
    public void addIIOWriteProgressListener(IIOWriteProgressListener listener) {
        if (listener == null) {
            return;
        }
        if (this.progressListeners == null) {
            this.progressListeners = new ArrayList<IIOWriteProgressListener>();
        }
        this.progressListeners.add(listener);
    }

    /** Lo da de baja. */
    public void removeIIOWriteProgressListener(IIOWriteProgressListener listener) {
        if (listener == null || this.progressListeners == null) {
            return;
        }
        this.progressListeners.remove(listener);
        if (this.progressListeners.isEmpty()) {
            this.progressListeners = null;
        }
    }

    /** Los da de baja a todos. */
    public void removeAllIIOWriteProgressListeners() {
        this.progressListeners = null;
    }

    /** Avisa que empieza una imagen. */
    protected void processImageStarted(int imageIndex) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).imageStarted(this, imageIndex);
            i = i + 1;
        }
    }

    /** Avisa del avance. */
    protected void processImageProgress(float percentageDone) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).imageProgress(this, percentageDone);
            i = i + 1;
        }
    }

    /** Avisa que termino. */
    protected void processImageComplete() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).imageComplete(this);
            i = i + 1;
        }
    }

    /** Avisa que empieza una miniatura. */
    protected void processThumbnailStarted(int imageIndex, int thumbnailIndex) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).thumbnailStarted(this, imageIndex, thumbnailIndex);
            i = i + 1;
        }
    }

    /** Avisa del avance de la miniatura. */
    protected void processThumbnailProgress(float percentageDone) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).thumbnailProgress(this, percentageDone);
            i = i + 1;
        }
    }

    /** Avisa que termino. */
    protected void processThumbnailComplete() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).thumbnailComplete(this);
            i = i + 1;
        }
    }

    /** Avisa que se corto. Ver {@link javax.imageio.event.IIOWriteProgressListener#writeAborted}. */
    protected void processWriteAborted() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).writeAborted(this);
            i = i + 1;
        }
    }

    /** Avisa de una advertencia. */
    protected void processWarningOccurred(int imageIndex, String warning) {
        if (this.warningListeners == null) {
            return;
        }
        if (warning == null) {
            throw new IllegalArgumentException("warning == null!");
        }
        int i = 0;
        while (i < this.warningListeners.size()) {
            this.warningListeners.get(i).warningOccurred(this, imageIndex, warning);
            i = i + 1;
        }
    }

    /**
     * Idem, con el texto sacado de un paquete de recursos.
     *
     * <p>Cada escucha recibe el mensaje en el idioma con el que se registro; ver {@link ImageReader}.
     *
     * @throws IllegalArgumentException si el paquete o la clave son null, o si la clave no esta
     */
    protected void processWarningOccurred(int imageIndex, String baseName, String keyword) {
        if (this.warningListeners == null) {
            return;
        }
        if (baseName == null) {
            throw new IllegalArgumentException("baseName == null!");
        }
        if (keyword == null) {
            throw new IllegalArgumentException("keyword == null!");
        }
        int i = 0;
        while (i < this.warningListeners.size()) {
            IIOWriteWarningListener listener = this.warningListeners.get(i);
            Locale where = this.warningLocales.get(i);
            if (where == null) {
                where = Locale.getDefault();
            }
            String warning;
            try {
                java.util.ResourceBundle bundle =
                    java.util.ResourceBundle.getBundle(baseName, where,
                                                       getClass().getClassLoader());
                warning = bundle.getString(keyword);
            } catch (java.util.MissingResourceException e) {
                throw new IllegalArgumentException("Bundle not found!");
            }
            listener.warningOccurred(this, imageIndex, warning);
            i = i + 1;
        }
    }

    /** Vuelve al estado inicial. Ver {@link ImageReader#reset}. */
    public void reset() {
        setOutput(null);
        setLocale(null);
        removeAllIIOWriteWarningListeners();
        removeAllIIOWriteProgressListeners();
        clearAbortRequest();
    }

    /** Libera lo que tenga tomado. Despues de esto no se puede usar mas. */
    public void dispose() {
    }
}
