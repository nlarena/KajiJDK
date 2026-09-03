package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante simple de elementos de las construcciones en **vista previa**. Ver
 * {@link SimpleElementVisitor6} por el mecanismo y {@link AbstractElementVisitorPreview} por que
 * significa "vista previa" aca.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleElementVisitorPreview<R, P> extends SimpleElementVisitor14<R, P> {

    protected SimpleElementVisitorPreview() {
        super(null);
    }

    protected SimpleElementVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
