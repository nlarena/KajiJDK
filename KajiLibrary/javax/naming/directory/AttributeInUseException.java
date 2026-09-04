package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.AttributeInUseException -- se intento agregar un atributo que ya estaba.
 *
 * <p>Sale de un {@code modifyAttributes} con {@link DirContext#ADD_ATTRIBUTE} sobre un atributo
 * que el directorio define como de un solo valor y que ya tiene uno. Con un atributo de
 * varios valores no pasa: ahi agregar es agregar.
 */
public class AttributeInUseException extends NamingException {

    private static final long serialVersionUID = 4437710305529322564L;

    /** Sin detalle. */
    public AttributeInUseException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public AttributeInUseException(String explanation) {
        super(explanation);
    }
}
