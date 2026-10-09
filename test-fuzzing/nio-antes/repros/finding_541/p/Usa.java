package p;

import java.lang.annotation.Annotation;

/**
 * Finding #541: `Marca.class` is not accepted as `Class<? extends Annotation>` when `Marca.java`
 * travels in the same javac invocation. Compiled apart --with `Marca.class` already on the
 * classpath-- the same call is accepted.
 */
public class Usa {
    public static Class<? extends Annotation> toma(Class<? extends Annotation> t) {
        return t;
    }

    public static Class<? extends Annotation> prueba() {
        return toma(Marca.class);
    }
}
