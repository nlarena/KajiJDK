package java.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * KajiLibrary's java.lang.SuppressWarnings — silences the named warnings on the element it
 * annotates, and only there. Scoping matters more than the silencing: applied to the one
 * declaration that needs it rather than to the enclosing class, it stops the NEXT unchecked
 * cast in that class from being hidden too.
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
         ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE, ElementType.MODULE})
@Retention(RetentionPolicy.SOURCE)
public @interface SuppressWarnings {

    // The warning names to suppress. Which names are recognised is the compiler business,
    // not the language, so an unknown one is ignored rather than rejected.
    String[] value();
}
