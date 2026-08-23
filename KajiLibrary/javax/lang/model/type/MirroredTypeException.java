package javax.lang.model.type;

public class MirroredTypeException extends MirroredTypesException {

    private static final long serialVersionUID = 269L;

    private transient TypeMirror type;

    public MirroredTypeException(TypeMirror type) {
        super("Attempt to access Class object for TypeMirror " + type.toString(), type);
        this.type = type;
    }

    public TypeMirror getTypeMirror() {
        return type;
    }
}
