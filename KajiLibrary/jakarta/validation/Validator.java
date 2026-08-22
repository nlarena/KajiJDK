package jakarta.validation;
import java.util.Set;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;
// KajiLibrary's jakarta.validation.Validator — validates beans, properties and values against their
// constraints. The central entry point.
public interface Validator {
    <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups);
    <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups);
    <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups);
    BeanDescriptor getConstraintsForClass(Class<?> clazz);
    <T> T unwrap(Class<T> type);
    ExecutableValidator forExecutables();
}
