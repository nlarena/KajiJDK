package java.nio.file.attribute;

import java.io.IOException;

// No se encontro el usuario o grupo que se estaba buscando.
//
// Guarda el nombre aparte del mensaje porque quien la atrapa suele querer el nombre crudo para
// reintentar o para armar su propio mensaje, y sacarlo de `getMessage()` a mano seria fragil.
public class UserPrincipalNotFoundException extends IOException {

    private static final long serialVersionUID = -5369283889045833024L;

    private final String name;

    /**
     * @param name el nombre que no se encontro, o `null` si no se sabe
     */
    public UserPrincipalNotFoundException(String name) {
        super();
        this.name = name;
    }

    /** El nombre que no se encontro, o `null` si no se sabe. */
    public String getName() {
        return this.name;
    }
}
