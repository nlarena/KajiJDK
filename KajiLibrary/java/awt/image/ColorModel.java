package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;

/**
 * Qué **color** es un píxel.
 *
 * <p>Es la tercera y última pieza de una imagen, y la única que habla de color. El
 * {@link DataBuffer} tiene números; el {@link SampleModel} sabe cuáles de esos números forman un
 * píxel; el modelo de color sabe qué color es. Sin él, `(255, 0, 0)` no es rojo: son tres números.
 *
 * <p>La traducción va y viene entre tres representaciones del mismo píxel, y casi toda la clase es
 * pasar de una a otra:
 *
 * <ul>
 *   <li><strong>el píxel</strong> — un `int`, o un arreglo si no entra en uno, tal como está
 *       guardado;
 *   <li><strong>las componentes</strong> — un valor entero por banda, ya separadas pero todavía en
 *       la escala de la imagen, que puede tener bandas de distinta cantidad de bits;
 *   <li><strong>las componentes normalizadas</strong> — un `float` por banda en el rango del
 *       {@link ColorSpace}, que es donde el color existe de verdad y donde dos imágenes de formatos
 *       distintos se pueden comparar.
 * </ul>
 *
 * <p>Las subclases redefinen las conversiones que su formato hace rápido; lo que no redefinen cae en
 * los métodos generales de acá, que pasan siempre por las componentes normalizadas. Los que **no**
 * tienen una versión general honesta —los que dependen de cómo está armado el píxel, como
 * {@link #getDataElements(int, Object)} o {@link #createCompatibleSampleModel}— tiran
 * `UnsupportedOperationException` en vez de inventar una respuesta.
 *
 * <p>El alfa premultiplicado merece una nota, porque no es una convención sino una cuenta que ya se
 * hizo. Con `isAlphaPremultiplied`, las componentes de color guardadas **ya están multiplicadas** por
 * el alfa: un rojo a media transparencia se guarda como 127 y no como 255. Eso vuelve trivial la
 * composición —sumar es superponer— y hace que recuperar el color original tenga que dividir, con la
 * división por cero cuando el alfa es cero. Esa rama aparece en cada conversión de esta clase.
 */
public abstract class ColorModel implements Transparency {

    /** Cuántos bits ocupa un píxel. */
    protected int pixel_bits;

    /** El tipo con el que se transfiere un píxel crudo. */
    protected int transferType;

    /** Cuántos bits usa cada componente, o `null` si no se declararon. */
    int[] nBits;

    /** El mayor de `nBits`. */
    int maxBits;

    // Estos campos son de paquete y no privados, igual que en el JDK: las subclases de acá adentro
    // los ajustan. PackedColorModel, por ejemplo, baja la transparencia a BITMASK cuando descubre
    // que el alfa tiene un solo bit, y eso recién se sabe después de descomponer las máscaras.
    ColorSpace colorSpace;
    int colorSpaceType;
    int numComponents;
    int numColorComponents;
    boolean supportsAlpha;
    boolean isAlphaPremultiplied;
    int transparency;
    boolean isSrgb;

    private static ColorModel rgbDefault;

    /**
     * El modelo ARGB de siempre: 32 bits, alfa translúcido y sRGB.
     *
     * <p>Es el formato en el que se habla de color en toda la API cuando no se dice otra cosa: el
     * `int` que devuelve {@link #getRGB(int)} y el que toma {@link #getDataElements(int, Object)}
     * están en este modelo, sin importar cómo esté guardada la imagen.
     */
    public static ColorModel getRGBdefault() {
        synchronized (ColorModel.class) {
            if (rgbDefault == null) {
                rgbDefault = new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF,
                        0xFF000000);
            }
            return rgbDefault;
        }
    }

    /** El tipo más chico en el que entra un píxel de tantos bits. */
    static int getDefaultTransferType(int pixelBits) {
        if (pixelBits <= 8) {
            return DataBuffer.TYPE_BYTE;
        }
        if (pixelBits <= 16) {
            return DataBuffer.TYPE_USHORT;
        }
        if (pixelBits <= 32) {
            return DataBuffer.TYPE_INT;
        }
        return DataBuffer.TYPE_UNDEFINED;
    }

    /**
     * Un modelo ARGB translúcido en sRGB, de tantos bits por píxel.
     *
     * <p>Es el constructor cómodo, y el que deja el modelo a medio declarar: no dice cuántos bits
     * usa cada componente, así que las conversiones que necesitan esa cuenta —las normalizadas—
     * tiran en vez de responder.
     *
     * @throws IllegalArgumentException si `bits` no es positivo
     */
    public ColorModel(int bits) {
        if (bits < 1) {
            throw new IllegalArgumentException("Number of bits must be > 0");
        }
        this.pixel_bits = bits;
        this.colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        this.colorSpaceType = ColorSpace.TYPE_RGB;
        this.numComponents = 4;
        this.numColorComponents = 3;
        this.supportsAlpha = true;
        this.isAlphaPremultiplied = false;
        this.transparency = Transparency.TRANSLUCENT;
        this.isSrgb = true;
        this.nBits = null;
        this.maxBits = bits;
        this.transferType = getDefaultTransferType(bits);
    }

    /**
     * El constructor general.
     *
     * <p>Sin alfa, `isAlphaPremultiplied` y `transparency` se ignoran y quedan en `false` y
     * `OPAQUE`: sin canal alfa no hay nada que premultiplicar ni transparencia que declarar, y
     * dejarlos como los pasaron sería guardar una contradicción.
     *
     * @throws IllegalArgumentException si `bits` no alcanza para todas las componentes, si la
     *     transparencia no es una de las tres, si algún ancho es negativo, si todos son cero, o si
     *     los bits por píxel no son positivos
     * @throws NullPointerException si falta el espacio de color
     */
    protected ColorModel(int pixel_bits, int[] bits, ColorSpace cspace, boolean hasAlpha,
            boolean isAlphaPremultiplied, int transparency, int transferType) {
        this.colorSpace = cspace;
        this.colorSpaceType = cspace.getType();
        this.numColorComponents = cspace.getNumComponents();
        this.numComponents = this.numColorComponents + (hasAlpha ? 1 : 0);
        this.supportsAlpha = hasAlpha;
        if (bits.length < this.numComponents) {
            throw new IllegalArgumentException("Number of color/alpha components should be "
                    + this.numComponents + " but length of bits array is " + bits.length);
        }
        if (transparency < Transparency.OPAQUE || transparency > Transparency.TRANSLUCENT) {
            throw new IllegalArgumentException("Unknown transparency: " + transparency);
        }
        if (!this.supportsAlpha) {
            this.isAlphaPremultiplied = false;
            this.transparency = Transparency.OPAQUE;
        } else {
            this.isAlphaPremultiplied = isAlphaPremultiplied;
            this.transparency = transparency;
        }
        this.nBits = bits.clone();
        this.pixel_bits = pixel_bits;
        if (pixel_bits <= 0) {
            throw new IllegalArgumentException("Number of pixel bits must be > 0");
        }
        this.maxBits = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] < 0) {
                throw new IllegalArgumentException("Number of bits must be >= 0");
            }
            if (this.maxBits < bits[i]) {
                this.maxBits = bits[i];
            }
        }
        if (this.maxBits == 0) {
            throw new IllegalArgumentException(
                    "There must be at least one component with > 0 pixel bits.");
        }
        this.isSrgb = cspace == ColorSpace.getInstance(ColorSpace.CS_sRGB);
        this.transferType = transferType;
    }

    /** Si tiene canal alfa. */
    public final boolean hasAlpha() {
        return this.supportsAlpha;
    }

    /** Si las componentes de color ya vienen multiplicadas por el alfa. */
    public final boolean isAlphaPremultiplied() {
        return this.isAlphaPremultiplied;
    }

    /** El tipo con el que se transfiere un píxel crudo. */
    public final int getTransferType() {
        return this.transferType;
    }

    /** Cuántos bits ocupa un píxel. */
    public int getPixelSize() {
        return this.pixel_bits;
    }

    /**
     * Cuántos bits usa esa componente.
     *
     * @throws NullPointerException si el modelo no declaró los anchos
     * @throws ArrayIndexOutOfBoundsException si la componente no existe
     */
    public int getComponentSize(int componentIdx) {
        if (this.nBits == null) {
            throw new NullPointerException("Number of bits array is null.");
        }
        return this.nBits[componentIdx];
    }

    /** Cuántos bits usa cada componente, o `null` si no se declararon. */
    public int[] getComponentSize() {
        if (this.nBits == null) {
            return null;
        }
        return this.nBits.clone();
    }

    /** `OPAQUE`, `BITMASK` o `TRANSLUCENT`. */
    public int getTransparency() {
        return this.transparency;
    }

    /** Cuántas componentes tiene un píxel, contando el alfa. */
    public int getNumComponents() {
        return this.numComponents;
    }

    /** Cuántas componentes tiene un píxel sin contar el alfa. */
    public int getNumColorComponents() {
        return this.numColorComponents;
    }

    /** El espacio de color. */
    public final ColorSpace getColorSpace() {
        return this.colorSpace;
    }

    /** Si el espacio de color es el sRGB de fábrica. */
    final boolean isSrgb() {
        return this.isSrgb;
    }

    /** El tipo del espacio de color, sin tener que pedirlo. */
    final int getColorSpaceType() {
        return this.colorSpaceType;
    }

    /** El rojo del píxel, de 0 a 255 y en sRGB. */
    public abstract int getRed(int pixel);

    /** El verde del píxel, de 0 a 255 y en sRGB. */
    public abstract int getGreen(int pixel);

    /** El azul del píxel, de 0 a 255 y en sRGB. */
    public abstract int getBlue(int pixel);

    /** El alfa del píxel, de 0 a 255. */
    public abstract int getAlpha(int pixel);

    /** El píxel entero como ARGB de ocho bits por canal. */
    public int getRGB(int pixel) {
        return (this.getAlpha(pixel) << 24) | (this.getRed(pixel) << 16)
                | (this.getGreen(pixel) << 8) | this.getBlue(pixel);
    }

    /**
     * Un píxel crudo llevado a un `int`.
     *
     * <p>Sólo funciona si el píxel entra en un elemento; si son varios, esta clase no sabe cómo
     * juntarlos y hay que redefinirlo.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int` o el píxel ocupa más de
     *     un elemento
     * @throws ClassCastException si el arreglo no es del tipo de transferencia
     */
    private int unPixel(Object inData) {
        int pixel;
        int length;
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] bdata = (byte[]) inData;
            pixel = bdata[0] & 0xFF;
            length = bdata.length;
        } else if (this.transferType == DataBuffer.TYPE_USHORT) {
            short[] sdata = (short[]) inData;
            pixel = sdata[0] & 0xFFFF;
            length = sdata.length;
        } else if (this.transferType == DataBuffer.TYPE_INT) {
            int[] idata = (int[]) inData;
            pixel = idata[0];
            length = idata.length;
        } else {
            throw new UnsupportedOperationException(
                    "This method has not been implemented for transferType " + this.transferType);
        }
        if (length != 1) {
            throw new UnsupportedOperationException(
                    "This method is not supported by this color model");
        }
        return pixel;
    }

    /**
     * El rojo de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el píxel no entra en un `int`
     */
    public int getRed(Object inData) {
        return this.getRed(this.unPixel(inData));
    }

    /**
     * El verde de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el píxel no entra en un `int`
     */
    public int getGreen(Object inData) {
        return this.getGreen(this.unPixel(inData));
    }

    /**
     * El azul de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el píxel no entra en un `int`
     */
    public int getBlue(Object inData) {
        return this.getBlue(this.unPixel(inData));
    }

    /**
     * El alfa de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el píxel no entra en un `int`
     */
    public int getAlpha(Object inData) {
        return this.getAlpha(this.unPixel(inData));
    }

    /**
     * Un píxel crudo como ARGB de ocho bits por canal.
     *
     * @throws UnsupportedOperationException si el píxel no entra en un `int`
     */
    public int getRGB(Object inData) {
        return (this.getAlpha(inData) << 24) | (this.getRed(inData) << 16)
                | (this.getGreen(inData) << 8) | this.getBlue(inData);
    }

    /**
     * Un ARGB llevado a un píxel de este modelo.
     *
     * <p>No hay versión general: armar el píxel depende enteramente de cómo lo guarde la subclase.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public Object getDataElements(int rgb, Object pixel) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * Las componentes de un píxel.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public int[] getComponents(int pixel, int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * Las componentes de un píxel crudo.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public int[] getComponents(Object pixel, int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * Componentes normalizadas llevadas a la escala de la imagen.
     *
     * <p>Con alfa premultiplicado cada componente de color se multiplica por el alfa antes de
     * escalarla, que es justamente lo que significa premultiplicar.
     *
     * @throws UnsupportedOperationException si el modelo no declaró los anchos de componente
     * @throws IllegalArgumentException si el arreglo de entrada no trae todas las componentes
     */
    public int[] getUnnormalizedComponents(float[] normComponents, int normOffset,
            int[] components, int offset) {
        if (this.colorSpace == null) {
            throw new UnsupportedOperationException(
                    "This method is not supported by this color model.");
        }
        if (this.nBits == null) {
            throw new UnsupportedOperationException("This method is not supported.  "
                    + "Unable to determine #bits per component.");
        }
        if (normComponents.length - normOffset < this.numComponents) {
            throw new IllegalArgumentException(
                    "Incorrect number of components.  Expecting " + this.numComponents);
        }
        int[] out = components;
        if (out == null) {
            out = new int[offset + this.numComponents];
        }
        if (this.supportsAlpha && this.isAlphaPremultiplied) {
            float normAlpha = normComponents[normOffset + this.numColorComponents];
            for (int i = 0; i < this.numColorComponents; i++) {
                out[offset + i] = (int) (normComponents[normOffset + i]
                        * ((1 << this.nBits[i]) - 1) * normAlpha + 0.5f);
            }
            out[offset + this.numColorComponents] = (int) (normAlpha
                    * ((1 << this.nBits[this.numColorComponents]) - 1) + 0.5f);
        } else {
            for (int i = 0; i < this.numComponents; i++) {
                out[offset + i] = (int) (normComponents[normOffset + i]
                        * ((1 << this.nBits[i]) - 1) + 0.5f);
            }
        }
        return out;
    }

    /**
     * Componentes de la imagen llevadas a la escala del espacio de color.
     *
     * <p>Es la inversa de {@link #getUnnormalizedComponents}. Con alfa premultiplicado hay que
     * **dividir** por el alfa para recuperar el color, y con alfa cero no hay color que recuperar:
     * el píxel es invisible y sus componentes salen en cero, que es lo único que se puede decir.
     *
     * @throws UnsupportedOperationException si el modelo no declaró los anchos de componente
     * @throws IllegalArgumentException si el arreglo de entrada no trae todas las componentes
     */
    public float[] getNormalizedComponents(int[] components, int offset, float[] normComponents,
            int normOffset) {
        if (this.colorSpace == null) {
            throw new UnsupportedOperationException(
                    "This method is not supported by this color model.");
        }
        if (this.nBits == null) {
            throw new UnsupportedOperationException("This method is not supported.  "
                    + "Unable to determine #bits per component.");
        }
        if (components.length - offset < this.numComponents) {
            throw new IllegalArgumentException(
                    "Incorrect number of components.  Expecting " + this.numComponents);
        }
        float[] out = normComponents;
        if (out == null) {
            out = new float[this.numComponents + normOffset];
        }
        if (this.supportsAlpha && this.isAlphaPremultiplied) {
            float normAlpha = (float) components[offset + this.numColorComponents];
            normAlpha = normAlpha / (float) ((1 << this.nBits[this.numColorComponents]) - 1);
            if (normAlpha != 0.0f) {
                for (int i = 0; i < this.numColorComponents; i++) {
                    out[normOffset + i] = ((float) components[offset + i])
                            / (normAlpha * ((float) ((1 << this.nBits[i]) - 1)));
                }
            } else {
                for (int i = 0; i < this.numColorComponents; i++) {
                    out[normOffset + i] = 0.0f;
                }
            }
            out[normOffset + this.numColorComponents] = normAlpha;
        } else {
            for (int i = 0; i < this.numComponents; i++) {
                out[normOffset + i] = ((float) components[offset + i])
                        / ((float) ((1 << this.nBits[i]) - 1));
            }
        }
        return out;
    }

    /**
     * Componentes llevadas a un píxel de un `int`.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public int getDataElement(int[] components, int offset) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * Componentes llevadas a un píxel crudo.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public Object getDataElements(int[] components, int offset, Object obj) {
        throw new UnsupportedOperationException("This method is not supported by this color model.");
    }

    /**
     * Componentes normalizadas llevadas a un píxel de un `int`.
     *
     * <p>Pasa por las componentes sin normalizar; una subclase que sepa hacerlo derecho lo redefine.
     *
     * @throws UnsupportedOperationException si el modelo no puede armar el píxel
     */
    public int getDataElement(float[] normComponents, int normOffset) {
        int[] components = this.getUnnormalizedComponents(normComponents, normOffset, null, 0);
        return this.getDataElement(components, 0);
    }

    /**
     * Componentes normalizadas llevadas a un píxel crudo.
     *
     * @throws UnsupportedOperationException si el modelo no puede armar el píxel
     */
    public Object getDataElements(float[] normComponents, int normOffset, Object obj) {
        int[] components = this.getUnnormalizedComponents(normComponents, normOffset, null, 0);
        return this.getDataElements(components, 0, obj);
    }

    /**
     * Las componentes normalizadas de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el modelo no sabe separar las componentes
     */
    public float[] getNormalizedComponents(Object pixel, float[] normComponents, int normOffset) {
        int[] components = this.getComponents(pixel, null, 0);
        return this.getNormalizedComponents(components, 0, normComponents, normOffset);
    }

    /**
     * Un modelo de muestras que le sirva a este modelo de color.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        throw new UnsupportedOperationException("This method is not supported by this color model");
    }

    /**
     * Un ráster que le sirva a este modelo de color.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        throw new UnsupportedOperationException("This method is not supported by this color model");
    }

    /**
     * Si ese modelo de muestras le sirve a éste.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        throw new UnsupportedOperationException("This method is not supported by this color model");
    }

    /**
     * Si ese ráster le sirve a éste.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public boolean isCompatibleRaster(Raster raster) {
        throw new UnsupportedOperationException(
                "This method has not been implemented for this ColorModel.");
    }

    /**
     * El canal alfa del ráster, como un ráster de una banda **sobre los mismos datos**.
     *
     * <p>Devuelve `null` cuando el modelo no tiene alfa o cuando el alfa no vive en una banda
     * separada que se pueda ver por sí sola. Es la respuesta honesta y no un error: hay formatos en
     * los que el alfa existe pero no como banda.
     */
    public WritableRaster getAlphaRaster(WritableRaster raster) {
        return null;
    }

    /**
     * Cambia el ráster a alfa premultiplicado, o de vuelta, y devuelve el modelo que corresponde.
     *
     * <p>Modifica el ráster **en el lugar**.
     *
     * @throws UnsupportedOperationException siempre, salvo que la subclase lo redefina
     */
    public ColorModel coerceData(WritableRaster raster, boolean isAlphaPremultiplied) {
        throw new UnsupportedOperationException("This method is not supported by this color model");
    }

    /** Igualdad por clase, tamaño, espacio de color y bandera de alfa. */
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        ColorModel cm = (ColorModel) obj;
        if (this.supportsAlpha != cm.supportsAlpha
                || this.isAlphaPremultiplied != cm.isAlphaPremultiplied
                || this.pixel_bits != cm.pixel_bits
                || this.transparency != cm.transparency
                || this.numComponents != cm.numComponents
                || this.transferType != cm.transferType) {
            return false;
        }
        if (this.colorSpace == null) {
            if (cm.colorSpace != null) {
                return false;
            }
        } else if (!this.colorSpace.equals(cm.colorSpace)) {
            return false;
        }
        if (this.nBits == null) {
            return cm.nBits == null;
        }
        if (cm.nBits == null || this.nBits.length != cm.nBits.length) {
            return false;
        }
        for (int i = 0; i < this.nBits.length; i++) {
            if (this.nBits[i] != cm.nBits[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = this.supportsAlpha ? 2 : 3;
        h = 31 * h + (this.isAlphaPremultiplied ? 1 : 0);
        h = 31 * h + this.pixel_bits;
        h = 31 * h + this.transparency;
        h = 31 * h + this.numComponents;
        if (this.nBits != null) {
            for (int i = 0; i < this.nBits.length; i++) {
                h = 31 * h + this.nBits[i];
            }
        }
        h = 31 * h + this.transferType;
        return h;
    }

    public String toString() {
        return "ColorModel: #pixelBits = " + this.pixel_bits + " numComponents = "
                + this.numComponents + " color space = " + this.colorSpace
                + " transparency = " + this.transparency + " has alpha = " + this.supportsAlpha
                + " isAlphaPre = " + this.isAlphaPremultiplied;
    }
}
