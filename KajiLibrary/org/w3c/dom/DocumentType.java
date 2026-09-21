package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DocumentType -- the {@code <!DOCTYPE ...>} of the document.
 *
 * <p>It hangs from the {@link Document} --it is reached through {@link Document#getDoctype}-- and
 * is **read-only**: there is not a single setter in the whole interface. The reason is that DOM
 * Level 1 and 2 never defined how to edit a DTD, and editing it halfway is worse than not being
 * able to: changing an entity declaration when there are already nodes in the tree expanded with
 * the previous one leaves the document incoherent with no way of fixing it.
 *
 * <p>That is why it does not expose the content model either: there is no way of asking it "which
 * children does this element admit". What it does expose are the declarations of entities and of
 * notations, each in a read-only {@link NamedNodeMap}.
 *
 * <p>The interface is declared whole.
 */
public interface DocumentType extends Node {

    /** The name of the DTD, which is that of the declared root element. */
    public String getName();

    /** The declared entities, general and parameter ones, indexed by name. Read-only. */
    public NamedNodeMap getEntities();

    /** The declared notations, indexed by name. Read-only. */
    public NamedNodeMap getNotations();

    /** The public identifier of the external subset, or {@code null}. */
    public String getPublicId();

    /** The system identifier of the external subset, or {@code null}. */
    public String getSystemId();

    /** The internal subset as text, or {@code null}; neither parsed nor normalised. */
    public String getInternalSubset();
}
