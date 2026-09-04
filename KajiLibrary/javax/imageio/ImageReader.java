package javax.imageio;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

/**
 * KajiLibrary's javax.imageio.ImageReader -- decodifica imagenes de un formato.
 *
 * <p>Lo que implementa quien agrega soporte para leer un formato. Una subclase concreta tiene que dar
 * seis metodos --{@link #getNumImages}, {@link #getWidth}, {@link #getHeight},
 * {@link #getImageTypes}, {@link #getStreamMetadata}, {@link #getImageMetadata} y
 * {@link #read(int, ImageReadParam)}-- y hereda todo lo demas.
 *
 * <h2>Un archivo puede tener varias imagenes</h2>
 *
 * <p>Por eso casi todos los metodos toman un indice. TIFF, GIF animado e ICO llevan varias; PNG y
 * JPEG llevan una y el indice siempre es 0.
 *
 * <h2>{@code seekForwardOnly} es una promesa que se paga</h2>
 *
 * <p>{@link #setInput(Object, boolean)} con true promete que las imagenes se van a leer <b>en orden y
 * sin volver</b>. A cambio, el lector puede tirar lo que ya paso, y por eso se puede leer un TIFF de
 * un gigabyte desde un socket.
 *
 * <p>El precio: despues de leer la imagen 3, pedir la 1 lanza {@link IndexOutOfBoundsException}.
 * {@link #getMinIndex} dice hasta donde se retrocedio.
 *
 * <h2>{@code ignoreMetadata} tambien</h2>
 *
 * <p>Prometer que no se van a pedir los metadatos deja al lector saltear bloques enteros del archivo.
 * En un JPEG con Exif y una miniatura incrustada eso es la mitad del trabajo.
 *
 * <h2>La entrada casi siempre tiene que ser un {@link ImageInputStream}</h2>
 *
 * <p>{@link #setInput} acepta un {@link Object} porque un lector especializado puede aceptar otra
 * cosa, pero lo normal es que solo acepte {@code ImageInputStream}. Pasarle un {@code File} directo
 * lanza {@link IllegalArgumentException}; {@code ImageIO.createImageInputStream} es el que envuelve.
 *
 * <h2>{@link #abort} se llama desde otro hilo</h2>
 *
 * <p>Es la unica parte de la clase pensada para concurrencia: {@code read} bloquea, asi que cancelar
 * solo se puede desde afuera. Una subclase tiene que consultar {@link #abortRequested} <b>seguido</b>
 * durante la decodificacion, y llamar {@link #processReadAborted} al cortar.
 *
 * <p>Y tiene que llamar {@link #clearAbortRequest} al empezar cada operacion: si no, una cancelacion
 * vieja aborta la lectura siguiente.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>La clase esta entera. Lo que esta biblioteca no trae son <b>subclases</b>: decodificar PNG, JPEG
 * o GIF pide los decodificadores, y eso es otro proyecto. Registrando un lector como servicio, todo
 * esto funciona sin cambios.
 */
public abstract class ImageReader {

    /** Quien lo creo, o null. */
    protected ImageReaderSpi originatingProvider;

    /** De donde lee, o null. */
    protected Object input = null;

    /** Si se prometio no volver atras. Ver la nota de la clase. */
    protected boolean seekForwardOnly = false;

    /** Si se prometio no pedir metadatos. */
    protected boolean ignoreMetadata = false;

    /** La imagen mas baja que todavia se puede pedir. */
    protected int minIndex = 0;

    /** En que idiomas sabe dar sus mensajes, o null. */
    protected Locale[] availableLocales = null;

    /** En cual los da, o null para el del sistema. */
    protected Locale locale = null;

    /** Los escuchas de advertencia, o null. */
    protected List<IIOReadWarningListener> warningListeners = null;

    /** El idioma de cada uno cuando se registro. */
    protected List<Locale> warningLocales = null;

    /** Los escuchas de avance, o null. */
    protected List<IIOReadProgressListener> progressListeners = null;

    /** Los escuchas de imagen parcial, o null. */
    protected List<IIOReadUpdateListener> updateListeners = null;

    /** Si alguien pidio cancelar. */
    private boolean abortFlag = false;

    /** Para las subclases. */
    protected ImageReader(ImageReaderSpi originatingProvider) {
        this.originatingProvider = originatingProvider;
    }

    /**
     * Como se llama el formato que lee.
     *
     * @throws IOException si no se puede averiguar
     */
    public String getFormatName() throws IOException {
        return this.originatingProvider.getFormatNames()[0];
    }

    /** Quien lo creo, o null si se instancio a mano. */
    public ImageReaderSpi getOriginatingProvider() {
        return this.originatingProvider;
    }

    /**
     * De donde leer, con las dos promesas. Ver la nota de la clase.
     *
     * @param input tipicamente un {@link ImageInputStream}; null lo desconecta
     * @throws IllegalArgumentException si ese tipo de entrada no se soporta
     */
    public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
        if (input != null) {
            boolean found = false;
            if (this.originatingProvider != null) {
                Class<?>[] classes = this.originatingProvider.getInputTypes();
                int i = 0;
                while (i < classes.length) {
                    if (classes[i].isInstance(input)) {
                        found = true;
                    }
                    i = i + 1;
                }
            } else if (input instanceof ImageInputStream) {
                found = true;
            }
            if (!found) {
                throw new IllegalArgumentException("Incorrect input type!");
            }
            this.seekForwardOnly = seekForwardOnly;
            this.ignoreMetadata = ignoreMetadata;
            this.minIndex = 0;
        }
        this.input = input;
    }

    /** Idem, sin prometer nada sobre los metadatos. */
    public void setInput(Object input, boolean seekForwardOnly) {
        setInput(input, seekForwardOnly, false);
    }

    /** Idem, sin prometer nada. */
    public void setInput(Object input) {
        setInput(input, false, false);
    }

    /** De donde lee, o null. */
    public Object getInput() {
        return this.input;
    }

    /** Si se prometio no volver atras. */
    public boolean isSeekForwardOnly() {
        return this.seekForwardOnly;
    }

    /** Si se prometio no pedir metadatos. */
    public boolean isIgnoringMetadata() {
        return this.ignoreMetadata;
    }

    /** La imagen mas baja que todavia se puede pedir. Ver la nota de la clase. */
    public int getMinIndex() {
        return this.minIndex;
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
     * Cuantas imagenes hay.
     *
     * @param allowSearch si esta permitido recorrer el archivo para contarlas. Con false, un lector
     *     que no lo sepa de antemano devuelve -1 en lugar de tardar
     * @throws IllegalStateException si no hay entrada
     * @throws IllegalStateException si se pide buscar y se prometio no volver atras
     * @throws IOException si fallo la lectura
     */
    public abstract int getNumImages(boolean allowSearch) throws IOException;

    /**
     * El ancho de esa imagen.
     *
     * @throws IllegalStateException si no hay entrada
     * @throws IndexOutOfBoundsException si esa imagen no existe o quedo atras
     * @throws IOException si fallo la lectura
     */
    public abstract int getWidth(int imageIndex) throws IOException;

    /**
     * El alto.
     *
     * @throws IOException si fallo la lectura
     */
    public abstract int getHeight(int imageIndex) throws IOException;

    /**
     * Si leer pedazos sueltos de esa imagen sale barato.
     *
     * <p>Por omision false, que es lo conservador: quien pregunte va a leer entera en lugar de por
     * partes, y eso siempre funciona.
     *
     * @throws IOException si fallo la lectura
     */
    public boolean isRandomAccessEasy(int imageIndex) throws IOException {
        return false;
    }

    /**
     * La relacion entre ancho y alto <b>tal como hay que mostrarla</b>.
     *
     * <p>Por omision es el ancho dividido el alto, que supone pixeles cuadrados. Un formato con
     * pixeles no cuadrados --video antiguo, algunos TIFF-- redefine esto, y ahi el numero no coincide
     * con la division.
     *
     * @throws IOException si fallo la lectura
     */
    public float getAspectRatio(int imageIndex) throws IOException {
        return (float) getWidth(imageIndex) / getHeight(imageIndex);
    }

    /**
     * De que tipo son los pixeles tal como estan en el archivo, o null si no aplica.
     *
     * <p>Por omision, el primero de {@link #getImageTypes}. Sirve para decodificar sin convertir, que
     * es lo mas rapido y lo unico que no pierde.
     *
     * @throws IOException si fallo la lectura
     */
    public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
        return getImageTypes(imageIndex).next();
    }

    /**
     * De que tipos puede entregar esa imagen, el mas natural primero.
     *
     * @throws IOException si fallo la lectura
     */
    public abstract Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException;

    /**
     * Un objeto de parametros vacio, del tipo que este lector entiende.
     *
     * <p>Una subclase con parametros propios lo redefine para devolver los suyos.
     */
    public ImageReadParam getDefaultReadParam() {
        return new ImageReadParam();
    }

    /**
     * Los metadatos del archivo entero, o null si no hay.
     *
     * @throws IOException si fallo la lectura
     */
    public abstract IIOMetadata getStreamMetadata() throws IOException;

    /**
     * Idem, en un formato concreto y pidiendo solo esos nodos.
     *
     * <p>El conjunto de nodos permite leer un arbol grande sin armarlo entero.
     *
     * @throws IllegalArgumentException si el formato no es uno de los que este lector entiende
     * @throws IOException si fallo la lectura
     */
    public IIOMetadata getStreamMetadata(String formatName, Set<String> nodeNames)
        throws IOException {
        return getMetadata(getStreamMetadata(), formatName, nodeNames);
    }

    /**
     * Los metadatos de esa imagen, o null.
     *
     * @throws IOException si fallo la lectura
     */
    public abstract IIOMetadata getImageMetadata(int imageIndex) throws IOException;

    /**
     * Idem, en un formato concreto.
     *
     * @throws IOException si fallo la lectura
     */
    public IIOMetadata getImageMetadata(int imageIndex, String formatName, Set<String> nodeNames)
        throws IOException {
        return getMetadata(getImageMetadata(imageIndex), formatName, nodeNames);
    }

    /**
     * Esa imagen, con los parametros por omision.
     *
     * @throws IOException si fallo la lectura
     */
    public BufferedImage read(int imageIndex) throws IOException {
        return read(imageIndex, null);
    }

    /**
     * Esa imagen.
     *
     * <p>Es el metodo que hace el trabajo, y el unico que una subclase <b>tiene</b> que escribir para
     * decodificar.
     *
     * @param param que parte y como, o null para todo
     * @throws IllegalStateException si no hay entrada
     * @throws IndexOutOfBoundsException si esa imagen no existe o quedo atras
     * @throws IOException si fallo la lectura
     */
    public abstract BufferedImage read(int imageIndex, ImageReadParam param) throws IOException;

    /**
     * Esa imagen con sus miniaturas y sus metadatos.
     *
     * @throws IOException si fallo la lectura
     */
    public IIOImage readAll(int imageIndex, ImageReadParam param) throws IOException {
        if (imageIndex < getMinIndex()) {
            throw new IndexOutOfBoundsException("imageIndex < getMinIndex()!");
        }
        BufferedImage im = read(imageIndex, param);
        ArrayList<BufferedImage> thumbnails = null;
        int numThumbnails = getNumThumbnails(imageIndex);
        if (numThumbnails > 0) {
            thumbnails = new ArrayList<BufferedImage>();
            int j = 0;
            while (j < numThumbnails) {
                thumbnails.add(readThumbnail(imageIndex, j));
                j = j + 1;
            }
        }
        IIOMetadata metadata = getImageMetadata(imageIndex);
        return new IIOImage(im, thumbnails, metadata);
    }

    /**
     * Todas las imagenes, de a una.
     *
     * <p>El iterador que devuelve es <b>perezoso</b>: cada {@code next()} decodifica la imagen
     * siguiente. Es lo que permite recorrer un TIFF de cien paginas sin tenerlas todas en memoria.
     *
     * @param params un parametro por imagen; null usa los de omision para todas
     * @throws IOException si fallo la lectura
     */
    public Iterator<IIOImage> readAll(Iterator<? extends ImageReadParam> params)
        throws IOException {
        List<IIOImage> output = new ArrayList<IIOImage>();
        int imageIndex = getMinIndex();
        processSequenceStarted(imageIndex);
        while (true) {
            ImageReadParam param = null;
            if (params != null && params.hasNext()) {
                Object o = params.next();
                if (o != null && !(o instanceof ImageReadParam)) {
                    throw new IllegalArgumentException("Non-ImageReadParam supplied as part of params!");
                }
                param = (ImageReadParam) o;
            }
            BufferedImage bi;
            try {
                bi = read(imageIndex, param);
            } catch (IndexOutOfBoundsException e) {
                // No hay mas imagenes. Es como termina el recorrido de un formato que no dice de
                // antemano cuantas tiene.
                break;
            }
            ArrayList<BufferedImage> thumbnails = null;
            int numThumbnails = getNumThumbnails(imageIndex);
            if (numThumbnails > 0) {
                thumbnails = new ArrayList<BufferedImage>();
                int j = 0;
                while (j < numThumbnails) {
                    thumbnails.add(readThumbnail(imageIndex, j));
                    j = j + 1;
                }
            }
            output.add(new IIOImage(bi, thumbnails, getImageMetadata(imageIndex)));
            imageIndex = imageIndex + 1;
        }
        processSequenceComplete();
        return output.iterator();
    }

    /**
     * Si sabe entregar pixeles crudos sin modelo de color.
     *
     * <p>Por omision false. Ver {@link #readRaster}.
     */
    public boolean canReadRaster() {
        return false;
    }

    /**
     * Los pixeles crudos, sin interpretarlos.
     *
     * <p>Existe para los formatos cuyos datos <b>no son colores</b>: una imagen medica, una banda
     * satelital. Forzar un modelo de color ahi seria inventar; ver {@link IIOImage}.
     *
     * @throws UnsupportedOperationException si este lector no sabe
     * @throws IOException si fallo la lectura
     */
    public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
        throw new UnsupportedOperationException("readRaster not supported!");
    }

    /**
     * Si esa imagen esta partida en teselas.
     *
     * @throws IOException si fallo la lectura
     */
    public boolean isImageTiled(int imageIndex) throws IOException {
        return false;
    }

    /**
     * El ancho de tesela; el de la imagen si no esta en teselas.
     *
     * @throws IOException si fallo la lectura
     */
    public int getTileWidth(int imageIndex) throws IOException {
        return getWidth(imageIndex);
    }

    /**
     * El alto de tesela.
     *
     * @throws IOException si fallo la lectura
     */
    public int getTileHeight(int imageIndex) throws IOException {
        return getHeight(imageIndex);
    }

    /**
     * El desplazamiento de la rejilla en X.
     *
     * @throws IOException si fallo la lectura
     */
    public int getTileGridXOffset(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * Idem en Y.
     *
     * @throws IOException si fallo la lectura
     */
    public int getTileGridYOffset(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * Una tesela.
     *
     * <p>Por omision, si se pide la tesela (0,0) de una imagen sin teselas, lee la imagen entera. Para
     * cualquier otra lanza, porque no existe.
     *
     * @throws IllegalArgumentException si esa tesela no existe
     * @throws IOException si fallo la lectura
     */
    public BufferedImage readTile(int imageIndex, int tileX, int tileY) throws IOException {
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException("Invalid tile indices");
        }
        return read(imageIndex);
    }

    /**
     * Una tesela como pixeles crudos.
     *
     * @throws UnsupportedOperationException si este lector no sabe leer rasters
     * @throws IOException si fallo la lectura
     */
    public Raster readTileRaster(int imageIndex, int tileX, int tileY) throws IOException {
        if (!canReadRaster()) {
            throw new UnsupportedOperationException("readTileRaster not supported!");
        }
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException("Invalid tile indices");
        }
        return readRaster(imageIndex, null);
    }

    /**
     * Esa imagen como {@link RenderedImage}.
     *
     * <p>Un lector que sepa decodificar por teselas puede devolver algo <b>perezoso</b>, que
     * decodifica cada tesela al pedirla. Esta implementacion decodifica todo de una.
     *
     * @throws IOException si fallo la lectura
     */
    public RenderedImage readAsRenderedImage(int imageIndex, ImageReadParam param)
        throws IOException {
        return read(imageIndex, param);
    }

    /** Si este lector sabe entregar las miniaturas incrustadas. */
    public boolean readerSupportsThumbnails() {
        return false;
    }

    /**
     * Si esa imagen tiene miniaturas.
     *
     * @throws IOException si fallo la lectura
     */
    public boolean hasThumbnails(int imageIndex) throws IOException {
        return getNumThumbnails(imageIndex) > 0;
    }

    /**
     * Cuantas.
     *
     * @throws IOException si fallo la lectura
     */
    public int getNumThumbnails(int imageIndex) throws IOException {
        return 0;
    }

    /**
     * El ancho de una miniatura.
     *
     * @throws UnsupportedOperationException si este lector no maneja miniaturas
     * @throws IOException si fallo la lectura
     */
    public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
        return readThumbnail(imageIndex, thumbnailIndex).getWidth();
    }

    /**
     * El alto.
     *
     * @throws IOException si fallo la lectura
     */
    public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
        return readThumbnail(imageIndex, thumbnailIndex).getHeight();
    }

    /**
     * Una miniatura.
     *
     * @throws UnsupportedOperationException si este lector no las maneja
     * @throws IOException si fallo la lectura
     */
    public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
        throw new UnsupportedOperationException("Thumbnails not supported!");
    }

    /** Pide cancelar. Se llama desde otro hilo; ver la nota de la clase. */
    public synchronized void abort() {
        this.abortFlag = true;
    }

    /** Si alguien pidio cancelar. La subclase lo consulta seguido. */
    protected synchronized boolean abortRequested() {
        return this.abortFlag;
    }

    /** Limpia el pedido. La subclase lo llama al empezar cada operacion; ver la nota de la clase. */
    protected synchronized void clearAbortRequest() {
        this.abortFlag = false;
    }

    /** Registra un escucha de advertencias; null no hace nada. */
    public void addIIOReadWarningListener(IIOReadWarningListener listener) {
        if (listener == null) {
            return;
        }
        if (this.warningListeners == null) {
            this.warningListeners = new ArrayList<IIOReadWarningListener>();
            this.warningLocales = new ArrayList<Locale>();
        }
        this.warningListeners.add(listener);
        // El idioma se guarda al registrar: un escucha registrado en frances tiene que seguir
        // recibiendo frances aunque despues el lector cambie de idioma.
        this.warningLocales.add(getLocale());
    }

    /** Lo da de baja. */
    public void removeIIOReadWarningListener(IIOReadWarningListener listener) {
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
    public void removeAllIIOReadWarningListeners() {
        this.warningListeners = null;
        this.warningLocales = null;
    }

    /** Registra un escucha de avance. */
    public void addIIOReadProgressListener(IIOReadProgressListener listener) {
        if (listener == null) {
            return;
        }
        if (this.progressListeners == null) {
            this.progressListeners = new ArrayList<IIOReadProgressListener>();
        }
        this.progressListeners.add(listener);
    }

    /** Lo da de baja. */
    public void removeIIOReadProgressListener(IIOReadProgressListener listener) {
        if (listener == null || this.progressListeners == null) {
            return;
        }
        this.progressListeners.remove(listener);
        if (this.progressListeners.isEmpty()) {
            this.progressListeners = null;
        }
    }

    /** Los da de baja a todos. */
    public void removeAllIIOReadProgressListeners() {
        this.progressListeners = null;
    }

    /** Registra un escucha de imagen parcial. */
    public void addIIOReadUpdateListener(IIOReadUpdateListener listener) {
        if (listener == null) {
            return;
        }
        if (this.updateListeners == null) {
            this.updateListeners = new ArrayList<IIOReadUpdateListener>();
        }
        this.updateListeners.add(listener);
    }

    /** Lo da de baja. */
    public void removeIIOReadUpdateListener(IIOReadUpdateListener listener) {
        if (listener == null || this.updateListeners == null) {
            return;
        }
        this.updateListeners.remove(listener);
        if (this.updateListeners.isEmpty()) {
            this.updateListeners = null;
        }
    }

    /** Los da de baja a todos. */
    public void removeAllIIOReadUpdateListeners() {
        this.updateListeners = null;
    }

    /** Avisa que empieza una secuencia. */
    protected void processSequenceStarted(int minIndex) {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).sequenceStarted(this, minIndex);
            i = i + 1;
        }
    }

    /** Avisa que termino. */
    protected void processSequenceComplete() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).sequenceComplete(this);
            i = i + 1;
        }
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

    /** Avisa que termino la imagen. */
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

    /** Avisa que se corto. La subclase lo llama al atender un {@link #abort}. */
    protected void processReadAborted() {
        if (this.progressListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.progressListeners.size()) {
            this.progressListeners.get(i).readAborted(this);
            i = i + 1;
        }
    }

    /** Avisa que empieza una pasada. */
    protected void processPassStarted(BufferedImage theImage, int pass, int minPass, int maxPass,
                                      int minX, int minY, int periodX, int periodY, int[] bands) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).passStarted(this, theImage, pass, minPass, maxPass, minX,
                                                    minY, periodX, periodY, bands);
            i = i + 1;
        }
    }

    /** Avisa que cambio un pedazo de la imagen. */
    protected void processImageUpdate(BufferedImage theImage, int minX, int minY, int width,
                                      int height, int periodX, int periodY, int[] bands) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).imageUpdate(this, theImage, minX, minY, width, height,
                                                    periodX, periodY, bands);
            i = i + 1;
        }
    }

    /** Avisa que termino la pasada. */
    protected void processPassComplete(BufferedImage theImage) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).passComplete(this, theImage);
            i = i + 1;
        }
    }

    /** Idem, para una miniatura. */
    protected void processThumbnailPassStarted(BufferedImage theThumbnail, int pass, int minPass,
                                               int maxPass, int minX, int minY, int periodX,
                                               int periodY, int[] bands) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).thumbnailPassStarted(this, theThumbnail, pass, minPass,
                                                             maxPass, minX, minY, periodX,
                                                             periodY, bands);
            i = i + 1;
        }
    }

    /** Idem. */
    protected void processThumbnailUpdate(BufferedImage theThumbnail, int minX, int minY,
                                          int width, int height, int periodX, int periodY,
                                          int[] bands) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).thumbnailUpdate(this, theThumbnail, minX, minY, width,
                                                        height, periodX, periodY, bands);
            i = i + 1;
        }
    }

    /** Idem. */
    protected void processThumbnailPassComplete(BufferedImage theThumbnail) {
        if (this.updateListeners == null) {
            return;
        }
        int i = 0;
        while (i < this.updateListeners.size()) {
            this.updateListeners.get(i).thumbnailPassComplete(this, theThumbnail);
            i = i + 1;
        }
    }

    /** Avisa de una advertencia. */
    protected void processWarningOccurred(String warning) {
        if (this.warningListeners == null) {
            return;
        }
        if (warning == null) {
            throw new IllegalArgumentException("warning == null!");
        }
        int i = 0;
        while (i < this.warningListeners.size()) {
            this.warningListeners.get(i).warningOccurred(this, warning);
            i = i + 1;
        }
    }

    /**
     * Idem, con el texto sacado de un paquete de recursos.
     *
     * <p>A cada escucha se le da el mensaje <b>en el idioma con el que se registro</b>, no en el que
     * el lector tiene puesto ahora; ver {@link #addIIOReadWarningListener}.
     *
     * @throws IllegalArgumentException si el paquete o la clave son null, o si la clave no esta
     */
    protected void processWarningOccurred(String baseName, String keyword) {
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
            IIOReadWarningListener listener = this.warningListeners.get(i);
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
            listener.warningOccurred(this, warning);
            i = i + 1;
        }
    }

    /**
     * Vuelve al estado inicial: sin entrada, sin escuchas, sin idioma.
     *
     * <p>Es lo que permite reusar un lector con otro archivo. No libera recursos nativos; para eso
     * esta {@link #dispose}.
     */
    public void reset() {
        setInput(null, false, false);
        setLocale(null);
        removeAllIIOReadUpdateListeners();
        removeAllIIOReadWarningListeners();
        removeAllIIOReadProgressListeners();
        clearAbortRequest();
    }

    /**
     * Libera lo que el lector tenga tomado.
     *
     * <p>Despues de esto el lector <b>no se puede usar mas</b>, a diferencia de {@link #reset}. Esta
     * implementacion no hace nada: solo una subclase con recursos propios necesita algo aca.
     */
    public void dispose() {
    }

    /**
     * Que region de la fuente hay que leer, ya recortada al tamano real.
     *
     * <p>Es el calculo que toda subclase necesita al empezar a decodificar, y esta aca para que no lo
     * repita cada una --y para que ninguna se olvide de recortar contra el tamano real, que es como se
     * termina leyendo fuera del archivo--.
     *
     * @param param los parametros, o null para toda la imagen
     */
    protected static Rectangle getSourceRegion(ImageReadParam param, int srcWidth,
                                               int srcHeight) {
        Rectangle sourceRegion = new Rectangle(0, 0, srcWidth, srcHeight);
        if (param != null) {
            Rectangle region = param.getSourceRegion();
            if (region != null) {
                sourceRegion = sourceRegion.intersection(region);
            }
            int subsampleXOffset = param.getSubsamplingXOffset();
            int subsampleYOffset = param.getSubsamplingYOffset();
            sourceRegion.x = sourceRegion.x + subsampleXOffset;
            sourceRegion.y = sourceRegion.y + subsampleYOffset;
            sourceRegion.width = sourceRegion.width - subsampleXOffset;
            sourceRegion.height = sourceRegion.height - subsampleYOffset;
        }
        return sourceRegion;
    }

    /**
     * Calcula que se lee y donde se escribe, contemplando recorte, submuestreo y desplazamiento.
     *
     * <p>Rellena los dos rectangulos que se le pasan. Es la contraparte de
     * {@link #getSourceRegion} del lado del destino, y hace la parte que mas se equivoca: recortar
     * tambien contra el tamano de la imagen destino.
     *
     * @throws IllegalArgumentException si alguno de los rectangulos es null, o si no queda ni un
     *     pixel que leer
     */
    protected static void computeRegions(ImageReadParam param, int srcWidth, int srcHeight,
                                         BufferedImage image, Rectangle srcRegion,
                                         Rectangle destRegion) {
        if (srcRegion == null) {
            throw new IllegalArgumentException("srcRegion == null!");
        }
        if (destRegion == null) {
            throw new IllegalArgumentException("destRegion == null!");
        }
        srcRegion.setBounds(getSourceRegion(param, srcWidth, srcHeight));
        int periodX = 1;
        int periodY = 1;
        int gridX = 0;
        int gridY = 0;
        if (param != null) {
            periodX = param.getSourceXSubsampling();
            periodY = param.getSourceYSubsampling();
            java.awt.Point p = param.getDestinationOffset();
            gridX = p.x;
            gridY = p.y;
        }
        destRegion.setBounds(gridX, gridY,
                             (srcRegion.width + periodX - 1) / periodX,
                             (srcRegion.height + periodY - 1) / periodY);
        if (gridX < 0) {
            // Un desplazamiento negativo descarta pixeles de la izquierda de lo leido; hay que
            // avanzar la region de origen para que lo que quede se corresponda.
            int delta = -gridX * periodX;
            srcRegion.x = srcRegion.x + delta;
            srcRegion.width = srcRegion.width - delta;
            destRegion.x = 0;
            destRegion.width = destRegion.width + gridX;
        }
        if (gridY < 0) {
            int delta = -gridY * periodY;
            srcRegion.y = srcRegion.y + delta;
            srcRegion.height = srcRegion.height - delta;
            destRegion.y = 0;
            destRegion.height = destRegion.height + gridY;
        }
        if (image != null) {
            Rectangle destBounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
            destRegion.setBounds(destRegion.intersection(destBounds));
        }
        if (destRegion.isEmpty()) {
            throw new IllegalArgumentException("Empty destination region!");
        }
        int deltaX = destRegion.x - gridX;
        if (deltaX > 0) {
            srcRegion.x = srcRegion.x + deltaX * periodX;
        }
        int deltaY = destRegion.y - gridY;
        if (deltaY > 0) {
            srcRegion.y = srcRegion.y + deltaY * periodY;
        }
        srcRegion.width = destRegion.width * periodX;
        srcRegion.height = destRegion.height * periodY;
        if (srcRegion.isEmpty()) {
            throw new IllegalArgumentException("Empty source region!");
        }
    }

    /**
     * Que las bandas pedidas existan de los dos lados y sean la misma cantidad.
     *
     * @throws IllegalArgumentException si las cantidades no coinciden
     * @throws IndexOutOfBoundsException si alguna banda no existe
     */
    protected static void checkReadParamBandSettings(ImageReadParam param, int numSrcBands,
                                                     int numDstBands) {
        int[] sourceBands = null;
        int[] destinationBands = null;
        if (param != null) {
            sourceBands = param.getSourceBands();
            destinationBands = param.getDestinationBands();
        }
        int paramSrcBandLength = 0;
        if (sourceBands != null) {
            paramSrcBandLength = sourceBands.length;
        }
        int paramDstBandLength = 0;
        if (destinationBands != null) {
            paramDstBandLength = destinationBands.length;
        }
        int virtualSrcBands = numSrcBands;
        if (paramSrcBandLength != 0) {
            virtualSrcBands = paramSrcBandLength;
        }
        int virtualDstBands = numDstBands;
        if (paramDstBandLength != 0) {
            virtualDstBands = paramDstBandLength;
        }
        if (virtualSrcBands != virtualDstBands) {
            throw new IllegalArgumentException("Number of source and destination bands differ!");
        }
        checkBandRange(sourceBands, numSrcBands, "Source band index out of bounds!");
        checkBandRange(destinationBands, numDstBands, "Destination band index out of bounds!");
    }

    /**
     * Consigue la imagen donde escribir: la que se dio, o una nueva del tipo que corresponda.
     *
     * <p>Es lo que centraliza la eleccion entre {@code setDestination} y {@code setDestinationType};
     * ver {@link ImageReadParam}.
     *
     * @param imageTypes los tipos que el lector puede entregar, el preferido primero
     * @throws IIOException si el tipo pedido no esta entre los que el lector ofrece
     * @throws IllegalArgumentException si los tipos son null o vacios, o si el tamano se desborda
     */
    protected static BufferedImage getDestination(ImageReadParam param,
                                                  Iterator<ImageTypeSpecifier> imageTypes,
                                                  int width, int height) throws IIOException {
        if (imageTypes == null || !imageTypes.hasNext()) {
            throw new IllegalArgumentException("imageTypes null or empty!");
        }
        if ((long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("width*height > Integer.MAX_VALUE!");
        }
        ImageTypeSpecifier imageType = null;
        if (param != null) {
            BufferedImage dest = param.getDestination();
            if (dest != null) {
                return dest;
            }
            imageType = param.getDestinationType();
        }
        if (imageType == null) {
            imageType = imageTypes.next();
        } else {
            // El tipo pedido tiene que estar entre los que el lector puede dar: si no, se estaria
            // prometiendo una conversion que nadie va a hacer.
            boolean foundIt = false;
            while (imageTypes.hasNext()) {
                ImageTypeSpecifier type = imageTypes.next();
                if (type.equals(imageType)) {
                    foundIt = true;
                }
            }
            if (!foundIt) {
                throw new IIOException("Destination type from ImageReadParam does not match!");
            }
        }
        Rectangle srcRegion = new Rectangle(0, 0, 0, 0);
        Rectangle destRegion = new Rectangle(0, 0, 0, 0);
        computeRegions(param, width, height, null, srcRegion, destRegion);
        int destWidth = destRegion.x + destRegion.width;
        int destHeight = destRegion.y + destRegion.height;
        if ((long) destWidth * destHeight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("destination width*height > Integer.MAX_VALUE!");
        }
        return imageType.createBufferedImage(destWidth, destHeight);
    }

    /** Que todas las bandas de ese arreglo existan. */
    private static void checkBandRange(int[] bands, int numBands, String message) {
        if (bands == null) {
            return;
        }
        int i = 0;
        while (i < bands.length) {
            if (bands[i] >= numBands) {
                throw new IllegalArgumentException(message);
            }
            i = i + 1;
        }
    }

    /**
     * Filtra unos metadatos por formato y por nodos.
     *
     * <p>Lo comparten los dos {@code getXxxMetadata} de tres argumentos. Comprobar que el formato sea
     * uno de los declarados es lo que convierte un nombre mal escrito en un error inmediato en lugar
     * de un arbol vacio.
     */
    private static IIOMetadata getMetadata(IIOMetadata metadata, String formatName,
                                           Set<String> nodeNames) {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (nodeNames == null) {
            throw new IllegalArgumentException("nodeNames == null!");
        }
        if (metadata == null) {
            return null;
        }
        String[] formats = metadata.getMetadataFormatNames();
        boolean found = false;
        int i = 0;
        while (formats != null && i < formats.length) {
            if (formatName.equals(formats[i])) {
                found = true;
            }
            i = i + 1;
        }
        if (!found) {
            throw new IllegalArgumentException("Unsupported format name");
        }
        return metadata;
    }
}
