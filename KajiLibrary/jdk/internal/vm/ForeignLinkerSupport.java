package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.ForeignLinkerSupport — si esta plataforma tiene enlazador nativo.
 *
 * <p>Una sola pregunta, y la respuesta acá es **no**. `java.lang.foreign.Linker` necesita llamar a
 * código nativo con la convención de llamada del sistema, y esta VM no lo hace.
 *
 * <p>Que la respuesta sea negativa es justamente lo que hace útil a esta clase: existe para que quien
 * pregunte pueda tomar otro camino en vez de estrellarse. Devolver `true` sería la mentira; devolver
 * `false` es información correcta, y coincide con lo que `Linker.nativeLinker()` ya hace en esta
 * biblioteca.
 */
public final class ForeignLinkerSupport {

    private ForeignLinkerSupport() {
    }

    /** Si hay enlazador nativo. En esta VM, `false`. */
    public static boolean isSupported() {
        return false;
    }
}
