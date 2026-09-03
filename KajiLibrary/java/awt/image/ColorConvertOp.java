package java.awt.image;

import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Convierte los colores de un espacio a otro.
 *
 * <p>Es la operación que hace que un rojo siga siendo el mismo rojo al pasar de una imagen a otra
 * con distinto espacio de color. No cambia los números por cambiarlos: cambia los números
 * **justamente para que el color no cambie**.
 *
 * <p>La conversión siempre pasa por CIEXYZ, que es el espacio de referencia donde el color se define
 * sin depender de ningún dispositivo. Encadenar varios perfiles es encadenar esos pasos: de cada uno
 * a XYZ y de XYZ al siguiente.
 *
 * <p>Hay cuatro maneras de armarla y la diferencia está en de dónde salen los dos extremos. Con un
 * solo {@link ColorSpace} el origen lo pone la imagen que se filtre y ése es el destino; con dos,
 * los dos están fijos y sirve también para rásters, que no tienen modelo de color propio; sin
 * ninguno, los dos salen de las imágenes.
 *
 * <p>Sobre un {@link Raster} hacen falta los dos espacios declarados y con la misma cantidad de
 * componentes que las bandas: un ráster es números sin interpretar, y sin decirle qué son no hay
 * nada que convertir.
 */
public class ColorConvertOp implements BufferedImageOp, RasterOp {

    private final ColorSpace[] spaces;
    private final ICC_Profile[] profiles;
    private final RenderingHints hints;

    /**
     * Sin espacios declarados: los dos salen de las imágenes que se filtren.
     *
     * <p>No sirve para rásters.
     */
    public ColorConvertOp(RenderingHints hints) {
        this.spaces = new ColorSpace[0];
        this.profiles = null;
        this.hints = hints;
    }

    /**
     * Con el espacio de destino; el de origen sale de la imagen.
     *
     * @throws NullPointerException si el espacio es `null`
     */
    public ColorConvertOp(ColorSpace cspace, RenderingHints hints) {
        if (cspace == null) {
            throw new NullPointerException("ColorSpace cannot be null");
        }
        this.spaces = new ColorSpace[1];
        this.spaces[0] = cspace;
        this.profiles = null;
        this.hints = hints;
    }

    /**
     * Con los dos espacios declarados.
     *
     * @throws NullPointerException si falta alguno de los dos
     */
    public ColorConvertOp(ColorSpace srcCspace, ColorSpace dstCspace, RenderingHints hints) {
        if (srcCspace == null || dstCspace == null) {
            throw new NullPointerException("ColorSpaces cannot be null");
        }
        this.spaces = new ColorSpace[2];
        this.spaces[0] = srcCspace;
        this.spaces[1] = dstCspace;
        this.profiles = null;
        this.hints = hints;
    }

    /**
     * Con una cadena de perfiles ICC.
     *
     * @throws NullPointerException si el arreglo es `null`
     * @throws IllegalArgumentException si el arreglo está vacío
     */
    public ColorConvertOp(ICC_Profile[] profiles, RenderingHints hints) {
        if (profiles == null) {
            throw new NullPointerException("Profiles cannot be null");
        }
        this.profiles = profiles.clone();
        this.spaces = new ColorSpace[profiles.length];
        for (int i = 0; i < profiles.length; i++) {
            this.spaces[i] = new ICC_ColorSpace(profiles[i]);
        }
        this.hints = hints;
    }

    /** Los perfiles con los que se armó, o un arreglo vacío si no se armó con perfiles. */
    public final ICC_Profile[] getICC_Profiles() {
        if (this.profiles == null) {
            return new ICC_Profile[0];
        }
        return this.profiles.clone();
    }

    /**
     * Convierte un color de un espacio a otro pasando por CIEXYZ.
     *
     * <p>Es donde vive toda la conversión: los dos espacios saben ir y venir de XYZ, y componer esas
     * dos funciones es la conversión entre ellos.
     */
    private static float[] convertir(ColorSpace de, ColorSpace a, float[] color) {
        if (de == a) {
            return color;
        }
        return a.fromCIEXYZ(de.toCIEXYZ(color));
    }

    /** La cadena de espacios que hay que atravesar, de origen a destino. */
    private ColorSpace[] cadena(ColorSpace desde, ColorSpace hasta) {
        if (this.spaces.length <= 1) {
            ColorSpace[] c = new ColorSpace[2];
            c[0] = desde;
            c[1] = this.spaces.length == 1 ? this.spaces[0] : hasta;
            return c;
        }
        return this.spaces;
    }

    /**
     * Convierte los colores de una imagen.
     *
     * @param dest el destino, o `null` para que se cree
     * @throws IllegalArgumentException si los tamaños no coinciden, o si no se puede determinar el
     *     espacio de destino
     */
    public final BufferedImage filter(BufferedImage src, BufferedImage dest) {
        BufferedImage destino = dest;
        if (destino == null) {
            destino = this.createCompatibleDestImage(src, null);
        } else if (src.getWidth() != destino.getWidth()
                || src.getHeight() != destino.getHeight()) {
            throw new IllegalArgumentException("Width or height of BufferedImages do not match");
        }
        ColorSpace[] cadena = this.cadena(src.getColorModel().getColorSpace(),
                destino.getColorModel().getColorSpace());
        ColorModel srcCM = src.getColorModel();
        ColorModel dstCM = destino.getColorModel();
        int w = src.getWidth();
        int h = src.getHeight();
        int srcColores = srcCM.getNumColorComponents();
        int dstColores = dstCM.getNumColorComponents();
        float[] norm = new float[srcCM.getNumComponents()];
        float[] color = new float[srcColores];
        float[] salida = new float[dstCM.getNumComponents()];
        Object crudo = null;
        Object destinoCrudo = null;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                crudo = src.getRaster().getDataElements(x, y, crudo);
                norm = srcCM.getNormalizedComponents(crudo, norm, 0);
                for (int i = 0; i < srcColores; i++) {
                    float min = cadena[0].getMinValue(i);
                    float max = cadena[0].getMaxValue(i);
                    color[i] = min + norm[i] * (max - min);
                }
                float[] convertido = color;
                for (int i = 1; i < cadena.length; i++) {
                    convertido = convertir(cadena[i - 1], cadena[i], convertido);
                }
                ColorSpace ultimo = cadena[cadena.length - 1];
                for (int i = 0; i < dstColores; i++) {
                    float min = ultimo.getMinValue(i);
                    float max = ultimo.getMaxValue(i);
                    float v = (convertido[i] - min) / (max - min);
                    if (v < 0.0f) {
                        v = 0.0f;
                    }
                    if (v > 1.0f) {
                        v = 1.0f;
                    }
                    salida[i] = v;
                }
                // El alfa no se convierte: es opacidad, no color, y no vive en ningun espacio.
                if (dstCM.hasAlpha()) {
                    salida[dstColores] = srcCM.hasAlpha() ? norm[srcColores] : 1.0f;
                }
                destinoCrudo = dstCM.getDataElements(salida, 0, destinoCrudo);
                destino.getRaster().setDataElements(x, y, destinoCrudo);
            }
        }
        return destino;
    }

    /**
     * Convierte los colores de un ráster.
     *
     * @param dest el destino, o `null` para que se cree
     * @throws IllegalArgumentException si no se declararon exactamente dos espacios, si su cantidad
     *     de componentes no coincide con las bandas, o si los tamaños no coinciden
     */
    public final WritableRaster filter(Raster src, WritableRaster dest) {
        if (this.spaces.length != 2) {
            throw new IllegalArgumentException(
                    "Destination ColorSpace is undefined");
        }
        ColorSpace de = this.spaces[0];
        ColorSpace a = this.spaces[1];
        if (src.getNumBands() != de.getNumComponents()) {
            throw new IllegalArgumentException(
                    "Numbers of source Raster bands and source color space components do not "
                    + "match");
        }
        WritableRaster destino = dest;
        if (destino == null) {
            destino = this.createCompatibleDestRaster(src);
        } else {
            if (src.getWidth() != destino.getWidth() || src.getHeight() != destino.getHeight()) {
                throw new IllegalArgumentException("Width or height of Rasters do not match");
            }
            if (destino.getNumBands() != a.getNumComponents()) {
                throw new IllegalArgumentException("Numbers of destination Raster bands and "
                        + "destination color space components do not match");
            }
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int sb = src.getNumBands();
        int db = destino.getNumBands();
        int[] entrada = new int[sb];
        int[] salida = new int[db];
        float[] color = new float[sb];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                entrada = src.getPixel(src.getMinX() + x, src.getMinY() + y, entrada);
                for (int i = 0; i < sb; i++) {
                    int max = (1 << src.getSampleModel().getSampleSize(i)) - 1;
                    float min = de.getMinValue(i);
                    float top = de.getMaxValue(i);
                    color[i] = min + (((float) entrada[i]) / max) * (top - min);
                }
                float[] convertido = convertir(de, a, color);
                for (int i = 0; i < db; i++) {
                    int max = (1 << destino.getSampleModel().getSampleSize(i)) - 1;
                    float min = a.getMinValue(i);
                    float top = a.getMaxValue(i);
                    float v = (convertido[i] - min) / (top - min);
                    if (v < 0.0f) {
                        v = 0.0f;
                    }
                    if (v > 1.0f) {
                        v = 1.0f;
                    }
                    salida[i] = (int) (v * max + 0.5f);
                }
                destino.setPixel(destino.getMinX() + x, destino.getMinY() + y, salida);
            }
        }
        return destino;
    }

    /**
     * Una imagen vacía en el espacio de destino.
     *
     * @throws IllegalArgumentException si no se puede determinar el espacio de destino
     */
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        ColorModel cm = destCM;
        if (cm == null) {
            ColorSpace destino;
            if (this.spaces.length == 0) {
                throw new IllegalArgumentException("Destination ColorSpace is undefined");
            }
            destino = this.spaces[this.spaces.length - 1];
            boolean alfa = src.getColorModel().hasAlpha();
            int n = destino.getNumComponents() + (alfa ? 1 : 0);
            int[] bits = new int[n];
            for (int i = 0; i < n; i++) {
                bits[i] = 8;
            }
            cm = new ComponentColorModel(destino, bits, alfa, src.isAlphaPremultiplied(),
                    alfa ? java.awt.Transparency.TRANSLUCENT : java.awt.Transparency.OPAQUE,
                    DataBuffer.TYPE_BYTE);
        }
        WritableRaster wr = cm.createCompatibleWritableRaster(src.getWidth(), src.getHeight());
        return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }

    /**
     * Un ráster vacío con tantas bandas como componentes tenga el espacio de destino.
     *
     * @throws IllegalArgumentException si no se declararon exactamente dos espacios
     */
    public WritableRaster createCompatibleDestRaster(Raster src) {
        if (this.spaces.length != 2) {
            throw new IllegalArgumentException("Destination ColorSpace is undefined");
        }
        int n = this.spaces[1].getNumComponents();
        return Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, src.getWidth(),
                src.getHeight(), n, new java.awt.Point(src.getMinX(), src.getMinY()));
    }

    /** El mismo rectángulo: esta operación no mueve nada de lugar. */
    public final Rectangle2D getBounds2D(BufferedImage src) {
        return this.getBounds2D(src.getRaster());
    }

    /** El mismo rectángulo. */
    public final Rectangle2D getBounds2D(Raster src) {
        return src.getBounds();
    }

    /** El mismo punto. */
    public final Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        Point2D out = dstPt;
        if (out == null) {
            out = new java.awt.geom.Point2D.Float();
        }
        out.setLocation(srcPt.getX(), srcPt.getY());
        return out;
    }

    /** Las pistas de dibujo, o `null` si no hay. */
    public final RenderingHints getRenderingHints() {
        return this.hints;
    }
}
