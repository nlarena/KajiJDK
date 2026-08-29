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
        // NOTA: el `+ types` directo del JDK real no se puede usar acá — el concat
        // String+Object se compila a nada en silencio (falta StringBuilder.append(Object)).
        super("Attempt to access Class objects for TypeMirrors " + String.valueOf(types));
        this.types = types;
    }

    public List<? extends TypeMirror> getTypeMirrors() {
        return types;
    }
}
