package javax.lang.model.util;

import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * The element-kind visitor for Java 14 onwards. See {@link ElementKindVisitor6} for the mechanism.
 *
 * <p>It is the version that closes the three kinds the 6 one had to set aside. Records brought two
 * of them: `RECORD`, which is one more `TypeElement`, and `RECORD_COMPONENT`, which is a
 * declaration of its own. The third, `BINDING_VARIABLE`, came with pattern `instanceof`. The three
 * move from `visitUnknown` to `defaultAction`.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ElementKindVisitor14<R, P> extends ElementKindVisitor9<R, P> {

    protected ElementKindVisitor14() {
        super(null);
    }

    protected ElementKindVisitor14(R defaultValue) {
        super(defaultValue);
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitTypeAsRecord(TypeElement e, P p) {
        return this.defaultAction(e, p);
    }

    public R visitVariableAsBindingVariable(VariableElement e, P p) {
        return this.defaultAction(e, p);
    }
}
