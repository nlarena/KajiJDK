package jdk.jshell;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * What it takes for an editor to understand what the user is writing.
 *
 * <h2>Why evaluating is not enough</h2>
 *
 * <p>A command-line interpreter has to decide things before evaluating anything: whether what was
 * written is already complete or the user will carry on on the next line
 * ({@link #analyzeCompletion}), what may come after the dot ({@link #completionSuggestions}), which
 * documentation to show ({@link #documentation}), what to colour ({@link #highlights}). None of
 * those questions is answered by running: they are answered by analysing.
 *
 * <h2>The wrappers</h2>
 *
 * <p>A snippet is not compiled as it stands: the interpreter puts it inside a synthetic class,
 * because a loose method is not a valid Java program. {@link SnippetWrapper} lets that assembled
 * code be seen, and above all lets positions be translated between the two, which is what makes an
 * error's underline fall where the user wrote and not where the compiler saw it.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The types are complete and the two enums really work. The analysis does not: answering any of
 * these questions needs the compiler's code analyser, and in this library
 * {@code javax.tools.ToolProvider.getSystemJavaCompiler()} returns {@code null}. The ten methods are
 * abstract, so the class promises nothing; what {@link JShell#sourceCodeAnalysis} returns throws
 * {@link UnsupportedOperationException} instead of answering just anything.
 *
 * @since 9
 */
public abstract class SourceCodeAnalysis {

    /** One. */
    protected SourceCodeAnalysis() {
    }

    /**
     * How complete what was written is.
     *
     * <p>It is what decides whether the interpreter evaluates or asks for another line.
     */
    public enum Completeness {

        /** It is complete as it stands. */
        COMPLETE(true),

        /** It is complete but missing the semicolon, which the interpreter adds itself. */
        COMPLETE_WITH_SEMI(true),

        /** Definitely missing something: there is an unclosed brace or parenthesis. */
        DEFINITELY_INCOMPLETE(false),

        /**
         * It could be valid, but it is most likely missing something.
         *
         * <p>It is the case of an {@code if} with no body: {@code if (x)} alone is legal, but nobody
         * writes it on purpose. Asking for another line is right nearly always.
         */
        CONSIDERED_INCOMPLETE(false),

        /** Nothing was written. */
        EMPTY(false),

        /**
         * It could not be told, nearly always because there is a syntax error.
         *
         * <p>It counts as complete: if it is badly written, asking for another line will not fix it,
         * and showing the error is better.
         */
        UNKNOWN(true);

        private final boolean complete;

        Completeness(boolean complete) {
            this.complete = complete;
        }

        /**
         * Whether the interpreter can evaluate already.
         *
         * @return true if there is no need to ask for another line
         */
        public boolean isComplete() {
            return this.complete;
        }
    }

    /** How a piece of code looks when coloured. */
    public enum Attribute {

        /** It is what is declared: a method's or a variable's name where it is defined. */
        DECLARATION,

        /** It is marked as deprecated. */
        DEPRECATED,

        /** It is a reserved word. */
        KEYWORD
    }

    /**
     * A piece of code and how it looks.
     *
     * <p>The positions refer to the text the user wrote.
     *
     * @param start where it starts
     * @param end where it ends
     * @param attributes how it looks
     * @since 20
     */
    public record Highlight(int start, int end, Set<Attribute> attributes) {
    }

    /** How complete what was written is, and what is left over. */
    public interface CompletionInfo {

        /**
         * How complete it is.
         *
         * @return the answer
         */
        Completeness completeness();

        /**
         * What was left over after the first snippet.
         *
         * <p>Writing two statements on one line gives a snippet and a remainder; the remainder is
         * analysed afterwards, just as if it had been written on its own.
         *
         * @return what is left over, or the empty string
         */
        String remaining();

        /**
         * The first snippet, with the semicolon added if it was needed.
         *
         * @return the snippet's code
         */
        String source();
    }

    /** Something that can be written in that place. */
    public interface Suggestion {

        /**
         * What would have to be written.
         *
         * @return the text
         */
        String continuation();

        /**
         * Whether the type of what is suggested is the one needed there.
         *
         * <p>It is for ordering the suggestions: what fits goes first.
         *
         * @return true if it fits
         */
        boolean matchesType();
    }

    /** The documentation of something.
     *
     * @since 9
     */
    public interface Documentation {

        /**
         * The signature of what is documented.
         *
         * @return the signature
         */
        String signature();

        /**
         * The documentation's text.
         *
         * @return the text, or {@code null} if there is none
         */
        String javadoc();
    }

    /** The code the interpreter builds around a snippet in order to compile it. */
    public interface SnippetWrapper {

        /**
         * The text exactly as it was written.
         *
         * @return the user's code
         */
        String source();

        /**
         * The assembled code, the one handed to the compiler.
         *
         * @return the full code
         */
        String wrapped();

        /**
         * The synthetic class's name.
         *
         * @return the class's full name
         */
        String fullClassName();

        /**
         * Which kind of snippet it is.
         *
         * @return the kind
         */
        Snippet.Kind kind();

        /**
         * Where that position of the user's code falls in the assembled code.
         *
         * @param pos the position in the user's code
         * @return the position in the assembled code
         */
        int sourceToWrappedPosition(int pos);

        /**
         * Where that position of the assembled code falls in the user's code.
         *
         * <p>It is the translation that makes a compiler error's underline fall where the user
         * wrote.
         *
         * @param pos the position in the assembled code
         * @return the position in the user's code, or {@code -1} if it falls in the wrapper
         */
        int wrappedToSourcePosition(int pos);
    }

    /** The full names a simple name could stand for. */
    public static final class QualifiedNames {

        private final List<String> names;
        private final int simpleNameLength;
        private final boolean upToDate;
        private final boolean resolvable;

        QualifiedNames(List<String> names, int simpleNameLength, boolean upToDate,
                boolean resolvable) {
            this.names = names == null ? Collections.<String>emptyList() : names;
            this.simpleNameLength = simpleNameLength;
            this.upToDate = upToDate;
            this.resolvable = resolvable;
        }

        /**
         * The full names it could be.
         *
         * @return the names, or an empty list if the name already resolves or nothing was found
         */
        public List<String> getNames() {
            return this.names;
        }

        /**
         * How many characters the simple name is.
         *
         * @return the length, so as to know what to replace
         */
        public int getSimpleNameLength() {
            return this.simpleNameLength;
        }

        /**
         * Whether the class index was up to date when the answer was given.
         *
         * <p>The index is built in the background; if it has not finished yet, the answer may fall
         * short and it is worth asking again.
         *
         * @return true if it was up to date
         */
        public boolean isUpToDate() {
            return this.upToDate;
        }

        /**
         * Whether the name already resolves on its own, with nothing added.
         *
         * @return true if it already resolves
         */
        public boolean isResolvable() {
            return this.resolvable;
        }
    }

    /**
     * How complete what was written is, and what is left over.
     *
     * @param input the code
     * @return the answer
     */
    public abstract CompletionInfo analyzeCompletion(String input);

    /**
     * What can be written in that place.
     *
     * @param input the code
     * @param cursor where the caret is
     * @param anchor where the position to replace from is written
     * @return the suggestions
     */
    public abstract List<Suggestion> completionSuggestions(String input, int cursor, int[] anchor);

    /**
     * The documentation of what is in that place.
     *
     * @param input the code
     * @param cursor where the caret is
     * @param computeJavadoc whether the text has to be fetched too and not only the signature
     * @return the documentation
     */
    public abstract List<Documentation> documentation(String input, int cursor,
            boolean computeJavadoc);

    /**
     * The type of the expression that ends there.
     *
     * @param code the code
     * @param cursor where the expression ends
     * @return the type's name, or {@code null} if there is no expression there
     */
    public abstract String analyzeType(String code, int cursor);

    /**
     * The full names that could stand for the simple name ending there.
     *
     * @param code the code
     * @param cursor where the name ends
     * @return the candidates
     */
    public abstract QualifiedNames listQualifiedNames(String code, int cursor);

    /**
     * The code assembled around that snippet.
     *
     * @param snippet the snippet
     * @return the wrapper
     */
    public abstract SnippetWrapper wrapper(Snippet snippet);

    /**
     * The code assembled around the snippets in that code.
     *
     * @param input the code
     * @return the wrappers
     */
    public abstract List<SnippetWrapper> wrappers(String input);

    /**
     * The snippets in that code, without evaluating them or adding them to the interpreter.
     *
     * @param input the code
     * @return the snippets
     */
    public abstract List<Snippet> sourceToSnippets(String input);

    /**
     * Which snippets depend on that one.
     *
     * @param snippet the snippet
     * @return the ones that depend on it
     */
    public abstract Collection<Snippet> dependents(Snippet snippet);

    /**
     * How each piece of that code looks when coloured.
     *
     * @param input the code
     * @return the pieces with their look
     * @since 20
     */
    public abstract List<Highlight> highlights(String input);
}
