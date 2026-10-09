package javax.lang.model.element;

import javax.lang.model.UnknownEntityException;
import javax.lang.model.element.Element;

// KajiLibrary's javax.lang.model.element.UnknownElementException — thrown from an
// ElementVisitor's visitUnknown when it meets a kind of element it was not written to
// handle, which is how a visitor compiled against an older language version reports that
// the model has moved on underneath it.
//
// Both fields are transient because neither an Element nor an arbitrary visitor argument is
// required to be serializable; after a round trip they read back as null, exactly as in the
// JDK.
public class UnknownElementException extends UnknownEntityException {

    private static final long serialVersionUID = 269L;

    private transient Element element;
    private transient Object parameter;

    public UnknownElementException(Element e, Object p) {
        super("Unknown element: \"" + String.valueOf(e) + "\"");
        element = e;
        parameter = p;
    }

    public Element getUnknownElement() {
        return element;
    }

    public Object getArgument() {
        return parameter;
    }
}
