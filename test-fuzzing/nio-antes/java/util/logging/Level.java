package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Level -- how much a message matters.
 *
 * <p>It is a class and not an enum, and that is deliberate in the JDK: the constructor is
 * `protected` precisely so that somebody can invent an intermediate level. What really orders is
 * {@link #intValue}, not identity -- comparing levels with `==` works for the nine standard ones and
 * fails for any level of one's own.
 *
 * <p>The values are not consecutive (1000, 900, 800, 700, 500, 400, 300) and that is the reason a
 * new one can be slotted in without renumbering anything.
 *
 * <p>{@link #OFF} and {@link #ALL} are not message levels but **filter** ones: nothing reaches them
 * or everything passes them, and that is why they are `Integer.MAX_VALUE` and `Integer.MIN_VALUE`.
 */
public class Level implements java.io.Serializable {

    /** Nothing is recorded. */
    public static final Level OFF = new Level("OFF", Integer.MAX_VALUE);

    /** A serious failure, of the kind that matters to whoever uses the program. */
    public static final Level SEVERE = new Level("SEVERE", 1000);

    /** Something worth looking at. */
    public static final Level WARNING = new Level("WARNING", 900);

    /** Normal information. */
    public static final Level INFO = new Level("INFO", 800);

    /** Configuration messages, for diagnosing the environment. */
    public static final Level CONFIG = new Level("CONFIG", 700);

    /** Coarse logging, for following the program. */
    public static final Level FINE = new Level("FINE", 500);

    /** More detailed logging; method entries and exits. */
    public static final Level FINER = new Level("FINER", 400);

    /** All the detail. */
    public static final Level FINEST = new Level("FINEST", 300);

    /** Everything is recorded. */
    public static final Level ALL = new Level("ALL", Integer.MIN_VALUE);

    private final String name;
    private final int value;
    private final String resourceBundleName;

    protected Level(String name, int value) {
        this(name, value, null);
    }

    protected Level(String name, int value, String resourceBundleName) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
        this.value = value;
        this.resourceBundleName = resourceBundleName;
    }

    /** The level's name. */
    public String getName() {
        return this.name;
    }

    /** The translated name; here, the same: there is no localisation. */
    public String getLocalizedName() {
        return this.name;
    }

    /** The resource bundle to translate it with, or `null`. */
    public String getResourceBundleName() {
        return this.resourceBundleName;
    }

    /** The number that orders this level against the others. */
    public final int intValue() {
        return this.value;
    }

    public final String toString() {
        return this.name;
    }

    /**
     * The level by that name, or the one of that number written as text.
     *
     * <p>It accepts both forms because the configuration comes from strings: `"FINE"` and `"500"`
     * name the same level, and a number corresponding to no standard one gives a new level -- which
     * is what lets a slotted-in level be configured without declaring it.
     *
     * @throws IllegalArgumentException if it is neither a known name nor a number
     */
    public static synchronized Level parse(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new NullPointerException("name");
        }
        Level[] known = new Level[] {OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST,
                ALL};
        int i = 0;
        while (i < known.length) {
            if (known[i].name.equals(name)) {
                return known[i];
            }
            i = i + 1;
        }
        int value;
        try {
            value = Integer.parseInt(name);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad level \"" + name + "\"");
        }
        i = 0;
        while (i < known.length) {
            if (known[i].value == value) {
                return known[i];
            }
            i = i + 1;
        }
        return new Level(name, value);
    }

    public boolean equals(Object ox) {
        if (ox instanceof Level) {
            return ((Level) ox).value == this.value;
        }
        return false;
    }

    public int hashCode() {
        return this.value;
    }
}
