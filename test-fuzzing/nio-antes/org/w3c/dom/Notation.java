package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Notation -- a notation declared in the DTD.
 *
 * <p>A notation gives a name to the **format** of something the XML processor does not understand:
 * the type of a binary referred to by an unparsed entity, or the target of a {@link
 * ProcessingInstruction}. It is the way a DTD has of saying "this is a TIFF" without the parser
 * knowing anything about TIFF.
 *
 * <p>It lives in {@link DocumentType#getNotations}, has no parent, does not appear when walking the
 * tree and is read-only. At least one of the two identifiers is present.
 *
 * <p>The interface is declared whole.
 */
public interface Notation extends Node {

    /** The public identifier, or {@code null}. */
    public String getPublicId();

    /** The system identifier, or {@code null}. */
    public String getSystemId();
}
