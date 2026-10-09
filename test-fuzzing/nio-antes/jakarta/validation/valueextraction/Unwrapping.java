package jakarta.validation.valueextraction;

import jakarta.validation.Payload;

// KajiLibrary's jakarta.validation.valueextraction.Unwrapping — payload markers to force/skip
// value unwrapping.
public interface Unwrapping {
    public interface Skip extends Payload {
    }
    public interface Unwrap extends Payload {
    }
}
