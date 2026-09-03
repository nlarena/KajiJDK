package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante por kind de elemento de las construcciones en **vista previa**. Ver
 * {@link ElementKindVisitor6} por el mecanismo y {@link AbstractElementVisitorPreview} por que significa
 * "vista previa" aca.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ElementKindVisitorPreview<R, P> extends ElementKindVisitor14<R, P> {

    protected ElementKindVisitorPreview() {
        super(null);
    }

    protected ElementKindVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
