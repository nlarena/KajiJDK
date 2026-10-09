package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.NoType;

/**
 * The type-kind visitor for Java 9. See {@link TypeKindVisitor6} for the mechanism.
 *
 * <p>With modules in the language, `visitNoTypeAsModule` moves from `visitUnknown` to
 * `defaultAction`. This is the class for which the kinds branch needs a version 9 and the simple
 * branch does not: it is the only place where the `MODULE` pseudo-type is told apart from the other
 * three `NoType`s.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class TypeKindVisitor9<R, P> extends TypeKindVisitor8<R, P> {

    protected TypeKindVisitor9() {
        super(null);
    }

    protected TypeKindVisitor9(R defaultValue) {
        super(defaultValue);
    }

    public R visitNoTypeAsModule(NoType t, P p) {
        return this.defaultAction(t, p);
    }
}
