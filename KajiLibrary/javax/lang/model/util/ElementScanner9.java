package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;

/**
 * The element scanner for Java 9. See {@link ElementScanner6} for the mechanism.
 *
 * <p>`visitModule` stops throwing and goes down through the module's enclosed elements, which are
 * its packages.
 *
 * <p>The JDK leaves a doubt noted in this very method about whether the enclosed elements are right
 * for a module: a module also has **directives** — `requires`, `exports` —, and those are not
 * elements and no walk reaches them. It is worth knowing before trusting that scanning a module
 * covers it whole: it does not, and for the directives {@link ElementFilter} over `getDirectives()`
 * is needed.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_14)
public class ElementScanner9<R, P> extends ElementScanner8<R, P> {

    protected ElementScanner9() {
        super(null);
    }

    protected ElementScanner9(R defaultValue) {
        super(defaultValue);
    }

    public R visitModule(ModuleElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }
}
