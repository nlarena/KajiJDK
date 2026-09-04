import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/** Los tres MethodHandle que fabrica MemoryLayout: escala, desplazamiento y rebanada. */
public class MhLayoutTest {

    public static int run() throws Throwable {
        int i = 0;
        Arena a = Arena.ofAuto();

        StructLayout punto = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("x"),
                ValueLayout.JAVA_INT.withName("y"));

        // -- scaleHandle: base + indice * byteSize()
        MethodHandle esc = punto.scaleHandle();
        if ((long) esc.invokeExact(0L, 3L) != 24L) { return i; } i++;
        if ((long) esc.invokeExact(8L, 1L) != 16L) { return i; } i++;
        if ((long) ValueLayout.JAVA_INT.scaleHandle().invokeExact(0L, 5L) != 20L) { return i; } i++;

        // -- byteOffsetHandle sobre un camino cerrado
        MethodHandle offY = punto.byteOffsetHandle(MemoryLayout.PathElement.groupElement("y"));
        if ((long) offY.invokeExact(0L) != 4L) { return i; } i++;
        if ((long) offY.invokeExact(100L) != 104L) { return i; } i++;

        // -- byteOffsetHandle con un paso abierto
        SequenceLayout puntos = MemoryLayout.sequenceLayout(4, punto);
        MethodHandle offAx = puntos.byteOffsetHandle(
                MemoryLayout.PathElement.sequenceElement(),
                MemoryLayout.PathElement.groupElement("x"));
        if ((long) offAx.invokeExact(0L, 0L) != 0L) { return i; } i++;
        if ((long) offAx.invokeExact(0L, 2L) != 16L) { return i; } i++;
        if ((long) offAx.invokeExact(4L, 1L) != 12L) { return i; } i++;

        // -- sliceHandle: recorta al tamano del layout de destino
        MemorySegment arr = a.allocate(puntos);
        arr.set(ValueLayout.JAVA_INT, 8L, 42);
        arr.set(ValueLayout.JAVA_INT, 12L, 43);
        MethodHandle slice = puntos.sliceHandle(MemoryLayout.PathElement.sequenceElement());
        MemorySegment uno = (MemorySegment) slice.invokeExact(arr, 0L, 1L);
        if (uno.byteSize() != 8L) { return i; } i++;
        if (uno.get(ValueLayout.JAVA_INT, 0L) != 42) { return i; } i++;
        if (uno.get(ValueLayout.JAVA_INT, 4L) != 43) { return i; } i++;

        MethodHandle sliceY = punto.sliceHandle(MemoryLayout.PathElement.groupElement("y"));
        MemorySegment soloY = (MemorySegment) sliceY.invokeExact(arr, 8L);
        if (soloY.byteSize() != 4L) { return i; } i++;
        if (soloY.get(ValueLayout.JAVA_INT, 0L) != 43) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) throws Throwable { System.out.println(run()); }
}
