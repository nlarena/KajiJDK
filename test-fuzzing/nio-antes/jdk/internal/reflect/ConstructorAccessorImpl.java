package jdk.internal.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * The {@link ConstructorAccessor} {@link ReflectionFactory#newConstructorAccessor} manufactures.
 *
 * <p>It delegates to {@link Constructor#newInstance}, that is to
 * {@code Intrinsic::ConstructorNewInstance}. The same holds as in {@link MethodAccessorImpl}: the
 * arrow goes the other way round than in HotSpot and that is why it does not close into a cycle.
 *
 * <p>The only thing it adds of its own is the check of instantiability. The JDK does not use this
 * accessor for an abstract class or an interface: {@code ReflectionFactory} returns for those an
 * {@code InstantiationExceptionConstructorAccessorImpl}, whose only job is to throw. Here that
 * distinction does not have to be a class apart because the check is two lines, but the observable
 * behaviour has to be the same: {@link InstantiationException} and not whatever the VM answers on
 * trying to allocate something that cannot be allocated.
 */
final class ConstructorAccessorImpl implements ConstructorAccessor {

    private final Constructor<?> constructor;

    ConstructorAccessorImpl(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public Object newInstance(Object[] args)
            throws InstantiationException, IllegalArgumentException, InvocationTargetException {
        Class<?> declaring = this.constructor.getDeclaringClass();
        if (declaring.isInterface() || Modifier.isAbstract(declaring.getModifiers())) {
            throw new InstantiationException(declaring.getName());
        }
        if (args == null) {
            return this.constructor.newInstance(new Object[0]);
        }
        return this.constructor.newInstance(args);
    }
}
