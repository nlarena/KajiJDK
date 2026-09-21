package javax.xml.crypto.dsig.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * KajiLibrary's javax.xml.crypto.dsig.spec.XPathFilter2ParameterSpec -- a sequence of XPath
 * filters.
 *
 * <p>The parameters of the XPath Filter 2.0 transform, which replaces the original one for being
 * much faster: it works by subtrees instead of node by node. See {@link XPathType} for the three
 * operations.
 *
 * <p>The list is a <b>sequence</b> and not a set: they are applied in order on the accumulated
 * result. Changing the order changes what is signed.
 */
public final class XPathFilter2ParameterSpec implements TransformParameterSpec {

    /** The filters, in order. Unmodifiable. */
    private final List<XPathType> xPathList;

    /**
     * @param xPathList the filters, in the order they are applied
     * @throws NullPointerException if the list is null
     * @throws IllegalArgumentException if it is empty: a sequence without filters selects nothing
     * @throws ClassCastException if some element is not an {@link XPathType}
     */
    public XPathFilter2ParameterSpec(List<XPathType> xPathList) {
        if (xPathList == null) {
            throw new NullPointerException("xPathList cannot be null");
        }
        if (xPathList.isEmpty()) {
            throw new IllegalArgumentException("xPathList cannot be empty");
        }
        List<XPathType> copy = new ArrayList<XPathType>();
        int i = 0;
        while (i < xPathList.size()) {
            Object x = xPathList.get(i);
            if (!(x instanceof XPathType)) {
                throw new ClassCastException("not an XPathType: " + x);
            }
            copy.add((XPathType) x);
            i = i + 1;
        }
        this.xPathList = Collections.unmodifiableList(copy);
    }

    /** The filters, in order. Unmodifiable. */
    public List<XPathType> getXPathList() {
        return this.xPathList;
    }
}
