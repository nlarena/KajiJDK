package javax.lang.model.element;

import javax.lang.model.UnknownEntityException;
import javax.lang.model.element.AnnotationValue;

// KajiLibrary's javax.lang.model.element.UnknownAnnotationValueException — the
// AnnotationValueVisitor counterpart of UnknownElementException: thrown from visitUnknown
// when the visitor meets a kind of annotation value it does not know.
public class UnknownAnnotationValueException extends UnknownEntityException {

    private static final long serialVersionUID = 269L;

    private transient AnnotationValue av;
    private transient Object parameter;

    public UnknownAnnotationValueException(AnnotationValue av, Object p) {
        super("Unknown annotation value: \"" + String.valueOf(av) + "\"");
        this.av = av;
        this.parameter = p;
    }

    public AnnotationValue getUnknownAnnotationValue() {
        return av;
    }

    public Object getArgument() {
        return parameter;
    }
}
