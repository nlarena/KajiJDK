package java.io;

import java.io.IOException;

// Thrown when a named file could not be opened. KajiLibrary has no File or FileInputStream
// yet (they need filesystem support from the VM), but the exception type is pure Java and
// appears in the signatures of APIs we do model, so it is worth having on its own.
public class FileNotFoundException extends IOException {

    public FileNotFoundException() {
    }

    public FileNotFoundException(String message) {
        super(message);
    }
}
