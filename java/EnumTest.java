import java.util.concurrent.TimeUnit;

/** Does an enum work end to end on our VM: the constants, values(), valueOf(String), ordinal. */
public class EnumTest {

    public static int constantes() {
        TimeUnit ms = TimeUnit.MILLISECONDS;
        if (ms == null) {
            return 1;
        }
        if (!ms.name().equals("MILLISECONDS")) {
            return 2;
        }
        return 0;
    }

    public static int valores() {
        TimeUnit[] all = TimeUnit.values();
        return all.length;
    }

    /** valueOf alone, without reading a constant field (which is blocked by finding #110). */
    public static int soloValueOf() {
        TimeUnit found = TimeUnit.valueOf("SECONDS");
        if (found == null) {
            return 1;
        }
        if (!found.name().equals("SECONDS")) {
            return 2;
        }
        if (found.ordinal() != 3) {
            return 3;
        }
        return 0;
    }

    /** And that an unknown name is refused rather than answered with null. */
    public static int nombreInvalido() {
        int bad = 0;
        try {
            TimeUnit gone = TimeUnit.valueOf("NOPE");
            if (gone != null) {
                bad = bad + 1;
            }
        } catch (IllegalArgumentException expected) {
            return 0;
        }
        return bad + 10;
    }

    public static int porNombre() {
        TimeUnit found = TimeUnit.valueOf("SECONDS");
        if (found == null) {
            return 1;
        }
        if (found != TimeUnit.SECONDS) {
            return 2;
        }
        if (found.ordinal() != TimeUnit.SECONDS.ordinal()) {
            return 3;
        }
        return 0;
    }
}
