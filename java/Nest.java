import java.lang.annotation.*;
@interface In { int x(); String y(); }
@Retention(RetentionPolicy.RUNTIME)
@interface Out { In in(); In[] many(); }
