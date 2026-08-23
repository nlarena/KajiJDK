package javax.lang.model.element;

// KajiLibrary's javax.lang.model.element.ElementKind — what an Element *is*. The
// discriminator you switch on when the visitor is more machinery than the job needs.
//
// Declaration order matches the JDK's, because ordinal() is observable through EnumSet and
// EnumMap and a reordering would silently change their iteration order.
//
// The predicates are the JDK's, and two of them are narrower than their names suggest:
// isClass() is true for CLASS, ENUM and RECORD but *not* for the interface-ish kinds, and
// isField() is true for FIELD and ENUM_CONSTANT only — a local variable is not a field.
public enum ElementKind {

    PACKAGE,

    ENUM,
    CLASS,
    ANNOTATION_TYPE,
    INTERFACE,

    ENUM_CONSTANT,
    FIELD,
    PARAMETER,
    LOCAL_VARIABLE,
    EXCEPTION_PARAMETER,

    METHOD,
    CONSTRUCTOR,
    STATIC_INIT,
    INSTANCE_INIT,

    TYPE_PARAMETER,

    OTHER,

    RESOURCE_VARIABLE,

    MODULE,

    RECORD,
    RECORD_COMPONENT,
    BINDING_VARIABLE;

    public boolean isClass() {
        return this == CLASS || this == ENUM || this == RECORD;
    }

    public boolean isInterface() {
        return this == INTERFACE || this == ANNOTATION_TYPE;
    }

    public boolean isDeclaredType() {
        return isClass() || isInterface();
    }

    public boolean isField() {
        return this == FIELD || this == ENUM_CONSTANT;
    }

    public boolean isExecutable() {
        return this == METHOD || this == CONSTRUCTOR
            || this == STATIC_INIT || this == INSTANCE_INIT;
    }

    public boolean isInitializer() {
        return this == STATIC_INIT || this == INSTANCE_INIT;
    }

    public boolean isVariable() {
        return this == ENUM_CONSTANT || this == FIELD || this == PARAMETER
            || this == LOCAL_VARIABLE || this == EXCEPTION_PARAMETER
            || this == RESOURCE_VARIABLE || this == BINDING_VARIABLE;
    }
}
