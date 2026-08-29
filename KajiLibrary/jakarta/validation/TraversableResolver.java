package jakarta.validation;

import jakarta.validation.Path;
import java.lang.annotation.ElementType;

// KajiLibrary's jakarta.validation.TraversableResolver — decides whether a property is reachable and
// cascadable during validation.
public interface TraversableResolver {

    boolean isReachable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType);

    boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType);
}
