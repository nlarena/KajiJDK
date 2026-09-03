package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMImplementationSource -- quien sabe donde hay implementaciones del DOM.
 *
 * <p>Un escalon mas arriba de {@link DOMImplementation}: la implementacion fabrica documentos, y
 * esto encuentra implementaciones. Lo implementa cada proveedor y lo consulta el registro de
 * arranque, que en el JDK es {@code org.w3c.dom.bootstrap.DOMImplementationRegistry}.
 *
 * <p>La cadena {@code features} tiene una sintaxis propia: nombres de modulo separados por espacios,
 * cada uno con una version opcional detras --por ejemplo {@code "XML 3.0 Traversal +Events 2.0"}--
 * donde el {@code +} pide que el modulo este disponible aunque sea por
 * {@link DOMImplementation#getFeature} y no directamente en el objeto.
 *
 * <p>Interfaz declarada entera.
 */
public interface DOMImplementationSource {

    /** Alguna implementacion que cumpla con eso, o {@code null} si no hay ninguna. */
    public DOMImplementation getDOMImplementation(String features);

    /** Todas las que cumplan; la lista puede venir vacia. */
    public DOMImplementationList getDOMImplementationList(String features);
}
