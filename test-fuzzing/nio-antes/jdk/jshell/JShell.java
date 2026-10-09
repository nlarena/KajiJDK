package jdk.jshell;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.tools.StandardJavaFileManager;

import jdk.jshell.spi.ExecutionControlProvider;

/**
 * The interpreter's engine: it holds the state of an interactive Java session.
 *
 * <h2>What it holds</h2>
 *
 * <p>The list of evaluated snippets and the state each one ended up in. That is the whole state of a
 * session: there is no file and no project, only the history of what was written. That is where
 * {@link #snippets}, {@link #status} and the ability to undo with {@link #drop} come from.
 *
 * <h2>The two halves</h2>
 *
 * <p>Evaluating a snippet is two different things: compiling it, which happens on this virtual
 * machine, and running it, which happens on another. The second is outside on purpose
 * --{@code jdk.jshell.spi} and {@code jdk.jshell.execution}-- because the user's code cannot share a
 * virtual machine with the interpreter: a {@code System.exit()} written in the session would take
 * the whole interpreter with it, and a static variable of the user's could clobber the engine's.
 *
 * <p>Compiling is what cannot be moved out: the interpreter has to put the snippet inside a
 * synthetic class, compile it, and then translate the errors' positions back to the text the user
 * wrote. It does that with the compiler {@code javax.tools.ToolProvider.getSystemJavaCompiler()}
 * returns.
 *
 * <h2>Why evaluating produces a list</h2>
 *
 * <p>Because one snippet drags the others along. Rewriting a method leaves the earlier one in
 * {@link Snippet.Status#OVERWRITTEN} and may make valid a third one that was waiting for it.
 * {@link SnippetEvent#causeSnippet} tells the one that was evaluated from the dragged ones.
 *
 * <h2>Where this library stands</h2>
 *
 * <p>The session's state, the notifications, the life cycle and the whole of the building really
 * work. What is missing is the compiler: in this library
 * {@code ToolProvider.getSystemJavaCompiler()} returns {@code null}, so there is nothing to build
 * the synthetic class with and nothing to compile it with. {@link #eval} throws
 * {@link IllegalStateException} and {@link #sourceCodeAnalysis}'s methods throw
 * {@link UnsupportedOperationException}. Everything else --which is most of it-- works: the session
 * starts empty and behaves like an empty session.
 *
 * @since 9
 */
public class JShell implements AutoCloseable {

    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;
    private final JShellConsole console;
    private final Supplier<String> tempVariableNameGenerator;
    private final BiFunction<Snippet, Integer, String> idGenerator;
    private final List<String> extraRemoteVMOptions;
    private final List<String> extraCompilerOptions;
    private final ExecutionControlProvider executionControlProvider;
    private final Map<String, String> executionControlParameters;
    private final String executionControlSpec;
    private final Function<StandardJavaFileManager, StandardJavaFileManager> fileManagerMapper;

    private final List<Snippet> snippets = new ArrayList<Snippet>();
    private final List<String> classpath = new ArrayList<String>();
    private final Map<Subscription, Consumer<SnippetEvent>> snippetListeners =
            new LinkedHashMap<Subscription, Consumer<SnippetEvent>>();
    private final Map<Subscription, Consumer<JShell>> shutdownListeners =
            new LinkedHashMap<Subscription, Consumer<JShell>>();

    private SourceCodeAnalysis analysis;
    private boolean closed;

    JShell(Builder b) {
        this.in = b.in;
        this.out = b.out;
        this.err = b.err;
        this.console = b.console;
        this.tempVariableNameGenerator = b.tempVariableNameGenerator;
        this.idGenerator = b.idGenerator;
        this.extraRemoteVMOptions = b.extraRemoteVMOptions;
        this.extraCompilerOptions = b.extraCompilerOptions;
        this.executionControlProvider = b.executionControlProvider;
        this.executionControlParameters = b.executionControlParameters;
        this.executionControlSpec = b.executionControlSpec;
        this.fileManagerMapper = b.fileManagerMapper;
    }

    /**
     * The token a requested notification is cancelled with.
     *
     * <p>It has no methods on purpose: the only thing that can be done with it is handing it back to
     * {@link JShell#unsubscribe}. That it is an opaque object and not a number keeps it from being
     * confused with another interpreter's.
     */
    public static class Subscription {

        Subscription() {
        }
    }

    /**
     * Builds an interpreter with options.
     *
     * <p>Every method returns the same builder, so calls can be chained. Whatever is not said takes
     * its default value: the virtual machine's input and output, the default execution engine, and
     * no extra options.
     */
    public static class Builder {

        InputStream in = new java.io.ByteArrayInputStream(new byte[0]);
        PrintStream out = System.out;
        PrintStream err = System.err;
        JShellConsole console;
        Supplier<String> tempVariableNameGenerator;
        BiFunction<Snippet, Integer, String> idGenerator;
        List<String> extraRemoteVMOptions = new ArrayList<String>();
        List<String> extraCompilerOptions = new ArrayList<String>();
        ExecutionControlProvider executionControlProvider;
        Map<String, String> executionControlParameters;
        String executionControlSpec;
        Function<StandardJavaFileManager, StandardJavaFileManager> fileManagerMapper;

        Builder() {
        }

        /**
         * Where the user's code reads from when it asks for input.
         *
         * @param in the input
         * @return this builder
         */
        public Builder in(InputStream in) {
            this.in = in;
            return this;
        }

        /**
         * Where the user's code writes.
         *
         * @param out the output
         * @return this builder
         */
        public Builder out(PrintStream out) {
            this.out = out;
            return this;
        }

        /**
         * Where the user's code writes its errors.
         *
         * @param err the error output
         * @return this builder
         */
        public Builder err(PrintStream err) {
            this.err = err;
            return this;
        }

        /**
         * Which console the user's code sees.
         *
         * <p>It is what makes {@code System.console()} work inside the session: the real console
         * belongs to the interpreter, not to the user's code, which runs on another virtual machine.
         *
         * @param console the console, or {@code null} for none
         * @return this builder
         * @since 22
         */
        public Builder console(JShellConsole console) {
            this.console = console;
            return this;
        }

        /**
         * What the variables the interpreter invents are called.
         *
         * <p>They are the ones holding the result of a loose expression. By default they are called
         * {@code $1}, {@code $2} and so on.
         *
         * @param generator where the names come from, or {@code null} for the usual ones
         * @return this builder
         */
        public Builder tempVariableNameGenerator(Supplier<String> generator) {
            this.tempVariableNameGenerator = generator;
            return this;
        }

        /**
         * Which identifiers the snippets are numbered with.
         *
         * <p>It is handed the snippet and the number it would get. It is for a program hosting the
         * interpreter to use its own numbering.
         *
         * @param generator where the identifiers come from, or {@code null} for the usual ones
         * @return this builder
         */
        public Builder idGenerator(BiFunction<Snippet, Integer, String> generator) {
            this.idGenerator = generator;
            return this;
        }

        /**
         * Extra options for the virtual machine the user's code runs on.
         *
         * @param options the options
         * @return this builder
         */
        public Builder remoteVMOptions(String... options) {
            for (int i = 0; i < options.length; i++) {
                this.extraRemoteVMOptions.add(options[i]);
            }
            return this;
        }

        /**
         * Extra options for the compiler.
         *
         * @param options the options
         * @return this builder
         */
        public Builder compilerOptions(String... options) {
            for (int i = 0; i < options.length; i++) {
                this.extraCompilerOptions.add(options[i]);
            }
            return this;
        }

        /**
         * Which execution engine to use, by name.
         *
         * <p>The name carries the parameters inside it, separated by commas, so that it can be
         * passed on the command line: {@code "jdi:launch(true)"}.
         *
         * @param name the engine's name with its parameters
         * @return this builder
         */
        public Builder executionEngine(String name) {
            this.executionControlSpec = name;
            return this;
        }

        /**
         * Which execution engine to use, given directly.
         *
         * @param executionControlProvider the engine
         * @param executionControlParameters its parameters, or {@code null} for the default ones
         * @return this builder
         */
        public Builder executionEngine(ExecutionControlProvider executionControlProvider,
                Map<String, String> executionControlParameters) {
            this.executionControlProvider = executionControlProvider;
            this.executionControlParameters = executionControlParameters;
            return this;
        }

        /**
         * How to wrap the compiler's file manager.
         *
         * <p>It is for a program hosting the interpreter to give the compiler its own sources: those
         * of an editor that has not saved yet, for instance.
         *
         * @param mapper what to do with the manager
         * @return this builder
         * @since 15
         */
        public Builder fileManager(
                Function<StandardJavaFileManager, StandardJavaFileManager> mapper) {
            this.fileManagerMapper = mapper;
            return this;
        }

        /**
         * Builds the interpreter.
         *
         * @return the interpreter, with an empty session
         * @throws IllegalStateException if it could not be built
         */
        public JShell build() throws IllegalStateException {
            return new JShell(this);
        }
    }

    /**
     * An interpreter with everything left at its default.
     *
     * @return the interpreter
     * @throws IllegalStateException if it could not be built
     */
    public static JShell create() throws IllegalStateException {
        return builder().build();
    }

    /**
     * A builder of interpreters.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * This interpreter's code analyser.
     *
     * <p>In this library there is no compiler, and without a compiler there is no analysis: the
     * object returned throws {@link UnsupportedOperationException} from all ten of its methods
     * instead of answering just anything. See {@link SourceCodeAnalysis}'s note.
     *
     * @return the analyser
     */
    public SourceCodeAnalysis sourceCodeAnalysis() {
        if (this.analysis == null) {
            this.analysis = new NoCompiler();
        }
        return this.analysis;
    }

    /**
     * Evaluates that code and returns what happened to each affected snippet.
     *
     * <p>In this library there is no compiler --{@code ToolProvider.getSystemJavaCompiler()} returns
     * {@code null}-- so there is nothing to build the synthetic class with and nothing to compile it
     * with, and this always throws.
     *
     * @param input the code
     * @return the events
     * @throws IllegalStateException if the interpreter is closed, or if there is no compiler
     */
    public List<SnippetEvent> eval(String input) throws IllegalStateException {
        checkAlive();
        throw new IllegalStateException(
                "no compiler: ToolProvider.getSystemJavaCompiler() returns null");
    }

    /**
     * Drops a snippet from the session.
     *
     * <p>The ones that depended on it are left unresolved, and that comes in the list too.
     *
     * @param snippet the snippet
     * @return the events
     * @throws IllegalStateException if the interpreter is closed
     * @throws NullPointerException if the snippet is {@code null}
     * @throws IllegalArgumentException if the snippet is not from this interpreter
     */
    public List<SnippetEvent> drop(Snippet snippet) throws IllegalStateException {
        checkAlive();
        checkSnippet(snippet);
        return new ArrayList<SnippetEvent>();
    }

    /**
     * Adds that to the class path used to compile and to run.
     *
     * @param path a file or a directory
     * @throws IllegalStateException if the interpreter is closed
     * @throws NullPointerException if the path is {@code null}
     */
    public void addToClasspath(String path) {
        checkAlive();
        if (path == null) {
            throw new NullPointerException("path");
        }
        this.classpath.add(path);
    }

    /**
     * Tries to cut short whatever of the user's code is running.
     *
     * <p>It may not manage it: if the code is blocked waiting for input or output, or if it catches
     * what is sent to it, it goes on running all the same.
     */
    public void stop() {
    }

    /**
     * Closes the interpreter and releases whatever it holds.
     *
     * <p>It tells the shutdown listeners once only, even when called again.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        final List<Consumer<JShell>> copy = new ArrayList<Consumer<JShell>>(
                this.shutdownListeners.values());
        for (int i = 0; i < copy.size(); i++) {
            try {
                copy.get(i).accept(this);
            } catch (Throwable t) {
                // A listener that throws cannot keep the interpreter from closing.
            }
        }
    }

    /**
     * Every snippet, in identifier order.
     *
     * @return the snippets
     */
    public Stream<Snippet> snippets() {
        return this.snippets.stream();
    }

    /**
     * The active variables.
     *
     * @return the variables
     */
    public Stream<VarSnippet> variables() {
        return filter(VarSnippet.class);
    }

    /**
     * The active methods.
     *
     * @return the methods
     */
    public Stream<MethodSnippet> methods() {
        return filter(MethodSnippet.class);
    }

    /**
     * The active declared types.
     *
     * @return the types
     */
    public Stream<TypeDeclSnippet> types() {
        return filter(TypeDeclSnippet.class);
    }

    /**
     * The active imports.
     *
     * @return the imports
     */
    public Stream<ImportSnippet> imports() {
        return filter(ImportSnippet.class);
    }

    /**
     * What state that snippet is in.
     *
     * @param snippet the snippet
     * @return the state
     * @throws NullPointerException if the snippet is {@code null}
     * @throws IllegalArgumentException if the snippet is not from this interpreter
     */
    public Snippet.Status status(Snippet snippet) {
        checkSnippet(snippet);
        return Snippet.Status.NONEXISTENT;
    }

    /**
     * That snippet's errors and warnings.
     *
     * @param snippet the snippet
     * @return the diagnostics
     * @throws NullPointerException if the snippet is {@code null}
     * @throws IllegalArgumentException if the snippet is not from this interpreter
     */
    public Stream<Diag> diagnostics(Snippet snippet) {
        checkSnippet(snippet);
        return new ArrayList<Diag>().stream();
    }

    /**
     * What that snippet is missing in order to be resolved.
     *
     * @param snippet the snippet
     * @return the names of what it is missing
     * @throws NullPointerException if the snippet is {@code null}
     * @throws IllegalArgumentException if the snippet is not from this interpreter
     */
    public Stream<String> unresolvedDependencies(DeclarationSnippet snippet) {
        checkSnippet(snippet);
        return new ArrayList<String>().stream();
    }

    /**
     * That variable's value, already turned into text.
     *
     * @param snippet the variable
     * @return the value
     * @throws IllegalStateException if the interpreter is closed
     * @throws NullPointerException if the snippet is {@code null}
     * @throws IllegalArgumentException if the snippet is not from this interpreter, or if the
     *     variable is not defined
     */
    public String varValue(VarSnippet snippet) throws IllegalStateException {
        checkAlive();
        checkSnippet(snippet);
        throw new IllegalArgumentException("the variable is not defined: " + snippet);
    }

    /**
     * Asks to be told every time something happens to a snippet.
     *
     * @param listener whom to tell
     * @return what to cancel the notification with
     * @throws IllegalStateException if the interpreter is closed
     * @throws NullPointerException if the listener is {@code null}
     */
    public Subscription onSnippetEvent(Consumer<SnippetEvent> listener)
            throws IllegalStateException {
        checkAlive();
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        final Subscription s = new Subscription();
        this.snippetListeners.put(s, listener);
        return s;
    }

    /**
     * Asks to be told when the interpreter closes.
     *
     * <p>It also tells when the virtual machine the user's code runs on dies by itself, which is
     * what happens when somebody writes {@code System.exit()} in the session.
     *
     * @param listener whom to tell
     * @return what to cancel the notification with
     * @throws IllegalStateException if the interpreter is closed
     * @throws NullPointerException if the listener is {@code null}
     */
    public Subscription onShutdown(Consumer<JShell> listener) throws IllegalStateException {
        checkAlive();
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        final Subscription s = new Subscription();
        this.shutdownListeners.put(s, listener);
        return s;
    }

    /**
     * Cancels a notification that had been asked for.
     *
     * <p>A token from another interpreter does nothing: cancelling something that is not there is
     * not an error.
     *
     * @param token the token the request returned
     * @throws NullPointerException if the token is {@code null}
     */
    public void unsubscribe(Subscription token) {
        if (token == null) {
            throw new NullPointerException("token");
        }
        this.snippetListeners.remove(token);
        this.shutdownListeners.remove(token);
    }

    /** The active snippets of that kind. */
    private <T extends Snippet> Stream<T> filter(Class<T> type) {
        final List<T> out = new ArrayList<T>();
        for (int i = 0; i < this.snippets.size(); i++) {
            final Snippet s = this.snippets.get(i);
            if (type.isInstance(s) && status(s).isActive()) {
                out.add(type.cast(s));
            }
        }
        return out.stream();
    }

    private void checkAlive() {
        if (this.closed) {
            throw new IllegalStateException("JShell (" + this + ") has been closed.");
        }
    }

    /**
     * Checks that the snippet is from this interpreter.
     *
     * <p>It never is: snippets are made by {@link #eval}, and {@link #eval} cannot make one without
     * a compiler. Any snippet that gets here comes from somewhere else.
     */
    private void checkSnippet(Snippet snippet) {
        if (snippet == null) {
            throw new NullPointerException("snippet");
        }
        throw new IllegalArgumentException("the snippet is not from this interpreter: " + snippet);
    }

    /** The analyser there is not, because there is no compiler to analyse with. */
    private static final class NoCompiler extends SourceCodeAnalysis {

        private static final String REASON =
                "no compiler: ToolProvider.getSystemJavaCompiler() returns null";

        @Override
        public CompletionInfo analyzeCompletion(String input) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public List<Suggestion> completionSuggestions(String input, int cursor, int[] anchor) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public List<Documentation> documentation(String input, int cursor, boolean computeJavadoc) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public String analyzeType(String code, int cursor) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public QualifiedNames listQualifiedNames(String code, int cursor) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public SnippetWrapper wrapper(Snippet snippet) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public List<SnippetWrapper> wrappers(String input) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public List<Snippet> sourceToSnippets(String input) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public Collection<Snippet> dependents(Snippet snippet) {
            throw new UnsupportedOperationException(REASON);
        }

        @Override
        public List<Highlight> highlights(String input) {
            throw new UnsupportedOperationException(REASON);
        }
    }
}
