package javax.lang.model.type;

import javax.lang.model.UnknownEntityException;

public class UnknownTypeException extends UnknownEntityException {

    private static final long serialVersionUID = 269L;

    private transient TypeMirror type;
    private transient Object parameter;

    public UnknownTypeException(TypeMirror t, Object p) {
        // The JDK's plain `+ t` works now: the note said a String+Object concat compiled to nothing
        // because StringBuilder.append(Object) was missing; it exists and #114 is closed. The
        // valueOf gives the same message.
        super("Unknown type: \"" + String.valueOf(t) + "\"");
        type = t;
        parameter = p;
    }

    public TypeMirror getUnknownType() {
        return type;
    }

    public Object getArgument() {
        return parameter;
    }
}
