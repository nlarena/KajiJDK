import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@interface Rich {
    boolean bo() default true;
    char ch() default 'x';
    long lo() default 0;
    float fl() default 0;
    double db() default 0;
    byte by() default 0;
    short sh() default 0;
    String[] strs() default {};
    Deprecated nest() default @Deprecated;
}
