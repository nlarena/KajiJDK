package jakarta.validation;

import jakarta.validation.metadata.ConstraintDescriptor;

// KajiLibrary's jakarta.validation.ConstraintViolation — one constraint failure for a validated bean.
public interface ConstraintViolation<T> {

    String getMessage();

    String getMessageTemplate();

    T getRootBean();

    Class<T> getRootBeanClass();

    Object getLeafBean();

    Object[] getExecutableParameters();

    Object getExecutableReturnValue();

    Path getPropertyPath();

    Object getInvalidValue();

    ConstraintDescriptor<?> getConstraintDescriptor();

    <U> U unwrap(Class<U> type);
}
