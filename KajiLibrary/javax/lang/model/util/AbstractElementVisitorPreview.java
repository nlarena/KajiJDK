package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

/**
 * The element visitor for **preview** constructs. See {@link AbstractElementVisitor6} for the
 * mechanism.
 *
 * <p>It is the place reserved for the next kind of declaration the language adds while in preview.
 * Today it adds nothing, and that is normal: the class exists **before** the construct, so that the
 * day it appears it has somewhere to go without touching `AbstractElementVisitor14` — which is
 * final API and cannot move.
 *
 * <p>It is **reflective** preview API, not a preview language construct. The difference is
 * observable: it compiles and runs without `--enable-preview`, and all one gets is a warning.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractElementVisitorPreview<R, P> extends AbstractElementVisitor14<R, P> {

    protected AbstractElementVisitorPreview() {
        super();
    }
}
