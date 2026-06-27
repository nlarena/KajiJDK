package java.lang;

// Minimal java.lang.Error — the Throwable branch for serious problems (not meant to
// be caught in normal code). Root of the linkage errors below.
public class Error extends Throwable {
    public Error() {
    }
}
