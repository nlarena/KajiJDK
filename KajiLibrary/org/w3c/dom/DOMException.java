package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMException -- the only exception of the DOM core.
 *
 * <p>It is the oddity of the package: everything else in {@code org.w3c.dom} is interfaces, and
 * this is a concrete class that extends {@link RuntimeException}. That it is **unchecked** is
 * deliberate and not an oversight: if it were checked, each of the thirty-odd methods of {@link
 * Node} and of {@link Document} would force a {@code try} on the caller, and walking a tree --which
 * almost never fails-- would be intolerable to write.
 *
 * <p>The design comes from OMG IDL, where there are no hierarchies of exceptions portable between
 * languages: instead of one subclass per error there is **one single class with a {@link #code}
 * field** that says which of the seventeen errors happened. That is why the useful {@code catch} is
 * always a {@code switch} on the code, and not a catch by type.
 *
 * <p>And for that same reason one has to be careful: {@link #code} is a **public and mutable**
 * {@code short}, the spec declares it so and so it is copied here. It is not a getter, it is not
 * final. It is API, not a slip that it would be as well to fix.
 *
 * <p>The seventeen values are fixed numbers of the specification --DOM Level 1 contributed 1 to 10,
 * Level 2 11 to 15 and Level 3 16 and 17; the note said "1 to 8" and "9 to 15"-- and they were
 * copied from there. Getting one wrong breaks no compilation: the error travels silently to
 * somebody else's {@code switch}, which goes into the wrong branch. The note added that the test
 * {@code java/W3cDomCodigosTest.java} verifies them one by one by reflection; there is no such test
 * in the repository, so nothing checks the copy but reading it.
 */
public class DOMException extends RuntimeException {

    static final long serialVersionUID = 6627732366795969916L;

    /**
     * Which of the errors it was, one of the {@code *_ERR} below.
     *
     * <p>Public and modifiable because that is how it is specified.
     */
    public short code;

    /**
     * @param code one of the {@code *_ERR}
     * @param message the description, which goes to {@link Throwable#getMessage}
     */
    public DOMException(short code, String message) {
        super(message);
        this.code = code;
    }

    /** The index or the size is negative, or goes past the maximum. */
    public static final short INDEX_SIZE_ERR = 1;

    /** The text does not fit in a {@code DOMString}. */
    public static final short DOMSTRING_SIZE_ERR = 2;

    /** An attempt was made to insert a node where it does not go. */
    public static final short HIERARCHY_REQUEST_ERR = 3;

    /** The node is used in a document other than the one that created it. */
    public static final short WRONG_DOCUMENT_ERR = 4;

    /** An invalid character, typically in a malformed name. */
    public static final short INVALID_CHARACTER_ERR = 5;

    /** An attempt was made to put data into a node that does not admit it. */
    public static final short NO_DATA_ALLOWED_ERR = 6;

    /** An attempt was made to modify a read-only node. */
    public static final short NO_MODIFICATION_ALLOWED_ERR = 7;

    /** The node is not in the context where it was looked for. */
    public static final short NOT_FOUND_ERR = 8;

    /** The implementation does not support that operation or that type of object. */
    public static final short NOT_SUPPORTED_ERR = 9;

    /** The attribute is already in use on another element. */
    public static final short INUSE_ATTRIBUTE_ERR = 10;

    /** An object was used that is not --or no longer is-- in a usable state. */
    public static final short INVALID_STATE_ERR = 11;

    /** An invalid string was specified, for example a malformed expression. */
    public static final short SYNTAX_ERR = 12;

    /** An attempt was made to change the type of an object that does not allow it. */
    public static final short INVALID_MODIFICATION_ERR = 13;

    /** An error with namespaces: a name and a URI that cannot go together. */
    public static final short NAMESPACE_ERR = 14;

    /** The object does not support the parameter or the operation asked for. */
    public static final short INVALID_ACCESS_ERR = 15;

    /** The operation would leave the document invalid with respect to its grammar. */
    public static final short VALIDATION_ERR = 16;

    /** The type of the value is not the one the parameter expected. */
    public static final short TYPE_MISMATCH_ERR = 17;
}
