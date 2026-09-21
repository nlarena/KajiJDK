package javax.lang.model;

/**
 * The versions of the Java **language** an annotation processor can declare it understands.
 *
 * <p>They are neither the JDK versions nor the class-file format ones: they are the language's, and
 * that is why the list has historical gaps where a version changed nothing in the language. The
 * constant that matters in practice is {@link #latestSupported()}, which is the one a processor
 * returns to say "I can handle this"; if it returns less than the version being compiled, the
 * compiler warns.
 *
 * <p>**All** the constants up to 25 are here, including the ones this implementation does not tell
 * apart inside: a missing constant is not "I do not support that version", it is that the enum
 * cannot even be named, and a `switch` over versions in a third-party processor would not compile.
 */
public enum SourceVersion {

    /** Java 1.0 and 1.1. */
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

    /** The latest version the language has. */
    public static SourceVersion latest() {
        return RELEASE_25;
    }

    /** The latest one **this** implementation understands. It matches `latest()`. */
    public static SourceVersion latestSupported() {
        return RELEASE_25;
    }

    /**
     * The runtime version that corresponds to this language version.
     *
     * <p>The constant's number **is** the feature number, except for `RELEASE_0` and `RELEASE_1`,
     * which are both 1.
     */
    public Runtime.Version runtimeVersion() {
        int n = this.ordinal();
        if (n == 0) {
            n = 1;
        }
        return Runtime.Version.parse(Integer.toString(n));
    }

    /**
     * The language version that corresponds to that runtime version.
     *
     * @throws IllegalArgumentException if that version corresponds to none of the language's
     * @throws NullPointerException if `rv` is `null`
     */
    public static SourceVersion valueOf(Runtime.Version rv) {
        if (rv == null) {
            throw new NullPointerException("rv");
        }
        int feature = rv.feature();
        if (feature < 1) {
            throw new IllegalArgumentException("No SourceVersion for " + rv);
        }
        SourceVersion[] all = SourceVersion.values();
        if (feature >= all.length) {
            throw new IllegalArgumentException("No SourceVersion for " + rv);
        }
        return all[feature];
    }

    // ---- the three questions about a string -----------------------------------------------------
    //
    // They are three **different** questions and it is as well not to confuse them, because the
    // difference is exactly what makes each one useful:
    //
    //   - `isIdentifier`: the lexical form. `class` **is** a well-formed identifier.
    //   - `isKeyword`: whether that form is reserved. `class` is.
    //   - `isName`: whether it can be used as a name. `class` cannot, because it is a keyword.
    //
    // A code generator that builds names uses `isName`; one that validates a fragment it read uses
    // `isIdentifier`; and `isKeyword` is the one that separates them.

    // The language's 50 reserved words, plus the three literals below. `var`, `yield`, `record`,
    // `sealed`, `permits` and company are **not** here: they are contextual, that is they only mean
    // something in one position and are still valid names anywhere else. `_` is not here either,
    // and it should be: it has been a keyword since 9 (JDK 25's `isKeyword("_")` is true).
    private static String[] reservedWords() {
        return new String[] {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while",
            // The three literals. The JLS calls them literals and not keywords, but they cannot be
            // used as names either, and `isKeyword` counts them -- which is what matters here.
            "true", "false", "null",
        };
    }

    /** Whether `name` has the lexical form of an identifier (§3.8). A keyword has it. */
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

    /** Whether `s` is a reserved word in the latest version of the language. */
    public static boolean isKeyword(CharSequence s) {
        return isKeyword(s, latest());
    }

    /**
     * Whether `s` is a reserved word in that version.
     *
     * <p>The version is accepted and **does not change the answer**. The note said that is because
     * no reserved word was added after 1.5; **that is false**: `_` became a keyword in 9, and
     * `assert` (1.4), `enum` (5) and `strictfp` (1.2) were not keywords before. The JDK answers per
     * version -- `isKeyword("_", RELEASE_8)` is false and `isKeyword("enum", RELEASE_4)` is false--
     * and this one answers the same for every version, without `_` (checked against JDK 25 on
     * 2026-09-18).
     */
    public static boolean isKeyword(CharSequence s, SourceVersion version) {
        if (s == null) {
            throw new NullPointerException("s");
        }
        if (version == null) {
            throw new NullPointerException("version");
        }
        String t = s.toString();
        String[] all = reservedWords();
        int i = 0;
        while (i < all.length) {
            if (all[i].equals(t)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Whether `name` can be used as a name: an identifier that is not a keyword, or several
     * separated by dots.
     *
     * <p>The dots are part of the contract: `java.util.List` **is** a valid name, and that is why
     * this is not simply "identifier and not reserved".
     */
    public static boolean isName(CharSequence name) {
        return isName(name, latest());
    }

    /** The one above, for that version. */
    public static boolean isName(CharSequence name, SourceVersion version) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        String s = name.toString();
        if (s.length() == 0) {
            return false;
        }
        int from = 0;
        while (true) {
            int dot = s.indexOf('.', from);
            String piece;
            if (dot < 0) {
                piece = s.substring(from);
            } else {
                piece = s.substring(from, dot);
            }
            if (!isIdentifier(piece) || isKeyword(piece, version)) {
                return false;
            }
            if (dot < 0) {
                return true;
            }
            from = dot + 1;
        }
    }
}
