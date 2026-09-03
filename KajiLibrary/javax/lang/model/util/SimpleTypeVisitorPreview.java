package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante simple de tipos de las construcciones en **vista previa**. Ver
 * {@link SimpleTypeVisitor6} por el mecanismo y {@link AbstractElementVisitorPreview} por que significa
 * "vista previa" aca.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleTypeVisitorPreview<R, P> extends SimpleTypeVisitor14<R, P> {

    protected SimpleTypeVisitorPreview() {
        super(null);
    }

    protected SimpleTypeVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
