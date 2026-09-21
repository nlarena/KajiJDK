package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.CDATASection -- a `&lt;![CDATA[...]]&gt;` section.
 *
 * <p>It adds **no** member over `Text`, and it is right that it should be so: for the data model a
 * CDATA is text and nothing else. The only thing that changes is the **serialisation** --the text
 * goes out without escaping `&amp;` nor `&lt;`-- and that is not an operation on the node. The
 * interface exists so that `getNodeType()` can tell it apart and so that the serialiser knows how
 * to write it.
 *
 * <p>The practical consequence is that `normalize()` may join a CDATA with the `Text` next to it
 * and lose the distinction, which is exactly what the standard allows.
 */
public interface CDATASection extends Text {
}
