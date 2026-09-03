package java.nio.file;

// Se pidio `StandardCopyOption.ATOMIC_MOVE` y el sistema no lo puede garantizar.
//
// **KajiJDK la levanta siempre.** `Files.move` esta hecho de copiar y borrar --no hay nativo de
// rename-- asi que hay un momento en el que el archivo esta en los dos lados y otro en el que un
// corte de luz lo dejaria duplicado. Fallar aca es la respuesta honesta: decir que si y mover en dos
// pasos seria prometer una atomicidad que no existe, y quien pide `ATOMIC_MOVE` la pide justamente
// porque le importa.
public class AtomicMoveNotSupportedException extends FileSystemException {

    private static final long serialVersionUID = 5402760225333135579L;

    /** @param source el origen; `target` el destino; `reason` el motivo. Pueden ser `null`. */
    public AtomicMoveNotSupportedException(String source, String target, String reason) {
        super(source, target, reason);
    }
}
