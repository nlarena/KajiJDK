package jakarta.validation.metadata;
import java.util.Set;
public interface BeanDescriptor extends ElementDescriptor {
    boolean isBeanConstrained();
    PropertyDescriptor getConstraintsForProperty(String propertyName);
    Set<PropertyDescriptor> getConstrainedProperties();
    MethodDescriptor getConstraintsForMethod(String methodName, Class<?>... parameterTypes);
    Set<MethodDescriptor> getConstrainedMethods(MethodType methodType, MethodType... methodTypes);
    ConstructorDescriptor getConstraintsForConstructor(Class<?>... parameterTypes);
    Set<ConstructorDescriptor> getConstrainedConstructors();
}
