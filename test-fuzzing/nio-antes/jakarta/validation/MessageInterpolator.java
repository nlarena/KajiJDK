package jakarta.validation;

import java.util.Locale;
import jakarta.validation.metadata.ConstraintDescriptor;

// KajiLibrary's jakarta.validation.MessageInterpolator — resolves a constraint's message template into
// the final message text.
public interface MessageInterpolator {

    String interpolate(String messageTemplate, Context context);

    String interpolate(String messageTemplate, Context context, Locale locale);

    public interface Context {
        ConstraintDescriptor<?> getConstraintDescriptor();
        Object getValidatedValue();
        <T> T unwrap(Class<T> type);
    }
}
