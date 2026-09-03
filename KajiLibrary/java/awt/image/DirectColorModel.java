package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;

/**
 * El modelo de color de una pantalla: rojo, verde, azul y opcionalmente alfa, en campos de bits de
 * un mismo píxel.
 *
 * <p>Es el caso concreto de {@link PackedColorModel} para RGB, y de lejos el más usado: el ARGB de
 * 32 bits que devuelve {@link ColorModel#getRGBdefault} es una instancia de esta clase, y también lo
 * son los 565 de 16 bits y los RGB de 24.
 *
 * <p>Toda la conversión pasa por la misma cadena: sacar la componente con su máscara, llevarla a
 * 0..1 dividiendo por su propio máximo, deshacer la premultiplicación si la hay, y recién ahí
 * convertir. Cuando el espacio es sRGB —el caso normal— esa conversión es una multiplicación por
 * 255; cuando no lo es, hay que pasar por {@link ColorSpace#toRGB}, que es lo que hace que un modelo
 * en otro espacio siga contestando bien qué tan rojo es un píxel.
 *
 * <p>El espacio tiene que ser de tipo RGB. No es una restricción de esta clase sino de su API: la
 * de tres máscaras llamadas rojo, verde y azul no significa nada en un espacio que no las tenga.
 */
public class DirectColorModel extends PackedColorModel {

    /**
     * Un modelo RGB opaco en sRGB.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 32 o si alguna máscara no es
     *     contigua
     */
    public DirectColorModel(int bits, int rmask, int gmask, int bmask) {
        this(bits, rmask, gmask, bmask, 0);
    }

    /**
     * Un modelo RGB en sRGB, con alfa si `amask` no es cero.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 32 o si alguna máscara no es
     *     contigua
     */
    public DirectColorModel(int bits, int rmask, int gmask, int bmask, int amask) {
        super(ColorSpace.getInstance(ColorSpace.CS_sRGB), bits, rmask, gmask, bmask, amask, false,
                amask == 0 ? Transparency.OPAQUE : Transparency.TRANSLUCENT,
                ColorModel.getDefaultTransferType(bits));
    }

    /**
     * El constructor general: cualquier espacio RGB, con o sin alfa premultiplicado.
     *
     * @throws IllegalArgumentException si el espacio no es de tipo RGB, si `bits` no está entre 1 y
     *     32, o si alguna máscara no es contigua
     */
    public DirectColorModel(ColorSpace space, int bits, int rmask, int gmask, int bmask, int amask,
            boolean isAlphaPremultiplied, int transferType) {
        super(space, bits, rmask, gmask, bmask, amask, isAlphaPremultiplied,
                amask == 0 ? Transparency.OPAQUE : Transparency.TRANSLUCENT, transferType);
    }

    /** La máscara del rojo. */
    public final int getRedMask() {
        return this.maskArray[0];
    }

    /** La máscara del verde. */
    public final int getGreenMask() {
        return this.maskArray[1];
    }

    /** La máscara del azul. */
    public final int getBlueMask() {
        return this.maskArray[2];
    }

    /** La máscara del alfa, o 0 si no tiene. */
    public final int getAlphaMask() {
        if (this.supportsAlpha) {
            return this.maskArray[3];
        }
        return 0;
    }

    /** La componente cruda, tal como está guardada. */
    private int crudo(int pixel, int idx) {
        return (pixel & this.maskArray[idx]) >>> this.maskOffsets[idx];
    }

    /** El alfa del píxel, de 0 a 1. */
    private float alfaNormalizado(int pixel) {
        if (!this.supportsAlpha) {
            return 1.0f;
        }
        int a = this.crudo(pixel, 3);
        return ((float) a) / ((float) ((1 << this.nBits[3]) - 1));
    }

    /**
     * Una componente de color de 0 a 1, ya deshecha la premultiplicación.
     *
     * <p>Con alfa cero no hay color que recuperar: el píxel es invisible y lo único que se puede
     * decir es cero. Dividir igual daría infinito o NaN, que no es un color.
     */
    private float colorNormalizado(int pixel, int idx) {
        float c = ((float) this.crudo(pixel, idx)) / ((float) ((1 << this.nBits[idx]) - 1));
        if (this.isAlphaPremultiplied) {
            float a = this.alfaNormalizado(pixel);
            if (a == 0.0f) {
                return 0.0f;
            }
            return c / a;
        }
        return c;
    }

    /**
     * Una de las tres componentes sRGB del píxel, de 0 a 255.
     *
     * <p>Si el espacio ya es sRGB no hay nada que convertir y basta con escalar. Si no lo es, hay
     * que pasar las tres juntas por el espacio: el rojo sRGB de un píxel en otro espacio depende de
     * sus tres componentes, no sólo de la primera.
     */
    private int enSrgb(int pixel, int idx) {
        if (this.isSrgb) {
            return (int) (this.colorNormalizado(pixel, idx) * 255.0f + 0.5f);
        }
        float[] comps = new float[this.numColorComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            float min = this.colorSpace.getMinValue(i);
            float max = this.colorSpace.getMaxValue(i);
            comps[i] = min + this.colorNormalizado(pixel, i) * (max - min);
        }
        float[] rgb = this.colorSpace.toRGB(comps);
        float v = rgb[idx];
        if (v < 0.0f) {
            v = 0.0f;
        }
        if (v > 1.0f) {
            v = 1.0f;
        }
        return (int) (v * 255.0f + 0.5f);
    }

    /** El rojo del píxel, de 0 a 255 y en sRGB. */
    public final int getRed(int pixel) {
        return this.enSrgb(pixel, 0);
    }

    /** El verde del píxel, de 0 a 255 y en sRGB. */
    public final int getGreen(int pixel) {
        return this.enSrgb(pixel, 1);
    }

    /** El azul del píxel, de 0 a 255 y en sRGB. */
    public final int getBlue(int pixel) {
        return this.enSrgb(pixel, 2);
    }

    /** El alfa del píxel, de 0 a 255; 255 si el modelo no tiene alfa. */
    public final int getAlpha(int pixel) {
        if (!this.supportsAlpha) {
            return 255;
        }
        return (int) (this.alfaNormalizado(pixel) * 255.0f + 0.5f);
    }

    /** El píxel entero como ARGB de ocho bits por canal. */
    public final int getRGB(int pixel) {
        return (this.getAlpha(pixel) << 24) | (this.getRed(pixel) << 16)
                | (this.getGreen(pixel) << 8) | this.getBlue(pixel);
    }

    /**
     * Un píxel crudo llevado a un `int`.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     * @throws ClassCastException si el arreglo no es del tipo de transferencia
     */
    private int desdeCrudo(Object inData) {
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            return ((byte[]) inData)[0] & 0xFF;
        }
        if (this.transferType == DataBuffer.TYPE_USHORT) {
            return ((short[]) inData)[0] & 0xFFFF;
        }
        if (this.transferType == DataBuffer.TYPE_INT) {
            return ((int[]) inData)[0];
        }
        throw new UnsupportedOperationException(
                "This method has not been implemented for transferType " + this.transferType);
    }

    /**
     * Un `int` guardado en un arreglo del tipo de transferencia.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    private Object aCrudo(int pixel, Object obj) {
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] out = obj == null ? new byte[1] : (byte[]) obj;
            out[0] = (byte) pixel;
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_USHORT) {
            short[] out = obj == null ? new short[1] : (short[]) obj;
            out[0] = (short) pixel;
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_INT) {
            int[] out = obj == null ? new int[1] : (int[]) obj;
            out[0] = pixel;
            return out;
        }
        throw new UnsupportedOperationException(
                "This method has not been implemented for transferType " + this.transferType);
    }

    /**
     * El rojo de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    public int getRed(Object inData) {
        return this.getRed(this.desdeCrudo(inData));
    }

    /**
     * El verde de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    public int getGreen(Object inData) {
        return this.getGreen(this.desdeCrudo(inData));
    }

    /**
     * El azul de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    public int getBlue(Object inData) {
        return this.getBlue(this.desdeCrudo(inData));
    }

    /**
     * El alfa de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    public int getAlpha(Object inData) {
        return this.getAlpha(this.desdeCrudo(inData));
    }

    /**
     * Un píxel crudo como ARGB de ocho bits por canal.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    public int getRGB(Object inData) {
        return this.getRGB(this.desdeCrudo(inData));
    }

    /**
     * Un ARGB llevado a un píxel de este modelo.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    public Object getDataElements(int rgb, Object pixel) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        float a = (rgb >>> 24) / 255.0f;
        float[] norm = new float[this.numColorComponents];
        if (this.isSrgb) {
            norm[0] = r;
            norm[1] = g;
            norm[2] = b;
        } else {
            float[] rgbf = new float[3];
            rgbf[0] = r;
            rgbf[1] = g;
            rgbf[2] = b;
            float[] comps = this.colorSpace.fromRGB(rgbf);
            for (int i = 0; i < this.numColorComponents; i++) {
                float min = this.colorSpace.getMinValue(i);
                float max = this.colorSpace.getMaxValue(i);
                norm[i] = (comps[i] - min) / (max - min);
            }
        }
        if (this.supportsAlpha && this.isAlphaPremultiplied) {
            for (int i = 0; i < this.numColorComponents; i++) {
                norm[i] = norm[i] * a;
            }
        }
        int intpixel = 0;
        for (int i = 0; i < this.numColorComponents; i++) {
            int v = (int) (norm[i] * ((1 << this.nBits[i]) - 1) + 0.5f);
            intpixel = intpixel | ((v << this.maskOffsets[i]) & this.maskArray[i]);
        }
        if (this.supportsAlpha) {
            int v = (int) (a * ((1 << this.nBits[3]) - 1) + 0.5f);
            intpixel = intpixel | ((v << this.maskOffsets[3]) & this.maskArray[3]);
        }
        return this.aCrudo(intpixel, pixel);
    }

    /** Las componentes crudas del píxel, cada una en su propia escala. */
    public final int[] getComponents(int pixel, int[] components, int offset) {
        int[] out = components;
        if (out == null) {
            out = new int[offset + this.numComponents];
        }
        for (int i = 0; i < this.numComponents; i++) {
            out[offset + i] = this.crudo(pixel, i);
        }
        return out;
    }

    /**
     * Las componentes crudas de un píxel crudo.
     *
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    public final int[] getComponents(Object pixel, int[] components, int offset) {
        return this.getComponents(this.desdeCrudo(pixel), components, offset);
    }

    /**
     * Componentes crudas llevadas a un píxel.
     *
     * @throws IllegalArgumentException si el arreglo no trae todas las componentes
     */
    public int getDataElement(int[] components, int offset) {
        if (components.length - offset < this.numComponents) {
            throw new IllegalArgumentException("Incorrect number of components.  Expecting "
                    + this.numComponents);
        }
        int intpixel = 0;
        for (int i = 0; i < this.numComponents; i++) {
            intpixel = intpixel
                    | ((components[offset + i] << this.maskOffsets[i]) & this.maskArray[i]);
        }
        return intpixel;
    }

    /**
     * Componentes crudas llevadas a un píxel crudo.
     *
     * @throws IllegalArgumentException si el arreglo no trae todas las componentes
     * @throws UnsupportedOperationException si el tipo no entra en un `int`
     */
    public Object getDataElements(int[] components, int offset, Object obj) {
        return this.aCrudo(this.getDataElement(components, offset), obj);
    }

    /**
     * Un ráster empaquetado con estas máscaras.
     *
     * @throws IllegalArgumentException si el tamaño es vacío
     */
    public final WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") cannot be <= 0");
        }
        int[] bandmasks;
        if (this.supportsAlpha) {
            bandmasks = new int[4];
            bandmasks[3] = this.maskArray[3];
        } else {
            bandmasks = new int[3];
        }
        bandmasks[0] = this.maskArray[0];
        bandmasks[1] = this.maskArray[1];
        bandmasks[2] = this.maskArray[2];
        if (this.pixel_bits > 16) {
            return Raster.createPackedRaster(DataBuffer.TYPE_INT, w, h, bandmasks, null);
        }
        if (this.pixel_bits > 8) {
            return Raster.createPackedRaster(DataBuffer.TYPE_USHORT, w, h, bandmasks, null);
        }
        return Raster.createPackedRaster(DataBuffer.TYPE_BYTE, w, h, bandmasks, null);
    }

    /** Si ese ráster está empaquetado con exactamente estas máscaras. */
    public boolean isCompatibleRaster(Raster raster) {
        SampleModel sm = raster.getSampleModel();
        if (!(sm instanceof SinglePixelPackedSampleModel)) {
            return false;
        }
        SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel) sm;
        if (sppsm.getNumBands() != this.numComponents) {
            return false;
        }
        int[] bitMasks = sppsm.getBitMasks();
        for (int i = 0; i < this.numComponents; i++) {
            if (bitMasks[i] != this.maskArray[i]) {
                return false;
            }
        }
        return raster.getTransferType() == this.transferType;
    }

    /**
     * Premultiplica el ráster por su alfa, o lo deshace, **en el lugar**.
     *
     * <p>Devuelve el modelo que describe al ráster después del cambio; si ya estaba como se pidió, o
     * si no hay alfa que premultiplicar, se devuelve a sí mismo sin tocar nada.
     *
     * <p>La operación pierde información en un sentido: premultiplicar un píxel de alfa cero lo
     * lleva a negro, y deshacerlo después no lo recupera. Es propio de la representación, no de esta
     * implementación.
     */
    public final ColorModel coerceData(WritableRaster raster, boolean isAlphaPremultiplied) {
        if (!this.supportsAlpha || this.isAlphaPremultiplied == isAlphaPremultiplied) {
            return this;
        }
        int w = raster.getWidth();
        int h = raster.getHeight();
        int aIdx = this.numColorComponents;
        int alphaMax = (1 << this.nBits[aIdx]) - 1;
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int[] pixel = null;
        for (int y = minY; y < minY + h; y++) {
            for (int x = minX; x < minX + w; x++) {
                pixel = raster.getPixel(x, y, pixel);
                float normAlpha = ((float) pixel[aIdx]) / ((float) alphaMax);
                if (isAlphaPremultiplied) {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = (int) (pixel[c] * normAlpha + 0.5f);
                    }
                } else if (normAlpha != 0.0f) {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = (int) (pixel[c] / normAlpha + 0.5f);
                    }
                } else {
                    for (int c = 0; c < this.numColorComponents; c++) {
                        pixel[c] = 0;
                    }
                }
                raster.setPixel(x, y, pixel);
            }
        }
        return new DirectColorModel(this.colorSpace, this.pixel_bits, this.maskArray[0],
                this.maskArray[1], this.maskArray[2], this.maskArray[3], isAlphaPremultiplied,
                this.transferType);
    }

    public String toString() {
        return "DirectColorModel: rmask=" + Integer.toHexString(this.maskArray[0])
                + " gmask=" + Integer.toHexString(this.maskArray[1])
                + " bmask=" + Integer.toHexString(this.maskArray[2])
                + " amask=" + Integer.toHexString(this.getAlphaMask());
    }
}
