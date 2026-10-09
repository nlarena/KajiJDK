package jakarta.persistence;

// A timeout, usable as a find/lock/refresh option. Constructed via the static factories;
// the stored value is always in milliseconds. New in Jakarta Persistence 3.2.
public class Timeout implements FindOption, RefreshOption, LockOption {

    private final int millis;

    private Timeout(int millis) {
        this.millis = millis;
    }

    public static Timeout s(int seconds) {
        return new Timeout(seconds * 1000);
    }

    public static Timeout ms(int milliseconds) {
        return new Timeout(milliseconds);
    }

    public static Timeout seconds(int seconds) {
        return new Timeout(seconds * 1000);
    }

    public static Timeout milliseconds(int milliseconds) {
        return new Timeout(milliseconds);
    }

    public int milliseconds() {
        return millis;
    }
}
