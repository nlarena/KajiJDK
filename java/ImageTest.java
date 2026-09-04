import java.awt.image.AffineTransformOp;
import java.awt.image.BandCombineOp;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.CropImageFilter;
import java.awt.image.DataBuffer;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.IndexColorModel;
import java.awt.image.Kernel;
import java.awt.image.LookupOp;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.awt.geom.AffineTransform;
import java.util.Arrays;

/**
 * `java.awt.image` de punta a punta: la imagen, la paleta, las operaciones y la tubería.
 *
 * <p>El mismo archivo da -1 con el JDK 25 corriendo **sus** clases.
 *
 * <p>Lo que se comprueba es lo que un error no cambiaría en una lectura suelta pero sí en una
 * imagen: que un recorte comparta los píxeles con el original en vez de copiarlos, que
 * premultiplicar y deshacerlo sea la misma imagen salvo donde el alfa era cero, que una paleta elija
 * el color más parecido y no el primero, que una convolución no toque el borde cuando se le pide que
 * no lo toque, y que la tubería de productor y consumidor entregue exactamente el rectángulo que se
 * recortó.
 */
public class ImageTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- los formatos con nombre
        BufferedImage rgb = new BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB);
        ok("el tipo se recuerda", rgb.getType() == BufferedImage.TYPE_INT_RGB);
        ok("y el tamaño", rgb.getWidth() == 4 && rgb.getHeight() == 3);
        ok("un RGB sin alfa es opaco", rgb.getColorModel().getTransparency() == 1);
        ok("y arranca en negro opaco", rgb.getRGB(0, 0) == 0xFF000000);

        rgb.setRGB(1, 2, 0xFF804020);
        ok("escribir y leer da lo mismo", rgb.getRGB(1, 2) == 0xFF804020);
        ok("y no toca al vecino", rgb.getRGB(2, 2) == 0xFF000000);

        // Sin canal alfa, el alfa que se escriba se descarta y sale opaco.
        rgb.setRGB(0, 0, 0x00112233);
        ok("sin alfa, el color sale opaco", rgb.getRGB(0, 0) == 0xFF112233);

        BufferedImage argb = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
        argb.setRGB(0, 0, 0x80FF0000);
        ok("con alfa, el alfa se guarda", argb.getRGB(0, 0) == 0x80FF0000);
        ok("y arranca transparente", argb.getRGB(3, 2) == 0x00000000);

        BufferedImage bgr = new BufferedImage(2, 2, BufferedImage.TYPE_3BYTE_BGR);
        bgr.setRGB(0, 0, 0xFF102030);
        ok("un 3BYTE_BGR guarda el mismo color", bgr.getRGB(0, 0) == 0xFF102030);
        ok("con tres bandas", bgr.getRaster().getNumBands() == 3);
        ok("y el rojo en la banda 0", bgr.getRaster().getSample(0, 0, 0) == 0x10);

        BufferedImage gris = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);
        ok("un gris tiene una banda", gris.getRaster().getNumBands() == 1);
        gris.getRaster().setSample(0, 0, 0, 255);
        ok("y su máximo es blanco", (gris.getRGB(0, 0) & 0xFFFFFF) == 0xFFFFFF);

        BufferedImage bin = new BufferedImage(8, 2, BufferedImage.TYPE_BYTE_BINARY);
        ok("un binario usa un bit por píxel", bin.getColorModel().getPixelSize() == 1);
        ok("y su paleta tiene dos entradas",
                ((IndexColorModel) bin.getColorModel()).getMapSize() == 2);
        bin.setRGB(3, 0, 0xFFFFFFFF);
        ok("el píxel escrito es blanco", bin.getRGB(3, 0) == 0xFFFFFFFF);
        ok("y el de al lado negro", bin.getRGB(4, 0) == 0xFF000000);

        // El tipo se deduce cuando se arma desde un modelo y un ráster.
        ColorModel cm = argb.getColorModel();
        WritableRaster wr = cm.createCompatibleWritableRaster(2, 2);
        BufferedImage deducida = new BufferedImage(cm, wr, false, null);
        ok("el tipo se deduce del modelo y la disposición",
                deducida.getType() == BufferedImage.TYPE_INT_ARGB);

        // ---- un recorte comparte los píxeles
        BufferedImage grande = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
        grande.setRGB(3, 3, 0xFF00FF00);
        BufferedImage sub = grande.getSubimage(2, 2, 3, 3);
        ok("el recorte mide lo pedido", sub.getWidth() == 3 && sub.getHeight() == 3);
        ok("y sus coordenadas arrancan en cero", sub.getRGB(1, 1) == 0xFF00FF00);
        sub.setRGB(0, 0, 0xFF0000FF);
        ok("escribir en el recorte cambia el original", grande.getRGB(2, 2) == 0xFF0000FF);

        // ---- premultiplicar y deshacerlo
        BufferedImage pre = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        pre.setRGB(0, 0, 0x80FFFFFF);
        pre.setRGB(1, 0, 0x00FF0000);
        ok("no arranca premultiplicada", !pre.isAlphaPremultiplied());
        pre.coerceData(true);
        ok("premultiplicar lo dice el modelo", pre.isAlphaPremultiplied());
        ok("y el color guardado bajó a la mitad",
                pre.getRaster().getSample(0, 0, 0) == 128);
        pre.coerceData(false);
        ok("deshacerlo recupera el color", (pre.getRGB(0, 0) & 0xFFFFFF) == 0xFFFFFF);
        ok("y el alfa quedó", (pre.getRGB(0, 0) >>> 24) == 0x80);
        // El píxel de alfa cero perdió su color, y eso es propio de la representación.
        ok("un píxel invisible pierde el color al premultiplicar", pre.getRGB(1, 0) == 0);

        // ---- paleta
        byte[] r = { (byte) 0, (byte) 255, (byte) 0 };
        byte[] g = { (byte) 0, (byte) 0, (byte) 255 };
        byte[] b = { (byte) 0, (byte) 0, (byte) 0 };
        IndexColorModel icm = new IndexColorModel(2, 3, r, g, b);
        ok("la paleta tiene tres entradas", icm.getMapSize() == 3);
        ok("sin alfa es opaca", icm.getTransparency() == 1);
        ok("la entrada 1 es roja", icm.getRGB(1) == 0xFFFF0000);
        ok("y su rojo es 255", icm.getRed(1) == 255);
        ok("todas sus entradas son válidas", icm.isValid());

        // Un color que no está en la paleta va al más parecido, no al primero.
        byte[] elegido = (byte[]) icm.getDataElements(0xFFEE0000, null);
        ok("un rojo oscuro elige la entrada roja", (elegido[0] & 0xFF) == 1);
        byte[] verde = (byte[]) icm.getDataElements(0xFF00EE00, null);
        ok("y un verde oscuro la verde", (verde[0] & 0xFF) == 2);

        // Con una entrada transparente, la paleta pasa a ser de máscara.
        IndexColorModel conTrans = new IndexColorModel(2, 3, r, g, b, 0);
        ok("marcar una entrada transparente la vuelve de máscara",
                conTrans.getTransparency() == 2);
        ok("y esa entrada tiene alfa cero", conTrans.getAlpha(0) == 0);
        ok("la entrada transparente se recuerda", conTrans.getTransparentPixel() == 0);

        // Deshacer la indirección da la misma imagen sin paleta.
        BufferedImage indexada = new BufferedImage(2, 1, BufferedImage.TYPE_BYTE_INDEXED, icm);
        indexada.getRaster().setSample(0, 0, 0, 1);
        indexada.getRaster().setSample(1, 0, 0, 2);
        BufferedImage discreta = icm.convertToIntDiscrete(indexada.getRaster(), false);
        ok("convertir a discreta conserva los colores",
                (discreta.getRGB(0, 0) & 0xFFFFFF) == 0xFF0000
                        && (discreta.getRGB(1, 0) & 0xFFFFFF) == 0x00FF00);

        // ---- escalar y correr
        BufferedImage src = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        src.setRGB(0, 0, 0xFF102030);
        src.setRGB(1, 0, 0xFF000000);
        RescaleOp doble = new RescaleOp(2.0f, 0.0f, null);
        BufferedImage escalada = doble.filter(src, null);
        ok("duplicar el brillo duplica cada componente",
                escalada.getRGB(0, 0) == 0xFF204060);
        ok("y el negro sigue negro", escalada.getRGB(1, 0) == 0xFF000000);
        ok("hay una sola constante", doble.getNumFactors() == 1);

        // Lo que se pasa del techo se recorta y no vuelve.
        BufferedImage claro = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        claro.setRGB(0, 0, 0xFF808080);
        BufferedImage saturada = new RescaleOp(4.0f, 0.0f, null).filter(claro, null);
        ok("lo que se pasa del techo se recorta", saturada.getRGB(0, 0) == 0xFFFFFFFF);

        // ---- tabla de búsqueda
        byte[] invertir = new byte[256];
        for (int i = 0; i < 256; i++) {
            invertir[i] = (byte) (255 - i);
        }
        LookupOp negativo = new LookupOp(new ByteLookupTable(0, invertir), null);
        BufferedImage invertida = negativo.filter(src, null);
        ok("la tabla invierte cada componente", invertida.getRGB(0, 0) == 0xFFEFDFCF);
        ok("y el negro se vuelve blanco", invertida.getRGB(1, 0) == 0xFFFFFFFF);
        ok("la tabla se puede recuperar", negativo.getTable().getNumComponents() == 1);

        // ---- convolución
        float[] identidad = { 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f };
        Kernel k = new Kernel(3, 3, identidad);
        ok("el núcleo mide 3x3", k.getWidth() == 3 && k.getHeight() == 3);
        ok("y su origen está en el centro", k.getXOrigin() == 1 && k.getYOrigin() == 1);

        BufferedImage campo = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                campo.setRGB(x, y, 0xFF000000 | (0x0A0A0A * (x + y)));
            }
        }
        ConvolveOp copia = new ConvolveOp(k, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage convolucionada = copia.filter(campo, null);
        ok("el núcleo identidad no cambia el centro",
                convolucionada.getRGB(2, 2) == campo.getRGB(2, 2));
        ok("y con EDGE_NO_OP el borde queda igual",
                convolucionada.getRGB(0, 0) == campo.getRGB(0, 0));
        ok("la condición de borde se recuerda",
                copia.getEdgeCondition() == ConvolveOp.EDGE_NO_OP);

        ConvolveOp ceros = new ConvolveOp(k, ConvolveOp.EDGE_ZERO_FILL, null);
        BufferedImage conMarco = ceros.filter(campo, null);
        ok("con EDGE_ZERO_FILL el borde se apaga",
                (conMarco.getRGB(0, 0) & 0xFFFFFF) == 0);
        ok("y el centro no", conMarco.getRGB(2, 2) == campo.getRGB(2, 2));

        // ---- transformación afín
        BufferedImage chica = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        chica.setRGB(0, 0, 0xFFFF0000);
        chica.setRGB(1, 0, 0xFF00FF00);
        chica.setRGB(0, 1, 0xFF0000FF);
        chica.setRGB(1, 1, 0xFFFFFFFF);
        AffineTransformOp doblar = new AffineTransformOp(
                AffineTransform.getScaleInstance(2, 2), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        BufferedImage agrandada = doblar.filter(chica, null);
        ok("duplicar da el doble de ancho y de alto",
                agrandada.getWidth() == 4 && agrandada.getHeight() == 4);
        ok("y cada píxel se repite en un bloque de 2x2",
                agrandada.getRGB(0, 0) == 0xFFFF0000 && agrandada.getRGB(1, 1) == 0xFFFF0000
                        && agrandada.getRGB(2, 0) == 0xFF00FF00
                        && agrandada.getRGB(3, 3) == 0xFFFFFFFF);
        ok("el tipo de interpolación se recuerda",
                doblar.getInterpolationType() == AffineTransformOp.TYPE_NEAREST_NEIGHBOR);

        // getPoint2D sí mueve el punto en esta operación, a diferencia de las otras.
        java.awt.geom.Point2D p = doblar.getPoint2D(
                new java.awt.geom.Point2D.Double(3, 4), null);
        ok("el punto se transforma", p.getX() == 6.0 && p.getY() == 8.0);

        // ---- combinar bandas
        float[][] alReves = { { 0f, 0f, 1f }, { 0f, 1f, 0f }, { 1f, 0f, 0f } };
        BandCombineOp swap = new BandCombineOp(alReves, null);
        WritableRaster fuente = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 3, null);
        fuente.setPixel(0, 0, new int[] { 10, 20, 30 });
        WritableRaster combinada = swap.filter(fuente, null);
        ok("la matriz da vuelta las bandas",
                Arrays.equals(combinada.getPixel(0, 0, (int[]) null), new int[] { 30, 20, 10 }));
        ok("la matriz se puede recuperar", swap.getMatrix().length == 3);

        // Una columna de más es un término constante.
        float[][] conBrillo = { { 1f, 0f, 0f, 5f }, { 0f, 1f, 0f, 5f }, { 0f, 0f, 1f, 5f } };
        WritableRaster masCinco = new BandCombineOp(conBrillo, null).filter(fuente, null);
        ok("la columna de más suma una constante",
                Arrays.equals(masCinco.getPixel(0, 0, (int[]) null), new int[] { 15, 25, 35 }));

        // ---- la tubería de productor y consumidor
        int[] pixeles = new int[16];
        for (int i = 0; i < 16; i++) {
            pixeles[i] = 0xFF000000 | (i * 0x0F0F0F);
        }
        MemoryImageSource fuenteMem = new MemoryImageSource(4, 4, pixeles, 0, 4);
        PixelGrabber entero = new PixelGrabber(fuenteMem, 0, 0, -1, -1, null, 0, 0);
        ok("juntar la imagen entera sale bien", entero.grabPixels());
        ok("y está completa", (entero.getStatus() & 32) != 0);
        ok("con el tamaño anunciado", entero.getWidth() == 4 && entero.getHeight() == 4);
        ok("y los mismos píxeles",
                Arrays.equals((int[]) entero.getPixels(), pixeles));

        // Un recorte en la tubería entrega exactamente el rectángulo pedido.
        ImageProducer recortada = new FilteredImageSource(fuenteMem,
                new CropImageFilter(1, 1, 2, 2));
        PixelGrabber pedazo = new PixelGrabber(recortada, 0, 0, -1, -1, null, 0, 0);
        ok("el recorte sale bien", pedazo.grabPixels());
        ok("y mide lo pedido", pedazo.getWidth() == 2 && pedazo.getHeight() == 2);
        int[] esperado = { pixeles[5], pixeles[6], pixeles[9], pixeles[10] };
        ok("con los píxeles del rectángulo recortado",
                Arrays.equals((int[]) pedazo.getPixels(), esperado));

        // Una imagen en memoria también se puede recorrer como productor.
        PixelGrabber deImagen = new PixelGrabber(argb.getSource(), 0, 0, -1, -1, null, 0, 0);
        ok("una BufferedImage se puede producir", deImagen.grabPixels());
        ok("y entrega su propio primer píxel",
                ((int[]) deImagen.getPixels())[0] == 0x80FF0000);

        // ---- una imagen es un solo mosaico
        ok("un solo mosaico a lo ancho", argb.getNumXTiles() == 1);
        ok("y a lo alto", argb.getNumYTiles() == 1);
        ok("del tamaño de la imagen", argb.getTileWidth() == 4 && argb.getTileHeight() == 3);
        ok("el mosaico (0,0) es el ráster", argb.getTile(0, 0) == argb.getRaster());
        ok("y siempre está escribible", argb.isTileWritable(0, 0));

        // getData copia; getRaster no.
        Raster copiado = argb.getData();
        ok("getData devuelve una copia", copiado != argb.getRaster());
        argb.setRGB(0, 0, 0xFF123456);
        ok("y la copia no se entera", copiado.getSample(0, 0, 0) != 0x12);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("ImageTest " + ImageTest.run());
    }
}
