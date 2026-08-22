package java.lang.annotation;

// KajiLibrary's java.lang.annotation.Repeatable — meta-annotation declaring an annotation repeatable,
// naming its containing annotation type.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Repeatable {

    Class<? extends Annotation> value();
}
