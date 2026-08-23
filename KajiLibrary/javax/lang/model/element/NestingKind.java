package javax.lang.model.element;

// KajiLibrary's javax.lang.model.element.NestingKind — where a class or interface
// declaration sits relative to other declarations. TOP_LEVEL is the only one that is not
// nested; MEMBER, LOCAL and ANONYMOUS all are, which is exactly what isNested() reports.
public enum NestingKind {

    TOP_LEVEL,
    MEMBER,
    LOCAL,
    ANONYMOUS;

    public boolean isNested() {
        return this != TOP_LEVEL;
    }
}
