package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.URIReference -- algo que apunta a otra cosa por URI.
 *
 * <p>Dos metodos: adonde apunta y de que tipo es lo apuntado. Lo implementan las dos estructuras de
 * XML-DSig que referencian datos: {@code Reference} --lo que se firma-- y {@code RetrievalMethod}
 * --de donde se saca una clave--.
 *
 * <p>El URI tiene tres formas y conviene distinguirlas: vacio significa <b>el documento entero</b>,
 * uno que empieza con almohadilla apunta adentro del mismo documento, y cualquier otro es externo.
 * Los externos son los peligrosos: resolverlos es ir a buscar algo que eligio quien firmo.
 */
public interface URIReference {

    /** Adonde apunta. Ver la nota de la clase sobre las tres formas. */
    String getURI();

    /** El tipo de lo apuntado, o null. */
    String getType();
}
