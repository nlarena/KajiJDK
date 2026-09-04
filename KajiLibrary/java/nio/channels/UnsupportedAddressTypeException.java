package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.UnsupportedAddressTypeException — La direccion es de un tipo que este canal no soporta.
 */
public class UnsupportedAddressTypeException extends IllegalArgumentException {

    private static final long serialVersionUID = 1000000023L;

    /** Construye una. Sin mensaje: el nombre de la clase **es** el mensaje. */
    public UnsupportedAddressTypeException() {
        super();
    }
}
