package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Entity -- una entidad declarada en el DTD.
 *
 * <p>No confundirla con {@link EntityReference}: esta es la **declaracion**, la que vive en el
 * {@link NamedNodeMap} de {@link DocumentType#getEntities}, y aquella es cada **uso** en el
 * documento. La declaracion no tiene padre y no aparece recorriendo el arbol.
 *
 * <p>Toda la interfaz --y el subarbol de hijos, que es el texto de reemplazo ya parseado-- es de
 * solo lectura, por la misma razon que {@link DocumentType}: no hay forma coherente de cambiar una
 * entidad cuando ya hay referencias expandidas con la version anterior.
 *
 * <p>Los tres identificadores distinguen los tres tipos de entidad: una **interna** no tiene ni
 * {@code publicId} ni {@code systemId}; una **externa parseada** tiene {@code systemId} y no
 * {@code notationName}; y una **no parseada** --un binario, una imagen-- tiene {@code notationName},
 * y entonces sus hijos son {@code null} porque no hay nada XML que parsear adentro.
 *
 * <p>Interfaz declarada entera.
 */
public interface Entity extends Node {

    /** El identificador publico, o {@code null}. */
    public String getPublicId();

    /** El identificador de sistema, o {@code null} si la entidad es interna. */
    public String getSystemId();

    /** El nombre de la notacion si la entidad es no parseada; {@code null} si es parseada. */
    public String getNotationName();

    /** La codificacion detectada al leer la entidad, o {@code null} si no vino de un parser. */
    public String getInputEncoding();

    /** La codificacion declarada en la declaracion de texto de la entidad, o {@code null}. */
    public String getXmlEncoding();

    /** La version XML declarada en la entidad, o {@code null}. */
    public String getXmlVersion();
}
