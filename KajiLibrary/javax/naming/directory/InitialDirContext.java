package javax.naming.directory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.NotContextException;

/**
 * KajiLibrary's javax.naming.directory.InitialDirContext -- el punto de entrada a un directorio.
 *
 * <p>Lo mismo que {@link InitialContext} pero para {@link DirContext}: no implementa ninguna
 * operacion, resuelve <b>que</b> contexto atiende cada nombre y le reenvia la llamada. Los
 * veintiocho metodos son el mismo gesto repetido.
 *
 * <p>El constructor protegido con un {@code boolean} existe para las subclases que quieren
 * inicializarse en dos pasos: con {@code true} el constructor <b>no</b> arma el contexto, y la
 * subclase lo hace despues con {@code init}. Sirve cuando la configuracion se calcula en el
 * constructor de la subclase, o sea despues de que el de la clase base ya corrio.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae ningun proveedor JNDI --no hay LDAP, no hay DNS-- asi que
 * {@code getDefaultInitCtx()} de {@link InitialContext} lanza {@link NoInitialContextException} y
 * todas las operaciones de aca terminan ahi. Es la salida que el API ya declara para "no hay
 * proveedor configurado", y con uno registrado esto funciona sin cambios.
 *
 * <p>Ademas, un proveedor que resuelva un nombre a un {@link Context} que <b>no</b> sea
 * {@link DirContext} produce {@link NotContextException}: pedirle atributos a un contexto que no es
 * un directorio no tiene sentido, y decirlo con el tipo correcto es mejor que un
 * {@code ClassCastException}.
 */
public class InitialDirContext extends InitialContext implements DirContext {

    /**
     * Para una subclase que se inicializa en dos pasos.
     *
     * @param lazy con true, no se arma el contexto todavia; ver la nota de la clase
     */
    protected InitialDirContext(boolean lazy) throws NamingException {
        super(lazy);
    }

    /** Con el ambiente por omision. */
    public InitialDirContext() throws NamingException {
        super();
    }

    /** Con un ambiente propio. */
    public InitialDirContext(Hashtable<?, ?> environment) throws NamingException {
        super(environment);
    }

    /**
     * El contexto que atiende ese nombre, ya comprobado como directorio.
     *
     * @throws NotContextException si lo que resuelve no es un {@link DirContext}
     */
    private DirContext dirOf(Name name) throws NamingException {
        return asDir(getURLOrDefaultInitCtx(name));
    }

    /** Idem, con el nombre como texto. */
    private DirContext dirOf(String name) throws NamingException {
        return asDir(getURLOrDefaultInitCtx(name));
    }

    /** La comprobacion comun; ver la nota de la clase. */
    private static DirContext asDir(Context ctx) throws NamingException {
        if (ctx instanceof DirContext) {
            return (DirContext) ctx;
        }
        if (ctx == null) {
            throw new NoInitialContextException("No initial directory context");
        }
        throw new NotContextException(
            "Not an instance of DirContext: " + ctx.getClass().getName());
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public Attributes getAttributes(Name name) throws NamingException {
        return dirOf(name).getAttributes(name);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public Attributes getAttributes(String name) throws NamingException {
        return dirOf(name).getAttributes(name);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return dirOf(name).getAttributes(name, attrIds);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return dirOf(name).getAttributes(name, attrIds);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        dirOf(name).modifyAttributes(name, mod_op, attrs);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        dirOf(name).modifyAttributes(name, mod_op, attrs);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        dirOf(name).modifyAttributes(name, mods);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        dirOf(name).modifyAttributes(name, mods);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        dirOf(name).bind(name, obj, attrs);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        dirOf(name).bind(name, obj, attrs);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        dirOf(name).rebind(name, obj, attrs);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        dirOf(name).rebind(name, obj, attrs);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return dirOf(name).createSubcontext(name, attrs);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return dirOf(name).createSubcontext(name, attrs);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public DirContext getSchema(Name name) throws NamingException {
        return dirOf(name).getSchema(name);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public DirContext getSchema(String name) throws NamingException {
        return dirOf(name).getSchema(name);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return dirOf(name).getSchemaClassDefinition(name);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return dirOf(name).getSchemaClassDefinition(name);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return dirOf(name).search(name, matchingAttributes, attributesToReturn);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn) throws NamingException {
        return dirOf(name).search(name, matchingAttributes, attributesToReturn);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        return dirOf(name).search(name, matchingAttributes);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return dirOf(name).search(name, matchingAttributes);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        return dirOf(name).search(name, filter, cons);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        return dirOf(name).search(name, filter, cons);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return dirOf(name).search(name, filterExpr, filterArgs, cons);
    }

    /** Reenvia al contexto que atiende el nombre; ver la nota de la clase. */
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons) throws NamingException {
        return dirOf(name).search(name, filterExpr, filterArgs, cons);
    }
}
