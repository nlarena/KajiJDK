package java.awt.image;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.math.BigInteger;

/**
 * Un modelo de color donde el píxel no es un color sino un **número de paleta**.
 *
 * <p>Es la indirección que hace posible un GIF: en vez de guardar tres bytes por píxel se guarda un
 * índice de ocho bits, y los colores de verdad viven en una tabla de 256 entradas. Una imagen así
 * ocupa un tercio, y cambiarle la paleta la recolorea entera sin tocar un solo píxel.
 *
 * <p>La contrapartida es que **convertir a este modelo pierde información**. Un color que no está en
 * la paleta se reemplaza por el más parecido, y eso lo hace {@link #getDataElements(int, Object)}
 * buscando en toda la tabla. Es la única operación cara de la clase, y es cara a propósito: elegir
 * mal el índice se ve.
 *
 * <p>La transparencia se **deduce** de la paleta y no se declara. Si ninguna entrada tiene alfa, el
 * modelo es opaco; si alguna tiene alfa cero y ninguna tiene un valor intermedio, es de máscara —el
 * píxel está o no está—; si hay valores intermedios, es translúcido. Eso cambia cuántas componentes
 * dice tener el modelo, y por eso se calcula antes de terminar de construirlo.
 *
 * <p>Una paleta puede tener **huecos**: entradas que no corresponden a ningún color válido, marcadas
 * en el {@link BigInteger} de {@link #getValidPixels}. Un hueco no es lo mismo que un color
 * transparente: el transparente es un color que se puede usar, el hueco no se elige nunca.
 */
public class IndexColorModel extends ColorModel {

    private static final int[] opaqueBits = { 8, 8, 8 };
    private static final int[] alphaBits = { 8, 8, 8, 8 };

    private int[] rgb;
    private int map_size;
    private int transparent_index = -1;
    private boolean allgrayopaque;
    private BigInteger validBits;

    /**
     * Con tres arreglos de componentes y sin alfa.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 16 o si `size` no es positivo
     * @throws NullPointerException si falta alguno de los arreglos
     * @throws ArrayIndexOutOfBoundsException si algún arreglo es más corto que `size`
     */
    public IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(size, r, g, b, null);
    }

    /**
     * Como el anterior, con una entrada marcada como transparente.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 16 o si `size` no es positivo
     */
    public IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b, int trans) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(size, r, g, b, null);
        this.setTransparentPixel(trans);
    }

    /**
     * Con cuatro arreglos de componentes.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 16 o si `size` no es positivo
     */
    public IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b, byte[] a) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(size, r, g, b, a);
    }

    /**
     * Con la paleta empaquetada en un arreglo de bytes, tres o cuatro por entrada.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 16, si `size` no es positivo, o
     *     si el arreglo no alcanza
     */
    public IndexColorModel(int bits, int size, byte[] cmap, int start, boolean hasalpha) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(bits, size, cmap, start, hasalpha, -1);
    }

    /**
     * Como el anterior, con una entrada marcada como transparente.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 16 o si `size` no es positivo
     */
    public IndexColorModel(int bits, int size, byte[] cmap, int start, boolean hasalpha,
            int trans) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, ColorModel.getDefaultTransferType(bits));
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        this.setRGBs(bits, size, cmap, start, hasalpha, trans);
    }

    /**
     * Con la paleta en un arreglo de ARGB y el tipo de transferencia dado.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 16, si `size` no es positivo, o
     *     si el tipo no es `byte` ni `ushort`
     */
    public IndexColorModel(int bits, int size, int[] cmap, int start, boolean hasalpha, int trans,
            int transferType) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, transferType);
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Map size (" + size + ") must be >= 1");
        }
        if (transferType != DataBuffer.TYPE_BYTE && transferType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException("transferType must be one of "
                    + "DataBuffer.TYPE_BYTE or DataBuffer.TYPE_USHORT");
        }
        this.setRGBs(size, cmap, start, hasalpha);
        this.setTransparentPixel(trans);
    }

    /**
     * Con la paleta en ARGB y un mapa de qué entradas son válidas.
     *
     * @throws IllegalArgumentException si `bits` no está entre 1 y 16, si `size` no es positivo, o
     *     si el tipo no es `byte` ni `ushort`
     */
    public IndexColorModel(int bits, int size, int[] cmap, int start, int transferType,
            BigInteger validBits) {
        super(bits, opaqueBits, ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false,
                Transparency.OPAQUE, transferType);
        if (bits < 1 || bits > 16) {
            throw new IllegalArgumentException("Number of bits must be between 1 and 16.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Map size (" + size + ") must be >= 1");
        }
        if (transferType != DataBuffer.TYPE_BYTE && transferType != DataBuffer.TYPE_USHORT) {
            throw new IllegalArgumentException("transferType must be one of "
                    + "DataBuffer.TYPE_BYTE or DataBuffer.TYPE_USHORT");
        }
        if (validBits != null) {
            // Una paleta llena de entradas validas es lo mismo que no tener mapa, y no tenerlo
            // ahorra una consulta de BigInteger por pixel en todo lo que sigue.
            boolean llena = true;
            for (int i = 0; i < size; i++) {
                if (!validBits.testBit(i)) {
                    llena = false;
                    break;
                }
            }
            if (!llena) {
                this.validBits = validBits;
            }
        }
        this.setRGBs(size, cmap, start, true);
    }

    /** Cuánto hay que reservar para la tabla, con lugar de sobra para índices fuera de rango. */
    private static int calcRealMapSize(int bits, int size) {
        int newSize = Math.max(1 << bits, size);
        return Math.max(newSize, 256);
    }

    /** Arma la tabla desde tres o cuatro arreglos de componentes. */
    private void setRGBs(int size, byte[] r, byte[] g, byte[] b, byte[] a) {
        if (size < 1) {
            throw new IllegalArgumentException("Map size (" + size + ") must be >= 1");
        }
        this.map_size = size;
        this.rgb = new int[calcRealMapSize(this.pixel_bits, size)];
        int alpha = 0xFF;
        int trans = Transparency.OPAQUE;
        boolean allgray = true;
        int transparentIndex = -1;
        for (int i = 0; i < size; i++) {
            int rc = r[i] & 0xFF;
            int gc = g[i] & 0xFF;
            int bc = b[i] & 0xFF;
            allgray = allgray && rc == gc && gc == bc;
            if (a != null) {
                alpha = a[i] & 0xFF;
                if (alpha != 0xFF) {
                    if (alpha == 0x00) {
                        if (trans == Transparency.OPAQUE) {
                            trans = Transparency.BITMASK;
                        }
                        if (transparentIndex < 0) {
                            transparentIndex = i;
                        }
                    } else {
                        trans = Transparency.TRANSLUCENT;
                    }
                    allgray = false;
                }
            }
            this.rgb[i] = (alpha << 24) | (rc << 16) | (gc << 8) | bc;
        }
        this.allgrayopaque = allgray;
        this.setTransparency(trans);
        this.setTransparentPixel(transparentIndex);
    }

    /** Arma la tabla desde un arreglo de bytes con tres o cuatro por entrada. */
    private void setRGBs(int bits, int size, byte[] cmap, int start, boolean hasalpha, int trans) {
        if (size < 1) {
            throw new IllegalArgumentException("Map size (" + size + ") must be >= 1");
        }
        this.map_size = size;
        this.rgb = new int[calcRealMapSize(bits, size)];
        int j = start;
        int alpha = 0xFF;
        int transparency = Transparency.OPAQUE;
        boolean allgray = true;
        int transparentIndex = -1;
        for (int i = 0; i < size; i++) {
            int rc = cmap[j] & 0xFF;
            j = j + 1;
            int gc = cmap[j] & 0xFF;
            j = j + 1;
            int bc = cmap[j] & 0xFF;
            j = j + 1;
            allgray = allgray && rc == gc && gc == bc;
            if (hasalpha) {
                alpha = cmap[j] & 0xFF;
                j = j + 1;
                if (alpha != 0xFF) {
                    if (alpha == 0x00) {
                        if (transparency == Transparency.OPAQUE) {
                            transparency = Transparency.BITMASK;
                        }
                        if (transparentIndex < 0) {
                            transparentIndex = i;
                        }
                    } else {
                        transparency = Transparency.TRANSLUCENT;
                    }
                    allgray = false;
                }
            }
            this.rgb[i] = (alpha << 24) | (rc << 16) | (gc << 8) | bc;
        }
        this.allgrayopaque = allgray;
        this.setTransparency(transparency);
        this.setTransparentPixel(trans >= 0 ? trans : transparentIndex);
    }

    /** Arma la tabla desde un arreglo de ARGB. */
    private void setRGBs(int size, int[] cmap, int start, boolean hasalpha) {
        this.map_size = size;
        this.rgb = new int[calcRealMapSize(this.pixel_bits, size)];
        int transparency = Transparency.OPAQUE;
        boolean allgray = true;
        BigInteger validBitsLocal = this.validBits;
        for (int i = 0; i < size; i++) {
            int cmapValue = cmap[i + start];
            this.rgb[i] = cmapValue;
            if (validBitsLocal != null && !validBitsLocal.testBit(i)) {
                continue;
            }
            int rc = (cmapValue >> 16) & 0xFF;
            int gc = (cmapValue >> 8) & 0xFF;
            int bc = cmapValue & 0xFF;
            allgray = allgray && rc == gc && gc == bc;
            if (hasalpha) {
                int alpha = cmapValue >>> 24;
                if (alpha != 0xFF) {
                    if (alpha == 0x00) {
                        if (transparency == Transparency.OPAQUE) {
                            transparency = Transparency.BITMASK;
                        }
                        if (this.transparent_index < 0) {
                            this.transparent_index = i;
                        }
                    } else {
                        transparency = Transparency.TRANSLUCENT;
                    }
                    allgray = false;
                }
            } else {
                this.rgb[i] = cmapValue | 0xFF000000;
            }
        }
        this.allgrayopaque = allgray;
        this.setTransparency(transparency);
    }

    /**
     * Fija la transparencia deducida y ajusta cuántas componentes tiene el modelo.
     *
     * <p>Un modelo indexado opaco tiene tres componentes y uno con alfa, cuatro. Eso cambia lo que
     * devuelven `getNumComponents` y `getComponentSize`, así que hay que ajustarlo acá y no en el
     * constructor: recién ahora se sabe.
     */
    private void setTransparency(int transparency) {
        if (this.transparency != transparency) {
            this.transparency = transparency;
            if (transparency == Transparency.OPAQUE) {
                this.supportsAlpha = false;
                this.numComponents = 3;
                this.nBits = opaqueBits;
            } else {
                this.supportsAlpha = true;
                this.numComponents = 4;
                this.nBits = alphaBits;
            }
        }
    }

    /** Marca una entrada como transparente, poniéndole alfa cero. */
    private void setTransparentPixel(int trans) {
        if (trans < 0 || trans >= this.map_size) {
            return;
        }
        this.rgb[trans] = this.rgb[trans] & 0x00FFFFFF;
        this.transparent_index = trans;
        this.allgrayopaque = false;
        if (this.transparency == Transparency.OPAQUE) {
            this.setTransparency(Transparency.BITMASK);
        }
    }

    /** `OPAQUE`, `BITMASK` o `TRANSLUCENT`, deducido de la paleta. */
    public int getTransparency() {
        return this.transparency;
    }

    /** Ocho bits por componente; cuatro componentes si hay alfa. */
    public int[] getComponentSize() {
        if (this.nBits == null) {
            return null;
        }
        return this.nBits.clone();
    }

    /** Cuántas entradas tiene la paleta. */
    public final int getMapSize() {
        return this.map_size;
    }

    /** La entrada marcada como transparente, o -1 si no hay. */
    public final int getTransparentPixel() {
        return this.transparent_index;
    }

    /** Copia los rojos de la paleta. */
    public final void getReds(byte[] r) {
        for (int i = 0; i < this.map_size; i++) {
            r[i] = (byte) (this.rgb[i] >> 16);
        }
    }

    /** Copia los verdes de la paleta. */
    public final void getGreens(byte[] g) {
        for (int i = 0; i < this.map_size; i++) {
            g[i] = (byte) (this.rgb[i] >> 8);
        }
    }

    /** Copia los azules de la paleta. */
    public final void getBlues(byte[] b) {
        for (int i = 0; i < this.map_size; i++) {
            b[i] = (byte) this.rgb[i];
        }
    }

    /** Copia los alfas de la paleta. */
    public final void getAlphas(byte[] a) {
        for (int i = 0; i < this.map_size; i++) {
            a[i] = (byte) (this.rgb[i] >> 24);
        }
    }

    /** Copia la paleta entera como ARGB. */
    public final void getRGBs(int[] rgb) {
        System.arraycopy(this.rgb, 0, rgb, 0, this.map_size);
    }

    /** El rojo de esa entrada de la paleta. */
    public final int getRed(int pixel) {
        return (this.rgb[pixel] >> 16) & 0xFF;
    }

    /** El verde de esa entrada de la paleta. */
    public final int getGreen(int pixel) {
        return (this.rgb[pixel] >> 8) & 0xFF;
    }

    /** El azul de esa entrada de la paleta. */
    public final int getBlue(int pixel) {
        return this.rgb[pixel] & 0xFF;
    }

    /** El alfa de esa entrada de la paleta. */
    public final int getAlpha(int pixel) {
        return (this.rgb[pixel] >> 24) & 0xFF;
    }

    /** El ARGB de esa entrada de la paleta. */
    public final int getRGB(int pixel) {
        return this.rgb[pixel];
    }

    /**
     * El índice de paleta que mejor representa ese color.
     *
     * <p>Primero se busca una coincidencia exacta y, si no la hay, la entrada más cercana por
     * distancia euclídea al cuadrado en las cuatro componentes. Ante un empate gana el índice más
     * chico, para que el resultado no dependa del orden en que se recorra.
     *
     * <p>Es la operación cara de la clase, y la que efectivamente pierde información: el color que
     * entra casi nunca es el que sale.
     *
     * @throws UnsupportedOperationException si el tipo de transferencia no es `byte` ni `ushort`
     */
    public synchronized Object getDataElements(int rgb, Object pixel) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int alpha = rgb >>> 24;
        int pix = 0;
        boolean encontrado = false;
        for (int i = 0; i < this.map_size; i++) {
            if (this.esValido(i) && this.rgb[i] == rgb) {
                pix = i;
                encontrado = true;
                break;
            }
        }
        if (!encontrado) {
            // Un color del todo transparente va a la entrada transparente si la hay: cualquier otra
            // le agregaria un color que no se ve pero que reaparece al componer.
            if (alpha == 0 && this.transparent_index >= 0) {
                pix = this.transparent_index;
            } else {
                int mejor = Integer.MAX_VALUE;
                for (int i = 0; i < this.map_size; i++) {
                    if (!this.esValido(i)) {
                        continue;
                    }
                    int c = this.rgb[i];
                    int da = (c >>> 24) - alpha;
                    int dr = ((c >> 16) & 0xFF) - red;
                    int dg = ((c >> 8) & 0xFF) - green;
                    int db = (c & 0xFF) - blue;
                    int error = da * da + dr * dr + dg * dg + db * db;
                    if (error < mejor) {
                        mejor = error;
                        pix = i;
                    }
                }
            }
        }
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            byte[] out = pixel == null ? new byte[1] : (byte[]) pixel;
            out[0] = (byte) pix;
            return out;
        }
        if (this.transferType == DataBuffer.TYPE_USHORT) {
            short[] out = pixel == null ? new short[1] : (short[]) pixel;
            out[0] = (short) pix;
            return out;
        }
        throw new UnsupportedOperationException(
                "This method has not been implemented for transferType " + this.transferType);
    }

    /** Si esa entrada de la paleta corresponde a un color de verdad. */
    private boolean esValido(int pixel) {
        return this.validBits == null || this.validBits.testBit(pixel);
    }

    /** Las componentes del color de esa entrada. */
    public int[] getComponents(int pixel, int[] components, int offset) {
        int[] out = components;
        if (out == null) {
            out = new int[offset + this.numComponents];
        }
        out[offset] = this.getRed(pixel);
        out[offset + 1] = this.getGreen(pixel);
        out[offset + 2] = this.getBlue(pixel);
        if (this.supportsAlpha && out.length - offset > 3) {
            out[offset + 3] = this.getAlpha(pixel);
        }
        return out;
    }

    /**
     * Las componentes del color de ese píxel crudo.
     *
     * @throws UnsupportedOperationException si el tipo de transferencia no es `byte` ni `ushort`
     */
    public int[] getComponents(Object pixel, int[] components, int offset) {
        int pix;
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            pix = ((byte[]) pixel)[0] & 0xFF;
        } else if (this.transferType == DataBuffer.TYPE_USHORT) {
            pix = ((short[]) pixel)[0] & 0xFFFF;
        } else if (this.transferType == DataBuffer.TYPE_INT) {
            pix = ((int[]) pixel)[0];
        } else {
            throw new UnsupportedOperationException(
                    "This method has not been implemented for transferType " + this.transferType);
        }
        return this.getComponents(pix, components, offset);
    }

    /**
     * El índice de paleta más cercano a ese color dado por componentes.
     *
     * @throws IllegalArgumentException si el arreglo no trae todas las componentes
     */
    public int getDataElement(int[] components, int offset) {
        int rgb = (components[offset] << 16) | (components[offset + 1] << 8)
                | components[offset + 2];
        if (this.supportsAlpha) {
            rgb = rgb | (components[offset + 3] << 24);
        } else {
            rgb = rgb | 0xFF000000;
        }
        Object inData = this.getDataElements(rgb, null);
        if (this.transferType == DataBuffer.TYPE_BYTE) {
            return ((byte[]) inData)[0] & 0xFF;
        }
        return ((short[]) inData)[0] & 0xFFFF;
    }

    /**
     * Como el anterior, en un arreglo del tipo de transferencia.
     *
     * @throws UnsupportedOperationException si el tipo de transferencia no es `byte` ni `ushort`
     */
    public Object getDataElements(int[] components, int offset, Object pixel) {
        int rgb = (components[offset] << 16) | (components[offset + 1] << 8)
                | components[offset + 2];
        if (this.supportsAlpha) {
            rgb = rgb | (components[offset + 3] << 24);
        } else {
            rgb = rgb | 0xFF000000;
        }
        return this.getDataElements(rgb, pixel);
    }

    /**
     * Un ráster que guarde un índice por píxel.
     *
     * <p>Con paletas chicas sale un ráster empaquetado de varios píxeles por elemento: una imagen de
     * dos colores ocupa un bit por píxel y no un byte.
     *
     * @throws IllegalArgumentException si el tamaño es vacío
     */
    public WritableRaster createCompatibleWritableRaster(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") cannot be <= 0");
        }
        if (this.pixel_bits == 1 || this.pixel_bits == 2 || this.pixel_bits == 4) {
            return Raster.createPackedRaster(DataBuffer.TYPE_BYTE, w, h, 1, this.pixel_bits, null);
        }
        if (this.pixel_bits <= 8) {
            return Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, w, h, 1, null);
        }
        if (this.pixel_bits <= 16) {
            return Raster.createInterleavedRaster(DataBuffer.TYPE_USHORT, w, h, 1, null);
        }
        throw new UnsupportedOperationException("This method is not supported for pixel bits > 16.");
    }

    /** Si ese ráster tiene una sola banda del tipo y ancho que corresponde. */
    public boolean isCompatibleRaster(Raster raster) {
        int size = raster.getSampleModel().getSampleSize(0);
        return raster.getTransferType() == this.transferType && raster.getNumBands() == 1
                && (1 << size) >= this.map_size;
    }

    /**
     * Un modelo de muestras de una banda.
     *
     * @throws IllegalArgumentException si el tamaño es vacío
     */
    public SampleModel createCompatibleSampleModel(int w, int h) {
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException("Width (" + w + ") and height (" + h
                    + ") cannot be <= 0");
        }
        if (this.pixel_bits == 1 || this.pixel_bits == 2 || this.pixel_bits == 4) {
            return new MultiPixelPackedSampleModel(this.transferType, w, h, this.pixel_bits);
        }
        int[] bandOffsets = new int[1];
        bandOffsets[0] = 0;
        return new ComponentSampleModel(this.transferType, w, h, 1, w, bandOffsets);
    }

    /** Si ese modelo de muestras guarda un índice por píxel. */
    public boolean isCompatibleSampleModel(SampleModel sm) {
        if (!(sm instanceof ComponentSampleModel) && !(sm instanceof MultiPixelPackedSampleModel)) {
            return false;
        }
        if (sm.getTransferType() != this.transferType) {
            return false;
        }
        return sm.getNumBands() == 1;
    }

    /** Si todas las entradas de la paleta son válidas. */
    public boolean isValid() {
        return this.validBits == null;
    }

    /** Si esa entrada de la paleta es válida. */
    public boolean isValid(int pixel) {
        if (pixel < 0 || pixel >= this.map_size) {
            return false;
        }
        return this.esValido(pixel);
    }

    /**
     * Qué entradas de la paleta son válidas, o `null` si lo son todas.
     *
     * <p>El bit `i` está prendido si la entrada `i` corresponde a un color de verdad.
     */
    public BigInteger getValidPixels() {
        if (this.validBits == null) {
            return this.getAllValid();
        }
        return this.validBits;
    }

    /** Un mapa con todas las entradas prendidas. */
    private BigInteger getAllValid() {
        int numbytes = (this.map_size + 7) / 8;
        byte[] valid = new byte[numbytes];
        java.util.Arrays.fill(valid, (byte) 0xFF);
        valid[0] = (byte) (0xFF >>> (numbytes * 8 - this.map_size));
        return new BigInteger(1, valid);
    }

    /**
     * Deshace la indirección: la misma imagen con el color en cada píxel.
     *
     * <p>Es la operación inversa a indexar, y no pierde nada — la paleta ya estaba en el color de
     * cada índice.
     *
     * @param forceARGB `true` para que el resultado tenga alfa aunque la paleta sea opaca
     */
    public BufferedImage convertToIntDiscrete(Raster raster, boolean forceARGB) {
        ColorModel cm;
        if (forceARGB || this.transparency == Transparency.TRANSLUCENT) {
            cm = ColorModel.getRGBdefault();
        } else if (this.transparency == Transparency.BITMASK) {
            cm = new DirectColorModel(25, 0xFF0000, 0x00FF00, 0x0000FF, 0x1000000);
        } else {
            cm = new DirectColorModel(24, 0xFF0000, 0x00FF00, 0x0000FF);
        }
        int w = raster.getWidth();
        int h = raster.getHeight();
        WritableRaster discreteRaster = cm.createCompatibleWritableRaster(w, h);
        int rX = raster.getMinX();
        int rY = raster.getMinY();
        int[] fila = new int[w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                fila[x] = this.rgb[raster.getSample(rX + x, rY + y, 0)];
            }
            discreteRaster.setDataElements(0, y, w, 1, fila);
        }
        return new BufferedImage(cm, discreteRaster, false, null);
    }

    public String toString() {
        return "IndexColorModel: #pixelBits = " + this.pixel_bits + " numComponents = "
                + this.numComponents + " color space = " + this.getColorSpace()
                + " transparency = " + this.transparency + " transIndex   = "
                + this.transparent_index + " has alpha = " + this.supportsAlpha
                + " isAlphaPre = " + this.isAlphaPremultiplied;
    }
}
