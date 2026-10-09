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
 * The reusable implementation of {@link BeanContextServices}.
 *
 * <p>It adds services on top of what {@link BeanContextSupport} already does with membership. One
 * idea explains almost the whole file: **a service is looked up upwards**. If this context does not
 * have it, the parent is asked, and so on up to the root.
 *
 * <h2>The delegating provider</h2>
 *
 * <p>The first time a service comes from further up, this context registers a {@link
 * BCSSProxyServiceProvider} under that class: it poses as a local provider and forwards to the
 * parent context. From then on the children here see a provider like any other, and a revocation
 * from above still arrives, because the proxy is also the listener that receives it.
 *
 * <p>This note said that, with the proxy registered, the request is no longer forwarded each time.
 * It is: the proxy's {@code getService} calls the parent's on every request. What the proxy saves
 * is finding the service, not the trip up.
 *
 * <h2>What is not recorded</h2>
 *
 * <p>This note also said that each handed-out instance is recorded with **who asked for it** and
 * **with which revocation listener**, and that this is why {@link BCSSServiceProvider} carries more
 * than the provider. Nothing here records either: {@code BCSSServiceProvider} holds only the
 * provider, {@code releaseService} hands the instance to every registered provider, and this class
 * never registers the listener a requestor passes to {@code getService} (on the way up it hands it
 * to the parent, which may). In the JDK that bookkeeping lives in {@code BCSSChild}, which this
 * tree does not have; see {@link #createBCSChild}.
 */
public class BeanContextServicesSupport extends BeanContextSupport implements BeanContextServices {

    /** The services registered here, from service class to its {@link BCSSServiceProvider}. */
    protected transient HashMap services;

    /** The listeners for services being added and revoked. */
    protected transient ArrayList bcsListeners;

    /** How many of the providers registered here are serializable. */
    protected transient int serializable;

    /** The provider that stands for the services coming from the parent context. */
    protected transient BCSSProxyServiceProvider proxy;

    /** A services context with no parent. */
    public BeanContextServicesSupport() {
        this(null, null, true, true);
    }

    /**
     * A context acting in the name of `peer`.
     *
     * <p>This javadoc said a child of `peer`. `peer` is the context this one acts for —see the
     * four-argument constructor of {@link BeanContextSupport}— not its parent.
     */
    public BeanContextServicesSupport(BeanContextServices peer) {
        this(peer, null, true, true);
    }

    /** With that locale. */
    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle) {
        this(peer, lcle, true, true);
    }

    /** With that locale and that design mode. */
    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle, boolean dTime) {
        this(peer, lcle, dTime, true);
    }

    /** The constructor the others end up in. */
    public BeanContextServicesSupport(BeanContextServices peer, Locale lcle, boolean dTime,
            boolean visible) {
        super(peer, lcle, dTime, visible);
    }

    /** The services context it acts in the name of. */
    public BeanContextServices getBeanContextServicesPeer() {
        return (BeanContextServices) this.getBeanContextChildPeer();
    }

    /**
     * Builds the internal structures.
     *
     * <p>It calls the superclass's first: the children have to exist before the services, because a
     * service can be offered to a child as soon as it is registered.
     */
    public void initialize() {
        super.initialize();
        this.services = new HashMap();
        this.bcsListeners = new ArrayList();
    }

    // ---- adding and revoking services -----------------------------------------------------------

    /** Registers a provider. `false` if there already was one for that class. */
    public boolean addService(Class serviceClass, BeanContextServiceProvider bcsp) {
        return this.addService(serviceClass, bcsp, true);
    }

    /**
     * Registers a provider, announcing it or not.
     *
     * <p>`fireEvent` is `false` for the registration the context itself makes when it discovers a
     * service of the parent's: the listeners already hear of it through the event coming from
     * above, and announcing it again would reach them twice. {@link #bcsPreDeserializationHook}
     * also passes `false` when it registers again the providers it reads back.
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
     * Revokes a service.
     *
     * <p>Only **the provider that registered it** can revoke it. It is not red tape: if anyone
     * could revoke another's service, one child could leave all its siblings without it.
     */
    public void revokeService(Class serviceClass, BeanContextServiceProvider bcsp,
            boolean revokeCurrentServicesNow) {
        if (serviceClass == null || bcsp == null) {
            throw new NullPointerException();
        }
        synchronized (BeanContext.globalHierarchyLock) {
            BCSSServiceProvider registered = (BCSSServiceProvider) this.services.get(serviceClass);
            if (registered == null) {
                return;
            }
            if (registered.getServiceProvider() != bcsp) {
                throw new IllegalArgumentException(
                        "only the provider that registered the service can revoke it");
            }
            this.services.remove(serviceClass);
            if (bcsp instanceof Serializable) {
                this.serializable = this.serializable - 1;
            }
            this.fireServiceRevoked(serviceClass, revokeCurrentServicesNow);
        }
    }

    /** Whether the service is here or further up. */
    public synchronized boolean hasService(Class serviceClass) {
        if (serviceClass == null) {
            throw new NullPointerException("serviceClass");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            if (this.services.containsKey(serviceClass)) {
                return true;
            }
            BeanContextServices above = this.parentServices();
            return above != null && above.hasService(serviceClass);
        }
    }

    // The parent context, if it is a services context. A parent that is a plain `BeanContext` hands
    // out no services, and there the upward lookup stops -- which is right and not an omission.
    private BeanContextServices parentServices() {
        BeanContext parent = this.getBeanContext();
        return parent instanceof BeanContextServices ? (BeanContextServices) parent : null;
    }

    // ---- requesting and releasing ---------------------------------------------------------------

    /**
     * Gets an instance of the service for that child.
     *
     * <p>If the service is not here, it is requested from the parent and **a local proxy is
     * registered** under that class. This javadoc said the proxy keeps later requests from going up
     * again; they still go up, through the proxy. See the class note.
     *
     * <p>{@code bcsrl} only reaches the parent on that first upward request. A provider registered
     * here never sees it, and later requests through the proxy pass the proxy instead.
     *
     * @throws TooManyListenersException if the revocation listener could not be registered
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
                BeanContextServices above = this.parentServices();
                if (above == null) {
                    return null;
                }
                Object service = above.getService(this.getBeanContextServicesPeer(), requestor,
                        serviceClass, serviceSelector, bcsrl);
                if (service == null) {
                    return null;
                }
                if (this.proxy == null) {
                    this.proxy = new BCSSProxyServiceProvider(above);
                }
                this.addService(serviceClass, this.proxy, false);
                return service;
            }
            return bcsssp.getServiceProvider().getService(this.getBeanContextServicesPeer(),
                    requestor, serviceClass, serviceSelector);
        }
    }

    /** The child no longer needs that instance. */
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

    /** The service classes registered here. */
    public Iterator getCurrentServiceClasses() {
        synchronized (BeanContext.globalHierarchyLock) {
            List<Object> copy = new ArrayList<Object>();
            Iterator it = this.services.keySet().iterator();
            while (it.hasNext()) {
                copy.add(it.next());
            }
            return copy.iterator();
        }
    }

    /** The selectors that service accepts, or `null`. */
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

    // ---- listeners ------------------------------------------------------------------------------

    /** Registers a listener for services being added and revoked. */
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

    /** Removes it. */
    public void removeBeanContextServicesListener(BeanContextServicesListener bcsl) {
        if (bcsl == null) {
            throw new NullPointerException("bcsl");
        }
        synchronized (BeanContext.globalHierarchyLock) {
            this.bcsListeners.remove(bcsl);
        }
    }

    // A copy, for the same reason as in `BeanContextSupport`: a listener can unregister while being
    // notified.
    private Object[] serviceListenersCopy() {
        synchronized (BeanContext.globalHierarchyLock) {
            return this.bcsListeners.toArray();
        }
    }

    /** Announces a new service. */
    protected final void fireServiceAdded(Class serviceClass) {
        this.fireServiceAdded(new BeanContextServiceAvailableEvent(
                this.getBeanContextServicesPeer(), serviceClass));
    }

    /** Announces a new service. */
    protected final void fireServiceAdded(BeanContextServiceAvailableEvent bcssae) {
        Object[] ls = this.serviceListenersCopy();
        for (int i = 0; i < ls.length; i++) {
            ((BeanContextServicesListener) ls[i]).serviceAvailable(bcssae);
        }
    }

    /** Announces a revocation. */
    protected final void fireServiceRevoked(Class serviceClass, boolean revokeNow) {
        this.fireServiceRevoked(new BeanContextServiceRevokedEvent(
                this.getBeanContextServicesPeer(), serviceClass, revokeNow));
    }

    /**
     * Announces a revocation.
     *
     * <p>It reaches the services listeners, and then the children that are themselves services
     * listeners, directly or through a {@link BeanContextProxy}, skipping those already registered
     * so none hears it twice. This javadoc said the second group is the children that listen for
     * revocations without listening for additions, so they need not write an empty {@code
     * serviceAvailable}. The check is for {@link BeanContextServicesListener}, which declares both.
     */
    protected final void fireServiceRevoked(BeanContextServiceRevokedEvent bcsre) {
        Object[] ls = this.serviceListenersCopy();
        for (int i = 0; i < ls.length; i++) {
            ((BeanContextServicesListener) ls[i]).serviceRevoked(bcsre);
        }
        Object[] children = this.copyChildren();
        for (int i = 0; i < children.length; i++) {
            BeanContextServicesListener l =
                    BeanContextServicesSupport.getChildBeanContextServicesListener(children[i]);
            if (l != null && !this.bcsListeners.contains(l)) {
                l.serviceRevoked(bcsre);
            }
        }
    }

    /** That object as a services listener, directly or through delegation, or `null`. */
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

    // ---- what this context hears from its parent ------------------------------------------------

    /**
     * A service appeared further up: it is passed on to this context's services listeners, unless a
     * service of that class is already registered here. This javadoc said it is passed on to the
     * children; only the listeners get it.
     */
    public void serviceAvailable(BeanContextServiceAvailableEvent bcssae) {
        synchronized (BeanContext.globalHierarchyLock) {
            if (this.services.containsKey(bcssae.getServiceClass())) {
                return;
            }
            this.fireServiceAdded(bcssae);
        }
    }

    /**
     * A service was revoked further up.
     *
     * <p>If this context had it registered through the proxy, it removes it: going on offering it
     * would promise something that can no longer be obtained.
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

    /** The {@link BCSSServiceProvider} for that service. */
    protected BCSSServiceProvider createBCSSServiceProvider(Class serviceClass,
            BeanContextServiceProvider bcsp) {
        return new BCSSServiceProvider(bcsp);
    }

    /**
     * The {@link BCSChild} for that child.
     *
     * <p>The JDK returns a {@code BCSSChild} here, a nested subclass that records, per child, the
     * services it requested, by class and by requestor, with their revocation listeners.
     * **KajiLibrary does not have it.** This javadoc gave as the reason a javac defect: a {@code
     * super(...)} from an inner class to the inner class of its superclass lost the enclosing
     * instance and emitted a constructor descriptor that does not exist. That is compiler finding
     * #480, closed on 2026-09-07: its repro now compiles and runs, though the finding notes the
     * repro does not capture the larger original shape from this file.
     *
     * <p>It also said nothing this implementation uses is lost, because the provider carries that
     * bookkeeping here, and that the class would be five lines. Nothing carries the bookkeeping
     * (see the class note), and the JDK's {@code BCSSChild} has nested reference classes of its own
     * and the methods that release a departed child's services and revoke them.
     */
    protected BCSChild createBCSChild(Object targetChild, Object peer) {
        return super.createBCSChild(targetChild, peer);
    }

    /**
     * Called when a child leaves.
     *
     * <p>This javadoc said the services the child held are released here, which is what stops a
     * provider from keeping a departed child alive through the instances it handed out. Nothing is
     * released: the method only calls the superclass hook, which is empty, and nothing here records
     * which instances a child holds (see the class note).
     */
    protected void childJustRemovedHook(Object child, BCSChild bcsc) {
        super.childJustRemovedHook(child, bcsc);
    }

    /** Takes the new context's resources: it registers as a listener of its services. */
    protected synchronized void initializeBeanContextResources() {
        super.initializeBeanContextResources();
        BeanContextServices above = this.parentServices();
        if (above != null) {
            above.addBeanContextServicesListener(this);
        }
    }

    /** Releases them, and drops the proxy. */
    protected synchronized void releaseBeanContextResources() {
        BeanContextServices above = this.parentServices();
        if (above != null) {
            above.removeBeanContextServicesListener(this);
        }
        this.proxy = null;
        super.releaseBeanContextResources();
    }

    /** Hook before writing: writes how many serializable providers there are, then each one. */
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

    /** Hook before reading: registers again the providers that were written. */
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
     * What the context keeps for a service: its provider.
     *
     * <p>This note also offered it as room for whatever a subclass wants to record. Its constructor
     * is package-private, so no class outside this package can extend it.
     */
    protected class BCSSServiceProvider implements Serializable {

        /** The provider. */
        protected BeanContextServiceProvider serviceProvider;

        BCSSServiceProvider(BeanContextServiceProvider bcsp) {
            this.serviceProvider = bcsp;
        }

        /** The provider. */
        protected BeanContextServiceProvider getServiceProvider() {
            return this.serviceProvider;
        }
    }

    /**
     * The provider that stands for the parent context's services.
     *
     * <p>See the class note: it exists so an inherited service looks, from here down, like a local
     * one.
     */
    protected class BCSSProxyServiceProvider
            implements BeanContextServiceProvider, BeanContextServiceRevokedListener {

        private final BeanContextServices delegate;

        BCSSProxyServiceProvider(BeanContextServices bcs) {
            this.delegate = bcs;
        }

        /**
         * Asks the context above for the instance, registering itself as the revocation listener.
         */
        public Object getService(BeanContextServices bcs, Object requestor, Class serviceClass,
                Object serviceSelector) {
            try {
                return this.delegate.getService(BeanContextServicesSupport.this
                        .getBeanContextServicesPeer(), requestor, serviceClass, serviceSelector,
                        this);
            } catch (TooManyListenersException e) {
                // The context above did not accept the revocation listener. Returning the instance
                // anyway would be worse than not giving it: whoever got it would never learn that
                // it stopped being valid, which is exactly what that listener guarantees.
                return null;
            }
        }

        /** Tells the context above. */
        public void releaseService(BeanContextServices bcs, Object requestor, Object service) {
            this.delegate.releaseService(BeanContextServicesSupport.this
                    .getBeanContextServicesPeer(), requestor, service);
        }

        /** The context above's selectors. */
        public Iterator getCurrentServiceSelectors(BeanContextServices bcs, Class serviceClass) {
            return this.delegate.getCurrentServiceSelectors(serviceClass);
        }

        /** The context above revoked it: it is passed on downwards. */
        public void serviceRevoked(BeanContextServiceRevokedEvent bcsre) {
            BeanContextServicesSupport.this.serviceRevoked(bcsre);
        }
    }
}
