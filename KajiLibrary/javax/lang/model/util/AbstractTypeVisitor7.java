package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.UnionType;

/**
 * El visitante de tipos de Java 7. Ver {@link AbstractTypeVisitor6} por el mecanismo.
 *
 * <p>Java 7 introdujo el tipo **union** con el `catch` multiple, asi que `visitUnion` pasa a abstracto:
 * quien extiende esta clase tiene que decir que hace con `catch (A | B e)`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public abstract class AbstractTypeVisitor7<R, P> extends AbstractTypeVisitor6<R, P> {

    @Deprecated(since = "12")
    protected AbstractTypeVisitor7() {
        super();
    }

    public abstract R visitUnion(UnionType t, P p);
}
