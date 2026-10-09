package java.lang;

// KajiLibrary's java.lang.UnsupportedClassVersionError — the class file is well formed but its
// major version is newer than this VM understands. The precise, and usefully specific, form of
// "compiled by a newer JDK than the one you are running on".
public class UnsupportedClassVersionError extends ClassFormatError {

    public UnsupportedClassVersionError() {
    }

    public UnsupportedClassVersionError(String message) {
        super(message);
    }
}
