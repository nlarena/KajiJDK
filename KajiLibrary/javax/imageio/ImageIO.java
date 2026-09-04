package javax.imageio;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageInputStreamSpi;
import javax.imageio.spi.ImageOutputStreamSpi;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageReaderWriterSpi;
import javax.imageio.spi.ImageTranscoderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

/**
 * KajiLibrary's javax.imageio.ImageIO -- leer y escribir imagenes en una linea.
 *
 * <p>La fachada de todo el paquete. Debajo consulta el {@link IIORegistry} y arma lo que haga falta;
 * para el caso normal --{@code ImageIO.read(new File("foto.png"))}-- eso queda invisible.
 *
 * <h2>{@link #read} devuelve null cuando nadie reconoce el formato</h2>
 *
 * <p>Es lo que mas sorprende de esta clase: no lanza. Un archivo que no es una imagen, o de un formato
 * sin lector registrado, da <b>null</b>; la excepcion queda para los errores de entrada y salida de
 * verdad.
 *
 * <p>Un programa que no comprueba el null termina con un {@code NullPointerException} lejos del sitio
 * que lo causo.
 *
 * <h2>{@link #write} devuelve false por lo mismo</h2>
 *
 * <p>Sin escritor para ese nombre de formato devuelve false y <b>no escribe nada</b>. El archivo de
 * destino igual se creo, y queda vacio -- vale la pena borrarlo.
 *
 * <h2>La cache</h2>
 *
 * <p>{@link #setUseCache} decide si los flujos que se creen sobre {@code InputStream} pueden usar un
 * archivo temporal en lugar de memoria. Por omision <b>si</b>, que es lo correcto para imagenes
 * grandes; apagarlo es lo que se hace en un entorno sin disco escribible.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Todo el mecanismo esta implementado: el registro, la busqueda por nombre, extension y tipo MIME,
 * la creacion de flujos, y el emparejamiento entre lectores y escritores. Lo que esta biblioteca no
 * trae son <b>complementos</b>: decodificar PNG o JPEG pide los codecs.
 *
 * <p>Con eso, {@link #read} devuelve null y {@link #write} devuelve false -- que es exactamente lo que
 * hace el JDK cuando nadie registro un lector para ese formato. Los flujos si funcionan de verdad:
 * {@link #createImageInputStream} sobre un {@code File} o un {@code InputStream} devuelve un flujo
 * usable, porque esos dos proveedores estan escritos.
 */
public final class ImageIO {

    /** Si los flujos pueden usar archivo temporal. Ver la nota de la clase. */
    private static boolean useCache = true;

    /** Donde ponerlo, o null para el del sistema. */
    private static File cacheDirectory = null;

    /** No se instancia. */
    private ImageIO() {
    }

    /**
     * Vuelve a buscar complementos en la ruta de clases.
     *
     * <p>Hace falta cuando aparecen despues de arrancar; ver
     * {@link IIORegistry#registerApplicationClasspathSpis}.
     */
    public static void scanForPlugins() {
        IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
    }

    /** Si los flujos pueden usar archivo temporal. */
    public static void setUseCache(boolean useCache) {
        ImageIO.useCache = useCache;
    }

    /** Si pueden. */
    public static boolean getUseCache() {
        return useCache;
    }

    /**
     * Donde poner los temporales; null usa el del sistema.
     *
     * @throws IllegalArgumentException si no es un directorio
     */
    public static void setCacheDirectory(File cacheDirectory) {
        if (cacheDirectory != null && !cacheDirectory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory!");
        }
        ImageIO.cacheDirectory = cacheDirectory;
    }

    /** Donde se ponen, o null. */
    public static File getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Envuelve eso en un flujo de entrada de imagenes.
     *
     * @return el flujo, o null si nadie sabe envolver esa clase de objeto
     * @throws IllegalArgumentException si es null
     * @throws IOException si no se pudo crear
     */
    public static ImageInputStream createImageInputStream(Object input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        Iterator<ImageInputStreamSpi> it =
            registry().getServiceProviders(ImageInputStreamSpi.class, true);
        while (it.hasNext()) {
            ImageInputStreamSpi spi = it.next();
            if (spi.getInputClass().isInstance(input)) {
                return spi.createInputStreamInstance(input, getUseCache(), getCacheDirectory());
            }
        }
        return null;
    }

    /**
     * Idem, de salida.
     *
     * @return el flujo, o null si nadie sabe
     * @throws IllegalArgumentException si es null
     * @throws IOException si no se pudo crear
     */
    public static ImageOutputStream createImageOutputStream(Object output) throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        Iterator<ImageOutputStreamSpi> it =
            registry().getServiceProviders(ImageOutputStreamSpi.class, true);
        while (it.hasNext()) {
            ImageOutputStreamSpi spi = it.next();
            if (spi.getOutputClass().isInstance(output)) {
                return spi.createOutputStreamInstance(output, getUseCache(), getCacheDirectory());
            }
        }
        return null;
    }

    /** Que formatos se pueden leer, sin repetidos. */
    public static String[] getReaderFormatNames() {
        return collect(ImageReaderSpi.class, NAMES);
    }

    /** Que tipos MIME. */
    public static String[] getReaderMIMETypes() {
        return collect(ImageReaderSpi.class, MIME);
    }

    /** Que extensiones. */
    public static String[] getReaderFileSuffixes() {
        return collect(ImageReaderSpi.class, SUFFIXES);
    }

    /**
     * Los lectores que reconocen lo que hay en esa entrada.
     *
     * <p>Le pregunta a cada proveedor con {@code canDecodeInput}; ver la regla de rebobinar en
     * {@link ImageReaderSpi}.
     *
     * @throws IllegalArgumentException si es null
     */
    public static Iterator<ImageReader> getImageReaders(Object input) {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        List<ImageReader> readers = new ArrayList<ImageReader>();
        Iterator<ImageReaderSpi> it =
            registry().getServiceProviders(ImageReaderSpi.class, true);
        while (it.hasNext()) {
            ImageReaderSpi spi = it.next();
            try {
                if (spi.canDecodeInput(input)) {
                    readers.add(spi.createReaderInstance());
                }
            } catch (IOException e) {
                // Ese proveedor no pudo mirar la entrada; los demas todavia pueden.
            }
        }
        return readers.iterator();
    }

    /**
     * Los lectores de ese formato.
     *
     * @throws IllegalArgumentException si el nombre es null
     */
    public static Iterator<ImageReader> getImageReadersByFormatName(String formatName) {
        return readersMatching(formatName, NAMES);
    }

    /**
     * Los de esa extension.
     *
     * @throws IllegalArgumentException si es null
     */
    public static Iterator<ImageReader> getImageReadersBySuffix(String fileSuffix) {
        return readersMatching(fileSuffix, SUFFIXES);
    }

    /**
     * Los de ese tipo MIME.
     *
     * @throws IllegalArgumentException si es null
     */
    public static Iterator<ImageReader> getImageReadersByMIMEType(String MIMEType) {
        return readersMatching(MIMEType, MIME);
    }

    /** Que formatos se pueden escribir. */
    public static String[] getWriterFormatNames() {
        return collect(ImageWriterSpi.class, NAMES);
    }

    /** Que tipos MIME. */
    public static String[] getWriterMIMETypes() {
        return collect(ImageWriterSpi.class, MIME);
    }

    /** Que extensiones. */
    public static String[] getWriterFileSuffixes() {
        return collect(ImageWriterSpi.class, SUFFIXES);
    }

    /**
     * Los escritores de ese formato.
     *
     * @throws IllegalArgumentException si es null
     */
    public static Iterator<ImageWriter> getImageWritersByFormatName(String formatName) {
        return writersMatching(formatName, NAMES);
    }

    /**
     * Los de esa extension.
     *
     * @throws IllegalArgumentException si es null
     */
    public static Iterator<ImageWriter> getImageWritersBySuffix(String fileSuffix) {
        return writersMatching(fileSuffix, SUFFIXES);
    }

    /**
     * Los de ese tipo MIME.
     *
     * @throws IllegalArgumentException si es null
     */
    public static Iterator<ImageWriter> getImageWritersByMIMEType(String MIMEType) {
        return writersMatching(MIMEType, MIME);
    }

    /**
     * El escritor del mismo formato que ese lector, o null.
     *
     * <p>Es como se reescribe lo que se acaba de leer sin cambiar de formato; ver
     * {@link ImageReaderSpi#getImageWriterSpiNames}.
     *
     * @throws IllegalArgumentException si es null
     */
    public static ImageWriter getImageWriter(ImageReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader == null!");
        }
        ImageReaderSpi readerSpi = reader.getOriginatingProvider();
        if (readerSpi == null) {
            return null;
        }
        String[] writerNames = readerSpi.getImageWriterSpiNames();
        if (writerNames == null || writerNames.length == 0) {
            return null;
        }
        int i = 0;
        while (i < writerNames.length) {
            ImageWriterSpi spi = spiByName(writerNames[i], ImageWriterSpi.class);
            if (spi != null) {
                try {
                    return spi.createWriterInstance();
                } catch (IOException e) {
                    // Ese no se pudo crear; se prueba con el siguiente hermano.
                }
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * El lector del mismo formato que ese escritor, o null.
     *
     * @throws IllegalArgumentException si es null
     */
    public static ImageReader getImageReader(ImageWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer == null!");
        }
        ImageWriterSpi writerSpi = writer.getOriginatingProvider();
        if (writerSpi == null) {
            return null;
        }
        String[] readerNames = writerSpi.getImageReaderSpiNames();
        if (readerNames == null || readerNames.length == 0) {
            return null;
        }
        int i = 0;
        while (i < readerNames.length) {
            ImageReaderSpi spi = spiByName(readerNames[i], ImageReaderSpi.class);
            if (spi != null) {
                try {
                    return spi.createReaderInstance();
                } catch (IOException e) {
                    // Ver arriba.
                }
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * Los escritores que puedan escribir ese tipo de imagen en ese formato.
     *
     * <p>Cruza las dos condiciones, que es lo que hace falta antes de escribir: que el formato exista
     * y que ademas soporte ese tipo de pixel.
     *
     * @throws IllegalArgumentException si el nombre es null
     */
    public static Iterator<ImageWriter> getImageWriters(ImageTypeSpecifier type,
                                                        String formatName) {
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        List<ImageWriter> writers = new ArrayList<ImageWriter>();
        Iterator<ImageWriterSpi> it =
            registry().getServiceProviders(ImageWriterSpi.class, true);
        while (it.hasNext()) {
            ImageWriterSpi spi = it.next();
            if (matches(spi, formatName, NAMES) && (type == null || spi.canEncodeImage(type))) {
                try {
                    writers.add(spi.createWriterInstance());
                } catch (IOException e) {
                    // Ver arriba.
                }
            }
        }
        return writers.iterator();
    }

    /**
     * Los traductores de metadatos entre ese lector y ese escritor.
     *
     * @throws IllegalArgumentException si alguno es null
     */
    public static Iterator<ImageTranscoder> getImageTranscoders(ImageReader reader,
                                                                ImageWriter writer) {
        if (reader == null) {
            throw new IllegalArgumentException("reader == null!");
        }
        if (writer == null) {
            throw new IllegalArgumentException("writer == null!");
        }
        List<ImageTranscoder> transcoders = new ArrayList<ImageTranscoder>();
        ImageReaderSpi readerSpi = reader.getOriginatingProvider();
        ImageWriterSpi writerSpi = writer.getOriginatingProvider();
        if (readerSpi == null || writerSpi == null) {
            return transcoders.iterator();
        }
        String readerName = readerSpi.getClass().getName();
        String writerName = writerSpi.getClass().getName();
        Iterator<ImageTranscoderSpi> it =
            registry().getServiceProviders(ImageTranscoderSpi.class, true);
        while (it.hasNext()) {
            ImageTranscoderSpi spi = it.next();
            if (readerName.equals(spi.getReaderServiceProviderName())
                && writerName.equals(spi.getWriterServiceProviderName())) {
                transcoders.add(spi.createTranscoderInstance());
            }
        }
        return transcoders.iterator();
    }

    /**
     * Lee la primera imagen de ese archivo.
     *
     * @return la imagen, o null si nadie reconoce el formato. Ver la nota de la clase
     * @throws IllegalArgumentException si es null
     * @throws IOException si fallo la lectura
     */
    public static BufferedImage read(File input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        if (!input.canRead()) {
            throw new IIOException("Can't read input file!");
        }
        ImageInputStream stream = createImageInputStream(input);
        if (stream == null) {
            throw new IIOException("Can't create an ImageInputStream!");
        }
        BufferedImage bi = read(stream);
        if (bi == null) {
            stream.close();
        }
        return bi;
    }

    /**
     * Idem, desde un flujo. El flujo <b>no</b> se cierra.
     *
     * @return la imagen, o null
     * @throws IOException si fallo la lectura
     */
    public static BufferedImage read(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        ImageInputStream stream = createImageInputStream(input);
        BufferedImage bi = read(stream);
        if (bi == null && stream != null) {
            stream.close();
        }
        return bi;
    }

    /**
     * Idem, desde una direccion.
     *
     * @return la imagen, o null
     * @throws IOException si fallo la lectura
     */
    public static BufferedImage read(URL input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input == null!");
        }
        InputStream istream = input.openStream();
        ImageInputStream stream = createImageInputStream(istream);
        BufferedImage bi;
        try {
            bi = read(stream);
            if (bi == null && stream != null) {
                stream.close();
            }
        } finally {
            // La conexion la abrio este metodo, asi que la cierra este metodo -- a diferencia del
            // flujo que recibe la version de InputStream.
            istream.close();
        }
        return bi;
    }

    /**
     * Idem, desde un flujo de imagenes ya armado.
     *
     * @return la imagen, o null si nadie reconoce el formato
     * @throws IOException si fallo la lectura
     */
    public static BufferedImage read(ImageInputStream stream) throws IOException {
        if (stream == null) {
            throw new IllegalArgumentException("stream == null!");
        }
        Iterator<ImageReader> iter = getImageReaders(stream);
        if (!iter.hasNext()) {
            return null;
        }
        ImageReader reader = iter.next();
        ImageReadParam param = reader.getDefaultReadParam();
        reader.setInput(stream, true, true);
        BufferedImage bi;
        try {
            bi = reader.read(0, param);
        } finally {
            reader.dispose();
            stream.close();
        }
        return bi;
    }

    /**
     * Escribe esa imagen en ese formato.
     *
     * @return si se escribio; false si no hay escritor para ese formato. Ver la nota de la clase
     * @throws IllegalArgumentException si algun argumento es null
     * @throws IOException si fallo la escritura
     */
    public static boolean write(RenderedImage im, String formatName, ImageOutputStream output)
        throws IOException {
        if (im == null) {
            throw new IllegalArgumentException("im == null!");
        }
        if (formatName == null) {
            throw new IllegalArgumentException("formatName == null!");
        }
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        ImageWriter writer = firstWriter(im, formatName);
        if (writer == null) {
            return false;
        }
        writer.setOutput(output);
        try {
            writer.write(new IIOImage(im, null, null));
        } finally {
            writer.dispose();
            output.flush();
        }
        return true;
    }

    /**
     * Idem, a un archivo.
     *
     * <p>Si no hay escritor devuelve false y el archivo queda creado y <b>vacio</b>; ver la nota de la
     * clase.
     *
     * @throws IOException si fallo la escritura
     */
    public static boolean write(RenderedImage im, String formatName, File output)
        throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        output.delete();
        ImageOutputStream stream = createImageOutputStream(output);
        if (stream == null) {
            throw new IIOException("Can't create an ImageOutputStream!");
        }
        try {
            return write(im, formatName, stream);
        } finally {
            stream.close();
        }
    }

    /**
     * Idem, a un flujo. El flujo <b>no</b> se cierra.
     *
     * @throws IOException si fallo la escritura
     */
    public static boolean write(RenderedImage im, String formatName, OutputStream output)
        throws IOException {
        if (output == null) {
            throw new IllegalArgumentException("output == null!");
        }
        ImageOutputStream stream = createImageOutputStream(output);
        if (stream == null) {
            throw new IIOException("Can't create an ImageOutputStream!");
        }
        try {
            return write(im, formatName, stream);
        } finally {
            stream.close();
        }
    }

    /** Los nombres del formato. */
    private static final int NAMES = 0;

    /** Las extensiones. */
    private static final int SUFFIXES = 1;

    /** Los tipos MIME. */
    private static final int MIME = 2;

    /** El registro que se consulta para todo. */
    private static IIORegistry registry() {
        return IIORegistry.getDefaultInstance();
    }

    /** Los nombres, extensiones o tipos MIME de todos los proveedores de esa categoria. */
    private static <T extends ImageReaderWriterSpi> String[] collect(Class<T> category, int which) {
        List<String> found = new ArrayList<String>();
        Iterator<T> it = registry().getServiceProviders(category, true);
        while (it.hasNext()) {
            String[] some = valuesOf(it.next(), which);
            int i = 0;
            while (some != null && i < some.length) {
                if (!found.contains(some[i])) {
                    found.add(some[i]);
                }
                i = i + 1;
            }
        }
        return found.toArray(new String[found.size()]);
    }

    /** Los lectores cuyo proveedor tenga ese nombre, extension o tipo MIME. */
    private static Iterator<ImageReader> readersMatching(String value, int which) {
        if (value == null) {
            throw new IllegalArgumentException("argument == null!");
        }
        List<ImageReader> readers = new ArrayList<ImageReader>();
        Iterator<ImageReaderSpi> it =
            registry().getServiceProviders(ImageReaderSpi.class, true);
        while (it.hasNext()) {
            ImageReaderSpi spi = it.next();
            if (matches(spi, value, which)) {
                try {
                    readers.add(spi.createReaderInstance());
                } catch (IOException e) {
                    // Ese no se pudo crear; los demas todavia sirven.
                }
            }
        }
        return readers.iterator();
    }

    /** Idem, escritores. */
    private static Iterator<ImageWriter> writersMatching(String value, int which) {
        if (value == null) {
            throw new IllegalArgumentException("argument == null!");
        }
        List<ImageWriter> writers = new ArrayList<ImageWriter>();
        Iterator<ImageWriterSpi> it =
            registry().getServiceProviders(ImageWriterSpi.class, true);
        while (it.hasNext()) {
            ImageWriterSpi spi = it.next();
            if (matches(spi, value, which)) {
                try {
                    writers.add(spi.createWriterInstance());
                } catch (IOException e) {
                    // Ver arriba.
                }
            }
        }
        return writers.iterator();
    }

    /** El primer escritor que sirva para esa imagen y ese formato, o null. */
    private static ImageWriter firstWriter(RenderedImage im, String formatName) {
        Iterator<ImageWriter> iter = getImageWriters(new ImageTypeSpecifier(im), formatName);
        if (!iter.hasNext()) {
            return null;
        }
        return iter.next();
    }

    /** Si ese proveedor declara ese nombre, extension o tipo MIME. */
    private static boolean matches(ImageReaderWriterSpi spi, String value, int which) {
        String[] values = valuesOf(spi, which);
        int i = 0;
        while (values != null && i < values.length) {
            if (value.equalsIgnoreCase(values[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** El arreglo que corresponde a esa categoria de nombre. */
    private static String[] valuesOf(ImageReaderWriterSpi spi, int which) {
        if (which == SUFFIXES) {
            return spi.getFileSuffixes();
        }
        if (which == MIME) {
            return spi.getMIMETypes();
        }
        return spi.getFormatNames();
    }

    /** El proveedor de esa clase, o null si no esta registrado o no se pudo cargar. */
    private static <T> T spiByName(String className, Class<T> category) {
        try {
            Class<?> cls = Class.forName(className, true,
                                         ImageIO.class.getClassLoader());
            Object spi = registry().getServiceProviderByClass(cls);
            if (category.isInstance(spi)) {
                return category.cast(spi);
            }
        } catch (Throwable e) {
            // Esa clase no esta; el hermano declarado no esta instalado.
        }
        return null;
    }
}
