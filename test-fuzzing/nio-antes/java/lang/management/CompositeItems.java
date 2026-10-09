package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * The item reading this package's five {@code from(CompositeData)} share.
 *
 * <p>Package-private: not API. It exists so the "is the item there and is it of the right type"
 * check is written once and not five times, and so the error message always says which type was
 * expected.
 */
final class CompositeItems {

    private CompositeItems() {
    }

    /** A text item that may be null. */
    static String string(CompositeData cd, String name, String type) {
        Object v = value(cd, name, type);
        if (v != null && !(v instanceof String)) {
            throw badType(name, type, "String");
        }
        return (String) v;
    }

    /** An int item. */
    static int integer(CompositeData cd, String name, String type) {
        Object v = value(cd, name, type);
        if (!(v instanceof Integer)) {
            throw badType(name, type, "Integer");
        }
        return ((Integer) v).intValue();
    }

    /** A long item. */
    static long longValue(CompositeData cd, String name, String type) {
        Object v = value(cd, name, type);
        if (!(v instanceof Long)) {
            throw badType(name, type, "Long");
        }
        return ((Long) v).longValue();
    }

    /** A boolean item. */
    static boolean bool(CompositeData cd, String name, String type) {
        Object v = value(cd, name, type);
        if (!(v instanceof Boolean)) {
            throw badType(name, type, "Boolean");
        }
        return ((Boolean) v).booleanValue();
    }

    /** An item that may be missing; it returns null if it is not there. */
    static Object optional(CompositeData cd, String name) {
        if (!cd.containsKey(name)) {
            return null;
        }
        return cd.get(name);
    }

    /** The item, demanding that it exist. */
    private static Object value(CompositeData cd, String name, String type) {
        if (!cd.containsKey(name)) {
            throw new IllegalArgumentException(
                "Unexpected composite type for " + type + ": missing item " + name);
        }
        return cd.get(name);
    }

    private static IllegalArgumentException badType(String name, String type, String want) {
        return new IllegalArgumentException(
            "Unexpected composite type for " + type + ": item " + name + " is not a " + want);
    }
}
