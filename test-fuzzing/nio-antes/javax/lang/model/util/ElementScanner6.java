package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;

/**
 * KajiLibrary's javax.lang.model.util.ElementScanner6 — the visitor that goes **down** the
 * structure instead of staying on the element it was given.
 *
 * <h2>How it differs from the `Simple` one</h2>
 *
 * <p>{@link SimpleElementVisitor6} visits **one** element: you give it a class and it answers about
 * the class. This one walks the tree: you give it a class and it visits the class, its fields, its
 * methods, the parameters of each method, and the nested types with all of theirs. It is what is
 * needed for "list all the public methods of this package", which is half of what an annotation
 * processor does.
 *
 * <p>The walk is in the `visitXxx` methods: each one, instead of falling into a funnel, calls
 * {@link #scan(Iterable, Object)} on its children. Which are "its children" depends on the element,
 * and there is an asymmetry worth seeing: a package and a type go down through
 * `getEnclosedElements()`, but an **executable goes down through its parameters**, not through its
 * enclosed elements. It is deliberate — a method's local variables are not in the model, and the
 * parameters are.
 *
 * <h2>How the results are combined, which is the surprising part</h2>
 *
 * <p>`scan(Iterable, P)` walks the children and returns **the result of the last one**, discarding
 * the earlier ones. It neither adds them up nor gathers them in a list: it could not, because `R`
 * is anything and there is no generic "combine". If the collection is empty it returns
 * `DEFAULT_VALUE`.
 *
 * <p>That makes the return value almost useless as it comes, and that is why the normal use of a
 * scanner is **accumulating in the visitor or in `P`** and not looking at what it returns. Whoever
 * wants a meaningful `R` overrides {@link #scan(Element, Object)} to combine; that is why it is not
 * final, while the other two forms of `scan` are.
 *
 * <p>`visitVariable` sets `RESOURCE_VARIABLE` aside for the same reason as in {@link
 * SimpleElementVisitor6}, and `visitRecordComponent` falls into `visitUnknown` because records are
 * from Java 14.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ElementScanner6<R, P> extends AbstractElementVisitor6<R, P> {

    /** What a walk that visited nothing returns. */
    protected final R DEFAULT_VALUE;

    @Deprecated(since = "9")
    protected ElementScanner6() {
        this.DEFAULT_VALUE = null;
    }

    @Deprecated(since = "9")
    protected ElementScanner6(R defaultValue) {
        this.DEFAULT_VALUE = defaultValue;
    }

    /**
     * Walks the children and returns the result of the last one. See the header for why only the
     * last.
     */
    public final R scan(Iterable<? extends Element> iterable, P p) {
        R result = this.DEFAULT_VALUE;
        for (Element e : iterable) {
            result = this.scan(e, p);
        }
        return result;
    }

    /** The step for one element. It is the point overridden to combine results or keep count. */
    public R scan(Element e, P p) {
        return e.accept(this, p);
    }

    /** The same, with a null parameter. */
    public final R scan(Element e) {
        return this.scan(e, null);
    }

    public R visitPackage(PackageElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }

    public R visitType(TypeElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }

    public R visitVariable(VariableElement e, P p) {
        if (e.getKind() != ElementKind.RESOURCE_VARIABLE) {
            return this.scan(e.getEnclosedElements(), p);
        }
        return this.visitUnknown(e, p);
    }

    // The parameters and not the enclosed elements: see the header.
    public R visitExecutable(ExecutableElement e, P p) {
        return this.scan(e.getParameters(), p);
    }

    public R visitTypeParameter(TypeParameterElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.visitUnknown(e, p);
    }
}
