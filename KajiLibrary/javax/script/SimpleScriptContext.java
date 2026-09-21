package javax.script;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

/**
 * KajiLibrary's javax.script.SimpleScriptContext -- the {@link ScriptContext} with two scopes.
 *
 * <p>It implements exactly the two scopes the interface defines and not one more, with an asymmetry
 * that is the whole class:
 *
 * <ul>
 *   <li>The engine one **always exists**. It starts as an empty {@link SimpleBindings} and cannot
 *       be set to null: {@code setBindings(null, ENGINE_SCOPE)} is a {@link NullPointerException}.
 *   <li>The global one **may not exist**, and in fact starts as null. Setting it to null is
 *       allowed, and with the global one absent writing to it is ignored silently and reading it
 *       returns null -- neither of the two is an error.
 * </ul>
 *
 * <p>The lookup rules come from there. {@link #getAttribute(String)} looks first at the engine one
 * and then at the global one, and returns what the first one that **has the key** has -- not the
 * first that is non-null, which is different when the stored value is null. {@link
 * #getAttributesScope(String)} does the same search but returns the number, or -1 if it is in none.
 *
 * <p>A scope that is neither 100 nor 200 is always an {@link IllegalArgumentException}. The message
 * is not always the same, and we copy it as it is: `setBindings` says "Invalid scope value." and
 * all the rest say "Illegal scope value.". It is an inconsistency of the original, but it is
 * observable.
 *
 * <p>Attribute names have their own guard, looser than {@link SimpleBindings}'s: null is a {@link
 * NullPointerException} with no message and empty is an {@link IllegalArgumentException} with "name
 * cannot be empty". As the parameter is already a `String`, there is no type case.
 *
 * <p><b>Implementation note.</b> The dispatch by scope is written with `if/else` chains and not
 * with `switch`, which is how the original has it. It is not a preference: the bytecode generator
 * of the frozen javac that builds this library does not fold as a `case` constant a value declared
 * in **another top-level type** --and `ENGINE_SCOPE` lives in {@link ScriptContext}--, so `case
 * ScriptContext.ENGINE_SCOPE` does not compile. It was checked by ablation: with the constant
 * declared in the same compilation unit the `switch` compiles, and with it in another file it fails
 * even if both are passed to the same `javac` invocation. It is finding #461; the source-built
 * javac no longer has it, the frozen `bin/javac.exe` still does (checked 2026-09-18). The
 * observable behaviour is identical -- each branch ended in `return`, `break` or `throw`.
 */
public class SimpleScriptContext implements ScriptContext {

    /** Where the script writes. */
    protected Writer writer;

    /** Where the script writes its errors. */
    protected Writer errorWriter;

    /** Where the script reads from. */
    protected Reader reader;

    /** The engine scope. Never null. */
    protected Bindings engineScope;

    /** The global scope. It may be null, and it starts that way. */
    protected Bindings globalScope;

    /** The two scopes, immutable and shared: it does not depend on the instance. */
    private static final List<Integer> scopes =
            List.of(Integer.valueOf(ScriptContext.ENGINE_SCOPE),
                    Integer.valueOf(ScriptContext.GLOBAL_SCOPE));

    /**
     * A context with the engine scope empty, the global one absent, and the three streams pointing
     * at the process's console.
     */
    public SimpleScriptContext() {
        this(new InputStreamReader(System.in),
             new PrintWriter(System.out, true),
             new PrintWriter(System.err, true));
    }

    /** The one that does the work; the public one passes it the console. */
    SimpleScriptContext(Reader reader, Writer writer, Writer errorWriter) {
        this.reader = reader;
        this.writer = writer;
        this.errorWriter = errorWriter;
        this.engineScope = new SimpleBindings();
        this.globalScope = null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if `bindings` is null and `scope` is {@link
     *     ScriptContext#ENGINE_SCOPE}
     * @throws IllegalArgumentException if `scope` is neither 100 nor 200
     */
    @Override
    public void setBindings(Bindings bindings, int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            if (bindings == null) {
                throw new NullPointerException("Engine scope cannot be null.");
            }
            engineScope = bindings;
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            globalScope = bindings;
        } else {
            throw new IllegalArgumentException("Invalid scope value.");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The engine one hides the global one, and what decides is that the scope **has** the key,
     * not that the value be non-null.
     */
    @Override
    public Object getAttribute(String name) {
        checkName(name);
        if (engineScope.containsKey(name)) {
            return getAttribute(name, ScriptContext.ENGINE_SCOPE);
        } else if (globalScope != null && globalScope.containsKey(name)) {
            return getAttribute(name, ScriptContext.GLOBAL_SCOPE);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if `name` is empty or `scope` is neither 100 nor 200
     * @throws NullPointerException if `name` is null
     */
    @Override
    public Object getAttribute(String name, int scope) {
        checkName(name);
        if (scope == ScriptContext.ENGINE_SCOPE) {
            return engineScope.get(name);
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            if (globalScope != null) {
                return globalScope.get(name);
            }
            return null;
        }
        throw new IllegalArgumentException("Illegal scope value.");
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if `name` is empty or `scope` is neither 100 nor 200
     * @throws NullPointerException if `name` is null
     */
    @Override
    public Object removeAttribute(String name, int scope) {
        checkName(name);
        if (scope == ScriptContext.ENGINE_SCOPE) {
            if (getBindings(ScriptContext.ENGINE_SCOPE) != null) {
                return getBindings(ScriptContext.ENGINE_SCOPE).remove(name);
            }
            return null;
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            if (getBindings(ScriptContext.GLOBAL_SCOPE) != null) {
                return getBindings(ScriptContext.GLOBAL_SCOPE).remove(name);
            }
            return null;
        }
        throw new IllegalArgumentException("Illegal scope value.");
    }

    /**
     * {@inheritDoc}
     *
     * <p>With the global scope absent, writing to it does nothing and does not complain either: the
     * request was valid, the destination was not there.
     *
     * @throws IllegalArgumentException if `name` is empty or `scope` is neither 100 nor 200
     * @throws NullPointerException if `name` is null
     */
    @Override
    public void setAttribute(String name, Object value, int scope) {
        checkName(name);
        if (scope == ScriptContext.ENGINE_SCOPE) {
            engineScope.put(name, value);
            return;
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            if (globalScope != null) {
                globalScope.put(name, value);
            }
            return;
        }
        throw new IllegalArgumentException("Illegal scope value.");
    }

    /** {@inheritDoc} */
    @Override
    public Writer getWriter() {
        return writer;
    }

    /** {@inheritDoc} */
    @Override
    public Reader getReader() {
        return reader;
    }

    /** {@inheritDoc} */
    @Override
    public void setReader(Reader reader) {
        this.reader = reader;
    }

    /** {@inheritDoc} */
    @Override
    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    /** {@inheritDoc} */
    @Override
    public Writer getErrorWriter() {
        return errorWriter;
    }

    /** {@inheritDoc} */
    @Override
    public void setErrorWriter(Writer writer) {
        this.errorWriter = writer;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if `name` is empty
     * @throws NullPointerException if `name` is null
     */
    @Override
    public int getAttributesScope(String name) {
        checkName(name);
        if (engineScope.containsKey(name)) {
            return ScriptContext.ENGINE_SCOPE;
        } else if (globalScope != null && globalScope.containsKey(name)) {
            return ScriptContext.GLOBAL_SCOPE;
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if `scope` is neither 100 nor 200
     */
    @Override
    public Bindings getBindings(int scope) {
        if (scope == ScriptContext.ENGINE_SCOPE) {
            return engineScope;
        } else if (scope == ScriptContext.GLOBAL_SCOPE) {
            return globalScope;
        }
        throw new IllegalArgumentException("Illegal scope value.");
    }

    /** {@inheritDoc} */
    @Override
    public List<Integer> getScopes() {
        return scopes;
    }

    /** The name guard: null without a message, empty with a message. */
    private void checkName(String name) {
        Objects.requireNonNull(name);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
    }
}
