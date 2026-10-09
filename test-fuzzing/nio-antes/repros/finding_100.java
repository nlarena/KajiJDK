package repro23;

import java.lang.annotation.Annotation;

// Finding #100 — a bounded type variable erases to java.lang.Object instead of to its leftmost bound
// (JLS §4.6). Our javac emits Object for every type variable, ignoring `extends` bounds. The JDK
// erases `A extends Annotation` to Annotation and `U extends Comparable<U>` to Comparable.
//
//   our javac:  single(A) -> (Ljava/lang/Object;)V           cmp(U) -> (Ljava/lang/Object;)V
//   JDK javac:  single(A) -> (Ljava/lang/annotation/Annotation;)V   cmp(U) -> (Ljava/lang/Comparable;)V
//   both agree: obj(T) -> (Ljava/lang/Object;)V   (unbounded T correctly erases to Object)
//
// Impact: any signature using a bounded type variable gets the wrong descriptor. Surfaced on
// jakarta.validation.ConstraintValidator<A extends Annotation, T>.initialize(A). Breaks descriptor
// fidelity (shape gate) and, more seriously, overriding of such methods (an override written with the
// concrete type won't match the Object-erased inherited descriptor). Not caught by the gate on its own
// — it shows up as a MISMATCH against the reference.
public interface finding_100<A extends Annotation, T> {

    void single(A a);                          // should erase to (Annotation)V, we emit (Object)V

    <U extends Comparable<U>> void cmp(U u);    // should erase to (Comparable)V, we emit (Object)V

    void obj(T t);                             // unbounded, correctly (Object)V on both
}
