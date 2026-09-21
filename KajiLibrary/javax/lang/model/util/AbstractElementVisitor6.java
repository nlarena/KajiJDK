package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.UnknownElementException;

/**
 * KajiLibrary's javax.lang.model.util.AbstractElementVisitor6 — the base of the element visitor
 * family, and the place where the problem of evolving a visitor interface is solved.
 *
 * <h2>Why there is one class per language version</h2>
 *
 * <p>{@link ElementVisitor} has one method per kind of declaration the language knows. But the
 * language grows: modules arrived in 9 and record components in 16. Adding an abstract method to
 * the interface would have broken **every** visitor already written, and giving it a `default`
 * returning anything would have made an old visitor silently treat a module as if it were nothing.
 *
 * <p>The way out is this family. Each `AbstractElementVisitorN` fixes the contract of language
 * version `N`: **what existed in `N` is abstract and has to be implemented; what came later has a
 * body that throws**. Whoever extends `AbstractElementVisitor6` promises to handle what there was
 * in Java 6, and if a module reaches it finds out with an exception instead of getting a made-up
 * result. Whoever wants to handle modules extends `AbstractElementVisitor9`, where `visitModule` is
 * abstract and the compiler forces them to write it.
 *
 * <h2>Why a throwing `visitUnknown` is not a member that lies</h2>
 *
 * <p>It is the only honest answer. The visitor was written against a language that did not have
 * that construct; there is no value of `R` that means "I do not know what this is". {@link
 * UnknownElementException} says exactly that, and whoever wants something else overrides
 * `visitUnknown` — which is why it is not final.
 *
 * <p>{@link #visit(Element, Object)} is final, and on purpose: it is the entry point and its body
 * is always the same double dispatch against {@link Element#accept}. Overriding it could only break
 * it.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public abstract class AbstractElementVisitor6<R, P> implements ElementVisitor<R, P> {

    protected AbstractElementVisitor6() {
    }

    /**
     * The dispatch: the one that knows which kind of element it is, the implementation of `accept`.
     */
    public final R visit(Element e, P p) {
        return e.accept(this, p);
    }

    /** The same, with a null parameter, for visitors that do not care about `P`. */
    public final R visit(Element e) {
        return e.accept(this, null);
    }

    public R visitUnknown(Element e, P p) {
        throw new UnknownElementException(e, p);
    }

    // Modules are from 9 and record components from 16: both are unknown to a 6 visitor. The body
    // repeats that of `ElementVisitor`'s `default` instead of delegating to it with
    // `ElementVisitor.super`, because the frozen javac that builds this library still rejects that
    // syntax (COMPILER_FINDINGS #400; closed in the source-built javac, which accepts it -- checked
    // 2026-09-18). It is the same call: both forms end in whatever `visitUnknown` the subclass has.

    public R visitModule(ModuleElement e, P p) {
        return this.visitUnknown(e, p);
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.visitUnknown(e, p);
    }
}
