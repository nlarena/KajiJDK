// A7 #9: runtime annotations (JSR 175) — Class.isAnnotationPresent. @AnMark is declared with
// RUNTIME retention, so javac writes a RuntimeVisibleAnnotations attribute (JVMS §4.7.16) into
// AnMarked.class holding the descriptor "LAnMark;". isAnnotationPresent reads that attribute off
// the mirror's class file and compares descriptors: true for AnMarked, false for the unannotated
// AnPlain and false for a mirror with no class file behind it (int.class). Deterministic score
// 20 + 12 + 10 → green ≡ os-gil ≡ os = 42.
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@interface AnMark {
}

@AnMark
class AnMarked {
}

class AnPlain {
}

public class AnTest {
    static int run() {
        int score = 0;
        if (AnMarked.class.isAnnotationPresent(AnMark.class)) {
            score += 20;
        }
        if (!AnPlain.class.isAnnotationPresent(AnMark.class)) {
            score += 12;
        }
        // A primitive mirror has no class file, so nothing is present on it.
        if (!int.class.isAnnotationPresent(AnMark.class)) {
            score += 10;
        }
        return score; // 42
    }
}
