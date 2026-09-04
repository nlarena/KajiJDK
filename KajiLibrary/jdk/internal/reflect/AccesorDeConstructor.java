package jdk.internal.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * El {@link ConstructorAccessor} que fabrica {@link ReflectionFactory#newConstructorAccessor}.
 *
 * <p>Delega en {@link Constructor#newInstance}, o sea en {@code Intrinsic::ConstructorNewInstance}.
 * Vale lo mismo que en {@link AccesorDeMetodo}: la flecha va al reves que en HotSpot y por eso no se
 * cierra en un ciclo.
 *
 * <p>Lo unico que agrega de suyo es el chequeo de instanciabilidad. El JDK no usa este accesor para
 * una clase abstracta o una interfaz: {@code ReflectionFactory} devuelve para esas un
 * {@code InstantiationExceptionConstructorAccessorImpl}, cuyo unico trabajo es tirar. Aca esa
 * distincion no hace falta que sea una clase aparte porque el chequeo es de dos lineas, pero el
 * comportamiento observable tiene que ser el mismo: {@link InstantiationException} y no lo que la VM
 * conteste al intentar alocar algo que no se puede alocar.
 */
final class AccesorDeConstructor implements ConstructorAccessor {

    private final Constructor<?> constructor;

    AccesorDeConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    public Object newInstance(Object[] args)
            throws InstantiationException, IllegalArgumentException, InvocationTargetException {
        Class<?> declara = this.constructor.getDeclaringClass();
        if (declara.isInterface() || Modifier.isAbstract(declara.getModifiers())) {
            throw new InstantiationException(declara.getName());
        }
        if (args == null) {
            return this.constructor.newInstance(new Object[0]);
        }
        return this.constructor.newInstance(args);
    }
}
