package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * El escaner de elementos de las construcciones en **vista previa**. Ver {@link ElementScanner6} por el
 * mecanismo y {@link AbstractElementVisitorPreview} por que significa "vista previa" aca.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ElementScannerPreview<R, P> extends ElementScanner14<R, P> {

    protected ElementScannerPreview() {
        super(null);
    }

    protected ElementScannerPreview(R defaultValue) {
        super(defaultValue);
    }
}
