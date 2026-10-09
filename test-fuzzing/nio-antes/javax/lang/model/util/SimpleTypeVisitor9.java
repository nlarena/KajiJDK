package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The simple type visitor for Java 9. See {@link SimpleTypeVisitor6} for the mechanism.
 *
 * <p>The `MODULE` pseudo-type Java 9 brought comes in through `visitNoType`, which was already in
 * the funnel since the 6 one, so there is nothing to reroute. It is {@link TypeKindVisitor9} — the
 * one that does break `visitNoType` down by kind — that has to do something with it.
 */
// RELEASE_14 and not RELEASE_9: the annotation states the latest language version this visitor
// **supports**, not the one in which it appeared. Between 9 and 14 no construct arrived that it
// cannot handle, so it is still adequate for both. It is the same value `TypeKindVisitor9` and
// `ElementScanner9` carry in the JDK.
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class SimpleTypeVisitor9<R, P> extends SimpleTypeVisitor8<R, P> {

    protected SimpleTypeVisitor9() {
        super(null);
    }

    protected SimpleTypeVisitor9(R defaultValue) {
        super(defaultValue);
    }
}
