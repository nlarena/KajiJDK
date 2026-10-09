package javax.annotation.processing;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

// Minimal support for APT's round loop (JSR 269, phase 2): a concrete implementation of
// RoundEnvironment that the driver builds once per round. `over` tells the final round apart
// (processingOver == true), which is the only thing the MVP needs to expose; the sets of root /
// annotated elements return empty until element reification exists (phase 3).
//
// `errorRaised()` always returns `false`, and that is the correct answer and not a placeholder: this
// project's round loop **aborts** as soon as an exception escapes a `process()`, so there is never a
// next round that could see an error from the previous one. Besides, the `Messager` here writes to
// the trace console and keeps no error count, so there is no other source an error could come
// from.
public class RoundEnvironmentImpl implements RoundEnvironment {
    private final boolean over;
    public RoundEnvironmentImpl(boolean over) { this.over = over; }
    public boolean processingOver() { return over; }
    public boolean errorRaised() { return false; }
    public Set<? extends Element> getRootElements() { return new HashSet<Element>(); }
    public Set<? extends Element> getElementsAnnotatedWith(TypeElement a) { return new HashSet<Element>(); }
    public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> a) { return new HashSet<Element>(); }
}
