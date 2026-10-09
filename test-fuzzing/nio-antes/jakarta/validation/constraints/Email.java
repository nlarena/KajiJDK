package jakarta.validation.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// KajiLibrary's jakarta.validation.constraints.Email — the annotated CharSequence must be a
// well-formed email address (optionally further constrained by a regular expression).
@Documented
@Constraint(validatedBy = {})
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Email.List.class)
public @interface Email {

    String message() default "{jakarta.validation.constraints.Email.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String regexp() default ".*";

    // NB: the JDK's Email also declares `Pattern.Flag[] flags()`; omitted here because a cross-file
    // nested-type reference (Pattern.Flag) can't be emitted with the right descriptor (finding #101).
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface List {
        Email[] value();
    }
}
