package javax.lang.model.element;
public enum ElementKind {
    PACKAGE, CLASS, INTERFACE, ENUM, RECORD, ANNOTATION_TYPE,
    METHOD, CONSTRUCTOR, FIELD, ENUM_CONSTANT, PARAMETER, TYPE_PARAMETER, OTHER;
    public boolean isClass() { return this == CLASS || this == ENUM || this == RECORD; }
    public boolean isInterface() { return this == INTERFACE || this == ANNOTATION_TYPE; }
    public boolean isField() { return this == FIELD || this == ENUM_CONSTANT; }
}
