package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.Entity -- an entity declared in the DTD.
 *
 * <p>Not to be confused with {@link EntityReference}: this is the **declaration**, the one that
 * lives in the {@link NamedNodeMap} of {@link DocumentType#getEntities}, and that one is each
 * **use** in the document. The declaration has no parent and does not appear when walking the tree.
 *
 * <p>The whole interface --and the subtree of children, which is the already parsed replacement
 * text-- is read-only, for the same reason as {@link DocumentType}: there is no coherent way of
 * changing an entity when there are already references expanded with the previous version.
 *
 * <p>The three identifiers tell the three types of entity apart: an **internal** one has neither
 * {@code publicId} nor {@code systemId}; an **external parsed** one has a {@code systemId} and no
 * {@code notationName}; and an **unparsed** one --a binary, an image-- has a {@code notationName},
 * and then its children are {@code null} because there is nothing XML to parse inside.
 *
 * <p>The interface is declared whole.
 */
public interface Entity extends Node {

    /** The public identifier, or {@code null}. */
    public String getPublicId();

    /** The system identifier, or {@code null} if the entity is internal. */
    public String getSystemId();

    /** The name of the notation if the entity is unparsed; {@code null} if it is parsed. */
    public String getNotationName();

    /**
     * The encoding detected when reading the entity, or {@code null} if it did not come from a
     * parser.
     */
    public String getInputEncoding();

    /** The encoding declared in the text declaration of the entity, or {@code null}. */
    public String getXmlEncoding();

    /** The XML version declared in the entity, or {@code null}. */
    public String getXmlVersion();
}
