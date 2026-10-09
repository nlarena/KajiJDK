package java.lang;

// KajiLibrary's java.lang.ClassFormatError — the bytes of a .class file are malformed: bad
// magic, a truncated constant pool, a nonsense attribute. Thrown by the class-file parser
// before anything in the class can run.
public class ClassFormatError extends LinkageError {

    public ClassFormatError() {
    }

    public ClassFormatError(String message) {
        super(message);
    }
}
