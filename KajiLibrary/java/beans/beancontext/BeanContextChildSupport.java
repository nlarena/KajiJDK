package java.beans.beancontext;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.Serializable;

/**
 * La implementación reusable de {@link BeanContextChild}.
 *
 * <p>Sirve de dos maneras, y por eso tiene dos constructores: una clase la **extiende**, o un bean
 * que ya extiende otra cosa la usa por **delegación** pasándose a sí mismo en el constructor. En el
 * segundo caso {@link #isDelegated} da `true` y todos los eventos salen a nombre del bean que
 * delega, no de este soporte — que es lo que un oyente espera ver.
 *
 * <h2>El veto de la mudanza, y `rejectedSetBCOnce`</h2>
 *
 * <p>Cambiar de contexto es una propiedad **vetable**: antes de mudarse se pregunta, y si alguien se
 * opone, la mudanza no ocurre. La parte que se lee mal es la bandera `rejectedSetBCOnce`, y merece
 * el párrafo:
 *
 * <p>Un contexto que expulsa a un hijo llama a `setBeanContext(null)`. Si el hijo vetara esa baja,
 * quedaría en un estado imposible — fuera de la colección del contexto pero creyéndose adentro—.
 * Por eso el veto se admite **una sola vez**: la primera negativa se respeta, y si el mismo cambio
 * vuelve a intentarse, se aplica igual. La bandera es lo que recuerda que ya hubo una negativa, y se
 * limpia en cuanto una mudanza termina bien.
 */
public class BeanContextChildSupport implements BeanContextChild, BeanContextServicesListener,
        Serializable {

    /** El bean a nombre del cual salen los eventos: este mismo, o el que delegó en él. */
    public BeanContextChild beanContextChildPeer;

    /** Los oyentes de cambio de propiedad. */
    protected PropertyChangeSupport pcSupport;

    /** Los oyentes de veto. */
    protected VetoableChangeSupport vcSupport;

    /** El contexto actual, o `null`. */
    protected transient BeanContext beanContext;

    /** Si ya se rechazó una vez este cambio de contexto. Ver la nota de la clase. */
    protected transient boolean rejectedSetBCOnce;

    // El contexto al que se está mudando mientras se pregunta por el veto. No es lo mismo que
    // `beanContext`: durante la consulta el hijo sigue en el de antes, y si alguien veta se queda
    // ahí. Recién cuando nadie vetó pasa a ser el actual.
    private transient BeanContext pendingContext;

    /** Un soporte que actúa a nombre de sí mismo. */
    public BeanContextChildSupport() {
        this.beanContextChildPeer = this;
        this.pcSupport = new PropertyChangeSupport(this.beanContextChildPeer);
        this.vcSupport = new VetoableChangeSupport(this.beanContextChildPeer);
    }

    /** Un soporte que actúa a nombre de `bcc`. `null` significa a nombre de sí mismo. */
    public BeanContextChildSupport(BeanContextChild bcc) {
        this.beanContextChildPeer = bcc == null ? this : bcc;
        this.pcSupport = new PropertyChangeSupport(this.beanContextChildPeer);
        this.vcSupport = new VetoableChangeSupport(this.beanContextChildPeer);
    }

    /** El bean a nombre del cual actúa. */
    public BeanContextChild getBeanContextChildPeer() {
        return this.beanContextChildPeer;
    }

    /** Si actúa a nombre de otro bean en vez de a nombre propio. */
    public boolean isDelegated() {
        return this.beanContextChildPeer != this;
    }

    /**
     * Muda este hijo a ese contexto.
     *
     * <p>El orden importa y es el que fija el contrato: primero se consulta el veto, después se
     * sueltan los recursos del contexto viejo, después se cambia, y recién al final se toman los del
     * nuevo y se avisa del cambio. Soltar antes de preguntar dejaría al hijo sin recursos si alguien
     * vetaba.
     *
     * @throws PropertyVetoException si un oyente se opone y todavía no había vetado este cambio
     */
    public synchronized void setBeanContext(BeanContext bc) throws PropertyVetoException {
        if (bc == this.beanContext) {
            return;
        }
        BeanContext old = this.beanContext;
        if (!this.rejectedSetBCOnce) {
            if (!this.validatePendingSetBeanContext(bc)) {
                this.rejectedSetBCOnce = true;
                throw new PropertyVetoException("el hijo rechaza el cambio de contexto",
                        new java.beans.PropertyChangeEvent(this.beanContextChildPeer,
                                "beanContext", old, bc));
            }
            try {
                this.pendingContext = bc;
                this.fireVetoableChange("beanContext", old, bc);
            } catch (PropertyVetoException e) {
                this.rejectedSetBCOnce = true;
                this.pendingContext = null;
                throw e;
            }
        }
        if (old != null) {
            this.releaseBeanContextResources();
        }
        this.beanContext = bc;
        this.pendingContext = null;
        this.rejectedSetBCOnce = false;
        this.firePropertyChange("beanContext", old, bc);
        if (bc != null) {
            this.initializeBeanContextResources();
        }
    }

    /** El contexto actual, o `null`. */
    public synchronized BeanContext getBeanContext() {
        return this.beanContext;
    }

    /**
     * La oportunidad de la subclase de rechazar una mudanza sin registrar un oyente.
     *
     * <p>Por omisión acepta todo. Redefinirla es lo más barato que hay para un hijo que sólo puede
     * vivir en cierta clase de contexto.
     */
    public boolean validatePendingSetBeanContext(BeanContext newValue) {
        return true;
    }

    /**
     * El gancho para tomar los recursos del contexto nuevo.
     *
     * <p>Vacío acá y no abstracto: la mayoría de los hijos no necesita ninguno, y obligarlos a
     * escribir un método vacío sería ruido. Se llama **después** de que el contexto ya cambió, así
     * que dentro se puede usar {@link #getBeanContext}.
     */
    protected void initializeBeanContextResources() {
    }

    /** El gancho para soltarlos. Se llama **antes** de cambiar, con el contexto viejo todavía puesto. */
    protected void releaseBeanContextResources() {
    }

    /** Registra un oyente para los cambios de esa propiedad. */
    public void addPropertyChangeListener(String name, PropertyChangeListener pcl) {
        this.pcSupport.addPropertyChangeListener(name, pcl);
    }

    /** Lo quita. */
    public void removePropertyChangeListener(String name, PropertyChangeListener pcl) {
        this.pcSupport.removePropertyChangeListener(name, pcl);
    }

    /** Registra un oyente que puede vetar los cambios de esa propiedad. */
    public void addVetoableChangeListener(String name, VetoableChangeListener vcl) {
        this.vcSupport.addVetoableChangeListener(name, vcl);
    }

    /** Lo quita. */
    public void removeVetoableChangeListener(String name, VetoableChangeListener vcl) {
        this.vcSupport.removeVetoableChangeListener(name, vcl);
    }

    /** Avisa de un cambio de propiedad a nombre del bean. */
    public void firePropertyChange(String name, Object oldValue, Object newValue) {
        this.pcSupport.firePropertyChange(name, oldValue, newValue);
    }

    /**
     * Consulta el veto de un cambio de propiedad.
     *
     * @throws PropertyVetoException si algún oyente se opone
     */
    public void fireVetoableChange(String name, Object oldValue, Object newValue)
            throws PropertyVetoException {
        this.vcSupport.fireVetoableChange(name, oldValue, newValue);
    }

    /**
     * Un servicio nuevo apareció.
     *
     * <p>Vacío por omisión, y es lo correcto: un hijo que no usa servicios no tiene nada que hacer
     * acá, y esta clase existe justamente para que no tenga que escribirlo.
     */
    public void serviceAvailable(BeanContextServiceAvailableEvent bcsae) {
    }

    /** Un servicio fue revocado. Vacío por omisión, por lo mismo. */
    public void serviceRevoked(BeanContextServiceRevokedEvent bcsre) {
    }
}
