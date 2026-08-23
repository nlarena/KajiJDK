package javax.lang.model.type;

import javax.lang.model.UnknownEntityException;

public class UnknownTypeException extends UnknownEntityException {

    private static final long serialVersionUID = 269L;

    private transient TypeMirror type;
    private transient Object parameter;

    public UnknownTypeException(TypeMirror t, Object p) {
        // NOTA: el `+ t` directo del JDK real se compila a nada en silencio
        // (concat String+Object sin StringBuilder.append(Object)); de ahí el valueOf.
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
