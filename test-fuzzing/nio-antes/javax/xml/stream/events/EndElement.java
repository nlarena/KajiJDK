package javax.xml.stream.events;

import java.util.Iterator;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.stream.events.EndElement -- the end of an element.
 *
 * <h2>Why an end carries namespaces</h2>
 *
 * <p>The tag {@code </a:x>} declares nothing, so the legitimate question is what {@link
 * #getNamespaces()} does here. The answer is that what it returns are not new declarations but the
 * ones that <b>stop holding</b> at this point: the ones the corresponding {@link StartElement} had
 * introduced and that, when the element closes, go out of scope.
 *
 * <p>It serves the only thing needed when closing: a writer that keeps its own prefix stack needs
 * to know which to pop, and a consumer building a model needs to know when a prefix means something
 * else again. Without this the stack would have to be kept outside, which is exactly the state the
 * event model exists so as not to force anyone to keep.
 */
public interface EndElement extends XMLEvent {

    /**
     * The name of the element being closed.
     *
     * <p>It is the same {@link QName} the {@link StartElement} carried: same namespace, same local
     * name. The prefix also matches, because XML requires tags to be written the same, but remember
     * that the prefix is not part of {@link QName#equals}.
     *
     * @return the qualified name; never null
     */
    QName getName();

    /**
     * The namespaces that go out of scope when this element closes.
     *
     * @return an iterator of {@link Namespace}; empty if the element had declared none, never null
     */
    Iterator<Namespace> getNamespaces();
}
