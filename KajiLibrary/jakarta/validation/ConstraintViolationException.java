package jakarta.validation;

import java.util.Set;
import java.util.HashSet;

// KajiLibrary's jakarta.validation.ConstraintViolationException — reports a set of constraint
// violations (e.g. thrown by Validator.validate callers).
public class ConstraintViolationException extends ValidationException {

    private final Set<ConstraintViolation<?>> constraintViolations;

    public ConstraintViolationException(String message, Set<? extends ConstraintViolation<?>> constraintViolations) {
        super(message);
        if (constraintViolations == null) {
            this.constraintViolations = null;
        } else {
            this.constraintViolations = new HashSet<ConstraintViolation<?>>(constraintViolations);
        }
    }

    public ConstraintViolationException(Set<? extends ConstraintViolation<?>> constraintViolations) {
        this(null, constraintViolations);
    }

    public Set<ConstraintViolation<?>> getConstraintViolations() {
        return this.constraintViolations;
    }
}
