package javax.lang.model.element;

// KajiLibrary's javax.lang.model.element.Modifier — the modifiers that may appear on a
// program element's declaration. Not all combinations are legal on every kind of element;
// the enum only names them, it does not police them. Declaration order matches the JDK's,
// which is the *canonical* order modifiers are written in source.
//
// toString() returns the modifier spelled as it is in source: lowercase, and NON_SEALED
// prints as "non-sealed". The JDK gets this from name().toLowerCase(Locale.US); KajiLibrary's
// java.lang.String has no toLowerCase, so the spellings are switched out explicitly. The
// observable behaviour is the same.
public enum Modifier {

    PUBLIC,
    PROTECTED,
    PRIVATE,
    ABSTRACT,
    DEFAULT,
    STATIC,
    SEALED,
    NON_SEALED,
    FINAL,
    TRANSIENT,
    VOLATILE,
    SYNCHRONIZED,
    NATIVE,
    STRICTFP;

    public String toString() {
        switch (this) {
            case PUBLIC:       return "public";
            case PROTECTED:    return "protected";
            case PRIVATE:      return "private";
            case ABSTRACT:     return "abstract";
            case DEFAULT:      return "default";
            case STATIC:       return "static";
            case SEALED:       return "sealed";
            case NON_SEALED:   return "non-sealed";
            case FINAL:        return "final";
            case TRANSIENT:    return "transient";
            case VOLATILE:     return "volatile";
            case SYNCHRONIZED: return "synchronized";
            case NATIVE:       return "native";
            case STRICTFP:     return "strictfp";
        }
        return name();
    }
}
