package javax.lang.model;

/**
 * Las versiones del **lenguaje** Java que un procesador de anotaciones puede declarar que entiende.
 *
 * <p>No son las versiones del JDK ni las del formato de clase: son las del lenguaje, y por eso la
 * lista tiene huecos historicos donde una version no cambio nada del idioma. La constante que
 * importa en la practica es {@link #latestSupported()}, que es la que un procesador devuelve para
 * decir "puedo con esto"; si devuelve menos de la version que se esta compilando, el compilador
 * avisa.
 *
 * <p>Aca estan **todas** las constantes hasta la 25, incluidas las que esta implementacion no
 * distingue por dentro: una constante que falta no es "no soporto esa version", es que el enum ni
 * siquiera se puede nombrar, y un `switch` sobre versiones de un procesador de terceros no
 * compilaria.
 */
public enum SourceVersion {

    /** Java 1.0 y 1.1. */
    RELEASE_0,
    RELEASE_1,
    RELEASE_2,
    RELEASE_3,
    RELEASE_4,
    RELEASE_5,
    RELEASE_6,
    RELEASE_7,
    RELEASE_8,
    RELEASE_9,
    RELEASE_10,
    RELEASE_11,
    RELEASE_12,
    RELEASE_13,
    RELEASE_14,
    RELEASE_15,
    RELEASE_16,
    RELEASE_17,
    RELEASE_18,
    RELEASE_19,
    RELEASE_20,
    RELEASE_21,
    RELEASE_22,
    RELEASE_23,
    RELEASE_24,
    RELEASE_25;

    /** La ultima version que el lenguaje tiene. */
    public static SourceVersion latest() {
        return RELEASE_25;
    }

    /** La ultima que **esta** implementacion entiende. Coincide con `latest()`. */
    public static SourceVersion latestSupported() {
        return RELEASE_25;
    }

    /**
     * La version del entorno de ejecucion que corresponde a esta version del lenguaje.
     *
     * <p>El numero de la constante **es** el numero de feature, salvo `RELEASE_0` y `RELEASE_1`, que
     * son las dos la 1.
     */
    public Runtime.Version runtimeVersion() {
        int n = this.ordinal();
        if (n == 0) {
            n = 1;
        }
        return Runtime.Version.parse(Integer.toString(n));
    }

    /**
     * La version del lenguaje que corresponde a esa version del entorno.
     *
     * @throws IllegalArgumentException si esa version no corresponde a ninguna del lenguaje
     * @throws NullPointerException si `rv` es `null`
     */
    public static SourceVersion valueOf(Runtime.Version rv) {
        if (rv == null) {
            throw new NullPointerException("rv");
        }
        int feature = rv.feature();
        if (feature < 1) {
            throw new IllegalArgumentException("No SourceVersion for " + rv);
        }
        SourceVersion[] todas = SourceVersion.values();
        if (feature >= todas.length) {
            throw new IllegalArgumentException("No SourceVersion for " + rv);
        }
        return todas[feature];
    }

    // ---- las tres preguntas sobre una cadena --------------------------------------------------------
    //
    // Son tres preguntas **distintas** y conviene no confundirlas, porque la diferencia es justo lo
    // que hace util a cada una:
    //
    //   - `isIdentifier`: la forma lexica. `class` **es** un identificador bien formado.
    //   - `isKeyword`: si esa forma esta reservada. `class` lo esta.
    //   - `isName`: si se puede usar como nombre. `class` no, porque es palabra clave.
    //
    // Un generador de codigo que arma nombres usa `isName`; uno que valida un fragmento leido usa
    // `isIdentifier`; y `isKeyword` es la que los separa.

    // Las 50 reservadas del lenguaje. `var`, `yield`, `record`, `sealed`, `permits` y compania **no**
    // estan: son contextuales, o sea que solo significan algo en una posicion y siguen siendo
    // nombres validos en cualquier otra.
    private static String[] reservadas() {
        return new String[] {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while",
            // Los tres literales. El JLS los llama literales y no palabras clave, pero tampoco se
            // pueden usar como nombre, y `isKeyword` los cuenta -- que es lo que importa aca.
            "true", "false", "null",
        };
    }

    /** Si `name` tiene la forma lexica de un identificador (§3.8). Una palabra clave la tiene. */
    public static boolean isIdentifier(CharSequence name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        String s = name.toString();
        if (s.length() == 0) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        int i = 1;
        while (i < s.length()) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Si `s` es una palabra reservada en la ultima version del lenguaje. */
    public static boolean isKeyword(CharSequence s) {
        return isKeyword(s, latest());
    }

    /**
     * Si `s` es una palabra reservada en esa version.
     *
     * <p>La version se acepta y **no cambia la respuesta**: las reservadas del lenguaje no se
     * agregaron desde la 1.5 --lo que se agrego desde entonces son palabras *contextuales*, que no
     * son reservadas en ninguna version--. Se documenta en vez de fingir que se distinguen.
     */
    public static boolean isKeyword(CharSequence s, SourceVersion version) {
        if (s == null) {
            throw new NullPointerException("s");
        }
        if (version == null) {
            throw new NullPointerException("version");
        }
        String t = s.toString();
        String[] todas = reservadas();
        int i = 0;
        while (i < todas.length) {
            if (todas[i].equals(t)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Si `name` se puede usar como nombre: un identificador que no sea palabra clave, o varios
     * separados por puntos.
     *
     * <p>Los puntos son parte del contrato: `java.util.List` **es** un nombre valido, y por eso esto
     * no es simplemente "identificador y no reservada".
     */
    public static boolean isName(CharSequence name) {
        return isName(name, latest());
    }

    /** El de arriba, para esa version. */
    public static boolean isName(CharSequence name, SourceVersion version) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        String s = name.toString();
        if (s.length() == 0) {
            return false;
        }
        int desde = 0;
        while (true) {
            int punto = s.indexOf('.', desde);
            String pieza;
            if (punto < 0) {
                pieza = s.substring(desde);
            } else {
                pieza = s.substring(desde, punto);
            }
            if (!isIdentifier(pieza) || isKeyword(pieza, version)) {
                return false;
            }
            if (punto < 0) {
                return true;
            }
            desde = punto + 1;
        }
    }
}
