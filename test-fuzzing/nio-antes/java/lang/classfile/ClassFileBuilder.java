package java.lang.classfile;

import java.lang.classfile.constantpool.ConstantPoolBuilder;
import java.util.function.Consumer;

/**
 * Where what is going to be a `.class` gets written: it is handed elements and it accumulates them.
 *
 * <p>It is a {@link Consumer} of its elements, and that is not decoration: it makes a builder
 * passable wherever a consumer is expected --to a `forEach`, to a `Stream`-- and makes copying a
 * whole model be `model.forEach(builder)`.
 *
 * <p>The two type parameters are the element it accepts and the builder itself, so that
 * {@link #with} returns the concrete type and the calls chain.
 */
public interface ClassFileBuilder<E extends ClassFileElement, B extends ClassFileBuilder<E, B>>
        extends Consumer<E> {

    /** It adds that element and returns this builder, so calls chain. */
    B with(E e);

    /**
     * The constant pool of what is being written.
     *
     * <p>It is exposed because whoever writes instructions needs entries: an `invokevirtual` carries
     * an index, not a name. The {@link CodeBuilder} factories taking a `ClassDesc` use it inside, so
     * it only has to be touched for something those factories do not cover.
     */
    ConstantPoolBuilder constantPool();

    /** The same as {@link #with}, so the builder can be passed as a {@link Consumer}. */
    default void accept(E e) {
        this.with(e);
    }

    /**
     * It walks that model applying that transformation, and writes the result here.
     *
     * <p>The order --`atStart`, one `accept` per element, `atEnd`-- is part of the contract: it is
     * what lets a transformation add something that was not in the original. See
     * {@link ClassFileTransform}.
     */
    default B transform(CompoundElement<E> model, ClassFileTransform<?, E, B> transform) {
        // The cast of `this` to `B`: the interface cannot declare that `this` is a `B`, and every
        // implementation is one by construction --that is what the parameter means-- so it cannot
        // fail.
        B self = (B) this;
        transform.atStart(self);
        for (E e : model) {
            transform.accept(self, e);
        }
        transform.atEnd(self);
        return self;
    }
}
