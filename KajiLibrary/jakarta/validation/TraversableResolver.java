package jakarta.validation;

import java.lang.annotation.ElementType;
import jakarta.validation.Path.Node;

// KajiLibrary's jakarta.validation.TraversableResolver — decides whether a property is reachable and
// cascadable during validation.
public interface TraversableResolver {

    boolean isReachable(Object traversableObject, Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType);

    boolean isCascadable(Object traversableObject, Node traversableProperty, Class<?> rootBeanType,
        Path pathToTraversableObject, ElementType elementType);
}
