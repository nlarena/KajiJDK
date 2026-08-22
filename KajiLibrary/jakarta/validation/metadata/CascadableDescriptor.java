package jakarta.validation.metadata;
import java.util.Set;
public interface CascadableDescriptor {
    boolean isCascaded();
    Set<GroupConversionDescriptor> getGroupConversions();
}
