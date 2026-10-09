package jakarta.validation.metadata;
import java.util.Set;
public interface ContainerDescriptor {
    Set<ContainerElementTypeDescriptor> getConstrainedContainerElementTypes();
}
