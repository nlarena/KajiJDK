package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El visitante por kind de tipo de las construcciones en **vista previa**. Ver
 * {@link TypeKindVisitor6} por el mecanismo y {@link AbstractElementVisitorPreview} por que significa
 * "vista previa" aca.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class TypeKindVisitorPreview<R, P> extends TypeKindVisitor14<R, P> {

    protected TypeKindVisitorPreview() {
        super(null);
    }

    protected TypeKindVisitorPreview(R defaultValue) {
        super(defaultValue);
    }
}
