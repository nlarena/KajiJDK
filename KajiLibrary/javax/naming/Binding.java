package javax.naming;

/**
 * Un `NameClassPair` **con el objeto adentro**: lo que devuelve `Context.listBindings()`.
 *
 * <p>La diferencia con listar nombres es el costo, y esta explicada en `NameClassPair`. Lo que esta
 * clase agrega es el objeto ya materializado, para el que de verdad los va a usar a todos.
 *
 * <h2>Por que hay ocho constructores y por que `getClassName` esta redefinido</h2>
 *
 * <p>El nombre de clase se puede deducir del objeto, asi que la mitad de los constructores no lo
 * piden: `getClassName()` mira primero el que se declaro y, si no hay, pregunta
 * `getObject().getClass().getName()`. La deduccion no siempre alcanza --el proveedor puede saber
 * que el objeto atado es de una clase que aca ni existe, o el objeto puede ser `null` y el nombre
 * de clase saberse igual--, y por eso los otros constructores dejan declararlo.
 *
 * <p>Y devuelve `null` cuando no hay ninguna de las dos cosas, en vez de tirar: un `list` de un
 * contexto medio roto tiene que poder devolver la fila.
 */
public class Binding extends NameClassPair {

    private static final long serialVersionUID = 8839217842691845890L;

    private Object boundObj;

    /** El nombre de clase queda sin declarar: se deduce del objeto en `getClassName()`. */
    public Binding(String name, Object obj) {
        super(name, null);
        this.boundObj = obj;
    }

    public Binding(String name, Object obj, boolean isRelative) {
        super(name, null, isRelative);
        this.boundObj = obj;
    }

    public Binding(String name, String className, Object obj) {
        super(name, className);
        this.boundObj = obj;
    }

    public Binding(String name, String className, Object obj, boolean isRelative) {
        super(name, className, isRelative);
        this.boundObj = obj;
    }

    /** El declarado gana; si no hay, se deduce del objeto; si tampoco hay objeto, `null`. */
    @Override
    public String getClassName() {
        String cname = super.getClassName();
        if (cname != null) {
            return cname;
        }
        Object obj = getObject();
        return (obj != null) ? obj.getClass().getName() : null;
    }

    public Object getObject() {
        return boundObj;
    }

    public void setObject(Object obj) {
        this.boundObj = obj;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + getObject();
    }
}
