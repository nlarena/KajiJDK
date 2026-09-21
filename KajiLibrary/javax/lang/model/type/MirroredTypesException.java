package javax.lang.model.type;

import java.util.ArrayList;
import java.util.List;

public class MirroredTypesException extends RuntimeException {

    private static final long serialVersionUID = 269L;

    transient List<? extends TypeMirror> types;

    MirroredTypesException(String message, TypeMirror type) {
        super(message);
        List<TypeMirror> tmp = new ArrayList<TypeMirror>();
        tmp.add(type);
        this.types = tmp;
    }

    public MirroredTypesException(List<? extends TypeMirror> types) {
        // The JDK's plain `+ types` works now: the note said a String+Object concat compiled to
        // nothing because StringBuilder.append(Object) was missing; it exists and #114 is closed.
        // The valueOf gives the same message.
        super("Attempt to access Class objects for TypeMirrors " + String.valueOf(types));
        this.types = types;
    }

    public List<? extends TypeMirror> getTypeMirrors() {
        return types;
    }
}
