package javax.naming;

import java.util.Enumeration;
import java.util.Vector;

/**
 * La receta para reconstruir un objeto: el nombre de su clase, sus direcciones, y quien lo arma.
 *
 * <h2>Por que no se ata el objeto</h2>
 *
 * <p>Un servicio de nombres guarda datos, no memoria de otro proceso. Atar un `DataSource` vivo no
 * significa nada para el que hace `lookup` desde otra maquina media hora despues. Lo que se ata es
 * esto: "clase `javax.sql.DataSource`, fabrica `com.x.DsFactory`, y estas direcciones --esta URL,
 * este usuario--". El que resuelve carga la fabrica y le pasa la referencia, y recibe un objeto
 * equivalente, no el mismo.
 *
 * <p>De ahi los tres campos de identidad. `className` es lo que el que hace `lookup` **espera**
 * recibir, y sirve para filtrar sin construir nada. `classFactory` es quien sabe construirlo, y
 * `classFactoryLocation` de donde bajar esa fabrica si no esta en el classpath --una URL de
 * codebase--, que puede ser `null` y casi siempre lo es.
 *
 * <h2>Por que las direcciones son una lista y no un mapa</h2>
 *
 * <p>Porque el orden importa y los tipos se repiten: un servicio con tres replicas tiene tres
 * direcciones de tipo `"URL"`, y el orden es la preferencia. `get(String)` devuelve **la primera**
 * de ese tipo, que es la interpretacion util de "dame la URL"; el que las quiere todas usa
 * `getAll()`.
 *
 * <h2>Igualdad y clonado</h2>
 *
 * <p>`equals` compara la clase y las direcciones **en orden**, y a proposito ignora la fabrica: dos
 * referencias al mismo objeto con distinta manera de construirlo describen el mismo objeto. El
 * javadoc del JDK lo dice explicitamente y no es un descuido.
 *
 * <p>`clone` es superficial en las direcciones --copia la lista, no los `RefAddr`--. Se puede
 * porque un `RefAddr` es inmutable en la practica: ninguna subclase del paquete tiene setters.
 */
public class Reference implements Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -1673475790065791735L;

    /** La clase del objeto que esta referencia describe. */
    protected String className;

    /** Las direcciones, en orden de preferencia. */
    protected Vector<RefAddr> addrs = null;

    /** Quien sabe construir el objeto; `null` si el que resuelve tiene que arreglarselas. */
    protected String classFactory = null;

    /** De donde bajar la fabrica si no esta en el classpath. Casi siempre `null`. */
    protected String classFactoryLocation = null;

    public Reference(String className) {
        this.className = className;
        addrs = new Vector<RefAddr>();
    }

    public Reference(String className, RefAddr addr) {
        this.className = className;
        addrs = new Vector<RefAddr>();
        addrs.addElement(addr);
    }

    public Reference(String className, String factory, String factoryLocation) {
        this(className);
        classFactory = factory;
        classFactoryLocation = factoryLocation;
    }

    public Reference(String className, RefAddr addr, String factory, String factoryLocation) {
        this(className, addr);
        classFactory = factory;
        classFactoryLocation = factoryLocation;
    }

    public String getClassName() {
        return className;
    }

    public String getFactoryClassName() {
        return classFactory;
    }

    public String getFactoryClassLocation() {
        return classFactoryLocation;
    }

    /** La **primera** direccion de ese tipo, que es la preferida; `null` si no hay ninguna. */
    public RefAddr get(String addrType) {
        int len = addrs.size();
        for (int i = 0; i < len; i++) {
            RefAddr addr = addrs.elementAt(i);
            if (addr.getType().compareTo(addrType) == 0) {
                return addr;
            }
        }
        return null;
    }

    public RefAddr get(int posn) {
        return addrs.elementAt(posn);
    }

    public Enumeration<RefAddr> getAll() {
        return addrs.elements();
    }

    public int size() {
        return addrs.size();
    }

    public void add(RefAddr addr) {
        addrs.addElement(addr);
    }

    public void add(int posn, RefAddr addr) {
        addrs.insertElementAt(addr, posn);
    }

    /** Devuelve `Object` y no `RefAddr` por la edad de la API; siempre es un `RefAddr`. */
    public Object remove(int posn) {
        Object r = addrs.elementAt(posn);
        addrs.removeElementAt(posn);
        return r;
    }

    public void clear() {
        addrs.setSize(0);
    }

    /** Clase y direcciones en orden. **No** mira la fabrica: es del contrato, no un olvido. */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Reference) {
            Reference target = (Reference) obj;
            if (target.className.equals(this.className) && target.size() == this.size()) {
                Enumeration<RefAddr> mycomps = getAll();
                Enumeration<RefAddr> comps = target.getAll();
                while (mycomps.hasMoreElements()) {
                    if (!mycomps.nextElement().equals(comps.nextElement())) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = className.hashCode();
        for (Enumeration<RefAddr> e = getAll(); e.hasMoreElements(); ) {
            hash += e.nextElement().hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("Reference Class Name: ");
        buf.append(className).append("\n");
        int len = addrs.size();
        for (int i = 0; i < len; i++) {
            buf.append(get(i).toString());
        }
        return buf.toString();
    }

    /**
     * Copia con lista de direcciones propia, pero compartiendo los `RefAddr`.
     *
     * <p>Alcanza porque un `RefAddr` no tiene setters: se puede compartir sin que nadie lo cambie
     * por debajo. Lo que si hay que copiar es la lista, que es lo que `add`/`remove` mutan.
     */
    @Override
    public Object clone() {
        Reference r = new Reference(className, classFactory, classFactoryLocation);
        Enumeration<RefAddr> a = getAll();
        r.addrs = new Vector<RefAddr>();
        while (a.hasMoreElements()) {
            r.addrs.addElement(a.nextElement());
        }
        return r;
    }
}
