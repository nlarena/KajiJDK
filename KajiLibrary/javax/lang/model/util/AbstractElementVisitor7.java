package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de elementos de Java 7. Ver {@link AbstractElementVisitor6} por el mecanismo de la
 * familia.
 *
 * <p>Java 7 no agrego ninguna clase de declaracion. El `try` con recursos agrego un `ElementKind`,
 * `RESOURCE_VARIABLE`, pero una variable de recurso sigue siendo un `VariableElement` y la toma
 * `visitVariable`: un kind nuevo no es un metodo nuevo. Por eso esta clase no agrega nada — existe para
 * que un visitante pueda **fechar** contra que version del lenguaje fue escrito, que es informacion
 * aunque el contrato no cambie.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractElementVisitor7<R, P> extends AbstractElementVisitor6<R, P> {

    @Deprecated(since = "12")
    protected AbstractElementVisitor7() {
        super();
    }
}
