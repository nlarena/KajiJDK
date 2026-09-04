package javax.imageio.plugins.bmp;

import java.util.Locale;
import javax.imageio.ImageWriteParam;

/**
 * KajiLibrary's javax.imageio.plugins.bmp.BMPImageWriteParam -- los parametros propios de BMP.
 *
 * <p>Agrega una sola cosa sobre {@link ImageWriteParam}: {@link #setTopDown}.
 *
 * <h2>BMP guarda las filas al reves</h2>
 *
 * <p>Un BMP clasico guarda la imagen <b>de abajo hacia arriba</b>: la primera fila del archivo es la
 * ultima de la pantalla. Es una herencia de OS/2 de 1987 y sigue siendo lo normal.
 *
 * <p>El formato admite tambien el orden natural, marcandolo con un alto negativo en el encabezado. Es
 * mas rapido de dibujar y bastante menos compatible: hay lectores viejos que no lo entienden.
 *
 * <p>Por eso el valor por omision es <b>false</b> --de abajo hacia arriba--, que es lo interoperable.
 * Ponerlo en true es una optimizacion que hay que decidir a conciencia.
 *
 * <p>Los seis tipos de compresion que declara la version del JDK son los del formato: {@code BI_RGB}
 * sin comprimir, los dos {@code BI_RLE}, {@code BI_BITFIELDS}, y los dos que encapsulan otro formato
 * entero adentro del BMP.
 */
public class BMPImageWriteParam extends ImageWriteParam {

    /** Si las filas van en orden natural. Ver la nota de la clase. */
    private boolean topDown = false;

    /**
     * @param locale en que idioma dar los textos, o null
     */
    public BMPImageWriteParam(Locale locale) {
        super(locale);
        // BMP comprime, y con varios metodos: hay que declararlo para que los setCompressionXxx de la
        // clase base no rechacen todo.
        this.canWriteCompressed = true;
        this.compressionTypes = new String[] {
            "BI_RGB", "BI_RLE8", "BI_RLE4", "BI_BITFIELDS", "BI_JPEG", "BI_PNG",
        };
    }

    /** Sin idioma. */
    public BMPImageWriteParam() {
        this(null);
    }

    /** Si escribir las filas en orden natural. Ver la nota de la clase. */
    public void setTopDown(boolean topDown) {
        this.topDown = topDown;
    }

    /** Si van en orden natural. */
    public boolean isTopDown() {
        return this.topDown;
    }
}
