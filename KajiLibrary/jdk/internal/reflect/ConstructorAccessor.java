package jdk.internal.reflect;

import java.lang.reflect.InvocationTargetException;

/**
 * KajiLibrary's jdk.internal.reflect.ConstructorAccessor -- the contract of "build an instance".
 *
 * <p>The same as {@link MethodAccessor} and for the same reason: in this VM the reflective
 * construction is the intrinsic {@code Intrinsic::ConstructorNewInstance}, which allocates the
 * object and runs the {@code <init>} from inside the interpreter. An accessor cannot be the
 * machinery because the machinery is below the floor of Java; it can be --and is-- the shape by
 * which it is named.
 *
 * <p>The intrinsic is in the interpreter and not in the bridge of natives for a reason that is
 * worth leaving said: it has to <strong>allocate</strong>, and the half-built object stays alive
 * while arbitrary bytecode runs, so the GC has to see it as a root. The bridge of natives does not
 * have that view.
 */
public interface ConstructorAccessor {

    /**
     * It allocates an instance and runs the constructor with {@code args}.
     *
     * @param args the arguments of the constructor
     * @return the instance already built
     * @throws InstantiationException if the class cannot be instantiated (abstract, interface)
     * @throws IllegalArgumentException if the arguments do not correspond
     * @throws InvocationTargetException wrapping whatever the constructor threw
     */
    Object newInstance(Object[] args)
            throws InstantiationException, IllegalArgumentException, InvocationTargetException;
}
