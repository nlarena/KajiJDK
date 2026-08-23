package javax.lang.model.element;

import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

// KajiLibrary's javax.lang.model.element.AnnotationValueVisitor<R,P> — a visitor over the
// value of an annotation element, in the classic visitor shape: R is what a visit produces,
// P is the extra argument threaded through it.
//
// visitUnknown is the escape hatch. New kinds of annotation value can appear in later
// language versions, and a visitor written today has to land *somewhere* when it meets one;
// the convention is for that method to throw UnknownAnnotationValueException.
public interface AnnotationValueVisitor<R, P> {

    R visit(AnnotationValue av, P p);

    default R visit(AnnotationValue av) {
        return visit(av, null);
    }

    R visitBoolean(boolean b, P p);

    R visitByte(byte b, P p);

    R visitChar(char c, P p);

    R visitDouble(double d, P p);

    R visitFloat(float f, P p);

    R visitInt(int i, P p);

    R visitLong(long i, P p);

    R visitShort(short s, P p);

    R visitString(String s, P p);

    R visitType(TypeMirror t, P p);

    R visitEnumConstant(VariableElement c, P p);

    R visitAnnotation(AnnotationMirror a, P p);

    R visitArray(List<? extends AnnotationValue> vals, P p);

    R visitUnknown(AnnotationValue av, P p);
}
