package java.nio.file.attribute;

import java.io.IOException;

// The user or group being looked up was not found.
//
// It keeps the name apart from the message because whoever catches it usually wants the raw name to
// retry or to build their own message, and taking it out of `getMessage()` by hand would be
// fragile.
public class UserPrincipalNotFoundException extends IOException {

    private static final long serialVersionUID = -5369283889045833024L;

    private final String name;

    /**
     * @param name the name that was not found, or `null` if it is not known
     */
    public UserPrincipalNotFoundException(String name) {
        super();
        this.name = name;
    }

    /** The name that was not found, or `null` if it is not known. */
    public String getName() {
        return this.name;
    }
}
