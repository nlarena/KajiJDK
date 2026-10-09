package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.RecordComponentElement;

/**
 * The element visitor for Java 14 onwards. See {@link AbstractElementVisitor6} for the mechanism.
 *
 * <p>**Record components** are the second new kind of declaration, and here `visitRecordComponent`
 * becomes abstract for the same reason as `visitModule` in the 9 one.
 *
 * <p>The number stayed at 14 although the visitor covers up to 25: records were preview in 14 and
 * final in 16, and the family has not grown since. That there is no `AbstractElementVisitor16` is
 * not an oversight — it was not needed, because no further kind of declaration appeared.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public abstract class AbstractElementVisitor14<R, P> extends AbstractElementVisitor9<R, P> {

    protected AbstractElementVisitor14() {
        super();
    }

    public abstract R visitRecordComponent(RecordComponentElement e, P p);
}
