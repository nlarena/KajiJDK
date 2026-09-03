package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;

/**
 * Un modelo de color cuyas componentes son **campos de bits** dentro de un solo píxel.
 *
 * <p>Es la mitad de color de lo que {@link SinglePixelPackedSampleModel} es de disposición, y las
 * dos clases se declaran igual: por máscaras. De cada máscara salen el corrimiento y el ancho de su
 * componente, y de ahí todo lo demás.
 *
 * <p>Una máscara tiene que ser **contigua** —un solo tramo de unos— y no puede pasarse de los bits
 * del píxel. Lo primero es lo que permite que la componente se lea con un corrimiento y una `y`
 * lógica en vez de tener que juntar pedazos sueltos.
 *
 * <p>El constructor puede terminar bajando la transparencia declarada a `BITMASK`: si el alfa quedó
 * con un solo bit, el píxel sólo puede estar del todo opaco o del todo transparente, y decir
 * `TRANSLUCENT` sería prometer algo que el formato no puede dar. Eso recién se sabe después de
 * descomponer las máscaras, que es por qué se corrige después y no en la lista de argumentos.
 */
public abstract class PackedColorModel extends ColorModel {

    int[] maskArray;
    int[] maskOffsets;
    float[] scaleFactors;

    /**
     * Con las máscaras de color en un arreglo y la de alfa aparte.
     *
     * <p>Sirve para espacios de color de cualquier cantidad de componentes; el otro constructor es
     * el atajo para RGB.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 32, si alguna máscara no es
     *     contigua, o si alguna se pasa de los bits del píxel
     * @throws NullPointerException si falta el espacio de color
     */
    public PackedColorModel(ColorSpace space, int bits, int[] colorMaskArray, int alphaMask,
            boolean isAlphaPremultiplied, int trans, int transferType) {
        super(bits, createBitsArray(colorMaskArray, alphaMask), space, alphaMask != 0,
                isAlphaPremultiplied, trans, transferType);
        if (bits < 1 || bits > 32) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 32.");
        }
        this.maskArray = new int[this.numComponents];
        this.maskOffsets = new int[this.numComponents];
        this.scaleFactors = new float[this.numComponents];
        for (int i = 0; i < this.numColorComponents; i++) {
            this.decomposeMask(colorMaskArray[i], i, space.getName(i));
        }
        if (alphaMask != 0) {
            this.decomposeMask(alphaMask, this.numColorComponents, "alpha");
            if (this.nBits[this.numComponents - 1] == 1) {
                this.transparency = Transparency.BITMASK;
            }
        }
    }

    /**
     * El atajo para RGB: las tres máscaras de color y la de alfa por separado.
     *
     * @throws IllegalArgumentException si el espacio no es de tipo RGB, si `bits` no está entre 1 y
     *     32, si alguna máscara no es contigua, o si alguna se pasa de los bits del píxel
     * @throws NullPointerException si falta el espacio de color
     */
    public PackedColorModel(ColorSpace space, int bits, int rmask, int gmask, int bmask, int amask,
            boolean isAlphaPremultiplied, int trans, int transferType) {
        super(bits, createBitsArray(rmask, gmask, bmask, amask), space, amask != 0,
                isAlphaPremultiplied, trans, transferType);
        if (space.getType() != ColorSpace.TYPE_RGB) {
            throw new IllegalArgumentException("ColorSpace must be TYPE_RGB.");
        }
        if (bits < 1 || bits > 32) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 32.");
        }
        this.maskArray = new int[this.numComponents];
        this.maskOffsets = new int[this.numComponents];
        this.scaleFactors = new float[this.numComponents];
        this.decomposeMask(rmask, 0, "red");
        this.decomposeMask(gmask, 1, "green");
        this.decomposeMask(bmask, 2, "blue");
        if (amask != 0) {
            this.decomposeMask(amask, 3, "alpha");
            if (this.nBits[3] == 1) {
                this.transparency = Transparency.BITMASK;
            }
        }
    }

    /**
     * Cuántos bits tiene la máscara, o -1 si no es contigua.
     *
     * <p>Se corren los ceros de abajo, después los unos, y si queda algo prendido es que había un
     * hueco: la máscara tenía dos tramos.
     */
    private static int countBits(int mask) {
        int m = mask;
        int count = 0;
        if (m != 0) {
            while ((m & 1) == 0) {
                m = m >>> 1;
            }
            while ((m & 1) == 1) {
                m = m >>> 1;
                count = count + 1;
            }
        }
        if (m != 0) {
            return -1;
        }
        return count;
    }

    /**
     * Los anchos de componente que salen de las máscaras.
     *
     * @throws IllegalArgumentException si alguna no es contigua
     */
    private static int[] createBitsArray(int[] colorMaskArray, int alphaMask) {
        int numColors = colorMaskArray.length;
        int numAlpha = alphaMask == 0 ? 0 : 1;
        int[] arr = new int[numColors + numAlpha];
        for (int i = 0; i < numColors; i++) {
            arr[i] = countBits(colorMaskArray[i]);
            if (arr[i] < 0) {
                throw new IllegalArgumentException("Noncontiguous color mask ("
                        + Integer.toHexString(colorMaskArray[i]) + "at index " + i);
            }
        }
        if (alphaMask != 0) {
            arr[numColors] = countBits(alphaMask);
            if (arr[numColors] < 0) {
                throw new IllegalArgumentException("Noncontiguous alpha mask ("
                        + Integer.toHexString(alphaMask));
            }
        }
        return arr;
    }

    /**
     * Lo mismo para el atajo RGB.
     *
     * @throws IllegalArgumentException si alguna no es contigua
     */
    private static int[] createBitsArray(int rmask, int gmask, int bmask, int amask) {
        int[] arr = new int[3 + (amask == 0 ? 0 : 1)];
        arr[0] = countBits(rmask);
        if (arr[0] < 0) {
            throw new IllegalArgumentException("Noncontiguous red mask ("
                    + Integer.toHexString(rmask));
        }
        arr[1] = countBits(gmask);
        if (arr[1] < 0) {
            throw new IllegalArgumentException("Noncontiguous green mask ("
                    + Integer.toHexString(gmask));
        }
        arr[2] = countBits(bmask);
        if (arr[2] < 0) {
            throw new IllegalArgumentException("Noncontiguous blue mask ("
                    + Integer.toHexString(bmask));
        }
        if (amask != 0) {
            arr[3] = countBits(amask);
            if (arr[3] < 0) {
                throw new IllegalArgumentException("Noncontiguous alpha mask ("
                        + Integer.toHexString(amask));
            }
        }
        return arr;
    }

    /**
     * Guarda la máscara y calcula su corrimiento y su factor de escala.
     *
     * <p>El factor lleva la componente a 0..255, que es la escala en la que se pide un color. Con
     * ocho bits vale exactamente 1 y la conversión no hace nada.
     *
     * @throws IllegalArgumentException si la máscara se pasa de los bits del píxel
     */
    private void decomposeMask(int mask, int idx, String componentName) {
        int off = 0;
        int count = this.nBits[idx];
        this.maskArray[idx] = mask;
        int m = mask;
        if (m != 0) {
            while ((m & 1) == 0) {
                m = m >>> 1;
                off = off + 1;
            }
        }
        if (count + off > this.pixel_bits) {
            throw new IllegalArgumentException(componentName + " mask "
                    + Integer.toHexString(this.maskArray[idx]) + " overflows pixel (expecting "
                    + this.pixel_bits + " bits");
        }
        this.maskOffsets[idx] = off;
        if (count == 0) {
            this.scaleFactors[idx] = 256.0f;
        } else {
            this.scaleFactors[idx] = 255.0f / ((1 << count) - 1);
        }
    }

    /**
     * La máscara de esa componente.
     *
     * @throws ArrayIndexOutOfBoundsException si la componente no existe
     */
    public final int getMask(int index) {
        return this.maskArray[index];
    }

    /** Las máscaras de todas las componentes. */
    public final int[] getMasks() {
        return this.maskArray.clone();
    }

    /** Un {@link SinglePixelPackedSampleModel} con estas mismas máscaras. */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        return new SinglePixelPackedSampleModel(this.transferType, w, h, this.maskArray);
    }

    /**
     * Si ese modelo de muestras usa exactamente estas máscaras.
     *
     * <p>Las máscaras se comparan **recortadas al tipo de transferencia**: los bits que el tipo no
     * puede guardar no distinguen a dos modelos que se comportan igual.
     */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (!(sm instanceof SinglePixelPackedSampleModel)) {
            return false;
        }
        if (this.numComponents != sm.getNumBands()) {
            return false;
        }
        if (sm.getTransferType() != this.transferType) {
            return false;
        }
        SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel) sm;
        int[] bitMasks = sppsm.getBitMasks();
        if (bitMasks.length != this.maskArray.length) {
            return false;
        }
        int maxMask = (int) ((1L << DataBuffer.getDataTypeSize(this.transferType)) - 1);
        for (int i = 0; i < bitMasks.length; i++) {
            if ((maxMask & bitMasks[i]) != (maxMask & this.maskArray[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * El canal alfa como un ráster de una banda **sobre los mismos datos**.
     *
     * <p>Devuelve `null` si el modelo no tiene alfa. En este formato el alfa siempre es la última
     * banda, así que la vista se arma con un hijo de una sola banda.
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

    /** Igualdad de {@link ColorModel} más las máscaras. */
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        PackedColorModel cm = (PackedColorModel) obj;
        for (int i = 0; i < this.maskArray.length; i++) {
            if (this.maskArray[i] != cm.maskArray[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = super.hashCode();
        for (int i = 0; i < this.maskArray.length; i++) {
            h = 31 * h + this.maskArray[i];
        }
        return h;
    }
}
