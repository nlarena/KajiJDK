package java.awt.desktop;

import java.net.URI;

/**
 * KajiLibrary's java.awt.desktop.OpenURIEvent -- the system asks for an address to be opened.
 *
 * <p>It is handed over by {@link OpenURIHandler}. It arrives when someone opens a link of a scheme
 * the program declared it handles -- {@code mailto:}, or one of its own like {@code myapp:}.
 *
 * <p>It is the entry point of data coming from <b>outside the program</b>, typically from a web page.
 * Whatever arrives here has to be validated like any untrusted input: the scheme guarantees nothing
 * about the rest of the address.
 */
public final class OpenURIEvent extends AppEvent {

    private static final long serialVersionUID = 221209100935933476L;

    /** The address. */
    final URI uri;

    /** @param uri the address to open */
    public OpenURIEvent(final URI uri) {
        this.uri = uri;
    }

    /** The address. See the class note: it is not to be trusted. */
    public URI getURI() {
        return this.uri;
    }
}
