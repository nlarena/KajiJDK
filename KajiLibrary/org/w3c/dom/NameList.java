package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.NameList -- an ordered list of (namespace, name) pairs.
 *
 * <p>It is {@link DOMStringList} with namespaces: each position has two strings instead of one, and
 * each half is accessed separately with {@link #getName} and {@link #getNamespaceURI} on the same
 * index. There is no "qualified name" type in the DOM, hence the pair of parallel accessors instead
 * of an object.
 *
 * <p>The standard declares it for the validation APIs --which ask which names are legal in a
 * place-- and the core does not return it anywhere. It is here all the same because it is public
 * API of the package.
 *
 * <p>The interface is declared whole.
 */
public interface NameList {

    /** The name at that position, or {@code null} if the index went out of range. */
    public String getName(int index);

    /** The namespace URI at that position, or {@code null}. */
    public String getNamespaceURI(int index);

    /** How many pairs there are. */
    public int getLength();

    /** Whether that name is there, looking only at the names. */
    public boolean contains(String str);

    /** Whether that (URI, name) pair is there. */
    public boolean containsNS(String namespaceURI, String name);
}
