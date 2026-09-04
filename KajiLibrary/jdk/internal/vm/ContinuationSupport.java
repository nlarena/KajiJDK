package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.ContinuationSupport — si esta VM sabe suspender y reanudar pilas.
 *
 * <p>Las continuaciones son el sustrato de los hilos virtuales: la VM levanta la pila de un hilo,
 * la guarda en el montón y la vuelve a poner después, quizás en otro hilo del sistema. Eso es soporte
 * de VM, no biblioteca — no hay manera de escribirlo en Java.
 *
 * <p>Esta VM no lo tiene, y las cuatro respuestas son consistentes con eso:
 *
 * <ul>
 * <li>{@link #isSupported()} da `false`.</li>
 * <li>{@link #ensureSupported()} **tira**, porque su razón de ser es cortar antes de que el que llama
 *     construya sobre algo que no está.</li>
 * <li>{@link #pinIfSupported()} y {@link #unpinIfSupported()} no hacen nada, y eso es lo correcto y no
 *     un atajo: "clavar" una continuación quiere decir *impedir que se suspenda mientras dura esta
 *     sección*. Donde nada se suspende, no hay nada que impedir. El nombre lo dice: `ifSupported`.</li>
 * </ul>
 */
public class ContinuationSupport {

    private ContinuationSupport() {
    }

    /** Si la VM soporta continuaciones. Acá, `false`. */
    public static boolean isSupported() {
        return false;
    }

    /**
     * Corta si no hay soporte.
     *
     * @throws UnsupportedOperationException siempre, en esta VM
     */
    public static void ensureSupported() {
        throw new UnsupportedOperationException(
                "esta VM no soporta continuaciones: no puede levantar ni restaurar pilas");
    }

    /** Clava la continuación en curso, si hay. Acá no hay ninguna, así que no hace nada. */
    public static void pinIfSupported() {
    }

    /** La desclava. Simétrico del anterior, y por lo mismo tampoco hace nada. */
    public static void unpinIfSupported() {
    }
}
