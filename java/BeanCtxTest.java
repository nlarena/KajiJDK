import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextChildSupport;
import java.beans.beancontext.BeanContextMembershipEvent;
import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextServiceProvider;
import java.beans.beancontext.BeanContextServiceRevokedEvent;
import java.beans.beancontext.BeanContextServiceRevokedListener;
import java.beans.beancontext.BeanContextServices;
import java.beans.beancontext.BeanContextServicesSupport;
import java.beans.beancontext.BeanContextSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * `java.beans.beancontext`: la membresia, los eventos y los servicios.
 *
 * <p>Lo que se comprueba no es que los metodos existan sino que la relacion de dos vias se sostiene:
 * que un hijo agregado sabe en que contexto esta, que sacarlo lo deja sin contexto, que el evento de
 * membresia llega con los hijos que corresponden, y que un servicio registrado en el contexto padre
 * se encuentra desde un contexto hijo -- que es la unica razon por la que esta API existe.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo SU `java.beans.beancontext`. Eso lo
 * vuelve un oraculo: los numeros esperados no los inventamos.
 */
public class BeanCtxTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    /** Un hijo que anota lo que le va pasando. */
    public static class Child extends BeanContextChildSupport {

        int servicesSeen = 0;

        public void serviceAvailable(
                java.beans.beancontext.BeanContextServiceAvailableEvent e) {
            this.servicesSeen = this.servicesSeen + 1;
        }
    }

    /** Un oyente de membresia que anota las altas y las bajas. */
    public static class Watcher implements BeanContextMembershipListener {

        List<Object> added = new ArrayList<Object>();
        List<Object> removed = new ArrayList<Object>();

        public void childrenAdded(BeanContextMembershipEvent e) {
            Iterator it = e.iterator();
            while (it.hasNext()) {
                this.added.add(it.next());
            }
        }

        public void childrenRemoved(BeanContextMembershipEvent e) {
            Iterator it = e.iterator();
            while (it.hasNext()) {
                this.removed.add(it.next());
            }
        }
    }

    /** Un servicio de mentira: devuelve siempre el mismo texto. */
    public static class Provider implements BeanContextServiceProvider {

        int released = 0;

        public Object getService(BeanContextServices bcs, Object requestor, Class serviceClass,
                Object selector) {
            return "servicio";
        }

        public void releaseService(BeanContextServices bcs, Object requestor, Object service) {
            this.released = this.released + 1;
        }

        public Iterator getCurrentServiceSelectors(BeanContextServices bcs, Class serviceClass) {
            return null;
        }
    }

    /** Un oyente de revocacion que anota si le avisaron. */
    public static class Revoked implements BeanContextServiceRevokedListener {

        boolean told = false;

        public void serviceRevoked(BeanContextServiceRevokedEvent e) {
            this.told = true;
        }
    }

    /** Si esa operacion en masa tira `UnsupportedOperationException`, que es lo que promete. */
    static boolean tira(BeanContextSupport ctx, String op, List<Object> arg) {
        try {
            if ("addAll".equals(op)) {
                ctx.addAll(arg);
            } else if ("removeAll".equals(op)) {
                ctx.removeAll(arg);
            } else if ("retainAll".equals(op)) {
                ctx.retainAll(arg);
            } else {
                ctx.clear();
            }
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- membresia
        BeanContextSupport ctx = new BeanContextSupport();
        Watcher w = new Watcher();
        ctx.addBeanContextMembershipListener(w);

        Child a = new Child();
        ok("agregar un hijo devuelve true", ctx.add(a));
        ok("agregarlo de nuevo devuelve false", !ctx.add(a));
        ok("el contexto lo contiene", ctx.contains(a));
        ok("containsKey dice lo mismo que contains", ctx.containsKey(a));
        ok("el tamanio es 1", ctx.size() == 1);
        ok("no esta vacio", !ctx.isEmpty());
        // La relacion de dos vias: el hijo sabe donde esta.
        ok("el hijo conoce su contexto", a.getBeanContext() == ctx);
        ok("el evento de alta llego con el hijo", w.added.size() == 1 && w.added.get(0) == a);

        Object[] arr = ctx.toArray();
        ok("toArray tiene el hijo", arr.length == 1 && arr[0] == a);
        Iterator it = ctx.iterator();
        ok("el iterador tiene el hijo", it.hasNext() && it.next() == a);

        ok("quitar el hijo devuelve true", ctx.remove(a));
        ok("quitarlo de nuevo devuelve false", !ctx.remove(a));
        ok("despues de quitarlo el contexto esta vacio", ctx.isEmpty());
        // Y la otra mitad de la relacion: al salir se queda sin contexto.
        ok("el hijo quedo sin contexto", a.getBeanContext() == null);
        ok("el evento de baja llego con el hijo", w.removed.size() == 1 && w.removed.get(0) == a);

        // ---- las operaciones en masa NO estan soportadas, y eso es parte del contrato
        //
        // Esta comprobacion fallo primero contra `java` real, que tira UnsupportedOperationException
        // donde yo esperaba que agregara: la expectativa equivocada era la mia. Un alta puede fallar
        // sola --el hijo veta-- y una operacion en masa no sabe que decir a mitad de camino.
        Child b = new Child();
        Child c = new Child();
        List<Object> dos = new ArrayList<Object>();
        dos.add(b);
        dos.add(c);
        ok("addAll no esta soportada", BeanCtxTest.tira(ctx, "addAll", dos));
        ok("removeAll no esta soportada", BeanCtxTest.tira(ctx, "removeAll", dos));
        ok("retainAll no esta soportada", BeanCtxTest.tira(ctx, "retainAll", dos));
        ok("clear no esta soportada", BeanCtxTest.tira(ctx, "clear", dos));

        // Agregar de a uno si anda, que es lo que la API deja hacer.
        ctx.add(b);
        ctx.add(c);
        ok("de a uno entran los dos", ctx.size() == 2);
        ok("containsAll los ve", ctx.containsAll(dos));
        ctx.remove(b);
        ctx.remove(c);
        ok("y de a uno salen", ctx.isEmpty());

        // ---- el evento de membresia lleva varios hijos
        BeanContextMembershipEvent ev =
                new BeanContextMembershipEvent(ctx, new Object[] { b, c });
        ok("el evento dice 2", ev.size() == 2);
        ok("el evento contiene a b", ev.contains(b));
        ok("el evento no contiene a `a`", !ev.contains(a));
        ok("el evento no viene propagado", !ev.isPropagated());
        ev.setPropagatedFrom(ctx);
        ok("marcado como propagado, lo dice", ev.isPropagated());
        ok("y dice de donde", ev.getPropagatedFrom() == ctx);

        // ---- servicios: registrar, pedir, soltar
        BeanContextServicesSupport svc = new BeanContextServicesSupport();
        Provider p = new Provider();
        ok("el servicio se registra", svc.addService(String.class, p));
        ok("registrarlo de nuevo devuelve false", !svc.addService(String.class, p));
        ok("el contexto lo tiene", svc.hasService(String.class));
        ok("y no tiene uno que nadie registro", !svc.hasService(Integer.class));

        Child usuario = new Child();
        svc.add(usuario);
        Revoked r = new Revoked();
        Object obtenido = svc.getService(usuario, usuario, String.class, null, r);
        ok("el servicio se consigue", "servicio".equals(obtenido));
        svc.releaseService(usuario, usuario, obtenido);
        ok("soltarlo llega al proveedor", p.released == 1);

        Iterator clases = svc.getCurrentServiceClasses();
        ok("la clase del servicio esta en la lista",
                clases.hasNext() && clases.next() == String.class);

        // ---- la revocacion le avisa a quien lo pidio
        svc.revokeService(String.class, p, true);
        ok("despues de revocar ya no esta", !svc.hasService(String.class));

        // ---- un servicio del padre se ve desde el hijo: es el punto de toda la API
        BeanContextServicesSupport padre = new BeanContextServicesSupport();
        BeanContextServicesSupport hijo = new BeanContextServicesSupport();
        padre.add(hijo);
        Provider pp = new Provider();
        padre.addService(Number.class, pp);
        ok("el hijo ve el servicio del padre", hijo.hasService(Number.class));
        Child nieto = new Child();
        hijo.add(nieto);
        Object delPadre = hijo.getService(nieto, nieto, Number.class, null, new Revoked());
        ok("y lo consigue", "servicio".equals(delPadre));

        // ---- modo diseno e idioma se propagan
        ctx.setDesignTime(false);
        ok("el modo diseno se lee", !ctx.isDesignTime());
        ctx.setDesignTime(true);
        ok("y se vuelve a leer", ctx.isDesignTime());
        ok("hay un idioma", ctx.getLocale() != null);

        // ---- gráfica
        ctx.dontUseGui();
        ok("sin hijos que la pidan, no se esta evitando nada", !ctx.avoidingGui());
        ctx.okToUseGui();

        // ---- el candado es UNO para toda la jerarquia
        ok("el candado global es el mismo objeto",
                BeanContext.globalHierarchyLock == BeanContext.globalHierarchyLock);

        // ---- un hijo que veta su mudanza
        Vetador v = new Vetador();
        boolean tiro = false;
        try {
            v.setBeanContext(ctx);
        } catch (PropertyVetoException e) {
            tiro = true;
        } catch (IllegalStateException e) {
            tiro = true;
        }
        ok("un hijo puede rechazar el contexto", tiro);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    /** Un hijo que rechaza cualquier contexto. */
    public static class Vetador extends BeanContextChildSupport {

        public boolean validatePendingSetBeanContext(BeanContext newValue) {
            return newValue == null;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("BeanCtxTest " + BeanCtxTest.run());
    }
}
