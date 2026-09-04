package java.beans.beancontext;

import java.beans.Beans;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.Visibility;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

/**
 * La implementación reusable de {@link BeanContext}.
 *
 * <p>Es a la vez la colección de los hijos y un hijo de otro contexto — hereda de
 * {@link BeanContextChildSupport} para no repetir la mitad de arriba de esa relación.
 *
 * <h2>Por qué los hijos van en un mapa y no en una lista</h2>
 *
 * <p>{@link #children} es un `HashMap` de bean a {@link BCSChild}, y no una lista, por dos cosas que
 * pasan seguido: `contains` y `remove` se llaman una vez por operación de membresía y con una lista
 * serían recorridos completos; y de cada hijo hay que guardar algo más que el hijo — su `BCSChild`
 * lleva quién lo trajo, que es lo que permite distinguir un bean agregado por sí mismo de uno que
 * llegó como delegado de otro.
 *
 * <h2>El candado</h2>
 *
 * <p>Todo lo que toca la membresía se sincroniza sobre {@link BeanContext#globalHierarchyLock}, no
 * sobre `this`. Ver la nota de {@link BeanContext}: una operación puede tocar dos contextos, y con
 * un candado por contexto dos mudanzas cruzadas se abrazan.
 *
 * <h2>La interfaz gráfica</h2>
 *
 * <p>{@link #needsGui} contesta preguntando a los hijos, uno por uno, en vez de guardar una bandera.
 * Es a propósito: la respuesta cambia cuando entra o sale un hijo, y una bandera obligaría a
 * recalcularla en cada alta y cada baja para contestar lo mismo.
 */
public class BeanContextSupport extends BeanContextChildSupport
        implements BeanContext, Serializable, PropertyChangeListener, VetoableChangeListener {

    /** Los hijos, de bean a su {@link BCSChild}. Ver la nota de la clase sobre por qué es un mapa. */
    protected transient HashMap children;

    /** Los oyentes de altas y bajas. */
    protected transient ArrayList bcmListeners;

    /** El idioma de este contexto. */
    protected Locale locale;

    /** Si está en modo diseño. */
    protected boolean designTime;

    /** Si se permite usar interfaz gráfica. */
    protected boolean okToUseGui;

    // Cuántos hijos serializables quedan por escribir; sólo vale durante `writeChildren`. Es lo que
    // hace que `isSerializing()` pueda contestar sin una bandera aparte.
    private transient int serializing;

    /** Un contexto sin padre, con el idioma por omisión, en modo diseño y con gráfica permitida. */
    public BeanContextSupport() {
        this(null, null, true, true);
    }

    /** Un contexto hijo de `peer`. */
    public BeanContextSupport(BeanContext peer) {
        this(peer, null, true, true);
    }

    /** Con ese idioma. */
    public BeanContextSupport(BeanContext peer, Locale lcle) {
        this(peer, lcle, true, true);
    }

    /** Con ese idioma y ese modo de diseño. */
    public BeanContextSupport(BeanContext peer, Locale lcle, boolean dTime) {
        this(peer, lcle, dTime, true);
    }

    /**
     * El constructor al que llegan todos los demás.
     *
     * @param peer el contexto a nombre del cual actúa, o `null` para actuar a nombre propio
     * @param lcle el idioma, o `null` para el del sistema
     * @param dTime si arranca en modo diseño
     * @param visible si se permite usar interfaz gráfica
     */
    public BeanContextSupport(BeanContext peer, Locale lcle, boolean dTime, boolean visible) {
        super(peer);
        this.locale = lcle == null ? Locale.getDefault() : lcle;
        this.designTime = dTime;
        this.okToUseGui = visible;
        this.initialize();
    }

    /** El contexto a nombre del cual actúa: este mismo, o el que delegó en él. */
    public BeanContext getBeanContextPeer() {
        return (BeanContext) this.getBeanContextChildPeer();
    }

    /**
     * Arma las estructuras internas.
     *
     * <p>Se llama desde el constructor y también al deserializar, que es el motivo de que sea un
     * método y no dos asignaciones en el constructor: un objeto que vuelve de un flujo no pasa por
     * ahí, y sin esto sus mapas quedarían nulos.
     */
    protected synchronized void initialize() {
        this.children = new HashMap();
        this.bcmListeners = new ArrayList();
    }

    // ---- membresía ----------------------------------------------------------------------------

    /** Cuántos hijos tiene. */
    public int size() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.size();
        }
    }

    /** Si no tiene ninguno. */
    public boolean isEmpty() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.isEmpty();
        }
    }

    /** Si ese objeto es hijo de este contexto. */
    public boolean contains(Object o) {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.containsKey(o);
        }
    }

    /**
     * Lo mismo que {@link #contains}.
     *
     * <p>Existen los dos porque los hijos se guardan en un mapa y `containsKey` es el nombre que esa
     * estructura usa. No es un alias caprichoso del JDK: quien mira la implementación busca
     * `containsKey`, y quien mira la colección busca `contains`.
     */
    public boolean containsKey(Object o) {
        return this.contains(o);
    }

    /** Si todos ésos son hijos de este contexto. */
    public boolean containsAll(Collection c) {
        synchronized (BeanContext.globalHierarchyLock) {
            Iterator it = c.iterator();
            while (it.hasNext()) {
                if (!this.contains(it.next())) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Los hijos, uno por uno. */
    public Iterator iterator() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.keySet().iterator();
        }
    }

    /** Los hijos, en un arreglo. */
    public Object[] toArray() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.keySet().toArray();
        }
    }

    /**
     * Los hijos, en ese arreglo si entran.
     *
     * <p>El JDK lo declara `Object[] toArray(Object[])` a secas, porque `BeanContext` extiende una
     * `Collection` **cruda** y ahí los miembros heredados vienen borrados. Acá va con parámetro de
     * tipo por un bug de nuestro javac: no borra los miembros de un supertipo crudo, así que
     * rechaza la forma del JDK (ver el informe del compilador). Las dos tienen el mismo borrado
     * --`Object[] toArray(Object[])`-- así que la firma que queda en el `.class` es la del JDK; lo
     * que cambia es la firma genérica, que acepta un poco más que la de allá.
     */
    public <T> T[] toArray(T[] arry) {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.keySet().toArray(arry);
        }
    }

    /** Los {@link BCSChild} de los hijos, uno por uno. */
    protected Iterator bcsChildren() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.values().iterator();
        }
    }

    /** Una copia del arreglo de hijos, para recorrer sin tener el candado tomado. */
    protected final Object[] copyChildren() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.children.keySet().toArray();
        }
    }

    /**
     * Agrega un hijo.
     *
     * <p>Lo que hace largo a este método es lo que tiene que pasar **antes** de que el bean quede
     * adentro: se valida, se le fija el contexto —que el hijo puede vetar— y recién después entra al
     * mapa. Si el orden fuera el otro, un veto dejaría un bean en la colección que no se cree
     * miembro.
     *
     * @return `false` si ya estaba
     * @throws IllegalStateException si el hijo vetó su mudanza a este contexto
     */
    public boolean add(Object targetChild) {
        if (targetChild == null) {
            throw new IllegalArgumentException("no se puede agregar null");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (this.children.containsKey(targetChild)) {
                return false;
            }
            if (!this.validatePendingAdd(targetChild)) {
                throw new IllegalStateException("el contexto rechaza a " + targetChild);
            }
            BeanContextChild cbcc = BeanContextSupport.getChildBeanContextChild(targetChild);
            if (cbcc != null) {
                try {
                    cbcc.setBeanContext(this.getBeanContextPeer());
                } catch (PropertyVetoException e) {
                    throw new IllegalStateException("el hijo vetó su ingreso al contexto", e);
                }
            }
            BCSChild bcsc = this.createBCSChild(targetChild, this.getBeanContextPeer());
            this.children.put(targetChild, bcsc);
            // Los oyentes se enganchan DESPUÉS de que el hijo ya está adentro: el contexto escucha
            // sus cambios de `beanContext` para enterarse si se muda por su cuenta, y engancharlos
            // antes haría que la propia mudanza de arriba se reciba como si fuera ajena.
            if (cbcc != null) {
                cbcc.addPropertyChangeListener("beanContext", this);
                cbcc.addVetoableChangeListener("beanContext", this);
            }
            Visibility v = BeanContextSupport.getChildVisibility(targetChild);
            if (v != null) {
                if (this.okToUseGui) {
                    v.okToUseGui();
                } else {
                    v.dontUseGui();
                }
            }
            if (targetChild instanceof java.beans.DesignMode) {
                ((java.beans.DesignMode) targetChild).setDesignTime(this.designTime);
            }
            this.childJustAddedHook(targetChild, bcsc);
            this.fireChildrenAdded(new BeanContextMembershipEvent(this.getBeanContextPeer(),
                    new Object[] { targetChild }));
            return true;
        }
    }

    /**
     * <strong>No está soportada.</strong> Siempre tira {@link UnsupportedOperationException}.
     *
     * <p>Es lo que hace el JDK, y no es una omisión suya: un alta puede fallar por su cuenta --el
     * hijo veta su mudanza, o el contexto lo rechaza-- y una operación en masa no tiene forma de
     * decir qué pasó a mitad de camino. Deshacer las que ya entraron dispararía eventos de baja de
     * cosas que nadie llegó a ver; dejarlas deja al llamador sin saber cuáles quedaron. Tirar es la
     * única respuesta que no miente.
     *
     * <p>Quien quiera agregar varios llama a {@link #add} en un bucle y decide él qué hacer con la
     * que falle.
     *
     * @throws UnsupportedOperationException siempre
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException(
                "un contexto no agrega en masa: ver el javadoc de addAll");
    }

    /** Quita un hijo. `false` si no estaba. */
    public boolean remove(Object targetChild) {
        return this.remove(targetChild, true);
    }

    /**
     * Quita un hijo, avisándole o no.
     *
     * <p>`callChildSetBC` en `false` es para cuando **el hijo ya se fue por su cuenta**: se enteró
     * este contexto por el evento de cambio de `beanContext`, y volver a llamarle `setBeanContext`
     * sería pisarle el contexto nuevo con un `null`.
     */
    protected boolean remove(Object targetChild, boolean callChildSetBC) {
        if (targetChild == null) {
            throw new IllegalArgumentException("no se puede quitar null");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (!this.children.containsKey(targetChild)) {
                return false;
            }
            if (!this.validatePendingRemove(targetChild)) {
                throw new IllegalStateException("el contexto rechaza la baja de " + targetChild);
            }
            BCSChild bcsc = (BCSChild) this.children.remove(targetChild);
            BeanContextChild cbcc = BeanContextSupport.getChildBeanContextChild(targetChild);
            if (cbcc != null) {
                cbcc.removePropertyChangeListener("beanContext", this);
                cbcc.removeVetoableChangeListener("beanContext", this);
                if (callChildSetBC) {
                    try {
                        cbcc.setBeanContext(null);
                    } catch (PropertyVetoException e) {
                        // El hijo no puede impedir su baja: ya no está en el mapa. Ver la nota de
                        // `rejectedSetBCOnce` en BeanContextChildSupport -- es el mismo caso, y por
                        // eso el veto acá se registra y no se propaga.
                        this.children.put(targetChild, bcsc);
                        throw new IllegalStateException("el hijo vetó su baja del contexto", e);
                    }
                }
            }
            this.childJustRemovedHook(targetChild, bcsc);
            this.fireChildrenRemoved(new BeanContextMembershipEvent(this.getBeanContextPeer(),
                    new Object[] { targetChild }));
            return true;
        }
    }

    /**
     * <strong>No está soportada.</strong> Ver {@link #addAll}, que lo explica.
     *
     * @throws UnsupportedOperationException siempre
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException(
                "un contexto no quita en masa: ver el javadoc de addAll");
    }

    /**
     * <strong>No está soportada.</strong> Ver {@link #addAll}, que lo explica.
     *
     * @throws UnsupportedOperationException siempre
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException(
                "un contexto no filtra en masa: ver el javadoc de addAll");
    }

    /**
     * <strong>No está soportada.</strong> Ver {@link #addAll}, que lo explica.
     *
     * @throws UnsupportedOperationException siempre
     */
    public void clear() {
        throw new UnsupportedOperationException(
                "un contexto no se vacía en masa: ver el javadoc de addAll");
    }

    /** La oportunidad de la subclase de rechazar un alta. Por omisión acepta. */
    protected boolean validatePendingAdd(Object targetChild) {
        return true;
    }

    /** La oportunidad de la subclase de rechazar una baja. Por omisión acepta. */
    protected boolean validatePendingRemove(Object targetChild) {
        return true;
    }

    /** El {@link BCSChild} de ese hijo. Una subclase lo redefine para guardar más. */
    protected BCSChild createBCSChild(Object targetChild, Object peer) {
        return new BCSChild(targetChild, peer);
    }

    /** Gancho de después del alta. Vacío por omisión. */
    protected void childJustAddedHook(Object child, BCSChild bcsc) {
    }

    /** Gancho de después de la baja. Vacío por omisión. */
    protected void childJustRemovedHook(Object child, BCSChild bcsc) {
    }

    /** Gancho de después de deserializar un hijo. Vacío por omisión. */
    protected void childDeserializedHook(Object child, BCSChild bcsc) {
    }

    // ---- los ayudantes de tipo -----------------------------------------------------------------
    //
    // Los seis siguen la misma regla y por eso van juntos: si el objeto ES del tipo, se devuelve; si
    // no, se le pregunta a su `BeanContextProxy` por el delegado y se prueba con ése. Es lo que hace
    // que un bean que delega participe igual que uno que implementa directo.

    /** Ese objeto como {@link BeanContextChild}, directo o por delegación, o `null`. */
    protected static final BeanContextChild getChildBeanContextChild(Object child) {
        if (child instanceof BeanContextChild) {
            return (BeanContextChild) child;
        }
        if (child instanceof BeanContextProxy) {
            return ((BeanContextProxy) child).getBeanContextProxy();
        }
        return null;
    }

    /** Ese objeto como oyente de membresía, o `null`. */
    protected static final BeanContextMembershipListener getChildBeanContextMembershipListener(
            Object child) {
        if (child instanceof BeanContextMembershipListener) {
            return (BeanContextMembershipListener) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof BeanContextMembershipListener) {
                return (BeanContextMembershipListener) d;
            }
        }
        return null;
    }

    /** Ese objeto como oyente de cambio de propiedad, o `null`. */
    protected static final PropertyChangeListener getChildPropertyChangeListener(Object child) {
        if (child instanceof PropertyChangeListener) {
            return (PropertyChangeListener) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof PropertyChangeListener) {
                return (PropertyChangeListener) d;
            }
        }
        return null;
    }

    /** Ese objeto como oyente de veto, o `null`. */
    protected static final VetoableChangeListener getChildVetoableChangeListener(Object child) {
        if (child instanceof VetoableChangeListener) {
            return (VetoableChangeListener) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof VetoableChangeListener) {
                return (VetoableChangeListener) d;
            }
        }
        return null;
    }

    /** Ese objeto como {@link Serializable}, o `null`. */
    protected static final Serializable getChildSerializable(Object child) {
        if (child instanceof Serializable) {
            return (Serializable) child;
        }
        return null;
    }

    /** Ese objeto como {@link Visibility}, o `null`. */
    protected static final Visibility getChildVisibility(Object child) {
        if (child instanceof Visibility) {
            return (Visibility) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof Visibility) {
                return (Visibility) d;
            }
        }
        return null;
    }

    /**
     * Si las dos clases son la misma.
     *
     * <p>Compara por nombre y no con `==` porque **dos cargadores distintos dan dos `Class` distintas
     * para el mismo tipo**, y en una jerarquía de beans es normal que los hijos vengan de cargadores
     * distintos. Con `==`, un servicio registrado por un hijo no lo encontraría otro.
     */
    protected static final boolean classEquals(Class first, Class second) {
        return first.equals(second) || first.getName().equals(second.getName());
    }

    // ---- recursos y creación --------------------------------------------------------------------

    /**
     * El recurso, buscado con el cargador del hijo que pregunta.
     *
     * <p>Que se use **su** cargador y no el del contexto es lo que hace útil al método: un hijo que
     * vino de otro `.jar` tiene sus recursos ahí, no acá.
     */
    public InputStream getResourceAsStream(String name, BeanContextChild bcc) {
        if (name == null || bcc == null) {
            throw new NullPointerException();
        }
        ClassLoader cl = bcc.getClass().getClassLoader();
        return cl == null ? ClassLoader.getSystemResourceAsStream(name)
                : cl.getResourceAsStream(name);
    }

    /** La URL del recurso, con el mismo criterio. */
    public URL getResource(String name, BeanContextChild bcc) {
        if (name == null || bcc == null) {
            throw new NullPointerException();
        }
        ClassLoader cl = bcc.getClass().getClassLoader();
        return cl == null ? ClassLoader.getSystemResource(name) : cl.getResource(name);
    }

    /**
     * Instancia ese bean y lo agrega a este contexto.
     *
     * @throws IOException si el bean no se pudo leer
     * @throws ClassNotFoundException si no se encontró la clase
     */
    public Object instantiateChild(String beanName) throws IOException, ClassNotFoundException {
        BeanContext bc = this.getBeanContextPeer();
        return Beans.instantiate(bc.getClass().getClassLoader(), beanName, bc);
    }

    // ---- oyentes de membresía -------------------------------------------------------------------

    /** Registra un oyente de altas y bajas. */
    public void addBeanContextMembershipListener(BeanContextMembershipListener bcml) {
        if (bcml == null) {
            throw new NullPointerException("bcml");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (!this.bcmListeners.contains(bcml)) {
                this.bcmListeners.add(bcml);
            }
        }
    }

    /** Lo quita. */
    public void removeBeanContextMembershipListener(BeanContextMembershipListener bcml) {
        if (bcml == null) {
            throw new NullPointerException("bcml");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            this.bcmListeners.remove(bcml);
        }
    }

    // Los eventos se reparten sobre una COPIA de la lista de oyentes. Un oyente que se da de baja a
    // sí mismo mientras lo notifican es lo normal --se entera de la baja de un hijo y se retira-- y
    // sin la copia eso es una modificación concurrente en el medio del recorrido.
    private Object[] listenersCopy() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.bcmListeners.toArray();
        }
    }

    /** Avisa de las altas. */
    protected final void fireChildrenAdded(BeanContextMembershipEvent bcme) {
        Object[] ls = this.listenersCopy();
        for (int i = 0; i < ls.length; i++) {
            ((BeanContextMembershipListener) ls[i]).childrenAdded(bcme);
        }
    }

    /** Avisa de las bajas. */
    protected final void fireChildrenRemoved(BeanContextMembershipEvent bcme) {
        Object[] ls = this.listenersCopy();
        for (int i = 0; i < ls.length; i++) {
            ((BeanContextMembershipListener) ls[i]).childrenRemoved(bcme);
        }
    }

    // ---- modo diseño, idioma y gráfica ----------------------------------------------------------

    /** Si está en modo diseño. */
    public synchronized boolean isDesignTime() {
        return this.designTime;
    }

    /** Fija el modo diseño y se lo propaga a los hijos que lo entiendan. */
    public synchronized void setDesignTime(boolean dTime) {
        if (this.designTime == dTime) {
            return;
        }
        boolean old = this.designTime;
        this.designTime = dTime;
        Object[] todos = this.copyChildren();
        for (int i = 0; i < todos.length; i++) {
            if (todos[i] instanceof java.beans.DesignMode) {
                ((java.beans.DesignMode) todos[i]).setDesignTime(dTime);
            }
        }
        this.firePropertyChange(java.beans.DesignMode.PROPERTYNAME, Boolean.valueOf(old),
                Boolean.valueOf(dTime));
    }

    /** El idioma. */
    public synchronized Locale getLocale() {
        return this.locale;
    }

    /**
     * Fija el idioma.
     *
     * @throws PropertyVetoException si un oyente se opone
     */
    public synchronized void setLocale(Locale newLocale) throws PropertyVetoException {
        if (newLocale == null || newLocale.equals(this.locale)) {
            return;
        }
        Locale old = this.locale;
        this.fireVetoableChange("locale", old, newLocale);
        this.locale = newLocale;
        this.firePropertyChange("locale", old, newLocale);
    }

    /**
     * Si algún hijo necesita interfaz gráfica. Ver la nota de la clase.
     *
     * <p>El JDK contesta `true` además para cualquier hijo que sea un `java.awt.Component`, porque
     * un componente necesita pantalla por definición. Acá no se puede preguntar eso: `java.awt` en
     * esta biblioteca llega hasta la geometría y el color, y no hay jerarquía de componentes. La
     * consecuencia es acotada y conviene tenerla presente -- un hijo que sea un componente y **no**
     * implemente {@link Visibility} se cuenta como que no necesita gráfica.
     */
    public synchronized boolean needsGui() {
        Object[] todos = this.copyChildren();
        for (int i = 0; i < todos.length; i++) {
            Visibility v = BeanContextSupport.getChildVisibility(todos[i]);
            if (v != null && v.needsGui()) {
                return true;
            }
        }
        return false;
    }

    /** Prohíbe la gráfica, acá y en los hijos que lo entiendan. */
    public synchronized void dontUseGui() {
        this.okToUseGui = false;
        Object[] todos = this.copyChildren();
        for (int i = 0; i < todos.length; i++) {
            Visibility v = BeanContextSupport.getChildVisibility(todos[i]);
            if (v != null) {
                v.dontUseGui();
            }
        }
    }

    /** La permite, acá y en los hijos que lo entiendan. */
    public synchronized void okToUseGui() {
        this.okToUseGui = true;
        Object[] todos = this.copyChildren();
        for (int i = 0; i < todos.length; i++) {
            Visibility v = BeanContextSupport.getChildVisibility(todos[i]);
            if (v != null) {
                v.okToUseGui();
            }
        }
    }

    /** Si se está evitando la gráfica: se prohibió y además hay quien la necesitaría. */
    public synchronized boolean avoidingGui() {
        return !this.okToUseGui && this.needsGui();
    }

    // ---- serialización --------------------------------------------------------------------------

    /** Si en este momento se están escribiendo los hijos. */
    public boolean isSerializing() {
        return this.serializing > 0;
    }

    /**
     * Escribe los hijos serializables.
     *
     * <p>Los que **no** lo son se saltean en silencio, y esa es la decisión de fondo de este método:
     * un contexto es una colección de beans ajenos y no puede exigir que todos sean serializables.
     * Romper la escritura entera por un hijo que no lo es haría inservible la de los demás.
     *
     * @throws IOException si el flujo falla
     */
    public final void writeChildren(ObjectOutputStream oos) throws IOException {
        synchronized (BeanContext.globalHierarchyLock) {
            this.serializing = this.serializing + 1;
            try {
                Object[] todos = this.copyChildren();
                int n = 0;
                for (int i = 0; i < todos.length; i++) {
                    if (BeanContextSupport.getChildSerializable(todos[i]) != null) {
                        n = n + 1;
                    }
                }
                oos.writeInt(n);
                for (int i = 0; i < todos.length; i++) {
                    Serializable s = BeanContextSupport.getChildSerializable(todos[i]);
                    if (s != null) {
                        oos.writeObject(s);
                    }
                }
            } finally {
                this.serializing = this.serializing - 1;
            }
        }
    }

    /**
     * Lee los hijos que {@link #writeChildren} escribió y los agrega.
     *
     * @throws IOException si el flujo falla
     * @throws ClassNotFoundException si falta la clase de algún hijo
     */
    public final void readChildren(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        synchronized (BeanContext.globalHierarchyLock) {
            int n = ois.readInt();
            for (int i = 0; i < n; i++) {
                Object child = ois.readObject();
                this.add(child);
                BCSChild bcsc = (BCSChild) this.children.get(child);
                this.childDeserializedHook(child, bcsc);
            }
        }
    }

    /** Escribe esa colección, salteando lo que no sea serializable. */
    protected final void serialize(ObjectOutputStream oos, Collection coll) throws IOException {
        Object[] todos = coll.toArray();
        int n = 0;
        for (int i = 0; i < todos.length; i++) {
            if (todos[i] instanceof Serializable) {
                n = n + 1;
            }
        }
        oos.writeInt(n);
        for (int i = 0; i < todos.length; i++) {
            if (todos[i] instanceof Serializable) {
                oos.writeObject(todos[i]);
            }
        }
    }

    /** Lee en esa colección lo que {@link #serialize} escribió. */
    protected final void deserialize(ObjectInputStream ois, Collection coll)
            throws IOException, ClassNotFoundException {
        int n = ois.readInt();
        for (int i = 0; i < n; i++) {
            coll.add(ois.readObject());
        }
    }

    /** Gancho de antes de escribir. Vacío por omisión. */
    protected void bcsPreSerializationHook(ObjectOutputStream oos) throws IOException {
    }

    /** Gancho de antes de leer. Vacío por omisión. */
    protected void bcsPreDeserializationHook(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
    }

    // ---- lo que este contexto escucha de sus hijos ----------------------------------------------

    /**
     * Un hijo cambió de contexto por su cuenta.
     *
     * <p>Si se fue a otro lado, este contexto lo saca de su colección **sin volver a avisarle** —ya
     * está donde quiere estar, y un `setBeanContext(null)` acá le pisaría el contexto nuevo. Ese es
     * exactamente el caso para el que existe `remove(Object, boolean)`.
     */
    public void propertyChange(PropertyChangeEvent pce) {
        if (!"beanContext".equals(pce.getPropertyName())) {
            return;
        }
        Object nuevo = pce.getNewValue();
        if (nuevo != this.getBeanContextPeer()) {
            this.remove(pce.getSource(), false);
        }
    }

    /**
     * Un hijo pregunta si puede cambiar de contexto.
     *
     * <p>Este contexto no se opone: un hijo que se quiere ir se va. Está escrito y no heredado
     * porque la interfaz lo exige, y porque el lugar natural donde alguien buscaría una política de
     * retención es éste.
     */
    public void vetoableChange(PropertyChangeEvent pce) throws PropertyVetoException {
    }

    /**
     * Lo que este contexto guarda de cada hijo.
     *
     * <p>Es más que el hijo: lleva también **quién lo trajo**, y de ahí que el constructor tome dos
     * cosas. Para un bean que entró por sí mismo los dos son distintos —el segundo es el contexto—;
     * para uno que llegó como delegado de otro, el `peer` es ese otro. Sin ese dato no habría forma
     * de deshacer bien la baja de un delegado.
     */
    protected class BCSChild implements Serializable {

        private final Object child;
        private final Object proxyPeer;

        BCSChild(Object bcc, Object peer) {
            this.child = bcc;
            this.proxyPeer = peer;
        }

        /** El hijo. */
        Object getChild() {
            return this.child;
        }

        /** Quién lo trajo. */
        Object getProxyPeer() {
            return this.proxyPeer;
        }
    }
}
