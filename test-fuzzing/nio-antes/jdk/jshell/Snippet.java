package jdk.jshell;

/**
 * A piece of Java code the interpreter treats as one unit.
 *
 * <h2>What a snippet is</h2>
 *
 * <p>In a file, code is ordered: declarations inside a class, statements inside a method. In an
 * interpreter there is no file, and what the user writes may be either of those, or a loose
 * expression, or an import. A snippet is that unit: what was written in one go, together with the
 * kind of thing it turned out to be.
 *
 * <h2>Why it has an identity</h2>
 *
 * <p>A snippet never changes: writing a method with the same signature again does not modify the
 * earlier snippet, it creates a new one and leaves the old in {@link Status#OVERWRITTEN}. That is
 * what makes undoing, listing the history, and knowing which snippets depended on which possible
 * --if a method is rewritten, everything that called it has to be recompiled.
 *
 * <p>{@link #id} identifies the snippet within its interpreter, and {@link #source} keeps the text
 * exactly as it was written, which is what is shown when the history is listed.
 *
 * <h2>The three levels</h2>
 *
 * <p>{@link #kind} says which kind of snippet it is --import, type declaration, method, variable,
 * expression, statement, or something that did not compile. {@link #subKind} sharpens that: within
 * `VAR` it tells a declaration with no value from one with a value and from the temporary variable
 * the interpreter invents to hold the result of a loose expression. The distinction matters because
 * it is where "does this snippet produce a value to show" comes from.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>Snippets are made by the interpreter while evaluating, and evaluating needs an in-process
 * compiler this library does not have --see {@link JShell#eval}. The classes are complete and the
 * three enums really work; what is missing is anybody to build a snippet.
 *
 * @since 9
 */
public abstract class Snippet {

    private final String id;
    private final String source;
    private final SubKind subkind;

    /** Which kind of snippet it is. */
    public enum Kind {

        /** An `import`. */
        IMPORT(true),

        /** A class, interface, enum, record or annotation declaration. */
        TYPE_DECL(true),

        /** A method declaration. */
        METHOD(true),

        /** A variable declaration. */
        VAR(true),

        /** A loose expression. */
        EXPRESSION(false),

        /** A loose statement. */
        STATEMENT(false),

        /** Something that could not be understood. */
        ERRONEOUS(false);

        private final boolean persistent;

        Kind(boolean persistent) {
            this.persistent = persistent;
        }

        /**
         * Whether a snippet of this kind stays declared for the ones that come after.
         *
         * <p>A method or a variable goes on existing after being written; an expression is evaluated
         * and done with. The difference decides which snippets have to be recompiled when something
         * changes.
         *
         * @return true if it stays declared
         */
        public boolean isPersistent() {
            return this.persistent;
        }
    }

    /** What state a snippet is in inside the interpreter. */
    public enum Status {

        /** It compiles and is active. */
        VALID(true, true),

        /**
         * It is missing something not declared yet, but it could be defined all the same.
         *
         * <p>It is what happens when a method calling another that does not exist yet is written:
         * the interpreter accepts it and leaves it pending, because in an interactive session the
         * order things are written in need not be the order they are used in.
         */
        RECOVERABLE_DEFINED(true, true),

        /** It is missing something and could not be defined either; still alive but unusable. */
        RECOVERABLE_NOT_DEFINED(true, false),

        /** The user dropped it. */
        DROPPED(false, false),

        /** A later snippet replaced it. */
        OVERWRITTEN(false, false),

        /** It did not compile and no other declaration can fix it. */
        REJECTED(false, false),

        /** It does not exist in this interpreter. */
        NONEXISTENT(false, false);

        private final boolean active;
        private final boolean defined;

        Status(boolean active, boolean defined) {
            this.active = active;
            this.defined = defined;
        }

        /**
         * Whether the snippet is still part of the interpreter's state.
         *
         * @return true if it is still alive
         */
        public boolean isActive() {
            return this.active;
        }

        /**
         * Whether what the snippet declares exists and can be used.
         *
         * @return true if it is defined
         */
        public boolean isDefined() {
            return this.defined;
        }
    }

    /** The kind of snippet, sharpened. */
    public enum SubKind {

        /** {@code import java.util.List;} */
        SINGLE_TYPE_IMPORT_SUBKIND(Kind.IMPORT),

        /** {@code import java.util.*;} */
        TYPE_IMPORT_ON_DEMAND_SUBKIND(Kind.IMPORT),

        /** {@code import static java.lang.Math.PI;} */
        SINGLE_STATIC_IMPORT_SUBKIND(Kind.IMPORT),

        /** {@code import static java.lang.Math.*;} */
        STATIC_IMPORT_ON_DEMAND_SUBKIND(Kind.IMPORT),

        /** {@code import module java.base;} */
        MODULE_IMPORT_SUBKIND(Kind.IMPORT),

        /** A class. */
        CLASS_SUBKIND(Kind.TYPE_DECL),

        /** An interface. */
        INTERFACE_SUBKIND(Kind.TYPE_DECL),

        /** An enum. */
        ENUM_SUBKIND(Kind.TYPE_DECL),

        /** A record. */
        RECORD_SUBKIND(Kind.TYPE_DECL),

        /** An annotation type. */
        ANNOTATION_TYPE_SUBKIND(Kind.TYPE_DECL),

        /** A method. */
        METHOD_SUBKIND(Kind.METHOD),

        /** A variable declared with no value. */
        VAR_DECLARATION_SUBKIND(Kind.VAR, true, true),

        /** A variable declared with a value. */
        VAR_DECLARATION_WITH_INITIALIZER_SUBKIND(Kind.VAR, true, true),

        /**
         * The variable the interpreter invents to hold the result of a loose expression.
         *
         * <p>It is what makes writing {@code 1 + 1} leave something named to refer to afterwards,
         * instead of a result that is lost.
         */
        TEMP_VAR_EXPRESSION_SUBKIND(Kind.VAR, true, true),

        /** An expression that is only a variable's name. */
        VAR_VALUE_SUBKIND(Kind.EXPRESSION, true, true),

        /** An assignment. */
        ASSIGNMENT_SUBKIND(Kind.EXPRESSION, true, true),

        /** Any other expression. */
        OTHER_EXPRESSION_SUBKIND(Kind.EXPRESSION, true, true),

        /** A statement. */
        STATEMENT_SUBKIND(Kind.STATEMENT, true, false),

        /** Something that could not be understood. */
        UNKNOWN_SUBKIND(Kind.ERRONEOUS);

        private final Kind kind;
        private final boolean executable;
        private final boolean withValue;

        SubKind(Kind kind) {
            this(kind, false, false);
        }

        SubKind(Kind kind, boolean executable, boolean withValue) {
            this.kind = kind;
            this.executable = executable;
            this.withValue = withValue;
        }

        /**
         * Whether evaluating a snippet of this kind runs code.
         *
         * <p>Declaring a method runs nothing; calling it does. The difference is what decides
         * whether the execution virtual machine is needed or compiling is enough.
         *
         * @return true if it runs code
         */
        public boolean isExecutable() {
            return this.executable;
        }

        /**
         * Whether evaluating a snippet of this kind leaves a value to show.
         *
         * @return true if it leaves a value
         */
        public boolean hasValue() {
            return this.withValue;
        }

        /**
         * Which kind of snippet it is a sharpening of.
         *
         * @return the kind
         */
        public Kind kind() {
            return this.kind;
        }
    }

    Snippet(String id, String source, SubKind subkind) {
        this.id = id;
        this.source = source;
        this.subkind = subkind;
    }

    /**
     * Which snippet this is, within its interpreter.
     *
     * <p>It is not a sequence number: the interpreter may generate them however it likes, and in
     * fact how it does can be changed with {@link JShell.Builder#idGenerator}.
     *
     * @return the identifier
     */
    public String id() {
        return this.id;
    }

    /**
     * Which kind of snippet it is.
     *
     * @return the kind
     */
    public Kind kind() {
        return this.subkind.kind();
    }

    /**
     * The kind of snippet, sharpened.
     *
     * @return the subkind
     */
    public Snippet.SubKind subKind() {
        return this.subkind;
    }

    /**
     * The text exactly as it was written.
     *
     * @return the code
     */
    public String source() {
        return this.source;
    }

    /**
     * For reading while debugging.
     *
     * @return the identifier, the subkind and the code
     */
    @Override
    public String toString() {
        return this.id + ":" + this.subkind + ":" + this.source;
    }
}
