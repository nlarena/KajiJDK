// Finding #110 — reading a STATIC field of a *classpath* class emits `getfield`, not
// `getstatic`. Same-compilation statics are fine, so the trigger is the class-file reader
// not recording ACC_STATIC for fields it loads from `-cp` (same family as #104's Exceptions
// attribute). Every enum-constant reference to a separately compiled class is affected.
import java.util.concurrent.TimeUnit;
public class finding_110 {
    static int fromClasspath() {
        return Integer.MAX_VALUE;      // emitted: getfield  java/lang/Integer.MAX_VALUE:I   (WRONG)
    }
    static Object enumConstant() {
        return TimeUnit.SECONDS;       // emitted: getfield  java/util/concurrent/TimeUnit.SECONDS  (WRONG)
    }
    static int fromSameFile() {
        return Holder.F;               // emitted: getstatic Holder.F:I                       (correct)
    }
}
class Holder {
    static final int F = 6;
}
