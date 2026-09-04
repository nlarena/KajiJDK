// `MemorySegment` sobre arreglos de Java: la mitad de `java.lang.foreign` que **si** toca memoria.
//
// **Se comprueba contra `java` real corriendo lo mismo**, y aca eso vale doble: un segmento lee bytes
// crudos, asi que un error de un bit en la composicion de un `int` no rompe nada visible -- devuelve
// otro numero. Comparar contra el JDK es lo unico que lo detecta.
//
// Lo que mas se cuida son las tres cosas que hacen que un acceso a memoria sea seguro y que aca son
// excepciones en vez de comportamiento indefinido: los **limites**, el **alineamiento**, y el
// **ambito** cerrado. Un segmento que dejara pasar cualquiera de las tres seria peor que uno que no
// existe.
//
// **No** se comprueba `isNative()` de un segmento de arena: en el JDK da `true` (memoria del sistema)
// y aca `false` (arreglo de Java). Es la diferencia deliberada que documenta `Arena`, no un fallo.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public class SegTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static void basicos() {
        byte[] arr = new byte[16];
        MemorySegment s = MemorySegment.ofArray(arr);
        ok(s.byteSize() == 16L);
        ok(s.address() == 0L);
        ok(!s.isNative());
        ok(s.heapBase().isPresent());
        ok(!s.isReadOnly());
        ok(!s.isMapped());
        // Un `byte[]` solo garantiza alineamiento 1: la JVM no promete donde cae en memoria.
        ok(s.maxByteAlignment() == 1L);
        ok(s.scope().isAlive());

        MemorySegment sl = s.asSlice(4, 8);
        ok(sl.byteSize() == 8L);
        // La direccion de un segmento de heap es el offset dentro del arreglo.
        ok(sl.address() == 4L);

        // Un `long[]` si garantiza 8.
        MemorySegment largos = MemorySegment.ofArray(new long[2]);
        ok(largos.maxByteAlignment() == 8L);
        ok(largos.byteSize() == 16L);

        MemorySegment enteros = MemorySegment.ofArray(new int[4]);
        ok(enteros.maxByteAlignment() == 4L);
        ok(enteros.byteSize() == 16L);

        ok(MemorySegment.NULL.byteSize() == 0L);
        ok(MemorySegment.NULL.address() == 0L);
        ok(MemorySegment.ofAddress(1024L).address() == 1024L);
        ok(MemorySegment.ofAddress(1024L).byteSize() == 0L);
    }

    static void leerYEscribir() {
        byte[] arr = new byte[16];
        MemorySegment s = MemorySegment.ofArray(arr);

        // Sobre un `byte[]` hay que usar la variante sin alinear: `JAVA_INT` exige 4 y el arreglo
        // solo garantiza 1. Que falle es lo correcto, y se comprueba abajo.
        s.set(ValueLayout.JAVA_INT_UNALIGNED, 0, 0x01020304);
        ok(s.get(ValueLayout.JAVA_INT_UNALIGNED, 0) == 0x01020304);
        // Little-endian: el byte menos significativo va primero.
        ok(arr[0] == (byte) 4);
        ok(arr[1] == (byte) 3);
        ok(arr[2] == (byte) 2);
        ok(arr[3] == (byte) 1);

        // Big-endian invierte, y eso es todo lo que el orden cambia.
        s.set(ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN), 4, 0x01020304);
        ok(arr[4] == (byte) 1);
        ok(arr[7] == (byte) 4);
        ok(s.get(ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN), 4) == 0x01020304);

        // Los ocho tipos, cada uno en su propio segmento para que no se pisen.
        MemorySegment b = MemorySegment.ofArray(new byte[64]);
        b.set(ValueLayout.JAVA_BYTE, 0, (byte) -7);
        ok(b.get(ValueLayout.JAVA_BYTE, 0) == (byte) -7);
        b.set(ValueLayout.JAVA_BOOLEAN, 1, true);
        ok(b.get(ValueLayout.JAVA_BOOLEAN, 1));
        b.set(ValueLayout.JAVA_BOOLEAN, 2, false);
        ok(!b.get(ValueLayout.JAVA_BOOLEAN, 2));
        b.set(ValueLayout.JAVA_CHAR_UNALIGNED, 4, 'Z');
        ok(b.get(ValueLayout.JAVA_CHAR_UNALIGNED, 4) == 'Z');
        b.set(ValueLayout.JAVA_SHORT_UNALIGNED, 8, (short) -300);
        ok(b.get(ValueLayout.JAVA_SHORT_UNALIGNED, 8) == (short) -300);
        b.set(ValueLayout.JAVA_INT_UNALIGNED, 12, -123456);
        ok(b.get(ValueLayout.JAVA_INT_UNALIGNED, 12) == -123456);
        b.set(ValueLayout.JAVA_LONG_UNALIGNED, 16, -9876543210L);
        ok(b.get(ValueLayout.JAVA_LONG_UNALIGNED, 16) == -9876543210L);
        b.set(ValueLayout.JAVA_FLOAT_UNALIGNED, 24, 3.5f);
        ok(b.get(ValueLayout.JAVA_FLOAT_UNALIGNED, 24) == 3.5f);
        b.set(ValueLayout.JAVA_DOUBLE_UNALIGNED, 32, -0.125d);
        ok(b.get(ValueLayout.JAVA_DOUBLE_UNALIGNED, 32) == -0.125d);

        // Por indice: el offset es `index * byteSize`.
        MemorySegment ints = MemorySegment.ofArray(new int[4]);
        ints.setAtIndex(ValueLayout.JAVA_INT, 0, 10);
        ints.setAtIndex(ValueLayout.JAVA_INT, 3, 40);
        ok(ints.getAtIndex(ValueLayout.JAVA_INT, 0) == 10);
        ok(ints.getAtIndex(ValueLayout.JAVA_INT, 3) == 40);
        ok(ints.get(ValueLayout.JAVA_INT, 12) == 40);

        // El respaldo se ve por los dos lados: un segmento es una **vista**, no una copia.
        int[] respaldo = new int[2];
        MemorySegment v = MemorySegment.ofArray(respaldo);
        v.setAtIndex(ValueLayout.JAVA_INT, 1, 0x0BADCAFE);
        ok(respaldo[1] == 0x0BADCAFE);
        respaldo[0] = 77;
        ok(v.getAtIndex(ValueLayout.JAVA_INT, 0) == 77);
    }

    static void loQueNoTieneQueAndar() {
        byte[] arr = new byte[8];
        MemorySegment s = MemorySegment.ofArray(arr);

        // Alineamiento: `JAVA_INT` exige 4 y un `byte[]` garantiza 1.
        boolean align = false;
        try {
            s.get(ValueLayout.JAVA_INT, 0);
        } catch (IllegalArgumentException e) {
            align = true;
        }
        ok(align);

        // Limites: leer un `long` en el offset 4 de un segmento de 8 se sale.
        boolean fuera = false;
        try {
            s.get(ValueLayout.JAVA_LONG_UNALIGNED, 4);
        } catch (IndexOutOfBoundsException e) {
            fuera = true;
        }
        ok(fuera);

        boolean negativo = false;
        try {
            s.get(ValueLayout.JAVA_BYTE, -1);
        } catch (IndexOutOfBoundsException e) {
            negativo = true;
        }
        ok(negativo);

        // Solo lectura.
        MemorySegment ro = s.asReadOnly();
        ok(ro.isReadOnly());
        ok(!s.isReadOnly());
        ok(ro.get(ValueLayout.JAVA_BYTE, 0) == 0);
        boolean escribio = false;
        try {
            ro.set(ValueLayout.JAVA_BYTE, 0, (byte) 1);
        } catch (IllegalArgumentException e) {
            escribio = true;
        }
        ok(escribio);

        // Un ambito cerrado: es lo que convierte un error de memoria en una excepcion.
        Arena a = Arena.ofConfined();
        MemorySegment reservado = a.allocate(8, 1);
        ok(reservado.scope().isAlive());
        reservado.set(ValueLayout.JAVA_BYTE, 0, (byte) 9);
        a.close();
        ok(!reservado.scope().isAlive());
        boolean cerrado = false;
        try {
            reservado.get(ValueLayout.JAVA_BYTE, 0);
        } catch (IllegalStateException e) {
            cerrado = true;
        }
        ok(cerrado);

        boolean doble = false;
        try {
            a.close();
        } catch (IllegalStateException e) {
            doble = true;
        }
        ok(doble);

        // La global no se cierra.
        boolean glob = false;
        try {
            Arena.global().close();
        } catch (UnsupportedOperationException e) {
            glob = true;
        }
        ok(glob);
        ok(Arena.global().scope().isAlive());
    }

    static void arenas() {
        Arena a = Arena.ofConfined();
        // El alineamiento pedido se respeta de verdad: la arena elige el arreglo de respaldo por el.
        MemorySegment ocho = a.allocate(16, 8);
        ok(ocho.byteSize() == 16L);
        ok(ocho.maxByteAlignment() >= 8L);
        ocho.set(ValueLayout.JAVA_LONG, 0, 123456789L);
        ok(ocho.get(ValueLayout.JAVA_LONG, 0) == 123456789L);

        MemorySegment porLayout = a.allocate(ValueLayout.JAVA_INT);
        ok(porLayout.byteSize() == 4L);
        porLayout.set(ValueLayout.JAVA_INT, 0, 42);
        ok(porLayout.get(ValueLayout.JAVA_INT, 0) == 42);

        MemorySegment varios = a.allocate(ValueLayout.JAVA_INT, 3);
        ok(varios.byteSize() == 12L);

        // `allocateFrom`: reservar y escribir de una.
        MemorySegment uno = a.allocateFrom(ValueLayout.JAVA_INT, 7);
        ok(uno.get(ValueLayout.JAVA_INT, 0) == 7);
        MemorySegment tres = a.allocateFrom(ValueLayout.JAVA_INT, 1, 2, 3);
        ok(tres.byteSize() == 12L);
        ok(tres.getAtIndex(ValueLayout.JAVA_INT, 0) == 1);
        ok(tres.getAtIndex(ValueLayout.JAVA_INT, 2) == 3);

        MemorySegment cad = a.allocateFrom("hola");
        // Cuatro letras mas el cero final: sin el, quien lea del otro lado seguiria hasta el proximo
        // cero que hubiera por ahi.
        ok(cad.byteSize() == 5L);
        ok(cad.getString(0).equals("hola"));
        a.close();
    }

    static void cadenas() {
        MemorySegment s = MemorySegment.ofArray(new byte[16]);
        s.setString(0, "hola");
        ok(s.getString(0).equals("hola"));
        // El terminador esta.
        ok(s.get(ValueLayout.JAVA_BYTE, 4) == 0);
        // Y se puede leer desde el medio.
        s.setString(6, "chau");
        ok(s.getString(6).equals("chau"));
        ok(s.getString(0).equals("hola"));

        MemorySegment corto = MemorySegment.ofArray(new byte[3]);
        boolean tiro = false;
        try {
            corto.setString(0, "demasiado");
        } catch (IndexOutOfBoundsException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void copiarComparar() {
        MemorySegment a = MemorySegment.ofArray(new byte[8]);
        MemorySegment b = MemorySegment.ofArray(new byte[8]);
        a.fill((byte) 5);
        ok(a.get(ValueLayout.JAVA_BYTE, 0) == 5);
        ok(a.get(ValueLayout.JAVA_BYTE, 7) == 5);

        // Iguales byte a byte: no difieren en ningun lado.
        b.fill((byte) 5);
        ok(a.mismatch(b) == -1L);
        b.set(ValueLayout.JAVA_BYTE, 3, (byte) 9);
        ok(a.mismatch(b) == 3L);

        // Uno prefijo del otro: "difieren" donde el corto se acaba.
        MemorySegment corto = b.asSlice(0, 3);
        b.set(ValueLayout.JAVA_BYTE, 3, (byte) 5);
        ok(a.mismatch(corto) == 3L);

        MemorySegment dst = MemorySegment.ofArray(new byte[8]);
        dst.copyFrom(a);
        ok(dst.mismatch(a) == -1L);

        MemorySegment.copy(a, 0, dst, 0, 4);
        ok(dst.get(ValueLayout.JAVA_BYTE, 0) == 5);

        // Y la version estatica sobre rangos.
        ok(MemorySegment.mismatch(a, 0, 8, dst, 0, 8) == -1L);

        // Copiar elementos respetando el orden de bytes de cada lado: copiar crudo dejaria el
        // destino al reves.
        MemorySegment le = MemorySegment.ofArray(new int[2]);
        MemorySegment be = MemorySegment.ofArray(new int[2]);
        le.setAtIndex(ValueLayout.JAVA_INT, 0, 0x01020304);
        MemorySegment.copy(le, ValueLayout.JAVA_INT, 0, be,
                ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN), 0, 1);
        ok(be.get(ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN), 0) == 0x01020304);
    }

    static void arreglosYVistas() {
        MemorySegment s = MemorySegment.ofArray(new int[] { 10, 20, 30 });
        int[] vuelta = s.toArray(ValueLayout.JAVA_INT);
        ok(vuelta.length == 3);
        ok(vuelta[0] == 10 && vuelta[2] == 30);

        MemorySegment bs = MemorySegment.ofArray(new byte[] { 1, 2, 3, 4 });
        byte[] bytes = bs.toArray(ValueLayout.JAVA_BYTE);
        ok(bytes.length == 4 && bytes[3] == 4);

        // Los elementos como flujo: un corte por elemento.
        MemorySegment cuatro = MemorySegment.ofArray(new int[] { 1, 2, 3, 4 });
        long n = cuatro.elements(ValueLayout.JAVA_INT).count();
        ok(n == 4L);

        // Superposicion: dos cortes del **mismo** respaldo se superponen; dos arreglos distintos
        // nunca, aunque los offsets coincidan.
        MemorySegment base = MemorySegment.ofArray(new byte[16]);
        ok(base.asSlice(0, 8).asOverlappingSlice(base.asSlice(4, 8)).isPresent());
        ok(!base.asSlice(0, 4).asOverlappingSlice(base.asSlice(8, 4)).isPresent());
        ok(!base.asOverlappingSlice(MemorySegment.ofArray(new byte[16])).isPresent());
    }

    static void asignadores() {
        MemorySegment bloque = MemorySegment.ofArray(new byte[32]);
        SegmentAllocator cortante = SegmentAllocator.slicingAllocator(bloque);
        MemorySegment p1 = cortante.allocate(8, 1);
        MemorySegment p2 = cortante.allocate(8, 1);
        // Van uno detras del otro: no se pisan.
        ok(p1.address() == 0L);
        ok(p2.address() == 8L);
        ok(!p1.asOverlappingSlice(p2).isPresent());

        SegmentAllocator prefijo = SegmentAllocator.prefixAllocator(bloque);
        MemorySegment q1 = prefijo.allocate(8, 1);
        MemorySegment q2 = prefijo.allocate(8, 1);
        // Al reves que el cortante: **siempre el mismo**. Es para reusar un buffer, y por eso la
        // segunda pisa a la primera.
        ok(q1.address() == q2.address());
    }

    public static int run() {
        basicos();
        leerYEscribir();
        loQueNoTieneQueAndar();
        arenas();
        cadenas();
        copiarComparar();
        arreglosYVistas();
        asignadores();
        return primerFallo;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
