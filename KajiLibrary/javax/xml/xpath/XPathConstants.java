package javax.xml.xpath;

import javax.xml.namespace.QName;

/**
 * KajiLibrary's javax.xml.xpath.XPathConstants -- the five XPath result types.
 *
 * <p>XPath 1.0 has exactly four types --boolean, number, string and node-set-- and the platform
 * adds {@link #NODE} for the convenient case of wanting just one. They are requested by {@link
 * QName} and not by {@code Class} because the API is from 2004 and generics were only just
 * arriving; the modern way is {@code evaluateExpression} with a {@code Class}.
 *
 * <p>The namespace of all five is XSLT's, not XPath's. It is a historical oddity --XPath came out
 * as part of XSLT-- and it has to be respected: a {@code QName} built by hand with the "right"
 * namespace does not match these and the evaluation fails.
 *
 * <p>Asking for a type other than the one the expression produces is not an error: XPath
 * <b>converts</b>. An expression that returns a node-set, asked for as {@link #STRING}, gives the
 * text of the first node, and asked for as {@link #BOOLEAN} gives whether the set is non-empty. The
 * latter is the classic source of confusion: {@code evaluate(expr, doc, BOOLEAN)} on {@code //node}
 * answers "is there any" and not the node's content.
 */
public class XPathConstants {

    /** The namespace of the five types; it is XSLT's. See the class note. */
    private static final String NS = "http://www.w3.org/1999/XSL/Transform";

    /** A number. XPath 1.0 has no integers: everything is a {@code double}. */
    public static final QName NUMBER = new QName(NS, "NUMBER");

    /** A string. */
    public static final QName STRING = new QName(NS, "STRING");

    /** A boolean. */
    public static final QName BOOLEAN = new QName(NS, "BOOLEAN");

    /** A node-set, which arrives as an {@code org.w3c.dom.NodeList}. */
    public static final QName NODESET = new QName(NS, "NODESET");

    /** A single node, or null if there is none. */
    public static final QName NODE = new QName(NS, "NODE");

    /** The DOM object model, which is the only one the platform ships. */
    public static final String DOM_OBJECT_MODEL = "http://java.sun.com/jaxp/xpath/dom";

    /** Private: the class is constants only. */
    private XPathConstants() {
    }
}
