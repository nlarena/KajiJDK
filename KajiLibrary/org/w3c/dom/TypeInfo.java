package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.TypeInfo -- el tipo que un esquema le asigno a un elemento o atributo.
 *
 * <p>La devuelven {@link Element#getSchemaTypeInfo} y {@link Attr#getSchemaTypeInfo}, y valen
 * {@code null} mientras no haya habido validacion: sin gramatica no hay tipos, solo texto.
 *
 * <p>Las cuatro constantes {@code DERIVATION_*} son **una mascara de bits** --1, 2, 4, 8-- y no una
 * enumeracion, porque a {@link #isDerivedFrom} se le pregunta por varias formas de derivacion a la
 * vez combinandolas con OR. Un {@code 0} en ese argumento tiene un significado propio y util: "por
 * cualquier via", que es lo que uno quiere casi siempre.
 *
 * <p>Interfaz declarada entera.
 */
public interface TypeInfo {

    /** Derivacion por restriccion. */
    public static final int DERIVATION_RESTRICTION = 0x00000001;

    /** Derivacion por extension. */
    public static final int DERIVATION_EXTENSION = 0x00000002;

    /** El tipo participa de una union. */
    public static final int DERIVATION_UNION = 0x00000004;

    /** El tipo es el de los items de una lista. */
    public static final int DERIVATION_LIST = 0x00000008;

    /** El nombre del tipo, o {@code null} si es anonimo. */
    public String getTypeName();

    /**
     * El espacio de nombres del tipo, o {@code null}.
     *
     * <p>Para un tipo del DTD --{@code ID}, {@code CDATA}, {@code IDREF}-- es
     * {@code "http://www.w3.org/TR/REC-xml"}, no el de XML Schema.
     */
    public String getTypeNamespace();

    /**
     * Si este tipo deriva de ese otro por alguna de las vias pedidas.
     *
     * @param derivationMethod una OR de los {@code DERIVATION_*}, o {@code 0} para "por cualquier via"
     */
    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod);
}
