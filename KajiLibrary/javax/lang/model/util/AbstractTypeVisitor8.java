package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.type.IntersectionType;

/**
 * El visitante de tipos de Java 8. Ver {@link AbstractTypeVisitor6} por el mecanismo.
 *
 * <p>Java 8 hizo denotable el tipo **interseccion** — el de `&lt;T extends A &amp; B&gt;` y el de los
 * casts con cotas multiples — asi que `visitIntersection` pasa a abstracto.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class AbstractTypeVisitor8<R, P> extends AbstractTypeVisitor7<R, P> {

    protected AbstractTypeVisitor8() {
        super();
    }

    public abstract R visitIntersection(IntersectionType t, P p);
}
