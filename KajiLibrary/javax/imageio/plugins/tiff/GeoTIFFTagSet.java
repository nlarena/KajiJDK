package javax.imageio.plugins.tiff;

import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.GeoTIFFTagSet -- las seis etiquetas de GeoTIFF.
 *
 * <p>Georreferenciacion: como se traducen los pixeles a coordenadas del terreno, y en que sistema de
 * referencia.
 *
 * <p>Son pocas porque casi todo GeoTIFF vive adentro de una sola: {@code TAG_GEO_KEY_DIRECTORY} es un
 * arreglo de enteros con su propio formato de claves anidadas. Es un formato dentro de una etiqueta, y
 * este conjunto solo declara el envase.
 *
 * <p>Es un singleton: se pide con {@link #getInstance}. Las etiquetas y sus valores nombrados se
 * transcribieron del JDK 25 y no a mano; un numero cambiado produce un TIFF que otros programas leen
 * distinto.
 */
public final class GeoTIFFTagSet extends TIFFTagSet {

    /** El unico, armado la primera vez que se pide. */
    private static GeoTIFFTagSet theInstance = null;

    /** El numero de la etiqueta model pixel scale. */
    public static final int TAG_MODEL_PIXEL_SCALE = 33550;

    /** El numero de la etiqueta model transformation. */
    public static final int TAG_MODEL_TRANSFORMATION = 34264;

    /** El numero de la etiqueta model tie point. */
    public static final int TAG_MODEL_TIE_POINT = 33922;

    /** El numero de la etiqueta geo key directory. */
    public static final int TAG_GEO_KEY_DIRECTORY = 34735;

    /** El numero de la etiqueta geo double params. */
    public static final int TAG_GEO_DOUBLE_PARAMS = 34736;

    /** El numero de la etiqueta geo ascii params. */
    public static final int TAG_GEO_ASCII_PARAMS = 34737;


    /** Se llega por {@link #getInstance}. */
    private GeoTIFFTagSet() {
        super(tags());
    }

    /** El conjunto. Ver la nota de la clase. */
    public static synchronized GeoTIFFTagSet getInstance() {
        if (theInstance == null) {
            theInstance = new GeoTIFFTagSet();
        }
        return theInstance;
    }

    /** Las etiquetas de este conjunto. */
    private static List<TIFFTag> tags() {
        List<TIFFTag> tags = new ArrayList<TIFFTag>();
        tags.add(new TIFFTag("ModelPixelScaleTag", 33550, 4096, -1));
        tags.add(new TIFFTag("ModelTiepointTag", 33922, 4096, -1));
        tags.add(new TIFFTag("ModelTransformationTag", 34264, 4096, -1));
        tags.add(new TIFFTag("GeoKeyDirectoryTag", 34735, 8, -1));
        tags.add(new TIFFTag("GeoDoubleParamsTag", 34736, 4096, -1));
        tags.add(new TIFFTag("GeoAsciiParamsTag", 34737, 4, -1));
        return tags;
    }
}
