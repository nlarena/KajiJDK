import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;


/** Los VarHandle que fabrica java.lang.foreign: camino cerrado, paso abierto y arreglo. */
public class VhTest {

    public static int run() {
        int i = 0;
        Arena a = Arena.ofAuto();
        // Se pide el alineamiento **explicito**. `allocate(n)` sola no sirve para esta prueba: el
        // JDK devuelve memoria nativa, que queda alineada a 8 aunque se haya pedido 1, y nuestro
        // segmento de heap se respalda en un `byte[]`, que solo puede garantizar 1. Los dos son
        // correctos para lo que representan, y la diferencia no tiene nada que ver con VarHandles.

        // -- el atajo de un ValueLayout suelto
        MemorySegment s = a.allocate(32, 8);
        VarHandle vi = ValueLayout.JAVA_INT.varHandle();
        vi.set(s, 0L, 7);
        if ((int) vi.get(s, 0L) != 7) { return i; } i++;
        vi.set(s, 4L, -12345);
        if ((int) vi.get(s, 4L) != -12345) { return i; } i++;
        if (s.get(ValueLayout.JAVA_INT, 4L) != -12345) { return i; } i++;

        VarHandle vl = ValueLayout.JAVA_LONG.varHandle();
        vl.set(s, 8L, 1234567890123L);
        if ((long) vl.get(s, 8L) != 1234567890123L) { return i; } i++;

        VarHandle vd = ValueLayout.JAVA_DOUBLE.varHandle();
        vd.set(s, 16L, 2.5d);
        if ((double) vd.get(s, 16L) != 2.5d) { return i; } i++;

        VarHandle vb = ValueLayout.JAVA_BYTE.varHandle();
        vb.set(s, 24L, (byte) 9);
        if ((byte) vb.get(s, 24L) != (byte) 9) { return i; } i++;

        // -- camino cerrado sobre un struct
        StructLayout punto = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("x"),
                ValueLayout.JAVA_INT.withName("y"));
        MemorySegment p = a.allocate(punto);
        VarHandle px = punto.varHandle(MemoryLayout.PathElement.groupElement("x"));
        VarHandle py = punto.varHandle(MemoryLayout.PathElement.groupElement("y"));
        px.set(p, 0L, 3);
        py.set(p, 0L, 4);
        if ((int) px.get(p, 0L) != 3) { return i; } i++;
        if ((int) py.get(p, 0L) != 4) { return i; } i++;
        if (p.get(ValueLayout.JAVA_INT, 4L) != 4) { return i; } i++;
        if (punto.byteOffset(MemoryLayout.PathElement.groupElement("y")) != 4L) { return i; } i++;

        // -- paso abierto: un indice libre
        SequenceLayout puntos = MemoryLayout.sequenceLayout(4, punto);
        MemorySegment arr = a.allocate(puntos);
        VarHandle ax = puntos.varHandle(MemoryLayout.PathElement.sequenceElement(), MemoryLayout.PathElement.groupElement("x"));
        int k = 0;
        while (k < 4) {
            ax.set(arr, 0L, (long) k, k * 10);
            k = k + 1;
        }
        if ((int) ax.get(arr, 0L, 0L) != 0) { return i; } i++;
        if ((int) ax.get(arr, 0L, 3L) != 30) { return i; } i++;
        if (arr.get(ValueLayout.JAVA_INT, 8L) != 10) { return i; } i++;

        // -- arrayElementVarHandle: el indice lo pone el propio layout
        MemorySegment cuatro = a.allocate(16, 8);
        VarHandle ae = ValueLayout.JAVA_INT.arrayElementVarHandle();
        ae.set(cuatro, 0L, 2L, 99);
        if ((int) ae.get(cuatro, 0L, 2L) != 99) { return i; } i++;
        if (cuatro.get(ValueLayout.JAVA_INT, 8L) != 99) { return i; } i++;

        // -- el desplazamiento del llamador se suma al del camino
        MemorySegment dos = a.allocate(16, 8);
        px.set(dos, 8L, 55);
        if ((int) px.get(dos, 8L) != 55) { return i; } i++;
        if (dos.get(ValueLayout.JAVA_INT, 8L) != 55) { return i; } i++;

        // -- modos de memoria: hacen lo mismo
        vi.setVolatile(s, 0L, 77);
        if ((int) vi.getVolatile(s, 0L) != 77) { return i; } i++;
        if ((int) vi.getAcquire(s, 0L) != 77) { return i; } i++;
        vi.setRelease(s, 0L, 88);
        if ((int) vi.getOpaque(s, 0L) != 88) { return i; } i++;

        // -- un indice de mas es un error, no un acceso corrido
        boolean cayo = false;
        try { vi.get(s, 0L, 1L); } catch (RuntimeException e) { cayo = true; }
        if (!cayo) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) { System.out.println(run()); }
}
