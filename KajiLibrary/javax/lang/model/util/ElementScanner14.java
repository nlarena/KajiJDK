package javax.lang.model.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

/**
 * The element scanner for Java 14 onwards. See {@link ElementScanner6} for the mechanism.
 *
 * <p>It brings record components into the walk, as expected. But it also **fixes an old hole**, and
 * that is the part that really sets it apart from the earlier versions.
 *
 * <p>The hole: the **type parameters** of a class or a method are neither in
 * `getEnclosedElements()` nor in `getParameters()`. In `&lt;T&gt; void f(T x)`, `T` is a
 * `TypeParameterElement` that no scanner before this version ever visited, even if the visitor had
 * a `visitTypeParameter` written and waiting. From here on, `visitType` and `visitExecutable` put
 * them before what they already walked.
 *
 * <p>They go **before** and not after because it is the order in which they are declared and in
 * which they are needed: the type parameter is in scope for the members that use it.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class ElementScanner14<R, P> extends ElementScanner9<R, P> {

    protected ElementScanner14() {
        super(null);
    }

    protected ElementScanner14(R defaultValue) {
        super(defaultValue);
    }

    public R visitType(TypeElement e, P p) {
        return this.scan(this.createScanningList(e, e.getEnclosedElements()), p);
    }

    public R visitExecutable(ExecutableElement e, P p) {
        return this.scan(this.createScanningList(e, e.getParameters()), p);
    }

    // Without type parameters the original list is returned and not a copy: it is by far the common
    // case, and copying for nothing would be one allocation per visited element.
    private List<? extends Element> createScanningList(Parameterizable element,
            List<? extends Element> toBeScanned) {
        List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
        if (typeParameters.isEmpty()) {
            return toBeScanned;
        }
        List<Element> scanningList = new ArrayList<Element>(typeParameters);
        scanningList.addAll(toBeScanned);
        return scanningList;
    }

    public R visitRecordComponent(RecordComponentElement e, P p) {
        return this.scan(e.getEnclosedElements(), p);
    }
}
