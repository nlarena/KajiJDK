package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * La lectura de items que comparten los cinco {@code from(CompositeData)} de este paquete.
 *
 * <p>De acceso de paquete: no es API. Existe para que el control de "esta el item y es del tipo que
 * corresponde" este escrito una vez y no cinco, y para que el mensaje de error diga siempre de que
 * tipo se esperaba.
 */
final class CompositeItems {

    private CompositeItems() {
    }

    /** Un item de texto que puede ser null. */
    static String string(CompositeData cd, String name, String type) {
        Object v = value(cd, name, type);
        if (v != null && !(v instanceof String)) {
            throw badType(name, type, "String");
        }
        return (String) v;
    }

    /** Un item entero. */
    static int integer(CompositeData cd, String name, String type) {
        Object v = value(cd, name, type);
        if (!(v instanceof Integer)) {
            throw badType(name, type, "Integer");
        }
        return ((Integer) v).intValue();
    }

    /** Un item entero largo. */
    static long longValue(CompositeData cd, String name, String type) {
        Object v = value(cd, name, type);
        if (!(v instanceof Long)) {
            throw badType(name, type, "Long");
        }
        return ((Long) v).longValue();
    }

    /** Un item booleano. */
    static boolean bool(CompositeData cd, String name, String type) {
        Object v = value(cd, name, type);
        if (!(v instanceof Boolean)) {
            throw badType(name, type, "Boolean");
        }
        return ((Boolean) v).booleanValue();
    }

    /** Un item que puede faltar; devuelve null si no esta. */
    static Object optional(CompositeData cd, String name) {
        if (!cd.containsKey(name)) {
            return null;
        }
        return cd.get(name);
    }

    /** El item, exigiendo que exista. */
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
