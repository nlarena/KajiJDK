package jakarta.validation.metadata;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.validation.ConstraintTarget;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.Payload;

// KajiLibrary's jakarta.validation.metadata.ConstraintDescriptor — describes a single constraint
// declaration (its annotation, message, groups, payload, validators, composing constraints, …).
public interface ConstraintDescriptor<T extends Annotation> {
    T getAnnotation();
    String getMessageTemplate();
    Set<Class<?>> getGroups();
    Set<Class<? extends Payload>> getPayload();
    ConstraintTarget getValidationAppliesTo();
    List<Class<? extends ConstraintValidator<T, ?>>> getConstraintValidatorClasses();
    Map<String, Object> getAttributes();
    Set<ConstraintDescriptor<?>> getComposingConstraints();
    boolean isReportAsSingleViolation();
    ValidateUnwrappedValue getValueUnwrapping();
    <U> U unwrap(Class<U> type);
}
