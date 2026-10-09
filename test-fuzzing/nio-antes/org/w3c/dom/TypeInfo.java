package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.TypeInfo -- the type a schema assigned to an element or attribute.
 *
 * <p>{@link Element#getSchemaTypeInfo} and {@link Attr#getSchemaTypeInfo} return it, and they are
 * {@code null} as long as there has been no validation: with no grammar there are no types, only
 * text.
 *
 * <p>The four {@code DERIVATION_*} constants are **a bit mask** --1, 2, 4, 8-- and not an
 * enumeration, because {@link #isDerivedFrom} is asked about several forms of derivation at once by
 * combining them with OR. A {@code 0} in that argument has a meaning of its own and a useful one:
 * "by any road", which is what one almost always wants.
 *
 * <p>The interface is declared whole.
 */
public interface TypeInfo {

    /** Derivation by restriction. */
    public static final int DERIVATION_RESTRICTION = 0x00000001;

    /** Derivation by extension. */
    public static final int DERIVATION_EXTENSION = 0x00000002;

    /** The type takes part in a union. */
    public static final int DERIVATION_UNION = 0x00000004;

    /** The type is that of the items of a list. */
    public static final int DERIVATION_LIST = 0x00000008;

    /** The name of the type, or {@code null} if it is anonymous. */
    public String getTypeName();

    /**
     * The namespace of the type, or {@code null}.
     *
     * <p>For a DTD type --{@code ID}, {@code CDATA}, {@code IDREF}-- it is
     * {@code "http://www.w3.org/TR/REC-xml"}, not that of XML Schema.
     */
    public String getTypeNamespace();

    /**
     * Whether this type derives from that other one by any of the roads asked for.
     *
     * @param derivationMethod an OR of the {@code DERIVATION_*}, or {@code 0} for "by any road"
     */
    public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod);
}
