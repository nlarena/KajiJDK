package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante de tipos de las construcciones en **vista previa**. Ver {@link AbstractTypeVisitor6} por
 * el mecanismo y {@link AbstractElementVisitorPreview} por que significa "vista previa" aca — es API
 * reflexiva, se usa sin `--enable-preview`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractTypeVisitorPreview<R, P> extends AbstractTypeVisitor14<R, P> {

    protected AbstractTypeVisitorPreview() {
        super();
    }
}
