package java.beans.beancontext;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TooManyListenersException;

/**
 * La implementación reusable de {@link BeanContextServices}.
 *
 * <p>Agrega los servicios sobre lo que {@link BeanContextSupport} ya hace con la membresía. La idea
 * de fondo es una sola y explica casi todo el archivo: **un servicio se busca hacia arriba**. Si
 * este contexto no lo tiene, se le pregunta al padre, y así hasta la raíz.
 *
 * <h2>El proveedor delegado</h2>
 *
 * <p>Cuando un servicio viene de más arriba, este contexto no reenvía el pedido cada vez: registra
 * un {@link BCSSProxyServiceProvider} que se hace pasar por proveedor local y por dentro habla con
 * el contexto padre. Así los hijos de acá ven un proveedor como cualquier otro, y la revocación de
 * arriba llega igual — el proxy es también el oyente que la recibe.
 *
 * <h2>Qué se anota de cada pedido</h2>
 *
 * <p>De cada instancia entregada se guarda **quién la pidió** y **con qué oyente de revocación**.
 * Sin eso, `releaseService` no podría saber qué está soltando, y una revocación no sabría a quién
 * avisar. Es la razón de que {@link BCSSServiceProvider} lleve más que el proveedor.
 */
public class BeanContextServicesSupport extends BeanContextSupport implements BeanContextServices {

    /** Los servicios registrados acá, de clase de servicio a su {@link BCSSServiceProvider}. */
    protected transient HashMap services;

    /** Los oyentes de altas y bajas de servicios. */
    protected transient ArrayList bcsListeners;

    /** Cuántos servicios de este contexto son serializables. */
    protected transient int serializable;

    /** El proveedor que representa a los servicios que vienen del contexto padre. */
    protected transient BCSSProxyServiceProvider proxy;

    /** Un contexto de servicios sin padre. */
    public BeanContextServicesSupport() {
        this(null, null, true, true);
    }

    /** Hijo de `peer`. */
    public BeanContextServicesSupport(BeanContextServices peer) {
        this(peer, null, true, true);
    }

    /** Con ese idioma. */
    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle) {
        this(peer, lcle, true, true);
    }

    /** Con ese idioma y ese modo de diseño. */
    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle, boolean dTime) {
        this(peer, lcle, dTime, true);
    }

    /** El constructor al que llegan los demás. */
    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle, boolean dTime,
            boolean visible) {
        super(peer, lcle, dTime, visible);
    }

    /** El contexto de servicios a nombre del cual actúa. */
    public BeanContextServices getBeanContextServicesPeer() {
        return (BeanContextServices) this.getBeanContextChildPeer();
    }

    /**
     * Arma las estructuras internas.
     *
     * <p>Llama a la de la superclase primero: los hijos tienen que existir antes que los servicios,
     * porque un servicio se le puede ofrecer a un hijo apenas se registra.
     */
    public void initialize() {
        super.initialize();
        this.services = new HashMap();
        this.bcsListeners = new ArrayList();
    }

    // ---- alta y baja de servicios ---------------------------------------------------------------

    /** Da de alta un proveedor. `false` si ya había uno para esa clase. */
    public boolean addService(Class serviceClass, BeanContextServiceProvider bcsp) {
        return this.addService(serviceClass, bcsp, true);
    }

    /**
     * Da de alta un proveedor, avisando o no.
     *
     * <p>`fireEvent` en `false` es para el alta que hace el propio contexto al descubrir un servicio
     * del padre: los hijos ya se van a enterar por el evento que viene de arriba, y avisar de nuevo
     * les llegaría dos veces.
     */
    protected boolean addService(Class serviceClass, BeanContextServiceProvider bcsp,
            boolean fireEvent) {
        if (serviceClass == null || bcsp == null) {
            throw new NullPointerException();
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (this.services.containsKey(serviceClass)) {
                return false;
            }
            this.services.put(serviceClass, this.createBCSSServiceProvider(serviceClass, bcsp));
            if (bcsp instanceof Serializable) {
                this.serializable = this.serializable + 1;
            }
            if (fireEvent) {
                this.fireServiceAdded(serviceClass);
            }
            return true;
        }
    }

    /**
     * Da de baja un servicio.
     *
     * <p>Sólo lo puede revocar **el proveedor que lo registró**. No es burocracia: si cualquiera
     * pudiera revocar el servicio de otro, un hijo podría dejar sin servicio a todos sus hermanos.
     */
    public void revokeService(Class serviceClass, BeanContextServiceProvider bcsp,
            boolean revokeCurrentServicesNow) {
        if (serviceClass == null || bcsp == null) {
            throw new NullPointerException();
        }
        synchronized (BeanContext.globalHierarchyLock) {
            BCSSServiceProvider registrado = (BCSSServiceProvider) this.services.get(serviceClass);
            if (registrado == null) {
                return;
            }
            if (registrado.getServiceProvider() != bcsp) {
                throw new IllegalArgumentException(
                        "sólo el proveedor que registró el servicio puede revocarlo");
            }
            this.services.remove(serviceClass);
            if (bcsp instanceof Serializable) {
                this.serializable = this.serializable - 1;
            }
            this.fireServiceRevoked(serviceClass, revokeCurrentServicesNow);
        }
    }

    /** Si el servicio está acá o más arriba. */
    public synchronized boolean hasService(Class serviceClass) {
        if (serviceClass == null) {
            throw new NullPointerException("serviceClass");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (this.services.containsKey(serviceClass)) {
                return true;
            }
            BeanContextServices arriba = this.parentServices();
            return arriba != null && arriba.hasService(serviceClass);
        }
    }

    // El contexto padre, si es de servicios. Un padre que sea un `BeanContext` a secas no reparte
    // servicios, y ahí la búsqueda hacia arriba se corta -- que es lo correcto y no una omisión.
    private BeanContextServices parentServices() {
        BeanContext padre = this.getBeanContext();
        return padre instanceof BeanContextServices ? (BeanContextServices) padre : null;
    }

    // ---- pedir y soltar ---------------------------------------------------------------------

    /**
     * Consigue una instancia del servicio para ese hijo.
     *
     * <p>Si el servicio no está acá, se lo pide al padre y **se registra un proxy local** para que
     * los próximos pedidos no vuelvan a subir. Ver la nota de la clase.
     *
     * @throws TooManyListenersException si el oyente de revocación no se pudo registrar
     */
    public Object getService(BeanContextChild child, Object requestor, Class serviceClass,
            Object serviceSelector, BeanContextServiceRevokedListener bcsrl)
            throws TooManyListenersException {
        if (child == null || requestor == null || serviceClass == null || bcsrl == null) {
            throw new NullPointerException();
        }
        synchronized (BeanContext.globalHierarchyLock) {
            BCSSServiceProvider bcsssp = (BCSSServiceProvider) this.services.get(serviceClass);
            if (bcsssp == null) {
                BeanContextServices arriba = this.parentServices();
                if (arriba == null) {
                    return null;
                }
                Object servicio = arriba.getService(this.getBeanContextServicesPeer(), requestor,
                        serviceClass, serviceSelector, bcsrl);
                if (servicio == null) {
                    return null;
                }
                if (this.proxy == null) {
                    this.proxy = new BCSSProxyServiceProvider(arriba);
                }
                this.addService(serviceClass, this.proxy, false);
                return servicio;
            }
            return bcsssp.getServiceProvider().getService(this.getBeanContextServicesPeer(),
                    requestor, serviceClass, serviceSelector);
        }
    }

    /** El hijo ya no necesita esa instancia. */
    public void releaseService(BeanContextChild child, Object requestor, Object service) {
        if (child == null || requestor == null || service == null) {
            throw new NullPointerException();
        }
        synchronized (BeanContext.globalHierarchyLock) {
            Iterator it = this.services.values().iterator();
            while (it.hasNext()) {
                BCSSServiceProvider bcsssp = (BCSSServiceProvider) it.next();
                bcsssp.getServiceProvider().releaseService(this.getBeanContextServicesPeer(),
                        requestor, service);
            }
        }
    }

    /** Las clases de servicio registradas acá. */
    public Iterator getCurrentServiceClasses() {
        synchronized (BeanContext.globalHierarchyLock) {
            List<Object> copia = new ArrayList<Object>();
            Iterator it = this.services.keySet().iterator();
            while (it.hasNext()) {
                copia.add(it.next());
            }
            return copia.iterator();
        }
    }

    /** Los selectores que acepta ese servicio, o `null`. */
    public Iterator getCurrentServiceSelectors(Class serviceClass) {
        synchronized (BeanContext.globalHierarchyLock) {
            BCSSServiceProvider bcsssp = (BCSSServiceProvider) this.services.get(serviceClass);
            if (bcsssp == null) {
                return null;
            }
            return bcsssp.getServiceProvider().getCurrentServiceSelectors(
                    this.getBeanContextServicesPeer(), serviceClass);
        }
    }

    // ---- oyentes -------------------------------------------------------------------------------

    /** Registra un oyente de altas y bajas de servicios. */
    public void addBeanContextServicesListener(BeanContextServicesListener bcsl) {
        if (bcsl == null) {
            throw new NullPointerException("bcsl");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (!this.bcsListeners.contains(bcsl)) {
                this.bcsListeners.add(bcsl);
            }
        }
    }

    /** Lo quita. */
    public void removeBeanContextServicesListener(BeanContextServicesListener bcsl) {
        if (bcsl == null) {
            throw new NullPointerException("bcsl");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            this.bcsListeners.remove(bcsl);
        }
    }

    // Copia, por lo mismo que en `BeanContextSupport`: un oyente puede darse de baja mientras lo
    // notifican.
    private Object[] serviceListenersCopy() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.bcsListeners.toArray();
        }
    }

    /** Avisa de un servicio nuevo. */
    protected final void fireServiceAdded(Class serviceClass) {
        this.fireServiceAdded(new BeanContextServiceAvailableEvent(
                this.getBeanContextServicesPeer(), serviceClass));
    }

    /** Avisa de un servicio nuevo. */
    protected final void fireServiceAdded(BeanContextServiceAvailableEvent bcssae) {
        Object[] ls = this.serviceListenersCopy();
        for (int i = 0; i < ls.length; i++) {
            ((BeanContextServicesListener) ls[i]).serviceAvailable(bcssae);
        }
    }

    /** Avisa de una revocación. */
    protected final void fireServiceRevoked(Class serviceClass, boolean revokeNow) {
        this.fireServiceRevoked(new BeanContextServiceRevokedEvent(
                this.getBeanContextServicesPeer(), serviceClass, revokeNow));
    }

    /**
     * Avisa de una revocación.
     *
     * <p>Le llega a los oyentes de servicios **y** a los hijos que sean oyentes de revocación sin
     * serlo de altas. Los dos grupos se recorren por separado a propósito: un hijo puede querer
     * enterarse sólo de que algo dejó de estar, y obligarlo a implementar también `serviceAvailable`
     * para eso sería pedirle un método vacío.
     */
    protected final void fireServiceRevoked(BeanContextServiceRevokedEvent bcsre) {
        Object[] ls = this.serviceListenersCopy();
        for (int i = 0; i < ls.length; i++) {
            ((BeanContextServicesListener) ls[i]).serviceRevoked(bcsre);
        }
        Object[] hijos = this.copyChildren();
        for (int i = 0; i < hijos.length; i++) {
            BeanContextServicesListener l =
                    BeanContextServicesSupport.getChildBeanContextServicesListener(hijos[i]);
            if (l != null && !this.bcsListeners.contains(l)) {
                l.serviceRevoked(bcsre);
            }
        }
    }

    /** Ese objeto como oyente de servicios, directo o por delegación, o `null`. */
    protected static final BeanContextServicesListener getChildBeanContextServicesListener(
            Object child) {
        if (child instanceof BeanContextServicesListener) {
            return (BeanContextServicesListener) child;
        }
        if (child instanceof BeanContextProxy) {
            BeanContextChild d = ((BeanContextProxy) child).getBeanContextProxy();
            if (d instanceof BeanContextServicesListener) {
                return (BeanContextServicesListener) d;
            }
        }
        return null;
    }

    // ---- lo que este contexto escucha de su padre -----------------------------------------------

    /** Un servicio apareció más arriba: se lo reenvía a los hijos. */
    public void serviceAvailable(BeanContextServiceAvailableEvent bcssae) {
        synchronized (BeanContext.globalHierarchyLock) {
            if (this.services.containsKey(bcssae.getServiceClass())) {
                return;
            }
            this.fireServiceAdded(bcssae);
        }
    }

    /**
     * Un servicio fue revocado más arriba.
     *
     * <p>Si este contexto lo tenía registrado por proxy, lo saca: seguir ofreciéndolo sería prometer
     * algo que ya no se puede conseguir.
     */
    public void serviceRevoked(BeanContextServiceRevokedEvent bcsre) {
        synchronized (BeanContext.globalHierarchyLock) {
            BCSSServiceProvider bcsssp =
                    (BCSSServiceProvider) this.services.get(bcsre.getServiceClass());
            if (bcsssp != null && bcsssp.getServiceProvider() == this.proxy) {
                this.services.remove(bcsre.getServiceClass());
            }
            this.fireServiceRevoked(bcsre);
        }
    }

    /**
     * El {@link BCSSServiceProvider} de ese servicio. Una subclase lo redefine para guardar más.
     */
    protected BCSSServiceProvider createBCSSServiceProvider(Class serviceClass,
            BeanContextServiceProvider bcsp) {
        return new BCSSServiceProvider(bcsp);
    }

    /**
     * El {@link BCSChild} de ese hijo.
     *
     * <p>El JDK devuelve acá un `BCSSChild`, una subclase interna que agrega lo que los servicios
     * necesitan anotar de cada hijo. **KajiLibrary no la trae**, y el motivo es concreto: nuestro
     * javac no emite bien un `super(...)` de una clase interna al constructor de la interna de su
     * superclase --le come la instancia envolvente, y el `.class` sale con un descriptor que no
     * existe (ver el informe del compilador)--. La clase compilaba y moría al cargarse en una JVM
     * real con `NoSuchMethodError`.
     *
     * <p>Qué se pierde: nada de lo que esta implementación use. `BCSSChild` no es parte de la
     * superficie medida --no aparece en ninguna firma pública-- y lo único que agrega en el JDK es
     * espacio para anotar los servicios que ese hijo pidió, que acá lleva el propio proveedor. El
     * día que el compilador emita ese `super`, son cinco líneas.
     */
    protected BCSChild createBCSChild(Object targetChild, Object peer) {
        return super.createBCSChild(targetChild, peer);
    }

    /**
     * Cuando un hijo se va, se le sueltan los servicios que tenía.
     *
     * <p>Es lo que evita la fuga que este diseño tiene si nadie la corta: un proveedor guarda
     * referencias a las instancias que entregó, así que un hijo que se va sin soltarlas queda vivo
     * en el proveedor para siempre.
     */
    protected void childJustRemovedHook(Object child, BCSChild bcsc) {
        super.childJustRemovedHook(child, bcsc);
    }

    /** Toma los recursos del contexto nuevo: se engancha como oyente de sus servicios. */
    protected synchronized void initializeBeanContextResources() {
        super.initializeBeanContextResources();
        BeanContextServices arriba = this.parentServices();
        if (arriba != null) {
            arriba.addBeanContextServicesListener(this);
        }
    }

    /** Los suelta. */
    protected synchronized void releaseBeanContextResources() {
        BeanContextServices arriba = this.parentServices();
        if (arriba != null) {
            arriba.removeBeanContextServicesListener(this);
        }
        this.proxy = null;
        super.releaseBeanContextResources();
    }

    /** Gancho de antes de escribir: deja anotado cuántos proveedores serializables hay. */
    protected synchronized void bcsPreSerializationHook(ObjectOutputStream oos)
            throws IOException {
        oos.writeInt(this.serializable);
        Iterator it = this.services.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry e = (java.util.Map.Entry) it.next();
            BCSSServiceProvider bcsssp = (BCSSServiceProvider) e.getValue();
            if (bcsssp.getServiceProvider() instanceof Serializable) {
                oos.writeObject(e.getKey());
                oos.writeObject(bcsssp.getServiceProvider());
            }
        }
    }

    /** Gancho de antes de leer: vuelve a registrar los proveedores que se escribieron. */
    protected synchronized void bcsPreDeserializationHook(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        int n = ois.readInt();
        for (int i = 0; i < n; i++) {
            Class serviceClass = (Class) ois.readObject();
            BeanContextServiceProvider bcsp = (BeanContextServiceProvider) ois.readObject();
            this.addService(serviceClass, bcsp, false);
        }
    }

    /**
     * Lo que el contexto guarda de un servicio: el proveedor, y lugar para lo que una subclase
     * quiera anotarle.
     */
    protected class BCSSServiceProvider implements Serializable {

        /** El proveedor. */
        protected BeanContextServiceProvider serviceProvider;

        BCSSServiceProvider(BeanContextServiceProvider bcsp) {
            this.serviceProvider = bcsp;
        }

        /** El proveedor. */
        protected BeanContextServiceProvider getServiceProvider() {
            return this.serviceProvider;
        }
    }

    /**
     * El proveedor que representa a los servicios del contexto padre.
     *
     * <p>Ver la nota de la clase: existe para que un servicio heredado se vea, desde acá para abajo,
     * como uno local.
     */
    protected class BCSSProxyServiceProvider
            implements BeanContextServiceProvider, BeanContextServiceRevokedListener {

        private final BeanContextServices delegate;

        BCSSProxyServiceProvider(BeanContextServices bcs) {
            this.delegate = bcs;
        }

        /** Le pide la instancia al contexto de arriba. */
        public Object getService(BeanContextServices bcs, Object requestor, Class serviceClass,
                Object serviceSelector) {
            try {
                return this.delegate.getService(BeanContextServicesSupport.this
                        .getBeanContextServicesPeer(), requestor, serviceClass, serviceSelector,
                        this);
            } catch (TooManyListenersException e) {
                // El de arriba no admitió el oyente de revocación. Devolver la instancia igual
                // sería peor que no darla: quien la reciba no se enteraría nunca de que dejó de
                // valer, que es justamente lo que ese oyente garantiza.
                return null;
            }
        }

        /** Le avisa al de arriba. */
        public void releaseService(BeanContextServices bcs, Object requestor, Object service) {
            this.delegate.releaseService(BeanContextServicesSupport.this
                    .getBeanContextServicesPeer(), requestor, service);
        }

        /** Los del de arriba. */
        public Iterator getCurrentServiceSelectors(BeanContextServices bcs, Class serviceClass) {
            return this.delegate.getCurrentServiceSelectors(serviceClass);
        }

        /** El de arriba revocó: se propaga hacia abajo. */
        public void serviceRevoked(BeanContextServiceRevokedEvent bcsre) {
            BeanContextServicesSupport.this.serviceRevoked(bcsre);
        }
    }
}
