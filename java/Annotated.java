import java.lang.annotation.*;
@Anno(i=5, s="hi", c=String.class, arr={1,2,3}, e=ElementType.METHOD)
public class Annotated {
    @Deprecated int field;
    @Anno(i=1) void m() {}
}
