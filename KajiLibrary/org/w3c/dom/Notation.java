package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Notation -- una notacion declarada en el DTD.
 *
 * <p>Una notacion le pone nombre al **formato** de algo que el procesador XML no entiende: el tipo
 * de un binario referido por una entidad no parseada, o el destino de una
 * {@link ProcessingInstruction}. Es la manera que tiene un DTD de decir "esto es un TIFF" sin que
 * el parser sepa nada de TIFF.
 *
 * <p>Vive en {@link DocumentType#getNotations}, no tiene padre, no aparece recorriendo el arbol y es
 * de solo lectura. Al menos uno de los dos identificadores esta presente.
 *
 * <p>Interfaz declarada entera.
 */
public interface Notation extends Node {

    /** El identificador publico, o {@code null}. */
    public String getPublicId();

    /** El identificador de sistema, o {@code null}. */
    public String getSystemId();
}
