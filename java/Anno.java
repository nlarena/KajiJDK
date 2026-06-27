import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@interface Anno {
    int i() default 0;
    String s() default "";
    Class<?> c() default Object.class;
    int[] arr() default {};
    ElementType e() default ElementType.TYPE;
    Deprecated nested() default @Deprecated;
}
