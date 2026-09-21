package javax.xml.namespace;

import java.util.Iterator;

import javax.xml.XMLConstants;

/**
 * KajiLibrary's javax.xml.namespace.NamespaceContext -- the prefix/URI dictionary in force at a
 * point of the document.
 *
 * <p>XML's {@code xmlns} declarations have lexical scope: they hold in the element that declares
 * them and in everything below it, and an inner declaration hides the outer one. A {@code
 * NamespaceContext} is that stack of declarations **seen from a position**; whoever implements it
 * is the parser, which knows where it stands.
 *
 * <h2>The relation is not one to one, and that is why there are three methods</h2>
 *
 * <p>A prefix points to a single URI --otherwise a name could not be resolved-- but a URI can have
 * several prefixes bound at once:
 *
 * <pre>{@code
 * <root xmlns:a="http://shop" xmlns:b="http://shop"/>
 * }</pre>
 *
 * <p>Hence the asymmetry of the interface: {@link #getNamespaceURI} returns **the** URI, {@link
 * #getPrefix} returns **any one** of the bound prefixes, and {@link #getPrefixes} returns them all.
 * Whoever has to write the document back uses the second; whoever has to decide whether two names
 * are the same uses the first.
 *
 * <h2>The bindings that cannot be changed</h2>
 *
 * <p>Three pairs are fixed by the specification and every implementation has to answer them even if
 * the document does not declare them, because they are not declarable:
 *
 * <ul>
 *   <li>{@code xml} -&gt; {@link XMLConstants#XML_NS_URI};
 *   <li>{@code xmlns} -&gt; {@link XMLConstants#XMLNS_ATTRIBUTE_NS_URI};
 *   <li>the default prefix, when there is no {@code xmlns=} in force, -&gt;
 *       {@link XMLConstants#NULL_NS_URI}.
 * </ul>
 *
 * <h2>What is here</h2>
 *
 * <p>The three methods, which is the whole interface. There is no concrete implementation in this
 * library because there is none in the JDK either: a context without a parser feeding it has
 * nowhere to get declarations from, so the useful class is the one each StAX or DOM implementation
 * writes with the stack it already keeps.
 */
public interface NamespaceContext {

    /**
     * The namespace bound to {@code prefix} at this position.
     *
     * <p>It never returns null: an unbound prefix gives {@link XMLConstants#NULL_NS_URI}, the empty
     * string. That it returns the empty string and not null is what makes the result passable
     * straight to a {@link QName} constructor without checking.
     *
     * @param prefix the prefix to resolve; the empty string asks for the default namespace
     * @return the bound URI, or the empty string if there is none
     * @throws IllegalArgumentException if {@code prefix} is null
     */
    String getNamespaceURI(String prefix);

    /**
     * A prefix bound to {@code namespaceURI}, or null if there is none.
     *
     * <p>Which of the bound ones it returns is not defined when there are several, and that is
     * deliberate: the choice depends on what is closer in the stack, which is up to each
     * implementation.
     *
     * <p>It returns null --and not the empty string-- when there is none, unlike {@link
     * #getNamespaceURI}. The asymmetry is the original contract's and it has its logic: the empty
     * string **is** a valid prefix (the default one), so it cannot also mean "there is none".
     *
     * @param namespaceURI the namespace to look for
     * @return a prefix bound to it, or null
     * @throws IllegalArgumentException if {@code namespaceURI} is null
     */
    String getPrefix(String namespaceURI);

    /**
     * All the prefixes bound to {@code namespaceURI}, in a read-only iterator.
     *
     * <p>The iterator does not admit {@code remove}: removing a namespace declaration from a
     * half-read document means nothing.
     *
     * @param namespaceURI the namespace to look for
     * @return the bound prefixes; empty if there are none, never null
     * @throws IllegalArgumentException if {@code namespaceURI} is null
     */
    Iterator<String> getPrefixes(String namespaceURI);
}
