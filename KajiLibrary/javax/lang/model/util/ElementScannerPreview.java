package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The element scanner for **preview** constructs. See {@link ElementScanner6} for the mechanism and
 * {@link AbstractElementVisitorPreview} for what "preview" means here.
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
