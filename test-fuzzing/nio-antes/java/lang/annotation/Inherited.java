package java.lang.annotation;

// KajiLibrary's java.lang.annotation.Inherited — meta-annotation making an annotation inherited by subclasses.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Inherited {
}
