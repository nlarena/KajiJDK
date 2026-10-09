package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.RecordComponentElement;

/**
 * The simple element visitor for Java 14 onwards. See {@link SimpleElementVisitor6} for the
 * mechanism.
 *
 * <p>Record components enter the funnel: `visitRecordComponent` stops throwing and goes to
 * `defaultAction`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class SimpleElementVisitor14<R, P> extends SimpleElementVisitor9<R, P> {

    protected SimpleElementVisitor14() {
        super(null);
    }

    protected SimpleElementVisitor14(R defaultValue) {
        super(defaultValue);
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.defaultAction(e, p);
    }
}
