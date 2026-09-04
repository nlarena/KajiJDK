package java.awt.image;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

/**
 * Una imagen con los píxeles **en memoria**, accesibles y modificables.
 *
 * <p>Es donde se juntan las tres piezas del paquete: un {@link WritableRaster} que tiene los
 * píxeles, un {@link ColorModel} que dice qué color son, y la interfaz {@link Image} que la hace
 * dibujable en cualquier lado. Todo lo demás de `java.awt.image` existe para producir una de éstas o
 * para transformarla.
 *
 * <p>Las constantes `TYPE_*` son atajos para las combinaciones usuales de modelo de color y
 * disposición, y elegir bien importa más de lo que parece: `TYPE_INT_RGB` guarda cuatro bytes por
 * píxel para tres componentes y se dibuja rapidísimo; `TYPE_3BYTE_BGR` guarda tres y es más lento.
 * `TYPE_CUSTOM` no es un formato sino la respuesta "esto no es ninguno de los otros".
 *
 * <p>La familia `_PRE` guarda el color ya multiplicado por el alfa. No es una variante decorativa:
 * componer imágenes premultiplicadas es sumar, y sin premultiplicar hay una multiplicación por píxel
 * y por operación. Se paga al crearla y se ahorra en cada dibujado.
 *
 * <p>Es una {@link RenderedImage} de **un solo mosaico**, y de ahí salen casi todas sus respuestas
 * sobre mosaicos: uno a lo ancho, uno a lo alto, el (0,0), del tamaño de la imagen.
 *
 * <p><strong>No se puede dibujar encima.</strong> {@link #createGraphics} y {@link #getGraphics}
 * tiran `UnsupportedOperationException`: devolver un {@link Graphics2D} exige un rasterizador
 * —relleno por barrido, recorte, trazo, composición— que esta biblioteca todavía no tiene. Todo lo
 * demás de la clase funciona: leer y escribir píxeles, recortar, copiar, convertir y filtrar. Un
 * miembro que falta es un subconjunto legal; uno que miente, no.
 */
public class BufferedImage extends Image implements WritableRenderedImage, Transparency {

    /** Ninguno de los formatos con nombre. */
    public static final int TYPE_CUSTOM = 0;

    /** Un `int` por píxel, sin alfa: 8 bits de relleno y 8 por componente. */
    public static final int TYPE_INT_RGB = 1;

    /** Un `int` por píxel, con alfa. */
    public static final int TYPE_INT_ARGB = 2;

    /** Como el anterior, con el color ya multiplicado por el alfa. */
    public static final int TYPE_INT_ARGB_PRE = 3;

    /** Un `int` por píxel con las componentes al revés: azul en los bits altos. */
    public static final int TYPE_INT_BGR = 4;

    /** Tres bytes por píxel, en orden azul, verde, rojo. */
    public static final int TYPE_3BYTE_BGR = 5;

    /** Cuatro bytes por píxel, en orden alfa, azul, verde, rojo. */
    public static final int TYPE_4BYTE_ABGR = 6;

    /** Como el anterior, con el color ya multiplicado por el alfa. */
    public static final int TYPE_4BYTE_ABGR_PRE = 7;

    /** Dieciséis bits por píxel repartidos 5-6-5. */
    public static final int TYPE_USHORT_565_RGB = 8;

    /** Quince bits por píxel repartidos 5-5-5. */
    public static final int TYPE_USHORT_555_RGB = 9;

    /** Un byte por píxel, en gris. */
    public static final int TYPE_BYTE_GRAY = 10;

    /** Dieciséis bits por píxel, en gris. */
    public static final int TYPE_USHORT_GRAY = 11;

    /** Uno, dos o cuatro bits por píxel, con paleta. */
    public static final int TYPE_BYTE_BINARY = 12;

    /** Un byte por píxel, con paleta de hasta 256 colores. */
    public static final int TYPE_BYTE_INDEXED = 13;

    private int imageType = TYPE_CUSTOM;
    private ColorModel colorModel;
    private final WritableRaster raster;
    private Hashtable<String, Object> properties;
    private ImageProducer source;

    /**
     * Una imagen de uno de los formatos con nombre.
     *
     * @throws IllegalArgumentException si el tipo no es uno de los catorce, si es
     *     {@link #TYPE_CUSTOM}, o si el tamaño es vacío
     */
    public BufferedImage(int width, int height, int imageType) {
        ColorModel cm;
        WritableRaster wr;
        if (imageType == TYPE_INT_RGB) {
            cm = new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_INT_ARGB) {
            cm = ColorModel.getRGBdefault();
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_INT_ARGB_PRE) {
            cm = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 32,
                    0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000, true, DataBuffer.TYPE_INT);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_INT_BGR) {
            cm = new DirectColorModel(24, 0x000000FF, 0x0000FF00, 0x00FF0000);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_3BYTE_BGR) {
            int[] nBits = { 8, 8, 8 };
            int[] bOffs = { 2, 1, 0 };
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, false,
                    false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, width * 3, 3,
                    bOffs, null);
        } else if (imageType == TYPE_4BYTE_ABGR || imageType == TYPE_4BYTE_ABGR_PRE) {
            int[] nBits = { 8, 8, 8, 8 };
            int[] bOffs = { 3, 2, 1, 0 };
            boolean pre = imageType == TYPE_4BYTE_ABGR_PRE;
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), nBits, true,
                    pre, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
            wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, width * 4, 4,
                    bOffs, null);
        } else if (imageType == TYPE_USHORT_565_RGB) {
            cm = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 16, 0xF800,
                    0x07E0, 0x001F, 0, false, DataBuffer.TYPE_USHORT);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_USHORT_555_RGB) {
            cm = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), 15, 0x7C00,
                    0x03E0, 0x001F, 0, false, DataBuffer.TYPE_USHORT);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_BYTE_GRAY) {
            int[] nBits = { 8 };
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), nBits, false,
                    true, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_USHORT_GRAY) {
            int[] nBits = { 16 };
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), nBits, false,
                    true, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_BYTE_BINARY) {
            byte[] arr = { (byte) 0, (byte) 0xFF };
            cm = new IndexColorModel(1, 2, arr, arr, arr);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else if (imageType == TYPE_BYTE_INDEXED) {
            // Un cubo de 6x6x6 colores, que son 216, y el resto una rampa de grises: es la paleta
            // de 256 con la que cualquier imagen queda razonable sin tener que calcularle una.
            int[] cmap = new int[256];
            int i = 0;
            for (int r = 0; r < 256; r = r + 51) {
                for (int g = 0; g < 256; g = g + 51) {
                    for (int b = 0; b < 256; b = b + 51) {
                        cmap[i] = (r << 16) | (g << 8) | b;
                        i = i + 1;
                    }
                }
            }
            int grayIncr = 256 / (256 - i);
            int gray = grayIncr * 3;
            while (i < 256) {
                cmap[i] = (gray << 16) | (gray << 8) | gray;
                gray = gray + grayIncr;
                i = i + 1;
            }
            cm = new IndexColorModel(8, 256, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
            wr = cm.createCompatibleWritableRaster(width, height);
        } else {
            throw new IllegalArgumentException("Unknown image type " + imageType);
        }
        this.colorModel = cm;
        this.raster = wr;
        this.imageType = imageType;
    }

    /**
     * Una imagen con paleta.
     *
     * @throws IllegalArgumentException si el tipo no es {@link #TYPE_BYTE_BINARY} ni
     *     {@link #TYPE_BYTE_INDEXED}, si la paleta tiene alfa premultiplicado, o si tiene más de 16
     *     entradas para el tipo binario
     */
    public BufferedImage(int width, int height, int imageType, IndexColorModel cm) {
        if (cm.hasAlpha() && cm.isAlphaPremultiplied()) {
            throw new IllegalArgumentException("This image types do not have premultiplied alpha.");
        }
        WritableRaster wr;
        if (imageType == TYPE_BYTE_BINARY) {
            int bits;
            int mapSize = cm.getMapSize();
            if (mapSize <= 2) {
                bits = 1;
            } else if (mapSize <= 4) {
                bits = 2;
            } else if (mapSize <= 16) {
                bits = 4;
            } else {
                throw new IllegalArgumentException("Color map for TYPE_BYTE_BINARY must have "
                        + "no more than 16 entries");
            }
            wr = Raster.createPackedRaster(DataBuffer.TYPE_BYTE, width, height, 1, bits, null);
        } else if (imageType == TYPE_BYTE_INDEXED) {
            wr = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 1, null);
        } else {
            throw new IllegalArgumentException("Invalid image type (" + imageType + ").  Image "
                    + "type must be either TYPE_BYTE_BINARY or  TYPE_BYTE_INDEXED");
        }
        if (!cm.isCompatibleRaster(wr)) {
            throw new IllegalArgumentException("Incompatible image type and IndexColorModel");
        }
        this.colorModel = cm;
        this.raster = wr;
        this.imageType = imageType;
    }

    /**
     * Una imagen sobre un ráster y un modelo de color dados.
     *
     * <p>Es el constructor general y el único que puede dar una imagen de {@link #TYPE_CUSTOM}. El
     * tipo se **deduce** mirando el modelo y la disposición: si coinciden con alguno de los formatos
     * con nombre, se declara ése.
     *
     * @throws IllegalArgumentException si el ráster no le sirve al modelo de color, o si su ángulo
     *     no está en el origen
     * @throws RasterFormatException si el ráster no tiene bandas suficientes para el modelo
     * @throws NullPointerException si falta el modelo o el ráster
     */
    public BufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
            Hashtable<?, ?> properties) {
        if (!cm.isCompatibleRaster(raster)) {
            throw new IllegalArgumentException("Raster " + raster
                    + " is incompatible with ColorModel " + cm);
        }
        if (raster.getMinX() != 0 || raster.getMinY() != 0) {
            throw new IllegalArgumentException("Raster " + raster
                    + " has minX or minY not equal to zero: " + raster.getMinX() + " "
                    + raster.getMinY());
        }
        this.colorModel = cm;
        this.raster = raster;
        if (properties != null && !properties.isEmpty()) {
            this.properties = new Hashtable<String, Object>();
            java.util.Enumeration<?> e = properties.keys();
            while (e.hasMoreElements()) {
                Object key = e.nextElement();
                if (key instanceof String) {
                    this.properties.put((String) key, properties.get(key));
                }
            }
        }
        // El estado de premultiplicacion del raster tiene que coincidir con el que declara el
        // modelo: si no, cada lectura de color estaria deshaciendo una cuenta que no se hizo.
        this.coerceData(isRasterPremultiplied);
        this.imageType = this.deducirTipo();
    }

    /**
     * Cuál de los formatos con nombre describe a este modelo y este ráster.
     *
     * @return el tipo, o {@link #TYPE_CUSTOM} si no es ninguno
     */
    private int deducirTipo() {
        ColorModel cm = this.colorModel;
        SampleModel sm = this.raster.getSampleModel();
        ColorSpace cs = cm.getColorSpace();
        boolean srgb = cs == ColorSpace.getInstance(ColorSpace.CS_sRGB);
        if (cm instanceof DirectColorModel && sm instanceof SinglePixelPackedSampleModel) {
            DirectColorModel dcm = (DirectColorModel) cm;
            if (srgb && dcm.getRedMask() == 0x00FF0000 && dcm.getGreenMask() == 0x0000FF00
                    && dcm.getBlueMask() == 0x000000FF) {
                if (!dcm.hasAlpha()) {
                    return TYPE_INT_RGB;
                }
                if (dcm.getAlphaMask() == 0xFF000000) {
                    return dcm.isAlphaPremultiplied() ? TYPE_INT_ARGB_PRE : TYPE_INT_ARGB;
                }
            }
            if (srgb && !dcm.hasAlpha() && dcm.getRedMask() == 0x000000FF
                    && dcm.getGreenMask() == 0x0000FF00 && dcm.getBlueMask() == 0x00FF0000) {
                return TYPE_INT_BGR;
            }
            if (srgb && !dcm.hasAlpha() && dcm.getTransferType() == DataBuffer.TYPE_USHORT) {
                if (dcm.getRedMask() == 0xF800 && dcm.getGreenMask() == 0x07E0
                        && dcm.getBlueMask() == 0x001F) {
                    return TYPE_USHORT_565_RGB;
                }
                if (dcm.getRedMask() == 0x7C00 && dcm.getGreenMask() == 0x03E0
                        && dcm.getBlueMask() == 0x001F) {
                    return TYPE_USHORT_555_RGB;
                }
            }
            return TYPE_CUSTOM;
        }
        if (cm instanceof IndexColorModel && sm.getNumBands() == 1) {
            if (sm instanceof MultiPixelPackedSampleModel) {
                int bits = ((MultiPixelPackedSampleModel) sm).getPixelBitStride();
                if (bits == 1 || bits == 2 || bits == 4) {
                    return TYPE_BYTE_BINARY;
                }
                return TYPE_CUSTOM;
            }
            if (cm.getPixelSize() == 8 && sm.getTransferType() == DataBuffer.TYPE_BYTE) {
                return TYPE_BYTE_INDEXED;
            }
            return TYPE_CUSTOM;
        }
        if (cm instanceof ComponentColorModel && sm instanceof ComponentSampleModel) {
            ComponentSampleModel csm = (ComponentSampleModel) sm;
            int[] offs = csm.getBandOffsets();
            if (cs.getType() == ColorSpace.TYPE_GRAY && srgbGris(cs) && !cm.hasAlpha()
                    && offs.length == 1 && csm.getPixelStride() == 1) {
                if (sm.getTransferType() == DataBuffer.TYPE_BYTE && cm.getComponentSize(0) == 8) {
                    return TYPE_BYTE_GRAY;
                }
                if (sm.getTransferType() == DataBuffer.TYPE_USHORT
                        && cm.getComponentSize(0) == 16) {
                    return TYPE_USHORT_GRAY;
                }
                return TYPE_CUSTOM;
            }
            if (!srgb || sm.getTransferType() != DataBuffer.TYPE_BYTE) {
                return TYPE_CUSTOM;
            }
            if (!cm.hasAlpha() && offs.length == 3 && csm.getPixelStride() == 3
                    && offs[0] == 2 && offs[1] == 1 && offs[2] == 0) {
                return TYPE_3BYTE_BGR;
            }
            if (cm.hasAlpha() && offs.length == 4 && csm.getPixelStride() == 4
                    && offs[0] == 3 && offs[1] == 2 && offs[2] == 1 && offs[3] == 0) {
                return cm.isAlphaPremultiplied() ? TYPE_4BYTE_ABGR_PRE : TYPE_4BYTE_ABGR;
            }
        }
        return TYPE_CUSTOM;
    }

    /** Si ese espacio es el gris de fábrica. */
    private static boolean srgbGris(ColorSpace cs) {
        return cs == ColorSpace.getInstance(ColorSpace.CS_GRAY);
    }

    /** El formato, o {@link #TYPE_CUSTOM}. */
    public int getType() {
        return this.imageType;
    }

    /** El modelo de color. */
    public ColorModel getColorModel() {
        return this.colorModel;
    }

    /**
     * El ráster con los píxeles.
     *
     * <p>Es el de verdad, no una copia: escribirlo cambia la imagen.
     */
    public WritableRaster getRaster() {
        return this.raster;
    }

    /**
     * El canal alfa como un ráster de una banda, o `null` si no hay.
     *
     * <p>Comparte los datos con la imagen.
     */
    public WritableRaster getAlphaRaster() {
        return this.colorModel.getAlphaRaster(this.raster);
    }

    /** El color de un píxel, en ARGB de ocho bits por canal. */
    public int getRGB(int x, int y) {
        return this.colorModel.getRGB(this.raster.getDataElements(x, y, null));
    }

    /**
     * Los colores de un rectángulo, en ARGB.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale o el arreglo no alcanza
     */
    public int[] getRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset,
            int scansize) {
        int[] out = rgbArray;
        if (out == null) {
            out = new int[offset + h * scansize];
        }
        Object data = null;
        int yoff = offset;
        for (int y = startY; y < startY + h; y++) {
            int off = yoff;
            for (int x = startX; x < startX + w; x++) {
                data = this.raster.getDataElements(x, y, data);
                out[off] = this.colorModel.getRGB(data);
                off = off + 1;
            }
            yoff = yoff + scansize;
        }
        return out;
    }

    /**
     * Pone el color de un píxel, dado en ARGB.
     *
     * <p>El color se convierte al formato de la imagen, y si el formato no lo puede representar se
     * guarda el más parecido: escribir y volver a leer no siempre devuelve lo mismo.
     */
    public void setRGB(int x, int y, int rgb) {
        this.raster.setDataElements(x, y, this.colorModel.getDataElements(rgb, null));
    }

    /**
     * Pone los colores de un rectángulo, dados en ARGB.
     *
     * @throws ArrayIndexOutOfBoundsException si el rectángulo se sale o el arreglo no alcanza
     */
    public void setRGB(int startX, int startY, int w, int h, int[] rgbArray, int offset,
            int scansize) {
        Object pixel = null;
        int yoff = offset;
        for (int y = startY; y < startY + h; y++) {
            int off = yoff;
            for (int x = startX; x < startX + w; x++) {
                pixel = this.colorModel.getDataElements(rgbArray[off], pixel);
                this.raster.setDataElements(x, y, pixel);
                off = off + 1;
            }
            yoff = yoff + scansize;
        }
    }

    /** Ancho, en píxeles. */
    public int getWidth() {
        return this.raster.getWidth();
    }

    /** Alto, en píxeles. */
    public int getHeight() {
        return this.raster.getHeight();
    }

    /**
     * Ancho, en píxeles.
     *
     * <p>El observador no se usa: los píxeles ya están.
     */
    public int getWidth(ImageObserver observer) {
        return this.raster.getWidth();
    }

    /** Alto, en píxeles; el observador no se usa. */
    public int getHeight(ImageObserver observer) {
        return this.raster.getHeight();
    }

    /** Un productor que entrega estos píxeles. */
    public ImageProducer getSource() {
        if (this.source == null) {
            this.source = new BufferedImageSource(this);
        }
        return this.source;
    }

    /**
     * Una propiedad de la imagen.
     *
     * @return el valor, o {@link Image#UndefinedProperty} si no está definida
     * @throws NullPointerException si el nombre es `null`
     */
    public Object getProperty(String name, ImageObserver observer) {
        return this.getProperty(name);
    }

    /**
     * Una propiedad de la imagen.
     *
     * @return el valor, o {@link Image#UndefinedProperty} si no está definida
     * @throws NullPointerException si el nombre es `null`
     */
    public Object getProperty(String name) {
        if (name == null) {
            throw new NullPointerException("null property name is not allowed");
        }
        if (this.properties == null) {
            return Image.UndefinedProperty;
        }
        Object o = this.properties.get(name);
        if (o == null) {
            return Image.UndefinedProperty;
        }
        return o;
    }

    /** Los nombres de las propiedades, o `null` si no hay ninguna. */
    public String[] getPropertyNames() {
        if (this.properties == null || this.properties.isEmpty()) {
            return null;
        }
        Set<String> keys = this.properties.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    /**
     * Un contexto para dibujar sobre esta imagen.
     *
     * <p><strong>Funciona.</strong> Devuelve el rasterizador de esta biblioteca: lineas por
     * Bresenham, rellenos por barrido, arcos, poligonos, recorte rectangular, traslacion, copia de
     * areas y dibujado de otras {@code BufferedImage}. Lo unico que declina es {@code drawString},
     * que necesita los contornos de los glifos — un subsistema aparte que todavia no esta.
     *
     * <p>No es lo mismo que {@link #createGraphics}: aquel promete un {@link Graphics2D}, con
     * transformaciones afines, trazos, composicion y sugerencias de renderizado, y eso es una capa
     * mas que esta arriba de esta.
     */
    public Graphics getGraphics() {
        return new KajiGraphics(this);
    }

    /**
     * Un contexto para dibujar sobre esta imagen.
     *
     * <p><strong>Funciona.</strong> Devuelve el mismo objeto que {@link #getGraphics}, que es un
     * {@link Graphics2D} completo: transformaciones afines —traslación, rotación, escala,
     * cizalladura—, {@code draw} y {@code fill} de cualquier {@link java.awt.Shape}, grosor de
     * trazo, recorte por figura y dibujado de imágenes con transformación.
     *
     * <p>Tres cosas se guardan y se reportan pero <strong>no se aplican</strong>, y conviene
     * saberlo: una {@link java.awt.Paint} que no sea un color uniforme, una
     * {@link java.awt.Composite} con alfa parcial, y las sugerencias de renderizado. Las tres piden
     * evaluación por píxel contra el destino, que es un mecanismo que este tier no tiene. Lo que sí
     * se cumple es que {@code getPaint}, {@code getComposite} y {@code getRenderingHint} devuelvan
     * lo que se fijó, porque hay código que las guarda y las restaura.
     *
     * <p>Y {@code drawString} sigue declinando: necesita los contornos de los glifos.
     */
    public Graphics2D createGraphics() {
        return new KajiGraphics(this);
    }

    /**
     * Un recorte que comparte los píxeles con ésta.
     *
     * <p>No copia nada: escribir en el recorte cambia la imagen original.
     *
     * @throws RasterFormatException si el rectángulo no cae adentro
     */
    public BufferedImage getSubimage(int x, int y, int w, int h) {
        return new BufferedImage(this.colorModel, this.raster.createWritableChild(x, y, w, h, 0, 0,
                null), this.colorModel.isAlphaPremultiplied(), this.properties);
    }

    /** Si el color guardado ya está multiplicado por el alfa. */
    public boolean isAlphaPremultiplied() {
        return this.colorModel.isAlphaPremultiplied();
    }

    /**
     * Premultiplica los píxeles por su alfa, o lo deshace, **en el lugar**.
     *
     * <p>La operación pierde información en un sentido: premultiplicar un píxel de alfa cero lo
     * lleva a negro, y deshacerlo después no lo recupera.
     */
    public void coerceData(boolean isAlphaPremultiplied) {
        if (this.colorModel.hasAlpha()
                && this.colorModel.isAlphaPremultiplied() != isAlphaPremultiplied) {
            this.colorModel = this.colorModel.coerceData(this.raster, isAlphaPremultiplied);
        }
    }

    public String toString() {
        return "BufferedImage@" + Integer.toHexString(this.hashCode()) + ": type = "
                + this.imageType + " " + this.colorModel + " " + this.raster;
    }

    /** Las imágenes de las que ésta se calcula: ninguna. */
    public Vector<RenderedImage> getSources() {
        return null;
    }

    /** Cómo están dispuestos los píxeles. */
    public SampleModel getSampleModel() {
        return this.raster.getSampleModel();
    }

    /** Siempre 0: la imagen empieza en el origen. */
    public int getMinX() {
        return this.raster.getMinX();
    }

    /** Siempre 0. */
    public int getMinY() {
        return this.raster.getMinY();
    }

    /** Siempre 1: un solo mosaico. */
    public int getNumXTiles() {
        return 1;
    }

    /** Siempre 1. */
    public int getNumYTiles() {
        return 1;
    }

    /** Siempre 0. */
    public int getMinTileX() {
        return 0;
    }

    /** Siempre 0. */
    public int getMinTileY() {
        return 0;
    }

    /** El mosaico único mide lo que la imagen. */
    public int getTileWidth() {
        return this.getWidth();
    }

    /** El mosaico único mide lo que la imagen. */
    public int getTileHeight() {
        return this.getHeight();
    }

    /** Siempre 0. */
    public int getTileGridXOffset() {
        return this.raster.getSampleModelTranslateX();
    }

    /** Siempre 0. */
    public int getTileGridYOffset() {
        return this.raster.getSampleModelTranslateY();
    }

    /**
     * El mosaico único.
     *
     * @throws ArrayIndexOutOfBoundsException si los índices no son (0,0)
     */
    public Raster getTile(int tileX, int tileY) {
        if (tileX == 0 && tileY == 0) {
            return this.raster;
        }
        throw new ArrayIndexOutOfBoundsException("BufferedImages only have one tile with index 0,0");
    }

    /**
     * Una **copia** de la imagen entera.
     *
     * <p>A diferencia de {@link #getRaster}, esto copia: el resultado no comparte datos.
     */
    public Raster getData() {
        int width = this.raster.getWidth();
        int height = this.raster.getHeight();
        int startX = this.raster.getMinX();
        int startY = this.raster.getMinY();
        WritableRaster wr = Raster.createWritableRaster(this.raster.getSampleModel(),
                new Point(this.raster.getSampleModelTranslateX(),
                        this.raster.getSampleModelTranslateY()));
        Object tdata = null;
        for (int i = startY; i < startY + height; i++) {
            tdata = this.raster.getDataElements(startX, i, width, 1, tdata);
            wr.setDataElements(startX, i, width, 1, tdata);
        }
        return wr;
    }

    /**
     * Una copia de una región de la imagen.
     *
     * @throws NullPointerException si el rectángulo es `null`
     */
    public Raster getData(Rectangle rect) {
        SampleModel sm = this.raster.getSampleModel();
        SampleModel nsm = sm.createCompatibleSampleModel(rect.width, rect.height);
        WritableRaster wr = Raster.createWritableRaster(nsm, rect.getLocation());
        int width = rect.width;
        int height = rect.height;
        int startX = rect.x;
        int startY = rect.y;
        Object tdata = null;
        for (int i = startY; i < startY + height; i++) {
            tdata = this.raster.getDataElements(startX, i, width, 1, tdata);
            wr.setDataElements(startX, i, width, 1, tdata);
        }
        return wr;
    }

    /**
     * Copia la imagen en el ráster dado, o en uno nuevo si es `null`.
     *
     * <p>Sólo se copia la parte donde los dos se superponen.
     */
    public WritableRaster copyData(WritableRaster outRaster) {
        WritableRaster out = outRaster;
        if (out == null) {
            return (WritableRaster) this.getData();
        }
        int width = out.getWidth();
        int height = out.getHeight();
        int startX = out.getMinX();
        int startY = out.getMinY();
        Object tdata = null;
        for (int i = startY; i < startY + height; i++) {
            tdata = this.raster.getDataElements(startX, i, width, 1, tdata);
            out.setDataElements(startX, i, width, 1, tdata);
        }
        return out;
    }

    /**
     * Escribe un ráster en la imagen.
     *
     * <p>Sólo se escribe la parte que caiga adentro; el resto se descarta.
     */
    public void setData(Raster r) {
        int width = r.getWidth();
        int height = r.getHeight();
        int startX = r.getMinX();
        int startY = r.getMinY();
        int[] tdata = null;
        Rectangle rclip = new Rectangle(startX, startY, width, height);
        Rectangle bclip = new Rectangle(0, 0, this.raster.getWidth(), this.raster.getHeight());
        Rectangle intersect = rclip.intersection(bclip);
        if (intersect.isEmpty()) {
            return;
        }
        width = intersect.width;
        height = intersect.height;
        startX = intersect.x;
        startY = intersect.y;
        for (int i = startY; i < startY + height; i++) {
            tdata = r.getPixels(startX, i, width, 1, tdata);
            this.raster.setPixels(startX, i, width, 1, tdata);
        }
    }

    /**
     * Suma un observador de mosaicos.
     *
     * <p>No hace nada: el mosaico único de una imagen en memoria está siempre disponible para
     * escribir, así que no hay transición de la que avisar.
     */
    public void addTileObserver(TileObserver to) {
    }

    /** Saca a ese observador; no hace nada, por el mismo motivo. */
    public void removeTileObserver(TileObserver to) {
    }

    /**
     * Si ese mosaico está tomado para escribir.
     *
     * <p>Siempre `true` para el (0,0): en una imagen en memoria el mosaico está siempre escribible.
     *
     * @throws IllegalArgumentException si los índices no son (0,0)
     */
    public boolean isTileWritable(int tileX, int tileY) {
        if (tileX == 0 && tileY == 0) {
            return true;
        }
        throw new IllegalArgumentException("Only 1 tile in image");
    }

    /** El índice del mosaico único. */
    public Point[] getWritableTileIndices() {
        Point[] p = new Point[1];
        p[0] = new Point(0, 0);
        return p;
    }

    /** Siempre `true`. */
    public boolean hasTileWriters() {
        return true;
    }

    /**
     * El mosaico único, para escribir.
     *
     * @throws IllegalArgumentException si los índices no son (0,0)
     */
    public WritableRaster getWritableTile(int tileX, int tileY) {
        if (tileX == 0 && tileY == 0) {
            return this.raster;
        }
        throw new IllegalArgumentException("Only 1 tile in image");
    }

    /**
     * Devuelve el mosaico único.
     *
     * <p>No hace nada más que comprobar los índices: no hay cuenta de préstamos que llevar.
     *
     * @throws IllegalArgumentException si los índices no son (0,0)
     */
    public void releaseWritableTile(int tileX, int tileY) {
        if (tileX != 0 || tileY != 0) {
            throw new IllegalArgumentException("Only 1 tile in image");
        }
    }

    /** La transparencia del modelo de color. */
    public int getTransparency() {
        return this.colorModel.getTransparency();
    }
}
