package java.net;

import java.io.IOException;

// KajiLibrary's java.net.MalformedURLException (finding #267): what a URL constructor throws when
// the string does not name a resource.
//
// It extends IOException and not IllegalArgumentException, which looks odd for a parse failure and
// is deliberate -- it is the JDK's hierarchy, and it is why every `new URL(...)` sits inside the
// same `catch (IOException)` as the read that follows it.
public class MalformedURLException extends IOException {

    public MalformedURLException() {
        super();
    }

    public MalformedURLException(String message) {
        super(message);
    }
}
