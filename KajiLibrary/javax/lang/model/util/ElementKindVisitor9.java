package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;

/**
 * The element-kind visitor for Java 9. See {@link ElementKindVisitor6} for the mechanism.
 *
 * <p>`visitModule` enters the funnel. It does not open into `visitModuleAsXxx` because `MODULE` is
 * a single kind: there is nothing to dispatch, just as with type parameters.
 */
// RELEASE_14 and not RELEASE_9: the annotation states the latest language version this visitor
// **supports**, not the one in which it appeared. Between 9 and 14 no construct arrived that it
// cannot handle, so it is still adequate for both. It is the same value `TypeKindVisitor9` and
// `ElementScanner9` carry in the JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class ElementKindVisitor9<R, P> extends ElementKindVisitor8<R, P> {

    protected ElementKindVisitor9() {
        super(null);
    }

    protected ElementKindVisitor9(R defaultValue) {
        super(defaultValue);
    }

    public R visitModule(ModuleElement e, P p) {
        return this.defaultAction(e, p);
    }
}
