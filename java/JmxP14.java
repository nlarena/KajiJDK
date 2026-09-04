import java.lang.annotation.*;
public class JmxP14 {
    @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.TYPE})
    public @interface Marca { boolean value() default true; }
    @Marca(false) public interface I { }
    @Marca(false) public static class C { }
    @Marca public static class D { }
    public static int run() {
        System.out.println("iface  anots=" + I.class.getAnnotations().length
            + " get=" + I.class.getAnnotation(Marca.class));
        System.out.println("clase  anots=" + C.class.getAnnotations().length
            + " get=" + C.class.getAnnotation(Marca.class));
        Marca m = D.class.getAnnotation(Marca.class);
        System.out.println("clase-defecto get=" + m + " value=" + (m==null?"n/a":""+m.value()));
        Marca c = C.class.getAnnotation(Marca.class);
        System.out.println("clase-false   value=" + (c==null?"n/a":""+c.value()));
        return -1;
    }
}
