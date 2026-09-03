import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.util.Arrays;

/**
 * `java.awt.image.DataBuffer` y sus seis subclases.
 *
 * <p>El mismo archivo da -1 con el JDK 25 corriendo **sus** clases.
 *
 * <p>Lo que se comprueba es lo que distingue a las seis entre si, que es donde una copia-y-pega se
 * equivoca: que `byte` y `ushort` se lean **sin signo** y `short` con signo, que los de coma
 * flotante **truncen** en `getElem`, que los constructores que reciben un arreglo **no lo copien**,
 * y que los desplazamientos por banco se respeten.
 */
public class DataBufferTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- los tamanos de cada tipo
        ok("byte son 8 bits", DataBuffer.getDataTypeSize(DataBuffer.TYPE_BYTE) == 8);
        ok("ushort son 16", DataBuffer.getDataTypeSize(DataBuffer.TYPE_USHORT) == 16);
        ok("short son 16", DataBuffer.getDataTypeSize(DataBuffer.TYPE_SHORT) == 16);
        ok("int son 32", DataBuffer.getDataTypeSize(DataBuffer.TYPE_INT) == 32);
        ok("float son 32", DataBuffer.getDataTypeSize(DataBuffer.TYPE_FLOAT) == 32);
        ok("double son 64", DataBuffer.getDataTypeSize(DataBuffer.TYPE_DOUBLE) == 64);
        boolean tipoMalo = false;
        try {
            DataBuffer.getDataTypeSize(DataBuffer.TYPE_UNDEFINED);
        } catch (IllegalArgumentException e) {
            tipoMalo = true;
        }
        ok("un tipo desconocido es IllegalArgument", tipoMalo);

        // ---- byte: SIN signo
        //
        // Java no tiene byte sin signo, asi que sin enmascarar, 0xFF saldria -1 y la mitad clara de
        // una imagen quedaria en negativo. Es la comprobacion mas importante de este archivo.
        DataBufferByte db = new DataBufferByte(new byte[] { 0, 1, (byte) 0x80, (byte) 0xFF }, 4);
        ok("un byte 0x00 es 0", db.getElem(0) == 0);
        ok("un byte 0x80 es 128 y no -128", db.getElem(2) == 128);
        ok("un byte 0xFF es 255 y no -1", db.getElem(3) == 255);
        ok("el tipo es TYPE_BYTE", db.getDataType() == DataBuffer.TYPE_BYTE);
        ok("y el tamano el que se paso", db.getSize() == 4);

        // ---- ushort contra short: la misma memoria, dos lecturas
        short[] mismos = new short[] { 0, (short) 0x8000, (short) 0xFFFF };
        DataBufferUShort du = new DataBufferUShort(mismos, 3);
        DataBufferShort ds = new DataBufferShort(mismos, 3);
        ok("ushort lee 0x8000 como 32768", du.getElem(1) == 32768);
        ok("short lo lee como -32768", ds.getElem(1) == -32768);
        ok("ushort lee 0xFFFF como 65535", du.getElem(2) == 65535);
        ok("short lo lee como -1", ds.getElem(2) == -1);
        ok("y los dos tipos se declaran distinto",
                du.getDataType() == DataBuffer.TYPE_USHORT
                        && ds.getDataType() == DataBuffer.TYPE_SHORT);

        // ---- int
        DataBufferInt di = new DataBufferInt(new int[] { -1, 0, Integer.MAX_VALUE }, 3);
        ok("int conserva el signo", di.getElem(0) == -1);
        ok("y el maximo", di.getElem(2) == Integer.MAX_VALUE);

        // ---- coma flotante: getElem TRUNCA
        DataBufferDouble dd = new DataBufferDouble(new double[] { 2.9, -2.9, 0.5 }, 3);
        ok("getElem de un double trunca hacia cero", dd.getElem(0) == 2);
        ok("y tambien con negativos", dd.getElem(1) == -2);
        ok("pero getElemDouble no", dd.getElemDouble(0) == 2.9);
        ok("y getElemFloat convierte", Math.abs(dd.getElemFloat(2) - 0.5f) < 1e-6f);

        DataBufferFloat df = new DataBufferFloat(new float[] { 1.75f, -3.25f }, 2);
        ok("getElem de un float trunca", df.getElem(0) == 1);
        ok("getElemFloat da el valor", df.getElemFloat(0) == 1.75f);
        ok("y getElemDouble lo ensancha", df.getElemDouble(1) == -3.25);

        // Un buffer entero convierte al leer como flotante, sin perder nada.
        ok("getElemFloat de un int no pierde", di.getElemFloat(2) == (float) Integer.MAX_VALUE);
        ok("getElemDouble de un int tampoco",
                di.getElemDouble(2) == (double) Integer.MAX_VALUE);
        // Y al escribir un flotante en un buffer entero, trunca.
        DataBufferInt escritura = new DataBufferInt(3);
        escritura.setElemFloat(0, 7.9f);
        ok("setElemFloat sobre un int trunca", escritura.getElem(0) == 7);
        escritura.setElemDouble(1, -7.9);
        ok("y setElemDouble tambien", escritura.getElem(1) == -7);

        // ---- el arreglo NO se copia
        byte[] crudo = new byte[] { 1, 2, 3 };
        DataBufferByte sobreCrudo = new DataBufferByte(crudo, 3);
        crudo[0] = 42;
        ok("el buffer ve los cambios del arreglo que se le dio",
                sobreCrudo.getElem(0) == 42);
        sobreCrudo.setElem(1, 99);
        ok("y al reves: escribirle cambia el arreglo", crudo[1] == 99);
        ok("getData devuelve el mismo arreglo", sobreCrudo.getData() == crudo);

        // ---- desplazamiento
        DataBufferByte conOffset = new DataBufferByte(new byte[] { 9, 9, 7, 8 }, 2, 2);
        ok("el desplazamiento se declara", conOffset.getOffset() == 2);
        ok("y el elemento 0 es el que esta en el desplazamiento",
                conOffset.getElem(0) == 7);
        ok("y el 1 el siguiente", conOffset.getElem(1) == 8);

        // ---- varios bancos
        byte[][] bancos = new byte[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } };
        DataBufferByte multi = new DataBufferByte(bancos, 2);
        ok("tres bancos", multi.getNumBanks() == 3);
        ok("el banco 0 por omision", multi.getElem(0) == 1);
        ok("y cada banco se lee por su indice",
                multi.getElem(1, 0) == 3 && multi.getElem(2, 1) == 6);
        multi.setElem(2, 0, 55);
        ok("escribir en un banco no toca a los otros",
                bancos[2][0] == 55 && bancos[0][0] == 1);
        // `getBankData` clona el arreglo de AFUERA pero no los de adentro. Esta
        // comprobacion esperaba el mismo arreglo y el oraculo la corrigio.
        ok("getBankData clona el arreglo de afuera", multi.getBankData() != bancos);
        ok("pero comparte los bancos", multi.getBankData()[0] == bancos[0]);
        ok("getData(1) es el segundo banco", multi.getData(1) == bancos[1]);

        // Un desplazamiento por banco.
        byte[][] bancos2 = new byte[][] { { 0, 1, 2 }, { 0, 3, 4 } };
        DataBufferByte multiOff = new DataBufferByte(bancos2, 2, new int[] { 1, 1 });
        ok("con desplazamiento por banco, el 0 es el que corresponde",
                multiOff.getElem(0, 0) == 1 && multiOff.getElem(1, 0) == 3);
        ok("getOffsets los devuelve todos",
                Arrays.equals(multiOff.getOffsets(), new int[] { 1, 1 }));
        // Y es una copia: tocarla no mueve el buffer.
        int[] sacados = multiOff.getOffsets();
        sacados[0] = 99;
        ok("y es una copia", multiOff.getOffsets()[0] == 1);

        boolean pocosOffsets = false;
        try {
            new DataBufferByte(new byte[][] { { 1 }, { 2 } }, 1, new int[] { 0 });
        } catch (ArrayIndexOutOfBoundsException e) {
            pocosOffsets = true;
        }
        ok("menos desplazamientos que bancos es ArrayIndexOutOfBounds", pocosOffsets);

        // ---- constructores que reservan
        DataBufferInt reservado = new DataBufferInt(4, 2);
        ok("reserva dos bancos", reservado.getNumBanks() == 2);
        ok("de cuatro elementos", reservado.getSize() == 4);
        ok("y arrancan en cero", reservado.getElem(0) == 0 && reservado.getElem(1, 3) == 0);
        ok("y son arreglos distintos", reservado.getData(0) != reservado.getData(1));

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("DataBufferTest " + DataBufferTest.run());
    }
}
