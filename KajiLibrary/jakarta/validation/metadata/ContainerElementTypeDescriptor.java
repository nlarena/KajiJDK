package jakarta.validation.metadata;
public interface ContainerElementTypeDescriptor extends ElementDescriptor, CascadableDescriptor, ContainerDescriptor {
    Integer getTypeArgumentIndex();
    Class<?> getContainerClass();
}
