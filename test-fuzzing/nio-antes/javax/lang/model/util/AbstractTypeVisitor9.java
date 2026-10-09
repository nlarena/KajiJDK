package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The type visitor for Java 9. See {@link AbstractTypeVisitor6} for the mechanism.
 *
 * <p>Java 9 added modules, but **not a new form of type**: a module is not a type. What it added
 * was the `MODULE` pseudo-type, and that is one more `TypeKind` inside `NoType`, which already had
 * its `visitNoType`. That is why this class does not change the contract — the asymmetry with
 * {@link AbstractElementVisitor9}, where modules did force a new method, is precisely the point: a
 * module is a declaration, not a type.
 */
// RELEASE_14 and not RELEASE_9: the annotation states the latest language version this visitor
// **supports**, not the one in which it appeared. Between 9 and 14 no construct arrived that it
// cannot handle, so it is still adequate for both. It is the same value `TypeKindVisitor9` and
// `ElementScanner9` carry in the JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public abstract class AbstractTypeVisitor9<R, P> extends AbstractTypeVisitor8<R, P> {

    protected AbstractTypeVisitor9() {
        super();
    }
}
