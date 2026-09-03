package java.awt.image;

import java.awt.color.ColorSpace;

/**
 * Un modelo de color con **una banda por componente**, cada una en su propio elemento del buffer.
 *
 * <p>Es el opuesto de {@link DirectColorModel}: allá las componentes son campos de bits apretados en
 * un píxel, acá cada una es un número entero. A cambio de gastar más memoria acepta lo que el otro
 * no puede: espacios de color de cualquier cantidad de componentes —un CMYK de cuatro, un
 * espectrales de nueve—, componentes de más de ocho bits, y valores en coma flotante.
 *
 * <p>La coma flotante cambia lo que significa el número guardado. Con tipos enteros, el valor es una
 * cuenta de pasos que hay que dividir por su máximo para saber qué color es; con `float` o `double`
 * el valor **ya es** la componente en las unidades del {@link ColorSpace}. Por eso los métodos
 * "sin normalizar" no tienen sentido en esos tipos y tiran en vez de contestar: no hay una escala
 * entera que devolver.
 *
 * <p>La otra distinción que atraviesa la clase es el signo. Los tipos sin signo cubren el rango
 * completo de su ancho; `TYPE_SHORT` es el único entero con signo, y para él los métodos que piden
 * un color en sRGB tiran, porque la mitad de sus valores caen fuera de cualquier escala de color y
 * no hay una respuesta que no sea inventada.
 */
public class ComponentColorModel extends ColorModel {

    private final boolean signed;
    private final boolean floating;
    private final float[] minValues;
    private final float[] rangeValues;

    /**
     * El constructor general.
     *
     * @throws IllegalArgumentException si `bits` no alcanza para todas las componentes, si algún
     *     ancho no entra en el tipo de transferencia, o si el tipo no es uno de los seis
     * @throws NullPointerException si falta el espacio de color
     */
    public ComponentColorModel(ColorSpace colorSpace, int[] bits, boolean hasAlpha,
            boolean isAlphaPremultiplied, int transparency, int transferType) {
        super(bitsPerPixel(bits, colorSpace, hasAlpha, transferType),
                createBitsArray(bits, colorSpace, hasAlpha, transferType), colorSpace, hasAlpha,
                isAlphaPremultiplied, transparency, transferType);
        if (transferType == DataBuffer.TYPE_BYTE || transferType == DataBuffer.TYPE_USHORT
                || transferType == DataBuffer.TYPE_INT) {
            this.signed = false;
            this.floating = false;
        } else if (transferType == DataBuffer.TYPE_SHORT) {
            this.signed = true;
            this.floating = false;
        } else if (transferType == DataBuffer.TYPE_FLOAT
                || transferType == DataBuffer.TYPE_DOUBLE) {
            this.signed = true;
            this.floating = true;
        } else {
            throw new IllegalArgumentException(
                    "This constructor does not support transferType " + transferType);
        }
        int tam = DataBuffer.getDataTypeSize(transferType);
        for (int i = 0; i < this.numComponents; i++) {
            if (this.nBits[i] > tam) {
                throw new IllegalArgumentException("Number of bits for component " + i
                        + " is greater than the size of the transfer type " + transferType);
            }
        }
        // El rango de cada componente se guarda porque cada conversion lo necesita y pedirselo al
        // espacio de color en cada pixel seria una llamada virtual por componente y por pixel.
        this.minValues = new float[this.numComponents];
        this.rangeValues = new float[this.numComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            this.minValues[i] = colorSpace.getMinValue(i);
            this.rangeValues[i] = colorSpace.getMaxValue(i) - this.minValues[i];
        }
        if (this.supportsAlpha) {
            this.minValues[this.numColorComponents] = 0.0f;
            this.rangeValues[this.numColorComponents] = 1.0f;
        }
    }

    /**
     * Como el anterior, con todas las componentes del ancho del tipo de transferencia.
     *
     * @throws IllegalArgumentException si el tipo no es uno de los seis
     * @throws NullPointerException si falta el espacio de color
     */
    public ComponentColorModel(ColorSpace colorSpace, boolean hasAlpha,
            boolean isAlphaPremultiplied, int transparency, int transferType) {
        this(colorSpace, null, hasAlpha, isAlphaPremultiplied, transparency, transferType);
    }

    /**
     * Los anchos de componente, o los del tipo si no se dieron.
     *
     * @throws IllegalArgumentException si el arreglo no trae todas las componentes
     */
    private static int[] createBitsArray(int[] origBits, ColorSpace colorSpace, boolean hasAlpha,
            int transferType) {
        int numComponents = colorSpace.getNumComponents() + (hasAlpha ? 1 : 0);
        if (origBits == null) {
            int[] bits = new int[numComponents];
            int bitsPer = DataBuffer.getDataTypeSize(transferType);
            for (int i = 0; i < numComponents; i++) {
                bits[i] = bitsPer;
            }
            return bits;
        }
        if (origBits.length < numComponents) {
            throw new IllegalArgumentException("Number of color/alpha components should be "
                    + numComponents + " but length of bits array is " + origBits.length);
        }
        return origBits;
    }

    /** Los bits por píxel: la suma de los anchos de todas las componentes. */
    private static int bitsPerPixel(int[] origBits, ColorSpace colorSpace, boolean hasAlpha,
            int transferType) {
        int[] bits = createBitsArray(origBits, colorSpace, hasAlpha, transferType);
        int numComponents = colorSpace.getNumComponents() + (hasAlpha ? 1 : 0);
        int total = 0;
        for (int i = 0; i < numComponents; i++) {
            total = total + bits[i];
        }
        return total;
    }

    /** El mayor valor que puede tomar esa componente entera. */
    private int maximo(int idx) {
        return (1 << this.nBits[idx]) - 1;
    }

    /**
     * Lee las componentes crudas de un píxel, cada una como `float` sin interpretar.
     *
     * @throws UnsupportedOperationException si el tipo no es uno de los seis
     * @throws ClassCastException si el arreglo no es del tipo de transferencia
     */
    private float[] leerCrudo(Object inData) {
        float[] out = new float[this.numComponents];
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] d = (byte[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i] & 0xFF;
            }
        } else if (this.transferType == DataBuffer.TYPE_USHORT) {
            short[] d = (short[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i] & 0xFFFF;
            }
        } else if (this.transferType == DataBuffer.TYPE_SHORT) {
            short[] d = (short[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i];
            }
        } else if (this.transferType == DataBuffer.TYPE_INT) {
            int[] d = (int[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i];
            }
        } else if (this.transferType == DataBuffer.TYPE_FLOAT) {
            float[] d = (float[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = d[i];
            }
        } else if (this.transferType == DataBuffer.TYPE_DOUBLE) {
            double[] d = (double[]) inData;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = (float) d[i];
            }
        } else {
            throw new UnsupportedOperationException(
                    "This method has not been implemented for transferType " + this.transferType);
        }
        return out;
    }

    /**
     * Guarda componentes crudas en un arreglo del tipo de transferencia.
     *
     * @throws UnsupportedOperationException si el tipo no es uno de los seis
     */
    private Object escribirCrudo(float[] crudo, Object obj) {
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[this.numComponents] : (byte[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = (byte) crudo[i];
            }
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_USHORT
                || this.transferType == DataBuffer.TYPE_SHORT) {
            short[] out = obj == null ? new short[this.numComponents] : (short[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = (short) crudo[i];
            }
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_INT) {
            int[] out = obj == null ? new int[this.numComponents] : (int[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = (int) crudo[i];
            }
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_FLOAT) {
            float[] out = obj == null ? new float[this.numComponents] : (float[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = crudo[i];
            }
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_DOUBLE) {
            double[] out = obj == null ? new double[this.numComponents] : (double[]) obj;
            for (int i = 0; i < this.numComponents; i++) {
                out[i] = crudo[i];
            }
            return out;
        }
        throw new UnsupportedOperationException(
                "This method has not been implemented for transferType " + this.transferType);
    }

    /**
     * Componentes crudas llevadas a las unidades del espacio de color, deshecha la premultiplicación.
     *
     * <p>En coma flotante el valor guardado ya está en esas unidades; en enteros hay que dividir por
     * el máximo de cada componente y estirar al rango del espacio.
     */
    private float[] aEspacio(float[] crudo) {
        float[] out = new float[this.numComponents];
        float alfa = 1.0f;
        if (this.supportsAlpha) {
            if (this.floating) {
                alfa = crudo[this.numColorComponents];
            } else {
                alfa = crudo[this.numColorComponents] / this.maximo(this.numColorComponents);
            }
            out[this.numColorComponents] = alfa;
        }
        for (int i = 0; i < this.numColorComponents; i++) {
            float v;
            if (this.floating) {
                v = crudo[i];
            } else {
                v = this.minValues[i] + (crudo[i] / this.maximo(i)) * this.rangeValues[i];
            }
            if (this.supportsAlpha && this.isAlphaPremultiplied) {
                if (alfa == 0.0f) {
                    v = this.minValues[i];
                } else {
                    v = this.minValues[i] + (v - this.minValues[i]) / alfa;
                }
            }
            out[i] = v;
        }
        return out;
    }

    /** La inversa de {@link #aEspacio}. */
    private float[] deEspacio(float[] comps) {
        float[] out = new float[this.numComponents];
        float alfa = 1.0f;
        if (this.supportsAlpha) {
            alfa = comps[this.numColorComponents];
        }
        for (int i = 0; i < this.numColorComponents; i++) {
            float v = comps[i];
            if (this.supportsAlpha && this.isAlphaPremultiplied) {
                v = this.minValues[i] + (v - this.minValues[i]) * alfa;
            }
            if (this.floating) {
                out[i] = v;
            } else {
                float norm = (v - this.minValues[i]) / this.rangeValues[i];
                out[i] = (int) (norm * this.maximo(i) + 0.5f);
            }
        }
        if (this.supportsAlpha) {
            if (this.floating) {
                out[this.numColorComponents] = alfa;
            } else {
                out[this.numColorComponents] =
                        (int) (alfa * this.maximo(this.numColorComponents) + 0.5f);
            }
        }
        return out;
    }

    /** Las tres componentes sRGB, de 0 a 255, de un píxel ya llevado al espacio de color. */
    private int[] aSrgb(float[] comps) {
        float[] color = new float[this.numColorComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            color[i] = comps[i];
        }
        float[] rgb = this.colorSpace.toRGB(color);
        int[] out = new int[3];
        for (int i = 0; i < 3; i++) {
            float v = rgb[i];
            if (v < 0.0f) {
                v = 0.0f;
            }
            if (v > 1.0f) {
                v = 1.0f;
            }
            out[i] = (int) (v * 255.0f + 0.5f);
        }
        return out;
    }

    /**
     * Comprueba que un `int` alcance para representar el píxel.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente o si es con signo
     */
    private void exigirUnaComponente() {
        if (this.numComponents > 1) {
            throw new IllegalArgumentException("More than one component per pixel");
        }
        if (this.signed) {
            throw new IllegalArgumentException("Component value is signed");
        }
    }

    /**
     * El rojo del píxel, de 0 a 255 y en sRGB.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente o es con signo
     */
    public int getRed(int pixel) {
        this.exigirUnaComponente();
        float[] crudo = new float[1];
        crudo[0] = pixel;
        return this.aSrgb(this.aEspacio(crudo))[0];
    }

    /**
     * El verde del píxel, de 0 a 255 y en sRGB.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente o es con signo
     */
    public int getGreen(int pixel) {
        this.exigirUnaComponente();
        float[] crudo = new float[1];
        crudo[0] = pixel;
        return this.aSrgb(this.aEspacio(crudo))[1];
    }

    /**
     * El azul del píxel, de 0 a 255 y en sRGB.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente o es con signo
     */
    public int getBlue(int pixel) {
        this.exigirUnaComponente();
        float[] crudo = new float[1];
        crudo[0] = pixel;
        return this.aSrgb(this.aEspacio(crudo))[2];
    }

    /**
     * El alfa del píxel.
     *
     * <p>Un modelo de una sola componente no tiene alfa, así que la respuesta es 255.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente o es con signo
     */
    public int getAlpha(int pixel) {
        this.exigirUnaComponente();
        return 255;
    }

    /**
     * El píxel entero como ARGB.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente o es con signo
     */
    public int getRGB(int pixel) {
        this.exigirUnaComponente();
        int[] rgb = this.aSrgb(this.aEspacio(new float[] { pixel }));
        return 0xFF000000 | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    /** El rojo de un píxel crudo, de 0 a 255 y en sRGB. */
    public int getRed(Object inData) {
        return this.aSrgb(this.aEspacio(this.leerCrudo(inData)))[0];
    }

    /** El verde de un píxel crudo, de 0 a 255 y en sRGB. */
    public int getGreen(Object inData) {
        return this.aSrgb(this.aEspacio(this.leerCrudo(inData)))[1];
    }

    /** El azul de un píxel crudo, de 0 a 255 y en sRGB. */
    public int getBlue(Object inData) {
        return this.aSrgb(this.aEspacio(this.leerCrudo(inData)))[2];
    }

    /** El alfa de un píxel crudo, de 0 a 255; 255 si el modelo no tiene alfa. */
    public int getAlpha(Object inData) {
        if (!this.supportsAlpha) {
            return 255;
        }
        float[] comps = this.aEspacio(this.leerCrudo(inData));
        float a = comps[this.numColorComponents];
        if (a < 0.0f) {
            a = 0.0f;
        }
        if (a > 1.0f) {
            a = 1.0f;
        }
        return (int) (a * 255.0f + 0.5f);
    }

    /** Un píxel crudo como ARGB de ocho bits por canal. */
    public int getRGB(Object inData) {
        float[] comps = this.aEspacio(this.leerCrudo(inData));
        int[] rgb = this.aSrgb(comps);
        int a = 255;
        if (this.supportsAlpha) {
            float av = comps[this.numColorComponents];
            if (av < 0.0f) {
                av = 0.0f;
            }
            if (av > 1.0f) {
                av = 1.0f;
            }
            a = (int) (av * 255.0f + 0.5f);
        }
        return (a << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
    }

    /** Un ARGB llevado a un píxel crudo de este modelo. */
    public Object getDataElements(int rgb, Object pixel) {
        float[] rgbf = new float[3];
        rgbf[0] = ((rgb >> 16) & 0xFF) / 255.0f;
        rgbf[1] = ((rgb >> 8) & 0xFF) / 255.0f;
        rgbf[2] = (rgb & 0xFF) / 255.0f;
        float[] color = this.colorSpace.fromRGB(rgbf);
        float[] comps = new float[this.numComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            comps[i] = color[i];
        }
        if (this.supportsAlpha) {
            comps[this.numColorComponents] = (rgb >>> 24) / 255.0f;
        }
        return this.escribirCrudo(this.deEspacio(comps), pixel);
    }

    /**
     * Las componentes crudas de un píxel de un `int`.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente
     */
    public int[] getComponents(int pixel, int[] components, int offset) {
        if (this.numComponents > 1) {
            throw new IllegalArgumentException("More than one component per pixel");
        }
        int[] out = components;
        if (out == null) {
            out = new int[offset + 1];
        }
        out[offset] = pixel;
        return out;
    }

    /**
     * Las componentes crudas de un píxel crudo.
     *
     * @throws IllegalArgumentException si el tipo es de coma flotante, que no tiene forma entera
     */
    public int[] getComponents(Object pixel, int[] components, int offset) {
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        float[] crudo = this.leerCrudo(pixel);
        int[] out = components;
        if (out == null) {
            out = new int[offset + this.numComponents];
        }
        for (int i = 0; i < this.numComponents; i++) {
            out[offset + i] = (int) crudo[i];
        }
        return out;
    }

    /**
     * Componentes normalizadas llevadas a la escala de la imagen.
     *
     * @throws IllegalArgumentException si el tipo es de coma flotante, o si el arreglo no trae todas
     *     las componentes
     */
    public int[] getUnnormalizedComponents(float[] normComponents, int normOffset,
            int[] components, int offset) {
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        return super.getUnnormalizedComponents(normComponents, normOffset, components, offset);
    }

    /**
     * Componentes de la imagen llevadas a 0..1.
     *
     * @throws IllegalArgumentException si el tipo es de coma flotante, o si el arreglo no trae todas
     *     las componentes
     */
    public float[] getNormalizedComponents(int[] components, int offset, float[] normComponents,
            int normOffset) {
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        return super.getNormalizedComponents(components, offset, normComponents, normOffset);
    }

    /**
     * Componentes crudas llevadas a un píxel de un `int`.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente
     */
    public int getDataElement(int[] components, int offset) {
        if (this.numComponents > 1) {
            throw new IllegalArgumentException("More than one component per pixel");
        }
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        return components[offset];
    }

    /**
     * Componentes crudas llevadas a un píxel crudo.
     *
     * @throws IllegalArgumentException si el tipo es de coma flotante
     */
    public Object getDataElements(int[] components, int offset, Object obj) {
        if (this.floating) {
            throw new IllegalArgumentException(
                    "This ColorModel does not support the unnormalized form");
        }
        float[] crudo = new float[this.numComponents];
        for (int i = 0; i < this.numComponents; i++) {
            crudo[i] = components[offset + i];
        }
        return this.escribirCrudo(crudo, obj);
    }

    /**
     * Componentes normalizadas llevadas a un píxel de un `int`.
     *
     * @throws IllegalArgumentException si el píxel tiene más de una componente
     */
    public int getDataElement(float[] normComponents, int normOffset) {
        if (this.numComponents > 1) {
            throw new IllegalArgumentException("More than one component per pixel");
        }
        float[] comps = new float[this.numComponents];
        for (int i = 0; i < this.numComponents; i++) {
            comps[i] = this.minValues[i]
                    + normComponents[normOffset + i] * this.rangeValues[i];
        }
        return (int) this.deEspacio(comps)[0];
    }

    /** Componentes normalizadas llevadas a un píxel crudo. */
    public Object getDataElements(float[] normComponents, int normOffset, Object obj) {
        float[] comps = new float[this.numComponents];
        for (int i = 0; i < this.numComponents; i++) {
            comps[i] = this.minValues[i]
                    + normComponents[normOffset + i] * this.rangeValues[i];
        }
        return this.escribirCrudo(this.deEspacio(comps), obj);
    }

    /**
     * Las componentes normalizadas de un píxel crudo.
     *
     * <p>"Normalizadas" acá quiere decir de 0 a 1, no en las unidades del espacio de color: por eso
     * el resultado se lleva de vuelta al rango unitario aunque el espacio tenga otro.
     */
    public float[] getNormalizedComponents(Object pixel, float[] normComponents, int normOffset) {
        float[] comps = this.aEspacio(this.leerCrudo(pixel));
        float[] out = normComponents;
        if (out == null) {
            out = new float[this.numComponents + normOffset];
        }
        for (int i = 0; i < this.numComponents; i++) {
            out[normOffset + i] = (comps[i] - this.minValues[i]) / this.rangeValues[i];
        }
        return out;
    }

    /** Un modelo de muestras intercalado con una banda por componente. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        int[] bandOffsets = new int[this.numComponents];
        for (int i = 0; i < this.numComponents; i++) {
            bandOffsets[i] = i;
        }
        if (this.transferType == DataBuffer.TYPE_BYTE
                || this.transferType == DataBuffer.TYPE_USHORT) {
            return new PixelInterleavedSampleModel(this.transferType, w, h, this.numComponents,
                    w * this.numComponents, bandOffsets);
        }
        return new ComponentSampleModel(this.transferType, w, h, this.numComponents,
                w * this.numComponents, bandOffsets);
    }

    /** Si ese modelo de muestras tiene una banda por componente y el mismo tipo. */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (!(sm instanceof ComponentSampleModel)) {
            return false;
        }
        if (this.numComponents != sm.getNumBands()) {
            return false;
        }
        return sm.getTransferType() == this.transferType;
    }

    /**
     * Un ráster con una banda por componente.
     *
     * <p>Con `byte` y menos de ocho bits por píxel sale un ráster empaquetado de varios píxeles por
     * elemento, que es lo que corresponde a una imagen de 1, 2 o 4 bits.
     *
     * @throws IllegalArgumentException si el tamaño es vacío
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") cannot be <= 0");
        }
        if (this.transferType == DataBuffer.TYPE_BYTE
                && (this.pixel_bits == 1 || this.pixel_bits == 2 || this.pixel_bits == 4)) {
            return Raster.createPackedRaster(new DataBufferByte(w * h), w, h, this.pixel_bits,
                    null);
        }
        SampleModel sm = this.createCompatibleSampleModel(w, h);
        return Raster.createWritableRaster(sm, sm.createDataBuffer(), null);
    }

    /** Si ese ráster tiene una banda por componente, con lugar suficiente en cada una. */
    public boolean isCompatibleRaster(Raster raster) {
        SampleModel sm = raster.getSampleModel();
        if (!(sm instanceof ComponentSampleModel)) {
            return false;
        }
        if (sm.getNumBands() != this.numComponents) {
            return false;
        }
        for (int i = 0; i < this.nBits.length; i++) {
            if (sm.getSampleSize(i) < this.nBits[i]) {
                return false;
            }
        }
        return raster.getTransferType() == this.transferType;
    }

    /**
     * El canal alfa como un ráster de una banda **sobre los mismos datos**.
     *
     * <p>Devuelve `null` si el modelo no tiene alfa.
     */
    public WritableRaster getAlphaRaster(WritableRaster raster) {
        if (!this.hasAlpha()) {
            return null;
        }
        int x = raster.getMinX();
        int y = raster.getMinY();
        int[] band = new int[1];
        band[0] = raster.getNumBands() - 1;
        return raster.createWritableChild(x, y, raster.getWidth(), raster.getHeight(), x, y, band);
    }

    /**
     * Premultiplica el ráster por su alfa, o lo deshace, **en el lugar**.
     *
     * <p>Devuelve el modelo que describe al ráster después del cambio; si ya estaba como se pidió, o
     * si no hay alfa, se devuelve a sí mismo sin tocar nada.
     */
    public ColorModel coerceData(WritableRaster raster, boolean isAlphaPremultiplied) {
        if (!this.supportsAlpha || this.isAlphaPremultiplied == isAlphaPremultiplied) {
            return this;
        }
        int w = raster.getWidth();
        int h = raster.getHeight();
        int aIdx = this.numColorComponents;
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        float[] pixel = null;
        for (int y = minY; y < minY + h; y++) {
            for (int x = minX; x < minX + w; x++) {
                pixel = raster.getPixel(x, y, pixel);
                float normAlpha;
                if (this.floating) {
                    normAlpha = pixel[aIdx];
                } else {
                    normAlpha = pixel[aIdx] / this.maximo(aIdx);
                }
                if (isAlphaPremultiplied) {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = pixel[c] * normAlpha;
                    }
                } else if (normAlpha != 0.0f) {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = pixel[c] / normAlpha;
                    }
                } else {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = 0.0f;
                    }
                }
                raster.setPixel(x, y, pixel);
            }
        }
        return new ComponentColorModel(this.colorSpace, this.nBits, this.supportsAlpha,
                isAlphaPremultiplied, this.transparency, this.transferType);
    }

    public String toString() {
        return "ComponentColorModel: #pixelBits = " + this.pixel_bits + " numComponents = "
                + this.numComponents + " color space = " + this.colorSpace + " transparency = "
                + this.transparency + " has alpha = " + this.supportsAlpha + " isAlphaPre = "
                + this.isAlphaPremultiplied;
    }
}
