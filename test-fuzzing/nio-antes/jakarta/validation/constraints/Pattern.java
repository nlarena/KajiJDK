package jakarta.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// KajiLibrary's jakarta.validation.constraints.Pattern — the annotated CharSequence must match the
// given regular expression. The Flag enum mirrors java.util.regex.Pattern's flag bits.
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Pattern.List.class)
public @interface Pattern {

    String regexp();

    Flag[] flags() default {};

    String message() default "{jakarta.validation.constraints.Pattern.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public enum Flag {

        UNIX_LINES(1),
        CASE_INSENSITIVE(2),
        COMMENTS(4),
        MULTILINE(8),
        DOTALL(32),
        UNICODE_CASE(64),
        CANON_EQ(128);

        private final int value;

        Flag(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface List {
        Pattern[] value();
    }
}
