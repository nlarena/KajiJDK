package jakarta.validation.metadata;

import java.lang.annotation.ElementType;
import java.util.Set;

// KajiLibrary's jakarta.validation.metadata.ElementDescriptor — the metadata common to every
// constrainable element (its type, its constraints, and a query API over them).
public interface ElementDescriptor {
    boolean hasConstraints();
    Class<?> getElementClass();
    Set<ConstraintDescriptor<?>> getConstraintDescriptors();
    ConstraintFinder findConstraints();

    public interface ConstraintFinder {
        ConstraintFinder unorderedAndMatchingGroups(Class<?>... groups);
        ConstraintFinder lookingAt(Scope scope);
        ConstraintFinder declaredOn(ElementType... types);
        Set<ConstraintDescriptor<?>> getConstraintDescriptors();
        boolean hasConstraints();
    }
}
