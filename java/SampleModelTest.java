import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Arrays;

/**
 * `java.awt.image.SampleModel` y sus cinco subclases: donde vive la aritmetica de pixeles.
 *
 * <p>El mismo archivo da -1 con el JDK 25 corriendo **sus** clases.
 *
 * <p>Se comprueba lo que un error de indice no cambiaria de forma visible en una sola lectura pero
 * si en una imagen: que la formula `y*scanlineStride + x*pixelStride + bandOffset` de con el
 * elemento correcto en las cinco disposiciones, que el empaquetado de bits cuente el primer pixel
 * **desde la izquierda**, que un pixel que se pasa de su campo se recorte en vez de pisar al de al
 * lado, y que un submodelo apunte a los mismos datos y no a una copia.
 */
public class SampleModelTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- intercalado: RGBRGBRGB en un solo banco
        //
        // Una imagen de 2x2 con tres bandas: doce numeros, tres por pixel.
        byte[] crudo = new byte[] {
            1, 2, 3, 4, 5, 6,
            7, 8, 9, 10, 11, 12 };
        PixelInterleavedSampleModel inter = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, 2, 2, 3, 6, new int[] { 0, 1, 2 });
        DataBufferByte db = new DataBufferByte(crudo, 12);
        ok("el pixel (0,0) son las tres primeras",
                Arrays.equals(inter.getPixel(0, 0, (int[]) null, db), new int[] { 1, 2, 3 }));
        ok("el (1,0) las tres siguientes",
                Arrays.equals(inter.getPixel(1, 0, (int[]) null, db), new int[] { 4, 5, 6 }));
        ok("y el (1,1) las ultimas",
                Arrays.equals(inter.getPixel(1, 1, (int[]) null, db), new int[] { 10, 11, 12 }));
        ok("la banda 1 del (0,1) es el octavo", inter.getSample(0, 1, 1, db) == 8);
        ok("el desplazamiento del (1,1) banda 2 es 11", inter.getOffset(1, 1, 2) == 11);
        ok("un elemento por banda", inter.getNumDataElements() == 3);
        ok("y ocho bits cada una", inter.getSampleSize(0) == 8);

        // Escribir por el modelo cambia el arreglo de verdad.
        inter.setSample(0, 0, 0, 99, db);
        ok("escribir una banda toca el elemento que corresponde", crudo[0] == 99);
        ok("y no los de al lado", crudo[1] == 2);

        // Las bandas dadas vuelta describen BGR sobre los MISMOS datos.
        PixelInterleavedSampleModel bgr = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, 2, 2, 3, 6, new int[] { 2, 1, 0 });
        ok("con las bandas al reves, el pixel sale invertido",
                Arrays.equals(bgr.getPixel(1, 0, (int[]) null, db), new int[] { 6, 5, 4 }));

        // ---- relleno de fila: el paso de fila mayor que el ancho
        byte[] conRelleno = new byte[] { 1, 2, 0, 0, 3, 4, 0, 0 };
        ComponentSampleModel relleno = new ComponentSampleModel(
                DataBuffer.TYPE_BYTE, 2, 2, 1, 4, new int[] { 0 });
        DataBufferByte dbRelleno = new DataBufferByte(conRelleno, 8);
        ok("la segunda fila arranca despues del relleno",
                relleno.getSample(0, 1, 0, dbRelleno) == 3);
        ok("y el relleno no se lee nunca", relleno.getSample(1, 0, 0, dbRelleno) == 2);

        // ---- por planos: una banda por banco
        byte[][] planos = new byte[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, { 9, 10, 11, 12 } };
        BandedSampleModel banded = new BandedSampleModel(DataBuffer.TYPE_BYTE, 2, 2, 3);
        DataBufferByte dbPlanos = new DataBufferByte(planos, 4);
        ok("el pixel (0,0) toma uno de cada banco",
                Arrays.equals(banded.getPixel(0, 0, (int[]) null, dbPlanos),
                        new int[] { 1, 5, 9 }));
        ok("y el (1,1) tambien",
                Arrays.equals(banded.getPixel(1, 1, (int[]) null, dbPlanos),
                        new int[] { 4, 8, 12 }));
        ok("el paso de pixel de un por-planos es 1", banded.getPixelStride() == 1);
        ok("y cada banda esta en su banco",
                Arrays.equals(banded.getBankIndices(), new int[] { 0, 1, 2 }));

        // ---- submodelo: los mismos datos, menos bandas
        SampleModel soloVerde = inter.createSubsetSampleModel(new int[] { 1 });
        ok("el submodelo tiene una banda", soloVerde.getNumBands() == 1);
        ok("y lee la que se pidio sobre los mismos datos",
                soloVerde.getSample(1, 0, 0, db) == 5);
        soloVerde.setSample(1, 0, 0, 77, db);
        ok("y escribe en los mismos datos", crudo[4] == 77);

        boolean bandaMala = false;
        try {
            inter.createSubsetSampleModel(new int[] { 5 });
        } catch (RuntimeException e) {
            bandaMala = true;
        }
        ok("pedir una banda que no existe tira", bandaMala);

        // ---- empaquetado de un pixel por elemento: el ARGB de siempre
        int[] pixeles = new int[] { 0xFF804020, 0x00010203 };
        SinglePixelPackedSampleModel argb = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_INT, 2, 1,
                new int[] { 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000 });
        DataBufferInt dbArgb = new DataBufferInt(pixeles, 2);
        ok("el rojo sale del segundo byte", argb.getSample(0, 0, 0, dbArgb) == 0x80);
        ok("el verde del tercero", argb.getSample(0, 0, 1, dbArgb) == 0x40);
        ok("el azul del cuarto", argb.getSample(0, 0, 2, dbArgb) == 0x20);
        ok("y el alfa del primero", argb.getSample(0, 0, 3, dbArgb) == 0xFF);
        ok("el pixel entero se desarma de una",
                Arrays.equals(argb.getPixel(0, 0, (int[]) null, dbArgb),
                        new int[] { 0x80, 0x40, 0x20, 0xFF }));
        ok("cada banda usa ocho bits", argb.getSampleSize(0) == 8);
        ok("y un solo elemento por pixel", argb.getNumDataElements() == 1);
        ok("los desplazamientos de bit salen de las mascaras",
                Arrays.equals(argb.getBitOffsets(), new int[] { 16, 8, 0, 24 }));

        // Un valor que se pasa de su campo se RECORTA, no pisa al vecino.
        argb.setSample(1, 0, 2, 0x1FF, dbArgb);
        ok("un valor que se pasa se recorta", argb.getSample(1, 0, 2, dbArgb) == 0xFF);
        ok("y no toca la banda de al lado", argb.getSample(1, 0, 1, dbArgb) == 0x02);

        // Un 5-6-5 de 16 bits: bandas de distinto ancho.
        SinglePixelPackedSampleModel rgb565 = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_USHORT, 1, 1, new int[] { 0xF800, 0x07E0, 0x001F });
        ok("el rojo del 565 usa cinco bits", rgb565.getSampleSize(0) == 5);
        ok("el verde seis", rgb565.getSampleSize(1) == 6);
        ok("y el azul cinco", rgb565.getSampleSize(2) == 5);

        // ---- empaquetado de varios pixeles por elemento
        //
        // Cuatro pixeles de dos bits en un byte. El pixel 0 va en los bits MAS ALTOS.
        byte[] bits = new byte[] { (byte) 0xE4 };
        MultiPixelPackedSampleModel dos = new MultiPixelPackedSampleModel(
                DataBuffer.TYPE_BYTE, 4, 1, 2);
        DataBufferByte dbBits = new DataBufferByte(bits, 1);
        ok("el pixel 0 son los dos bits mas altos", dos.getSample(0, 0, 0, dbBits) == 3);
        ok("el 1 los siguientes", dos.getSample(1, 0, 0, dbBits) == 2);
        ok("el 2 los siguientes", dos.getSample(2, 0, 0, dbBits) == 1);
        ok("y el 3 los mas bajos", dos.getSample(3, 0, 0, dbBits) == 0);
        ok("una sola banda", dos.getNumBands() == 1);
        ok("de dos bits", dos.getPixelBitStride() == 2);
        ok("los cuatro caen en el mismo elemento",
                dos.getOffset(0, 0) == 0 && dos.getOffset(3, 0) == 0);
        ok("y sus corrimientos van de a dos",
                dos.getBitOffset(0) == 0 && dos.getBitOffset(1) == 2
                        && dos.getBitOffset(3) == 6);

        dos.setSample(1, 0, 0, 0, dbBits);
        ok("escribir un pixel no toca a los otros tres",
                dos.getSample(0, 0, 0, dbBits) == 3 && dos.getSample(2, 0, 0, dbBits) == 1
                        && dos.getSample(3, 0, 0, dbBits) == 0);
        ok("y el que se escribio quedo", dos.getSample(1, 0, 0, dbBits) == 0);

        // Un bit por pixel: ocho en un byte.
        MultiPixelPackedSampleModel uno = new MultiPixelPackedSampleModel(
                DataBuffer.TYPE_BYTE, 8, 1, 1);
        DataBufferByte dbUno = new DataBufferByte(new byte[] { (byte) 0x81 }, 1);
        ok("con un bit por pixel, el 0 y el 7 estan prendidos",
                uno.getSample(0, 0, 0, dbUno) == 1 && uno.getSample(7, 0, 0, dbUno) == 1);
        ok("y los del medio apagados", uno.getSample(3, 0, 0, dbUno) == 0);

        // El tipo de transferencia es el mas chico que contenga un pixel, no el del buffer.
        ok("un pixel de un bit se transfiere en byte",
                uno.getTransferType() == DataBuffer.TYPE_BYTE);
        MultiPixelPackedSampleModel doce = new MultiPixelPackedSampleModel(
                DataBuffer.TYPE_INT, 2, 1, 16);
        ok("uno de dieciseis bits, en ushort",
                doce.getTransferType() == DataBuffer.TYPE_USHORT);

        // Un ancho de pixel que no divide al elemento no se admite.
        boolean noDivide = false;
        try {
            new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE, 4, 1, 3);
        } catch (RuntimeException e) {
            noDivide = true;
        }
        ok("tres bits no dividen a ocho, y no se admite", noDivide);

        // ---- createDataBuffer da un buffer que alcanza
        SampleModel modelo = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, 3, 4, 3, 9, new int[] { 0, 1, 2 });
        DataBuffer reservado = modelo.createDataBuffer();
        ok("el buffer es del tipo del modelo",
                reservado.getDataType() == DataBuffer.TYPE_BYTE);
        ok("y alcanza para la ultima banda del ultimo pixel",
                reservado.getSize() >= modelo.getNumBands() * 3 * 4);
        // Y de verdad se puede escribir el ultimo pixel sin salirse.
        modelo.setSample(2, 3, 2, 7, reservado);
        ok("se puede escribir el ultimo pixel", modelo.getSample(2, 3, 2, reservado) == 7);

        // ---- elementos de datos: copiar sin desempaquetar
        SinglePixelPackedSampleModel origen = new SinglePixelPackedSampleModel(
                DataBuffer.TYPE_INT, 2, 2, new int[] { 0x00FF0000, 0x0000FF00, 0x000000FF });
        DataBufferInt dbOrigen = (DataBufferInt) origen.createDataBuffer();
        origen.setPixel(0, 0, new int[] { 10, 20, 30 }, dbOrigen);
        Object crudoPixel = origen.getDataElements(0, 0, null, dbOrigen);
        ok("el elemento crudo de un empaquetado es un int", crudoPixel instanceof int[]);
        DataBufferInt dbDestino = (DataBufferInt) origen.createDataBuffer();
        origen.setDataElements(1, 1, crudoPixel, dbDestino);
        ok("y copiarlo reproduce el pixel entero",
                Arrays.equals(origen.getPixel(1, 1, (int[]) null, dbDestino),
                        new int[] { 10, 20, 30 }));

        // ---- igualdad
        PixelInterleavedSampleModel a = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, 2, 2, 3, 6, new int[] { 0, 1, 2 });
        PixelInterleavedSampleModel b = new PixelInterleavedSampleModel(
                DataBuffer.TYPE_BYTE, 2, 2, 3, 6, new int[] { 0, 1, 2 });
        ok("dos modelos iguales lo son", a.equals(b));
        ok("y comparten hashCode", a.hashCode() == b.hashCode());
        ok("uno con otro paso de fila no es igual",
                !a.equals(new PixelInterleavedSampleModel(
                        DataBuffer.TYPE_BYTE, 2, 2, 3, 9, new int[] { 0, 1, 2 })));

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("SampleModelTest " + SampleModelTest.run());
    }
}
