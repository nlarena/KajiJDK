package java.security;

// La costura hacia el generador del sistema operativo.
//
// ===============================================================================================
// POR QUE HACE FALTA UN NATIVE
// ===============================================================================================
//
// Todo lo demas de esta biblioteca es determinista: dado el mismo estado hace lo mismo. Un
// generador criptografico necesita justo lo contrario, y no lo puede calcular -- la semilla tiene
// que venir de afuera, de algo que un atacante no pueda reproducir. Por eso este es el unico lugar
// de `java.security` que baja a la VM.
//
// Lo que hay del otro lado es el generador **del sistema**, no uno propio:
//
//   - En Windows, `BCryptGenRandom` con el algoritmo preferido del sistema.
//   - En el resto, `/dev/urandom`.
//
// Son los mismos dos que usan el JDK y la biblioteca estandar de Rust. La eleccion importa: un
// generador propio sembrado con la hora y el identificador del proceso se ve igual desde afuera y
// es adivinable, que es exactamente el modo en que este tipo de codigo falla.
//
// ===============================================================================================
// POR QUE UN FALLO PARCIAL SE TRATA COMO FALLO TOTAL
// ===============================================================================================
//
// El native devuelve un booleano y no una cantidad de bytes. Si no pudo llenar el arreglo entero,
// devuelve false y el arreglo queda como estaba. La alternativa --devolver cuantos bytes llenó--
// obligaria a cada llamador a acordarse de mirarlo, y el que se olvide se queda con una semilla
// mitad ceros que parece entera. Aca eso no se puede: o hay entropia o hay excepcion.
final class OsEntropy {

    private OsEntropy() {
    }

    /** Llena `out` con bytes del sistema. Devuelve si pudo. */
    private static native boolean fill0(byte[] out);

    /**
     * Devuelve `count` bytes del generador del sistema.
     *
     * @throws ProviderException si el sistema no pudo darlos. Es un error de la plataforma, no del
     *     llamador, y no hay forma razonable de seguir: cualquier cosa que se devolviera en su
     *     lugar seria adivinable.
     */
    static byte[] bytes(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("negative count: " + count);
        }
        byte[] out = new byte[count];
        fill(out);
        return out;
    }

    /**
     * Llena `out` con bytes del generador del sistema.
     *
     * @throws ProviderException si el sistema no pudo darlos
     */
    static void fill(byte[] out) {
        if (out.length == 0) {
            return;
        }
        if (!fill0(out)) {
            throw new ProviderException("the operating system's random generator is unavailable");
        }
    }

    /** Si el sistema puede dar entropia. Lo usa el proveedor para no registrar lo que no puede. */
    static boolean available() {
        try {
            byte[] probe = new byte[1];
            return fill0(probe);
        } catch (Throwable e) {
            return false;
        }
    }
}
