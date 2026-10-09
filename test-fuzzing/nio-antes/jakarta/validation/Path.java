package jakarta.validation;

import java.util.List;

// KajiLibrary's jakarta.validation.Path — the navigation path to the value that failed validation, an
// iterable of typed nodes.
public interface Path extends Iterable<Node> {

    String toString();

    public interface Node {
        String getName();
        boolean isInIterable();
        Integer getIndex();
        Object getKey();
        ElementKind getKind();
        <T extends Node> T as(Class<T> nodeType);
        String toString();
    }

    public interface PropertyNode extends Node {
        Class<?> getContainerClass();
        Integer getTypeArgumentIndex();
    }

    public interface MethodNode extends Node {
        List<Class<?>> getParameterTypes();
    }

    public interface ConstructorNode extends Node {
        List<Class<?>> getParameterTypes();
    }

    public interface ParameterNode extends Node {
        int getParameterIndex();
    }

    public interface ReturnValueNode extends Node {
    }

    public interface CrossParameterNode extends Node {
    }

    public interface BeanNode extends Node {
        Class<?> getContainerClass();
        Integer getTypeArgumentIndex();
    }

    public interface ContainerElementNode extends Node {
        Class<?> getContainerClass();
        Integer getTypeArgumentIndex();
    }
}
