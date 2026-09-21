package java.nio.file;

// `StandardCopyOption.ATOMIC_MOVE` was asked for and the system cannot guarantee it.
//
// **KajiJDK always throws it.** `Files.move` is made of copy and delete --there is no rename
// native-- so there is a moment when the file is on both sides and another when a power cut would
// leave it duplicated. Failing here is the honest answer: saying yes and moving in two steps would
// be promising an atomicity that does not exist, and whoever asks for `ATOMIC_MOVE` asks precisely
// because it matters to them.
public class AtomicMoveNotSupportedException extends FileSystemException {

    private static final long serialVersionUID = 5402760225333135579L;

    /** @param source the source; `target` the target; `reason` the reason. They may be `null`. */
    public AtomicMoveNotSupportedException(String source, String target, String reason) {
        super(source, target, reason);
    }
}
