package javax.xml.xpath;

import javax.xml.namespace.QName;
import org.w3c.dom.Node;

/**
 * KajiLibrary's javax.xml.xpath.XPathEvaluationResult -- a result that knows its own type.
 *
 * <p>It is what {@code evaluateExpression} returns when the expected type is not given. It carries
 * the value and its type together, which is the only way to answer without forcing the caller to
 * guess.
 *
 * <p>It exists because the old way --{@code evaluate} with a {@link QName}-- forces you to decide
 * the type <b>before</b> evaluating, and there are expressions whose type depends on the document.
 * With this you evaluate first and decide afterwards.
 */
public interface XPathEvaluationResult<T> {

    /** Which type the value is. */
    XPathResultType type();

    /** The value. */
    T value();

    /**
     * The types XPath can produce, as an enum.
     *
     * <p>It is the modern version of the {@link XPathConstants} constants, which are {@code
     * QName}s. The two sets coexist and {@link #getQNameType} is the bridge between them.
     *
     * <p>{@link #ANY} is not a result type but a request: it means "whatever comes out".
     *
     * <p>Unlike the JDK, the constants here carry neither a {@code QName} nor a {@code Class}. In
     * the JDK 25 sources {@code ANY} is bound to the {@code QName} {@code any} in the XSLT
     * namespace and to {@code XPathEvaluationResult.class}, so {@code
     * getQNameType(XPathEvaluationResult.class)} returns that {@code QName} there; here it returns
     * null. (An earlier note said {@code ANY} has no {@code QName}; that holds for this library,
     * not for the JDK.)
     */
    public static enum XPathResultType {

        /** Any; used when asking, not when receiving. */
        ANY,

        /** A boolean. */
        BOOLEAN,

        /** A number; in XPath 1.0 always a {@code double}. */
        NUMBER,

        /** A string. */
        STRING,

        /** A node-set. */
        NODESET,

        /** A single node. */
        NODE;

        /**
         * The {@link QName} that corresponds to that Java class.
         *
         * <p>The mapping has two surprises worth keeping in mind:
         *
         * <ul>
         *   <li>any {@link Number} gives {@link XPathConstants#NUMBER}, so asking for {@code
         *       Integer.class} works even though XPath 1.0 has no integers;
         *   <li>{@code org.w3c.dom.NodeList} returns <b>null</b>, even though it is exactly the
         *       type the old way uses for a node-set. The modern node-set is {@link XPathNodes},
         *       and this table is the modern API's.
         * </ul>
         *
         * <p>This differs from the JDK 25 sources in two places. There, only {@code Number}, {@code
         * Double}, {@code Integer} and {@code Long} map to {@code NUMBER} ({@code Float}, {@code
         * Short} or {@code BigDecimal} give null), and every subtype of {@code Node} --{@code
         * Element.class}, say-- maps to {@code NODE}, where here only {@code Node.class} itself
         * does.
         *
         * @return null if that class is not an XPath result type
         */
        public static QName getQNameType(Class<?> clsType) {
            if (clsType == null) {
                return null;
            }
            if (Boolean.class.equals(clsType)) {
                return XPathConstants.BOOLEAN;
            }
            if (String.class.equals(clsType)) {
                return XPathConstants.STRING;
            }
            if (XPathNodes.class.equals(clsType)) {
                return XPathConstants.NODESET;
            }
            if (Node.class.equals(clsType)) {
                return XPathConstants.NODE;
            }
            // Any number, not just Double; see the method note.
            if (Number.class.isAssignableFrom(clsType)) {
                return XPathConstants.NUMBER;
            }
            return null;
        }
    }
}
