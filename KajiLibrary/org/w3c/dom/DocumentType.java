package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DocumentType -- el {@code <!DOCTYPE ...>} del documento.
 *
 * <p>Cuelga del {@link Document} --se llega por {@link Document#getDoctype}-- y es de **solo
 * lectura**: no hay un solo setter en toda la interfaz. La razon es que el DOM Level 1 y 2 nunca
 * definieron como editar un DTD, y editarlo a medias es peor que no poder: cambiar una declaracion
 * de entidad cuando ya hay nodos en el arbol expandidos con la anterior deja el documento
 * incoherente sin manera de arreglarlo.
 *
 * <p>Por eso tampoco expone el modelo de contenido: no hay forma de preguntarle "que hijos admite
 * este elemento". Lo que si expone son las declaraciones de entidades y de notaciones, cada una en
 * un {@link NamedNodeMap} de solo lectura.
 *
 * <p>Interfaz declarada entera.
 */
public interface DocumentType extends Node {

    /** El nombre del DTD, que es el del elemento raiz declarado. */
    public String getName();

    /** Las entidades declaradas, generales y de parametro, indexadas por nombre. Solo lectura. */
    public NamedNodeMap getEntities();

    /** Las notaciones declaradas, indexadas por nombre. Solo lectura. */
    public NamedNodeMap getNotations();

    /** El identificador publico del subconjunto externo, o {@code null}. */
    public String getPublicId();

    /** El identificador de sistema del subconjunto externo, o {@code null}. */
    public String getSystemId();

    /** El subconjunto interno como texto, o {@code null}; sin parsear ni normalizar. */
    public String getInternalSubset();
}
