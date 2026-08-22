package jakarta.validation.executable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
// KajiLibrary's jakarta.validation.executable.ExecutableValidator — validates method/constructor
// parameters and return values.
public interface ExecutableValidator {
    <T> Set<ConstraintViolation<T>> validateParameters(T object, Method method, Object[] parameterValues, Class<?>... groups);
    <T> Set<ConstraintViolation<T>> validateReturnValue(T object, Method method, Object returnValue, Class<?>... groups);
    <T> Set<ConstraintViolation<T>> validateConstructorParameters(Constructor<? extends T> constructor, Object[] parameterValues, Class<?>... groups);
    <T> Set<ConstraintViolation<T>> validateConstructorReturnValue(Constructor<? extends T> constructor, T createdObject, Class<?>... groups);
}
