package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;

/**
 * The simple element visitor for Java 9. See {@link SimpleElementVisitor6} for the mechanism.
 *
 * <p>With modules in the language, `visitModule` enters the funnel: it inherits from {@link
 * AbstractElementVisitor6} a body that throws, and here it goes to `defaultAction`. Note that this
 * class does **not** extend {@link AbstractElementVisitor9} — it extends `SimpleElementVisitor8` —
 * so `visitModule` does not become abstract. It is the difference between the two branches: the
 * abstract one forces you to decide, the simple one decides for you.
 */
// RELEASE_14 and not RELEASE_9: the annotation states the latest language version this visitor
// **supports**, not the one in which it appeared. Between 9 and 14 no construct arrived that it
// cannot handle, so it is still adequate for both. It is the same value `TypeKindVisitor9` and
// `ElementScanner9` carry in the JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class SimpleElementVisitor9<R, P> extends SimpleElementVisitor8<R, P> {

    protected SimpleElementVisitor9() {
        super(null);
    }

    protected SimpleElementVisitor9(R defaultValue) {
        super(defaultValue);
    }

    public R visitModule(ModuleElement e, P p) {
        return this.defaultAction(e, p);
    }
}
