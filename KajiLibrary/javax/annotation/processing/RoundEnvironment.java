package javax.annotation.processing;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

// What a processor sees of **one** round (JSR 269 §RoundEnvironment). It is built afresh each round:
// asking an old one does not give the new elements.
//
// This project's implementor is `RoundEnvironmentImpl`.
public interface RoundEnvironment {

    /**
     * Whether this is the **final** round, the one that runs after nothing more has been generated.
     * In it a processor may do its global verification, but generating no longer makes sense: there
     * is no round left to process what was generated.
     */
    boolean processingOver();

    /**
     * Whether anyone reported an error in the **previous** round. It serves to avoid chaining derived
     * errors on top of a model already known to be broken.
     */
    boolean errorRaised();

    /** This round's root types: what the tool is going to compile. */
    Set<? extends Element> getRootElements();

    /** The elements annotated with `a`, searching the roots and what is nested in them. */
    Set<? extends Element> getElementsAnnotatedWith(TypeElement a);

    /**
     * The same, but naming the annotation type by its `Class`. It is the convenient variant when the
     * processor has the annotation on its own classpath; the `TypeElement` one is the general case
     * (an annotation may not be loaded).
     */
    Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> a);

    // The two below are `default` in the contract: the union over several annotations is defined
    // entirely in terms of the one-at-a-time lookup, so there is nothing an implementor could know
    // better. They are written without streams, which is what there is here.

    /** The union of {@link #getElementsAnnotatedWith(TypeElement)} over all of `annotations`. */
    default Set<? extends Element> getElementsAnnotatedWithAny(TypeElement... annotations) {
        // `LinkedHashSet` and not `HashSet`: the order is determined by that of `annotations`, so two
        // equal runs give the same sequence and the messages do not dance about. Immutable on the way
        // out, because the set is manufactured by the contract and is nobody's to modify.
        Set<Element> result = new LinkedHashSet<Element>();
        for (TypeElement a : annotations) {
            result.addAll(this.getElementsAnnotatedWith(a));
        }
        return Collections.unmodifiableSet(result);
    }

    /** The union of {@link #getElementsAnnotatedWith(Class)} over all of `annotations`. */
    default Set<? extends Element> getElementsAnnotatedWithAny(
            Set<Class<? extends Annotation>> annotations) {
        Set<Element> result = new LinkedHashSet<Element>();
        for (Class<? extends Annotation> a : annotations) {
            result.addAll(this.getElementsAnnotatedWith(a));
        }
        return Collections.unmodifiableSet(result);
    }
}
