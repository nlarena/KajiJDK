package jakarta.validation.metadata;
import java.util.List;
public interface ExecutableDescriptor extends ElementDescriptor {
    String getName();
    List<ParameterDescriptor> getParameterDescriptors();
    CrossParameterDescriptor getCrossParameterDescriptor();
    ReturnValueDescriptor getReturnValueDescriptor();
    boolean hasConstrainedParameters();
    boolean hasConstrainedReturnValue();
}
