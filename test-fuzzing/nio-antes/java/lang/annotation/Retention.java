package java.lang.annotation;

// KajiLibrary's java.lang.annotation.Retention — meta-annotation setting an annotation type's retention.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Retention {

    RetentionPolicy value();
}
