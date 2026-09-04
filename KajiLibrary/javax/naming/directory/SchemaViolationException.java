package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.SchemaViolationException -- la operacion contradice el esquema del directorio.
 *
 * <p>La generica de las de esquema, para lo que no cae en las otras: borrar una entrada que tiene
 * hijos, mover algo adonde su clase de objeto no puede vivir, cambiar una clase de objeto
 * por otra incompatible.
 */
public class SchemaViolationException extends NamingException {

    private static final long serialVersionUID = -3041762429525049663L;

    /** Sin detalle. */
    public SchemaViolationException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public SchemaViolationException(String explanation) {
        super(explanation);
    }
}
