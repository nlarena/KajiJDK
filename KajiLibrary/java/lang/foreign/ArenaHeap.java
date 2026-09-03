package java.lang.foreign;

// La implementacion de `Arena` sobre arreglos de Java. De paquete: se llega por los cuatro estaticos
// de `Arena`.
//
// Cada reserva se respalda con **un arreglo propio**, del tipo que da el alineamiento pedido. Repartir
// un solo bloque grande seria mas parecido a lo que hace el JDK, pero no se puede: un arreglo de Java
// solo garantiza el alineamiento de **su elemento**, asi que un `long` a mitad de un `byte[]` no
// estaria alineado por mas que la cuenta del offset diera. Un arreglo por reserva es lo que hace que
// el alineamiento prometido sea cierto.
//
// El costo es un objeto por reserva, que para el uso que este paquete tiene aca --describir y armar
// estructuras, no manejar megabytes-- es irrelevante frente a la correccion.
final class ArenaHeap implements Arena {

    // La global no se cierra nunca, y por eso es una sola.
    private static final ArenaHeap GLOBAL = new ArenaHeap(true);

    private final boolean global;
    private final Ambito ambito;

    private ArenaHeap(boolean global) {
        this.global = global;
        this.ambito = global ? Ambito.GLOBAL : new Ambito();
    }

    static Arena laGlobal() {
        return GLOBAL;
    }

    static Arena nueva() {
        return new ArenaHeap(false);
    }

    public MemorySegment.Scope scope() {
        return this.ambito;
    }

    public void close() {
        if (this.global) {
            throw new UnsupportedOperationException("la arena global no se cierra");
        }
        if (!this.ambito.isAlive()) {
            throw new IllegalStateException("esta arena ya estaba cerrada");
        }
        this.ambito.cerrar();
    }

    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteSize < 0L) {
            throw new IllegalArgumentException("tamanio negativo: " + byteSize);
        }
        Layouts.exigirAlineamiento(byteAlignment);
        if (!this.ambito.isAlive()) {
            throw new IllegalStateException("esta arena ya se cerro");
        }
        if (byteAlignment > 8L) {
            // Se rechaza en vez de fingirse. El elemento mas ancho de un arreglo de Java son 8 bytes;
            // prometer 16 y respaldarlo con un `long[]` seria decir que algo esta alineado cuando no
            // hay forma de saberlo.
            throw new UnsupportedOperationException(
                    "esta biblioteca respalda la memoria con arreglos de Java y no puede garantizar"
                            + " un alineamiento mayor que 8: se pidio " + byteAlignment);
        }
        // El tipo del arreglo lo elige el alineamiento, no el tamanio: es de donde sale la garantia.
        if (byteAlignment == 8L) {
            return this.envolver(new long[(int) redondear(byteSize, 8L)], 8);
        }
        if (byteAlignment == 4L) {
            return this.envolver(new int[(int) redondear(byteSize, 4L)], 4);
        }
        if (byteAlignment == 2L) {
            return this.envolver(new short[(int) redondear(byteSize, 2L)], 2);
        }
        return this.envolver(new byte[(int) byteSize], 1);
    }

    // Cuantos elementos de `tam` bytes hacen falta para cubrir `bytes`. Se redondea para arriba: un
    // pedido de 5 bytes con alineamiento 4 necesita dos `int`, no uno.
    private static long redondear(long bytes, long tam) {
        return (bytes + tam - 1L) / tam;
    }

    private MemorySegment envolver(Object arr, int tamElem) {
        long n = SegmentoHeap.largoDe(arr) * (long) tamElem;
        return new SegmentoHeap(arr, tamElem, 0L, n, false, this.ambito);
    }
}
