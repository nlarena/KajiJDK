package javax.imageio;

import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

/**
 * KajiLibrary's javax.imageio.ImageTypeSpecifier -- de que tipo es o va a ser una imagen.
 *
 * <p>Un {@link ColorModel} mas un {@link SampleModel}, <b>sin tamano</b>. Esa ausencia es todo el
 * punto de la clase: describe el <i>formato</i> de los pixeles y no una imagen concreta.
 *
 * <p>Por eso sirve para lo que un {@code BufferedImage} no puede: preguntarle a un lector "en que
 * formatos podes darme esta imagen" antes de decodificar nada, o decirle a un escritor "escribila en
 * este formato". Alocar una imagen entera para responder eso seria absurdo.
 *
 * <h2>Las siete fabricas</h2>
 *
 * <p>Armar el par a mano es facil de hacer mal, y de ahi que haya una fabrica por cada organizacion de
 * pixel usual:
 *
 * <ul>
 *   <li>{@link #createInterleaved}: las bandas intercaladas en un mismo arreglo, {@code RGBRGBRGB};
 *   <li>{@link #createBanded}: una banda por arreglo, {@code RRR GGG BBB};
 *   <li>{@link #createPacked}: varias bandas empaquetadas en un entero por pixel, con mascaras;
 *   <li>{@link #createGrayscale}: una sola banda de gris;
 *   <li>{@link #createIndexed}: una tabla de colores y un indice por pixel;
 *   <li>{@link #createFromBufferedImageType} y {@link #createFromRenderedImage}: copiar el tipo de
 *       algo que ya existe.
 * </ul>
 *
 * <p>La diferencia entre intercalado y por bandas parece cosmetica y no lo es: leer un canal completo
 * es una pasada contigua en el segundo y saltos en el primero.
 *
 * <h2>{@link #getBufferedImageType}</h2>
 *
 * <p>Devuelve una de las constantes {@code TYPE_} de {@link BufferedImage}, o
 * {@link BufferedImage#TYPE_CUSTOM} si el par no corresponde a ninguna.
 *
 * <p>Y {@code TYPE_CUSTOM} es lo normal, no una falla: los tipos con nombre son un punado de casos
 * frecuentes, y cualquier cosa un poco distinta --sRGB intercalado en orden RGB, por ejemplo-- cae en
 * personalizado.
 */
public class ImageTypeSpecifier {

    /** Como se interpretan los pixeles. */
    protected ColorModel colorModel;

    /** Como estan organizados. */
    protected SampleModel sampleModel;

    /**
     * El par, directo.
     *
     * @throws IllegalArgumentException si alguno es null, o si no son compatibles
     */
    public ImageTypeSpecifier(ColorModel colorModel, SampleModel sampleModel) {
        if (colorModel == null) {
            throw new IllegalArgumentException("colorModel == null!");
        }
        if (sampleModel == null) {
            throw new IllegalArgumentException("sampleModel == null!");
        }
        if (!colorModel.isCompatibleSampleModel(sampleModel)) {
            throw new IllegalArgumentException("sampleModel is incompatible with colorModel!");
        }
        this.colorModel = colorModel;
        this.sampleModel = sampleModel;
    }

    /**
     * El tipo de esa imagen.
     *
     * @throws IllegalArgumentException si es null
     */
    public ImageTypeSpecifier(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        this.colorModel = image.getColorModel();
        this.sampleModel = image.getSampleModel();
    }

    /**
     * Bandas empaquetadas en un entero por pixel. Ver la nota de la clase.
     *
     * @param redMask que bits son el rojo
     * @param alphaMask que bits son el alfa, o 0 si no hay
     * @param transferType {@link DataBuffer#TYPE_BYTE}, {@code TYPE_USHORT} o {@code TYPE_INT}
     * @param isAlphaPremultiplied si el color ya viene multiplicado por el alfa
     * @throws IllegalArgumentException si el espacio de color no es de tres componentes, si las
     *     mascaras estan mal, o si el tipo de transferencia no sirve
     */
    public static ImageTypeSpecifier createPacked(ColorSpace colorSpace, int redMask,
                                                  int greenMask, int blueMask, int alphaMask,
                                                  int transferType,
                                                  boolean isAlphaPremultiplied) {
        if (colorSpace == null) {
            throw new IllegalArgumentException("colorSpace == null!");
        }
        if (colorSpace.getType() != ColorSpace.TYPE_RGB) {
            throw new IllegalArgumentException("colorSpace is not of type TYPE_RGB!");
        }
        if (transferType != DataBuffer.TYPE_BYTE && transferType != DataBuffer.TYPE_USHORT
            && transferType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Bad value for transferType!");
        }
        if (redMask == 0 && greenMask == 0 && blueMask == 0 && alphaMask == 0) {
            throw new IllegalArgumentException("No mask has at least 1 bit set!");
        }
        int bits = 32;
        ColorModel colorModel = new DirectColorModel(colorSpace, bits, redMask, greenMask,
                                                     blueMask, alphaMask, isAlphaPremultiplied,
                                                     transferType);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(1, 1);
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /**
     * Bandas intercaladas en un mismo arreglo. Ver la nota de la clase.
     *
     * @param bandOffsets en que orden salen las bandas
     * @param hasAlpha si la ultima banda es alfa
     * @throws IllegalArgumentException si algo no cierra
     */
    public static ImageTypeSpecifier createInterleaved(ColorSpace colorSpace, int[] bandOffsets,
                                                       int dataType, boolean hasAlpha,
                                                       boolean isAlphaPremultiplied) {
        if (colorSpace == null) {
            throw new IllegalArgumentException("colorSpace == null!");
        }
        if (bandOffsets == null) {
            throw new IllegalArgumentException("bandOffsets == null!");
        }
        int numBands = bandOffsets.length;
        int numComponents = colorSpace.getNumComponents();
        if (hasAlpha) {
            numComponents = numComponents + 1;
        }
        if (numBands != numComponents) {
            throw new IllegalArgumentException(
                "bandOffsets.length is wrong for colorSpace!");
        }
        int transparency;
        if (hasAlpha) {
            transparency = java.awt.Transparency.TRANSLUCENT;
        } else {
            transparency = java.awt.Transparency.OPAQUE;
        }
        int[] bits = new int[numBands];
        int size = DataBuffer.getDataTypeSize(dataType);
        int i = 0;
        while (i < numBands) {
            bits[i] = size;
            i = i + 1;
        }
        ColorModel colorModel = new ComponentColorModel(colorSpace, bits, hasAlpha,
                                                        isAlphaPremultiplied, transparency,
                                                        dataType);
        int minBandOffset = bandOffsets[0];
        int maxBandOffset = bandOffsets[0];
        i = 0;
        while (i < bandOffsets.length) {
            if (bandOffsets[i] < minBandOffset) {
                minBandOffset = bandOffsets[i];
            }
            if (bandOffsets[i] > maxBandOffset) {
                maxBandOffset = bandOffsets[i];
            }
            i = i + 1;
        }
        int pixelStride = maxBandOffset - minBandOffset + 1;
        pixelStride = Math.max(pixelStride, bandOffsets.length);
        SampleModel sampleModel = new PixelInterleavedSampleModel(dataType, 1, 1, pixelStride,
                                                                  pixelStride, bandOffsets);
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /**
     * Una banda por arreglo. Ver la nota de la clase.
     *
     * @throws IllegalArgumentException si algo no cierra
     */
    public static ImageTypeSpecifier createBanded(ColorSpace colorSpace, int[] bankIndices,
                                                  int[] bandOffsets, int dataType,
                                                  boolean hasAlpha,
                                                  boolean isAlphaPremultiplied) {
        if (colorSpace == null) {
            throw new IllegalArgumentException("colorSpace == null!");
        }
        if (bankIndices == null) {
            throw new IllegalArgumentException("bankIndices == null!");
        }
        if (bandOffsets == null) {
            throw new IllegalArgumentException("bandOffsets == null!");
        }
        if (bankIndices.length != bandOffsets.length) {
            throw new IllegalArgumentException(
                "bankIndices.length != bandOffsets.length!");
        }
        int numBands = bandOffsets.length;
        int numComponents = colorSpace.getNumComponents();
        if (hasAlpha) {
            numComponents = numComponents + 1;
        }
        if (numBands != numComponents) {
            throw new IllegalArgumentException("bandOffsets.length is wrong for colorSpace!");
        }
        int transparency;
        if (hasAlpha) {
            transparency = java.awt.Transparency.TRANSLUCENT;
        } else {
            transparency = java.awt.Transparency.OPAQUE;
        }
        int[] bits = new int[numBands];
        int size = DataBuffer.getDataTypeSize(dataType);
        int i = 0;
        while (i < numBands) {
            bits[i] = size;
            i = i + 1;
        }
        ColorModel colorModel = new ComponentColorModel(colorSpace, bits, hasAlpha,
                                                        isAlphaPremultiplied, transparency,
                                                        dataType);
        SampleModel sampleModel = new BandedSampleModel(dataType, 1, 1, 1, bankIndices,
                                                        bandOffsets);
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /** Una sola banda de gris, sin alfa. */
    public static ImageTypeSpecifier createGrayscale(int bits, int dataType, boolean isSigned) {
        return createGrayscale(bits, dataType, isSigned, false);
    }

    /**
     * Una sola banda de gris.
     *
     * <p>Con 1, 2 o 4 bits y sin alfa usa un {@link MultiPixelPackedSampleModel}: varios pixeles por
     * byte, que es como se guardan las imagenes de un bit.
     *
     * @throws IllegalArgumentException si los bits no son 1, 2, 4, 8, 16 o 32, o no entran en el tipo
     */
    public static ImageTypeSpecifier createGrayscale(int bits, int dataType, boolean isSigned,
                                                     boolean isAlphaPremultiplied) {
        if (bits != 1 && bits != 2 && bits != 4 && bits != 8 && bits != 16 && bits != 32) {
            throw new IllegalArgumentException("Bad value for bits!");
        }
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_SHORT
            && dataType != DataBuffer.TYPE_USHORT && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Bad value for dataType!");
        }
        if (bits > DataBuffer.getDataTypeSize(dataType)) {
            throw new IllegalArgumentException("Too many bits for dataType!");
        }
        ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        int numBands = 1;
        boolean hasAlpha = false;
        int transparency = java.awt.Transparency.OPAQUE;
        ColorModel colorModel = new ComponentColorModel(colorSpace, new int[] { bits },
                                                        hasAlpha, isAlphaPremultiplied,
                                                        transparency, dataType);
        SampleModel sampleModel;
        if (bits < 8 && numBands == 1) {
            // Varios pixeles por byte: es lo que hace que una imagen de un bit ocupe un octavo.
            sampleModel = new MultiPixelPackedSampleModel(dataType, 1, 1, bits);
        } else {
            sampleModel = new PixelInterleavedSampleModel(dataType, 1, 1, numBands, numBands,
                                                          new int[] { 0 });
        }
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /**
     * Una tabla de colores y un indice por pixel.
     *
     * @param redLUT la componente roja de cada entrada
     * @param alphaLUT el alfa de cada entrada, o null para opaco
     * @param bits cuantos bits por indice: 1, 2, 4, 8 o 16
     * @throws IllegalArgumentException si las tablas no tienen el mismo largo, si los bits no sirven,
     *     o si la tabla es mas grande de lo que los bits permiten
     */
    public static ImageTypeSpecifier createIndexed(byte[] redLUT, byte[] greenLUT, byte[] blueLUT,
                                                   byte[] alphaLUT, int bits, int dataType) {
        if (redLUT == null || greenLUT == null || blueLUT == null) {
            throw new IllegalArgumentException("LUT is null!");
        }
        if (bits != 1 && bits != 2 && bits != 4 && bits != 8 && bits != 16) {
            throw new IllegalArgumentException("Bad value for bits!");
        }
        if (dataType != DataBuffer.TYPE_BYTE && dataType != DataBuffer.TYPE_SHORT
            && dataType != DataBuffer.TYPE_USHORT && dataType != DataBuffer.TYPE_INT) {
            throw new IllegalArgumentException("Bad value for dataType!");
        }
        int len = 1 << bits;
        if (redLUT.length != greenLUT.length || redLUT.length != blueLUT.length
            || (alphaLUT != null && redLUT.length != alphaLUT.length)) {
            throw new IllegalArgumentException("LUTs have different lengths!");
        }
        if (redLUT.length > len) {
            throw new IllegalArgumentException("LUT has improper length!");
        }
        ColorModel colorModel;
        if (alphaLUT == null) {
            colorModel = new IndexColorModel(bits, redLUT.length, redLUT, greenLUT, blueLUT);
        } else {
            colorModel = new IndexColorModel(bits, redLUT.length, redLUT, greenLUT, blueLUT,
                                             alphaLUT);
        }
        SampleModel sampleModel;
        if (bits == 8) {
            int[] bandOffsets = new int[1];
            sampleModel = new PixelInterleavedSampleModel(dataType, 1, 1, 1, 1, bandOffsets);
        } else {
            sampleModel = new MultiPixelPackedSampleModel(dataType, 1, 1, bits);
        }
        return new ImageTypeSpecifier(colorModel, sampleModel);
    }

    /**
     * El tipo de una de las constantes {@code TYPE_} de {@link BufferedImage}.
     *
     * @throws IllegalArgumentException si es {@link BufferedImage#TYPE_CUSTOM} o no es una constante
     */
    public static ImageTypeSpecifier createFromBufferedImageType(int bufferedImageType) {
        if (bufferedImageType == BufferedImage.TYPE_CUSTOM) {
            throw new IllegalArgumentException("Cannot create from TYPE_CUSTOM!");
        }
        if (bufferedImageType < BufferedImage.TYPE_CUSTOM
            || bufferedImageType > BufferedImage.TYPE_BYTE_INDEXED) {
            throw new IllegalArgumentException("Invalid BufferedImage type!");
        }
        // Se arma una imagen de un pixel y se le toma el par: replicar a mano las trece
        // combinaciones seria duplicar lo que BufferedImage ya sabe, y desincronizarse con ella.
        BufferedImage bi = new BufferedImage(1, 1, bufferedImageType);
        return new ImageTypeSpecifier(bi);
    }

    /**
     * El tipo de esa imagen.
     *
     * @throws IllegalArgumentException si es null
     */
    public static ImageTypeSpecifier createFromRenderedImage(RenderedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("image == null!");
        }
        return new ImageTypeSpecifier(image);
    }

    /**
     * Cual de las constantes {@code TYPE_}, o {@link BufferedImage#TYPE_CUSTOM}.
     *
     * <p>Ver la nota de la clase: personalizado es lo normal.
     */
    public int getBufferedImageType() {
        BufferedImage bi = createBufferedImage(1, 1);
        return bi.getType();
    }

    /** Cuantas componentes tiene el modelo de color. */
    public int getNumComponents() {
        return this.colorModel.getNumComponents();
    }

    /** Cuantas bandas tiene el modelo de muestras. */
    public int getNumBands() {
        return this.sampleModel.getNumBands();
    }

    /**
     * Cuantos bits tiene esa banda.
     *
     * @throws IllegalArgumentException si la banda no existe
     */
    public int getBitsPerBand(int band) {
        if (band < 0 || band >= getNumBands()) {
            throw new IllegalArgumentException("band out of range!");
        }
        return this.sampleModel.getSampleSize(band);
    }

    /** El modelo de muestras, de un pixel. */
    public SampleModel getSampleModel() {
        return this.sampleModel;
    }

    /**
     * El modelo de muestras a ese tamano.
     *
     * @throws IllegalArgumentException si el ancho o el alto no son positivos
     * @throws IllegalArgumentException si el producto se desborda
     */
    public SampleModel getSampleModel(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width or height <= 0!");
        }
        if ((long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("width * height > Integer.MAX_VALUE!");
        }
        return this.sampleModel.createCompatibleSampleModel(width, height);
    }

    /** El modelo de color. */
    public ColorModel getColorModel() {
        return this.colorModel;
    }

    /**
     * Una imagen vacia de ese tamano y este tipo.
     *
     * @throws IllegalArgumentException si el ancho o el alto no son positivos, o si el producto se
     *     desborda
     */
    public BufferedImage createBufferedImage(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width or height <= 0!");
        }
        if ((long) width * height > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("width * height > Integer.MAX_VALUE!");
        }
        SampleModel sm = this.sampleModel.createCompatibleSampleModel(width, height);
        WritableRaster raster = Raster.createWritableRaster(sm, new java.awt.Point(0, 0));
        return new BufferedImage(this.colorModel, raster,
                                 this.colorModel.isAlphaPremultiplied(), null);
    }

    /**
     * Igual si el modelo de color y el de muestras son iguales.
     *
     * <p>El tamano del modelo de muestras no entra: dos tipos con el mismo formato son el mismo tipo,
     * aunque sus modelos de muestras se hayan armado para tamanos distintos.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ImageTypeSpecifier)) {
            return false;
        }
        ImageTypeSpecifier that = (ImageTypeSpecifier) o;
        return this.colorModel.equals(that.colorModel)
            && this.sampleModel.equals(that.sampleModel);
    }

    /** Coherente con {@link #equals}. */
    @Override
    public int hashCode() {
        return 9 * this.colorModel.hashCode() + 14 * this.sampleModel.hashCode();
    }
}
