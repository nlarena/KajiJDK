package java.lang.annotation;

// KajiLibrary's java.lang.annotation.Target — meta-annotation restricting the contexts of an annotation.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Target {

    ElementType[] value();
}
