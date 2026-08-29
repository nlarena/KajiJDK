import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

public class Anno {
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD, ElementType.TYPE})
  @interface Tag {
    String value();
    int order() default 0;
  }
  @Tag(value = "run", order = 2)
  public int run() { return 1; }

  @Tag("plain")
  public int plain() { return 2; }
}
