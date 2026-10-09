package javax.xml.crypto.dsig.spec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.XPathType -- an XPath expression with its set operation.
 *
 * <p>It is the piece of {@link XPathFilter2ParameterSpec}: each expression comes with an operation
 * that says what to do with what it selects.
 *
 * <ul>
 *   <li>{@link Filter#INTERSECT} -- keeps what is in both;
 *   <li>{@link Filter#SUBTRACT} -- removes what is selected, <b>with its whole subtree</b>;
 *   <li>{@link Filter#UNION} -- adds it.
 * </ul>
 *
 * <p>Working by subtrees and not by node is the underlying difference from the original XPath
 * transform: selecting an element takes its descendants along, which is what one expects and what
 * makes the filtering fast.
 *
 * <p>The order matters: the expressions are applied in sequence on the accumulated result, so
 * subtracting before or after joining gives different sets.
 */
public class XPathType {

    /** The expression. */
    private final String expression;

    /** What to do with what it selects. */
    private final Filter filter;

    /** Prefix to namespace; never null. */
    private final Map<String, String> nsMap;

    /**
     * Without namespaces.
     *
     * @throws NullPointerException if either is null
     */
    public XPathType(String expression, Filter filter) {
        if (expression == null || filter == null) {
            throw new NullPointerException("expression and filter cannot be null");
        }
        this.expression = expression;
        this.filter = filter;
        this.nsMap = Collections.emptyMap();
    }

    /**
     * With the declared prefixes; the map is copied.
     *
     * @throws NullPointerException if either is null
     */
    public XPathType(String expression, Filter filter, Map<String, String> namespaceMap) {
        if (expression == null || filter == null || namespaceMap == null) {
            throw new NullPointerException(
                "expression, filter and namespaceMap cannot be null");
        }
        this.expression = expression;
        this.filter = filter;
        this.nsMap = Collections.unmodifiableMap(new HashMap<String, String>(namespaceMap));
    }

    /** The expression. */
    public String getExpression() {
        return this.expression;
    }

    /** What to do with what it selects. */
    public Filter getFilter() {
        return this.filter;
    }

    /** The declared prefixes. Unmodifiable. */
    public Map<String, String> getNamespaceMap() {
        return this.nsMap;
    }

    /**
     * The three set operations.
     *
     * <p>It is not an enum; it is three constants with a private constructor, the hand-made enum
     * pattern of that time. (The note said the reason is that the class is from 2005; Java 5
     * already had enums in 2004, but JSR 105 was also meant to run on J2SE 1.4, which had none.)
     */
    public static class Filter {

        /** Keeps what is in both. */
        public static final Filter INTERSECT = new Filter("intersect");

        /** Removes it, with its whole subtree. */
        public static final Filter SUBTRACT = new Filter("subtract");

        /** Adds it. */
        public static final Filter UNION = new Filter("union");

        private final String operation;

        private Filter(String operation) {
            this.operation = operation;
        }

        /** The name of the operation, as it goes in the XML. */
        public String toString() {
            return this.operation;
        }
    }
}
