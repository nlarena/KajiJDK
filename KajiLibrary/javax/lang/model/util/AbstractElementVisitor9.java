package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;

/**
 * The element visitor for Java 9. See {@link AbstractElementVisitor6} for the mechanism.
 *
 * <p>Java 9 brought **modules**, the first new kind of declaration since the model exists. That is
 * why here `visitModule` stops having a body that throws and becomes **abstract**: extending this
 * class instead of the 8 one is exactly the way for the compiler to force you to decide what to do
 * with a module, instead of discovering it at run time with an exception.
 */
// RELEASE_14 and not RELEASE_9: the annotation states the latest language version this visitor
// **supports**, not the one in which it appeared. Between 9 and 14 no construct arrived that it
// cannot handle, so it is still adequate for both. It is the same value `TypeKindVisitor9` and
// `ElementScanner9` carry in the JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public abstract class AbstractElementVisitor9<R, P> extends AbstractElementVisitor8<R, P> {

    protected AbstractElementVisitor9() {
        super();
    }

    public abstract R visitModule(ModuleElement e, P p);
}
