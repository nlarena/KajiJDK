package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * KajiLibrary's javax.lang.model.util.SimpleElementVisitor6 — the element visitor for which almost
 * every case gives the same.
 *
 * <h2>What it adds over the abstract one</h2>
 *
 * <p>{@link AbstractElementVisitor6} forces writing the five `visitXxx`. But the typical visitor
 * does not want five: it wants one. "Give me the name of any element", "count everything that is
 * public". Writing five methods with the same body is noise.
 *
 * <p>This class puts in a funnel: each `visitXxx` calls {@link #defaultAction}, and whoever extends
 * overrides **a single thing** — `defaultAction` for the general case, and also the particular
 * `visitXxx` they want to treat differently. The `defaultAction` here returns `DEFAULT_VALUE`, the
 * value passed to the constructor, for the most common case of all: a visitor that is only
 * interested in one kind of element and wants a fixed value for the rest.
 *
 * <h2>Why `visitVariable` does not always call `defaultAction`</h2>
 *
 * <p>This is the only place where "everything falls into the same funnel" has an exception, and it
 * is not a whim. Java 7 added `RESOURCE_VARIABLE` for try-with-resources. It is a
 * `VariableElement`, so it reaches `visitVariable` **without the signature changing** — and a
 * visitor written for Java 6 never decided what to do with a resource variable, because they did
 * not exist.
 *
 * <p>Sending it to `defaultAction` would silently return the answer for a case that was never
 * considered. That is why it goes to `visitUnknown`, which throws: a kind the visitor cannot have
 * foreseen is exactly what `visitUnknown` means. {@link SimpleElementVisitor7} removes it, because
 * there it did exist.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class SimpleElementVisitor6<R, P> extends AbstractElementVisitor6<R, P> {

    /** What `defaultAction` returns until it is overridden. */
    protected final R DEFAULT_VALUE;

    @Deprecated(since = "9")
    protected SimpleElementVisitor6() {
        this.DEFAULT_VALUE = null;
    }

    @Deprecated(since = "9")
    protected SimpleElementVisitor6(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /** The funnel. Overriding it is the way to treat all elements alike. */
    protected R defaultAction(Element e, P p) {
        return this.DEFAULT_VALUE;
    }

    public R visitPackage(PackageElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitType(TypeElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitVariable(VariableElement e, P p) {
        // See the header: a resource variable is from Java 7 and this visitor is from Java 6.
        if (e.getKind() != ElementKind.RESOURCE_VARIABLE) {
            return this.defaultAction(e, p);
        }
        return this.visitUnknown(e, p);
    }

    public R visitExecutable(ExecutableElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitTypeParameter(TypeParameterElement e, P p) {
        return this.defaultAction(e, p);
    }
}
