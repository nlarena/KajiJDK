package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de elementos de Java 8. Ver {@link AbstractElementVisitor6} por el mecanismo.
 *
 * <p>Java 8 tampoco agrego clases de declaracion. Lo suyo fueron las lambdas y los metodos `default`, y
 * ninguna de las dos es una declaracion nueva para el modelo: una lambda no tiene elemento propio — es
 * una expresion — y un `default` es un `ExecutableElement` con un modificador mas.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class AbstractElementVisitor8<R, P> extends AbstractElementVisitor7<R, P> {

    protected AbstractElementVisitor8() {
        super();
    }
}
