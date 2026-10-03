package p;

import java.lang.reflect.Field;

/**
 * Finding #542: a partially qualified name -- `reflect.Field`, `util.List` -- is accepted as if it
 * were the full one. JDK 25 rejects both with "package reflect does not exist" / "package util
 * does not exist"; there are no packages called `reflect` or `util`.
 */
public class Usa {
    public static String name(reflect.Field f) {
        return f.getName();
    }

    public static int size(util.List<String> l) {
        return l.size();
    }
}
