package javax.xml.namespace;

import java.io.Serializable;

import javax.xml.XMLConstants;

/**
 * KajiLibrary's javax.xml.namespace.QName -- an XML qualified name: namespace plus local name, and
 * a prefix that comes along for the ride.
 *
 * <p>It is the smallest class of the whole XML stack and the one that appears in all of it: DOM,
 * SAX, StAX, XPath, validation and JAXB use it. All it does is put two strings together, but
 * **which two** is exactly the question XML Namespaces came to answer, and all the subtlety comes
 * from there.
 *
 * <h2>The prefix is not part of the identity</h2>
 *
 * <p>This is the rule to keep in mind and the one that surprises most: {@link #equals} and
 * {@link #hashCode} look at the namespace and the local name, and **not** at the prefix. The two
 * documents
 *
 * <pre>{@code
 * <a:price xmlns:a="http://shop"/>
 * <b:price xmlns:b="http://shop"/>
 * }</pre>
 *
 * <p>say the same thing: the prefix is a local abbreviation of the document, chosen by whoever
 * wrote it, and two documents that chose differently are not talking about different things because
 * of that. If the prefix counted, a {@code Map<QName, ?>} would fail depending on who serialized
 * the entry, which is the kind of bug that is never found.
 *
 * <p>The prefix is still kept --and that is why {@link #getPrefix} exists-- because whoever writes
 * the document back needs it so as not to invent new prefixes on every element. That is: the prefix
 * is information, not identity. Both methods are {@code final} precisely so that no subclass can
 * change that rule underneath.
 *
 * <h2>The {@code {uri}local} format</h2>
 *
 * <p>{@link #toString} and {@link #valueOf} are inverses and use James Clark's notation: a name
 * with a namespace is written <code>{http://shop}price</code>, and one without a namespace is
 * written bare, {@code price}. It is a round-trip format for configurations and error messages, not
 * an XML syntax: it appears in no document.
 *
 * <p>The round trip loses the prefix on purpose --{@code valueOf} always returns {@link
 * XMLConstants#DEFAULT_NS_PREFIX}-- and that is coherent with the above: the prefix is not part of
 * the name, so it is not carried.
 *
 * <h2>What is here</h2>
 *
 * <p>The class is complete: the three constructors, the three accessors, {@code equals}, {@code
 * hashCode}, {@code toString} and {@code valueOf}, with the same validations and the same error
 * messages as the JDK --there is code that compares those texts--. It is {@link Serializable} and
 * declares the same {@code serialVersionUID} as the original class, so that an instance written by
 * one library can be read with the other.
 */
public class QName implements Serializable {

    /** The same as the original class's: two equivalent instances have to be interchangeable. */
    private static final long serialVersionUID = -9120448754896609940L;

    /** The namespace; never null, the empty string when there is none. */
    private final String namespaceURI;

    /** The local name; never null, and the only thing a name cannot lack. */
    private final String localPart;

    /** The prefix it was written with; never null, and not part of {@link #equals}. */
    private final String prefix;

    /**
     * A name without a namespace.
     *
     * <p>A shortcut for {@code QName(NULL_NS_URI, localPart, DEFAULT_NS_PREFIX)}: the namespace is
     * left as the empty string, which is how "it has none" is represented, and not as null.
     *
     * @param localPart the local name
     * @throws IllegalArgumentException if {@code localPart} is null
     */
    public QName(String localPart) {
        this(XMLConstants.NULL_NS_URI, localPart, XMLConstants.DEFAULT_NS_PREFIX);
    }

    /**
     * A qualified name without an associated prefix.
     *
     * <p>The normal case when the name is built by hand: one knows which vocabulary it belongs to,
     * not how it was abbreviated in a document that may not exist.
     *
     * @param namespaceURI the namespace; null is taken as the empty string
     * @param localPart the local name
     * @throws IllegalArgumentException if {@code localPart} is null
     */
    public QName(String namespaceURI, String localPart) {
        this(namespaceURI, localPart, XMLConstants.DEFAULT_NS_PREFIX);
    }

    /**
     * A qualified name with the prefix it appeared with.
     *
     * <p>The three validations are asymmetric and it is worth understanding why: an absent
     * namespace **is** a valid case --an unqualified name-- so null is normalized to the empty
     * string; an absent local name or prefix are caller errors, because there is nothing sensible
     * to mean with them, so they blow up.
     *
     * @param namespaceURI the namespace; null is taken as the empty string
     * @param localPart the local name
     * @param prefix the prefix; the empty string if there is none
     * @throws IllegalArgumentException if {@code localPart} or {@code prefix} is null
     */
    public QName(String namespaceURI, String localPart, String prefix) {
        if (namespaceURI == null) {
            this.namespaceURI = XMLConstants.NULL_NS_URI;
        } else {
            this.namespaceURI = namespaceURI;
        }
        if (localPart == null) {
            throw new IllegalArgumentException("local part cannot be \"null\" when creating a QName");
        }
        this.localPart = localPart;
        if (prefix == null) {
            throw new IllegalArgumentException("prefix cannot be \"null\" when creating a QName");
        }
        this.prefix = prefix;
    }

    /**
     * The namespace, or the empty string if the name is not qualified.
     *
     * @return never null
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * The local name, which is the only mandatory part.
     *
     * @return never null
     */
    public String getLocalPart() {
        return localPart;
    }

    /**
     * The prefix this name was written with, or the empty string.
     *
     * <p>It takes no part in {@link #equals} nor in {@link #hashCode}; see the class header.
     *
     * @return never null
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Two names are the same if namespace and local name match.
     *
     * <p>It is {@code final} so that the rule cannot be relaxed in a subclass: if a derived {@code
     * QName} brought the prefix into the comparison, it would break the symmetry with the library's
     * {@code QName}s --{@code a.equals(b)} and {@code b.equals(a)} would differ-- and with it any
     * table that uses them as keys.
     *
     * @param objectToTest the other object
     * @return true if it is a {@code QName} with the same namespace and local name
     */
    public final boolean equals(Object objectToTest) {
        if (objectToTest == this) {
            return true;
        }
        if (!(objectToTest instanceof QName)) {
            return false;
        }
        QName other = (QName) objectToTest;
        return localPart.equals(other.localPart) && namespaceURI.equals(other.namespaceURI);
    }

    /**
     * The exclusive or of the two hashes that matter.
     *
     * <p>It does not look at the prefix either, which is what is needed for it to be coherent with
     * {@link #equals}. Also {@code final}, and for the same reason.
     *
     * @return the hash
     */
    public final int hashCode() {
        return namespaceURI.hashCode() ^ localPart.hashCode();
    }

    /**
     * The name in {@code {uri}local} notation, or bare if it has no namespace.
     *
     * <p>Without a cache: it is computed each time. Keeping it would save concatenations on a path
     * that is almost always an error message or a trace, and in exchange would add a field that
     * takes no part in the identity and would have to be excluded by hand from serialization.
     *
     * @return the textual representation, which {@link #valueOf} knows how to undo
     */
    public String toString() {
        if (namespaceURI.equals(XMLConstants.NULL_NS_URI)) {
            return localPart;
        }
        return "{" + namespaceURI + "}" + localPart;
    }

    /**
     * Undoes {@link #toString}: the qualified name comes out of {@code {uri}local}.
     *
     * <p>The prefix of the result is always {@link XMLConstants#DEFAULT_NS_PREFIX}, because the
     * format does not carry it.
     *
     * <p>The edge cases are not arbitrary and are worth reading together:
     *
     * <ul>
     *   <li>the empty string gives a name with an empty local name, which is legal even though it
     *       is not a valid XML name: it is accepted for compatibility with version 1.0 of this
     *       class;
     *   <li><code>{}local</code> **fails**, and it is the only surprising case: asking explicitly
     *       for the empty namespace is an error, because the way of saying that is writing {@code
     *       local} alone, and whoever wrote the empty braces almost certainly thought they were
     *       saying something else;
     *   <li>a brace that opens and does not close fails;
     *   <li>a closing brace without an opening one is a local name, not an error: {@code }x} is an
     *       odd local name but it is a local name.
     * </ul>
     *
     * @param qNameAsString the string to interpret
     * @return the qualified name
     * @throws IllegalArgumentException if it is null or the format is wrong
     */
    public static QName valueOf(String qNameAsString) {
        if (qNameAsString == null) {
            throw new IllegalArgumentException("cannot create QName from \"null\" or \"\" String");
        }
        if (qNameAsString.length() == 0) {
            return new QName(
                    XMLConstants.NULL_NS_URI, qNameAsString, XMLConstants.DEFAULT_NS_PREFIX);
        }
        if (qNameAsString.charAt(0) != '{') {
            return new QName(
                    XMLConstants.NULL_NS_URI, qNameAsString, XMLConstants.DEFAULT_NS_PREFIX);
        }
        if (qNameAsString.startsWith("{" + XMLConstants.NULL_NS_URI + "}")) {
            throw new IllegalArgumentException(
                    "Namespace URI .equals(XMLConstants.NULL_NS_URI), "
                            + ".equals(\"" + XMLConstants.NULL_NS_URI + "\"), "
                            + "only the local part, "
                            + "\"" + qNameAsString.substring(2) + "\", "
                            + "should be provided.");
        }
        int endOfUri = qNameAsString.indexOf('}');
        if (endOfUri == -1) {
            throw new IllegalArgumentException(
                    "cannot create QName from \"" + qNameAsString + "\", missing closing \"}\"");
        }
        return new QName(
                qNameAsString.substring(1, endOfUri),
                qNameAsString.substring(endOfUri + 1),
                XMLConstants.DEFAULT_NS_PREFIX);
    }
}
