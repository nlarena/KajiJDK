package javax.script;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Objects;

/**
 * KajiLibrary's javax.script.SimpleScriptContext -- el {@link ScriptContext} con dos ambitos.
 *
 * <p>Implementa exactamente los dos ambitos que define la interfaz y ni uno mas, con una asimetria
 * que es toda la clase:
 *
 * <ul>
 *   <li>El de motor **siempre existe**. Arranca en un {@link SimpleBindings} vacio y no se puede
 *       poner en nulo: {@code setBindings(null, ENGINE_SCOPE)} es un
 *       {@link NullPointerException}.
 *   <li>El global **puede no existir**, y de hecho arranca en nulo. Ponerlo en nulo esta
 *       permitido, y con el global ausente escribir en el se ignora en silencio y leerlo devuelve
 *       nulo -- ninguna de las dos cosas es un error.
 * </ul>
 *
 * <p>De ahi salen las reglas de busqueda. {@link #getAttribute(String)} mira primero el de motor y
 * despues el global, y devuelve lo del primero que **tenga la clave** -- no lo primero que no sea
 * nulo, que es distinto cuando el valor guardado es nulo. {@link #getAttributesScope(String)}
 * hace la misma busqueda pero devuelve el numero, o -1 si no esta en ninguno.
 *
 * <p>Un ambito que no sea 100 ni 200 es siempre un {@link IllegalArgumentException}. El mensaje no
 * es siempre el mismo, y lo copiamos como esta: `setBindings` dice "Invalid scope value." y todo
 * el resto dice "Illegal scope value.". Es una inconsistencia del original, pero es observable.
 *
 * <p>Los nombres de atributo tienen su propia guarda, mas floja que la de {@link SimpleBindings}:
 * nulo es {@link NullPointerException} sin mensaje y vacio es {@link IllegalArgumentException} con
 * "name cannot be empty". Como el parametro ya es `String`, no hay caso de tipo.
 *
 * <p><b>Nota de implementacion.</b> El despacho por ambito se escribe con cadenas de `if/else` y
 * no con `switch`, que es como lo tiene el original. No es una preferencia: nuestro generador de
 * bytecode todavia no pliega como constante de `case` un valor declarado en **otro tipo de primer
 * nivel** --y `ENGINE_SCOPE` vive en {@link ScriptContext}--, asi que `case ScriptContext.ENGINE_SCOPE`
 * no compila. Se comprobo por ablacion: con la constante declarada en la misma unidad de
 * compilacion el `switch` compila, y con ella en otro archivo falla aunque los dos se pasen a la
 * misma invocacion de `javac`. El comportamiento observable es identico -- cada rama terminaba en
 * `return`, `break` o `throw`.
 */
public class SimpleScriptContext implements ScriptContext {

    /** Donde escribe el script. */
    protected Writer writer;

    /** Donde escribe el script sus errores. */
    protected Writer errorWriter;

    /** De donde lee el script. */
    protected Reader reader;

    /** El ambito de motor. Nunca es nulo. */
    protected Bindings engineScope;

    /** El ambito global. Puede ser nulo, y arranca asi. */
    protected Bindings globalScope;

    /** Los dos ambitos, inmutable y compartido: no depende de la instancia. */
    private static final List<Integer> scopes =
            List.of(Integer.valueOf(ScriptContext.ENGINE_SCOPE),
                    Integer.valueOf(ScriptContext.GLOBAL_SCOPE));

    /**
     * Un contexto con el ambito de motor vacio, el global ausente, y los tres canales apuntando a
     * la consola del proceso.
     */
    public SimpleScriptContext() {
        this(new InputStreamReader(System.in),
             new PrintWriter(System.out, true),
             new PrintWriter(System.err, true));
    }

    /** El que hace el trabajo; el publico le pasa la consola. */
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
     * @throws NullPointerException si `bindings` es nulo y `scope` es {@link ScriptContext#ENGINE_SCOPE}
     * @throws IllegalArgumentException si `scope` no es 100 ni 200
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
     * <p>El de motor tapa al global, y lo que decide es que el ambito **tenga** la clave, no que
     * el valor no sea nulo.
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
     * @throws IllegalArgumentException si `name` es vacio o `scope` no es 100 ni 200
     * @throws NullPointerException si `name` es nulo
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
     * @throws IllegalArgumentException si `name` es vacio o `scope` no es 100 ni 200
     * @throws NullPointerException si `name` es nulo
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
     * <p>Con el ambito global ausente, escribir en el no hace nada y tampoco se queja: el pedido
     * era valido, el destino no estaba.
     *
     * @throws IllegalArgumentException si `name` es vacio o `scope` no es 100 ni 200
     * @throws NullPointerException si `name` es nulo
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
     * @throws IllegalArgumentException si `name` es vacio
     * @throws NullPointerException si `name` es nulo
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
     * @throws IllegalArgumentException si `scope` no es 100 ni 200
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

    /** La guarda de nombres: nulo sin mensaje, vacio con mensaje. */
    private void checkName(String name) {
        Objects.requireNonNull(name);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
    }
}
