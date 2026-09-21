package java.lang.classfile;

/**
 * A transformation: it takes the elements of something that already exists and writes out what that
 * something is to become.
 *
 * <p>The shape is what gives this API all its sense. A transformer does **not** modify a model --
 * models are immutable-- but is walked over it element by element and, for each one, decides what to
 * put into the copy's builder. Letting it through as it is, changing it, dropping it, or emitting
 * several in its place: all four are the same gesture.
 *
 * <p><strong>{@link #atStart} and {@link #atEnd} are the half people forget.</strong> Without them a
 * transformer can only react to what it sees, and there are two things that cannot be done that way:
 * adding something the original did not have (that goes in `atStart` or in `atEnd`, depending on
 * where it has to end up) and closing a state that has been accumulating (a counter, a table of what
 * was seen). A stateful transformer is made with `ofStateful`, which gives a new one per use -- see
 * {@link ClassTransform#ofStateful}.
 *
 * <p>The three type parameters read like this: `C` is the transformer itself (so that
 * {@link #andThen} returns its type and not this one), `E` the element it consumes and `B` the
 * builder it writes into. The four specialisations --{@link ClassTransform},
 * {@link MethodTransform}, {@link FieldTransform}, {@link CodeTransform}-- fix all three.
 *
 * <p>In the JDK this interface is `sealed`; here it is not, for the same reason as
 * {@link ClassFileElement}: sealing it would force the public package to name its internal
 * implementations.
 */
public interface ClassFileTransform<C extends ClassFileTransform<C, E, B>,
        E extends ClassFileElement, B extends ClassFileBuilder<E, B>> {

    /** What to do with this element. The usual thing is writing something into `builder`. */
    void accept(B builder, E element);

    /**
     * It is called **before** the first element.
     *
     * <p>Empty by default. It is where whatever has to end up at the start of the copy goes, and
     * where a stateful transformer initialises its state.
     */
    default void atStart(B builder) {
    }

    /**
     * It is called **after** the last one.
     *
     * <p>Empty by default. It is where whatever has to end up at the end goes, and where a stateful
     * transformer dumps what it gathered.
     */
    default void atEnd(B builder) {
    }

    /**
     * This transformer and then that other one, as a single one.
     *
     * <p>What comes out of this one goes into the next, not into the final builder: chaining two is
     * applying the second **to the result** of the first, not running both over the original. That is
     * why the order matters, and why `a.andThen(b)` is not the same as `b.andThen(a)`.
     */
    C andThen(C next);
}
