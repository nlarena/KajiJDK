package java.lang;

// KajiLibrary's java.lang.MatchException — thrown when pattern matching finds no applicable
// case in a construct the compiler proved exhaustive. That sounds contradictory, and the
// resolution is the interesting part: exhaustiveness is checked against the sealed hierarchy as
// it was at compile time. If a permitted subclass is added and only that file is recompiled,
// the switch's proof is stale, and this is the VM admitting it rather than falling through.
//
// It also covers a record deconstruction pattern whose accessor threw: the match neither
// succeeded nor cleanly failed, so the original exception is wrapped as the cause.
public class MatchException extends RuntimeException {

    public MatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
