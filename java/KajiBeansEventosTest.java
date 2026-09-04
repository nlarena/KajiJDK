import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;

// Prueba diferencial de la mitad de eventos de java.beans. Corre igual en las dos VMs (en la real
// usa el java.beans del JDK), asi que un -1 en ambas dice que nuestro comportamiento es el del JDK.
//
// El caso que le da sentido a la prueba es el bit 8: un oyente que se desuscribe A SI MISMO desde
// adentro de propertyChange(). Si se notificara sobre la lista viva en vez de sobre una copia, eso
// la modifica en pleno recorrido y el despacho se rompe o saltea oyentes.

class Contador implements PropertyChangeListener {
    int veces;
    String ultima;
    Object ultimoNuevo;
    public void propertyChange(PropertyChangeEvent e) {
        veces = veces + 1;
        ultima = e.getPropertyName();
        ultimoNuevo = e.getNewValue();
    }
}

// Se borra a si mismo apenas lo notifican.
class Suicida implements PropertyChangeListener {
    PropertyChangeSupport pcs;
    int veces;
    public void propertyChange(PropertyChangeEvent e) {
        veces = veces + 1;
        pcs.removePropertyChangeListener(this);
    }
}

// Agrega otro oyente durante la notificacion.
class Reproductor implements PropertyChangeListener {
    PropertyChangeSupport pcs;
    Contador hijo = new Contador();
    int veces;
    public void propertyChange(PropertyChangeEvent e) {
        veces = veces + 1;
        pcs.addPropertyChangeListener(hijo);
    }
}

class Vetador implements VetoableChangeListener {
    public void vetoableChange(PropertyChangeEvent e) throws PropertyVetoException {
        throw new PropertyVetoException("no", e);
    }
}

// Cuenta cuantas veces lo llamaron y con que valor nuevo: sirve para ver la vuelta atras.
class TestigoVeto implements VetoableChangeListener {
    int veces;
    Object ultimoNuevo;
    public void vetoableChange(PropertyChangeEvent e) {
        veces = veces + 1;
        ultimoNuevo = e.getNewValue();
    }
}

public class KajiBeansEventosTest {

    public static int run() throws Exception {
        int fallas = 0;
        Object fuente = new Object();

        // --- 1: despacho basico y el evento que llega -----------------------------
        PropertyChangeSupport pcs = new PropertyChangeSupport(fuente);
        Contador c = new Contador();
        pcs.addPropertyChangeListener(c);
        pcs.firePropertyChange("x", "viejo", "nuevo");
        if (c.veces != 1 || !"x".equals(c.ultima) || !"nuevo".equals(c.ultimoNuevo)) {
            System.out.println("F1 despacho: veces=" + c.veces);
            fallas |= 1;
        }

        // --- 2: valores iguales no disparan ---------------------------------------
        int antes = c.veces;
        pcs.firePropertyChange("x", "igual", "igual");
        pcs.firePropertyChange("y", 5, 5);
        pcs.firePropertyChange("z", true, true);
        if (c.veces != antes) {
            System.out.println("F2 valores iguales dispararon");
            fallas |= 2;
        }
        // pero null a null si dispara: "no se sabe" no es "no cambio"
        pcs.firePropertyChange("w", null, null);
        if (c.veces != antes + 1) {
            System.out.println("F2b null/null no disparo");
            fallas |= 2;
        }

        // --- 4: registro por nombre ------------------------------------------------
        PropertyChangeSupport p2 = new PropertyChangeSupport(fuente);
        Contador soloA = new Contador();
        p2.addPropertyChangeListener("a", soloA);
        p2.firePropertyChange("b", 1, 2);
        if (soloA.veces != 0) {
            System.out.println("F4a recibio una propiedad ajena");
            fallas |= 4;
        }
        p2.firePropertyChange("a", 1, 2);
        if (soloA.veces != 1) {
            System.out.println("F4b no recibio la suya");
            fallas |= 4;
        }
        if (p2.getPropertyChangeListeners("a").length != 1
            || p2.getPropertyChangeListeners("b").length != 0) {
            System.out.println("F4c getPropertyChangeListeners(nombre)");
            fallas |= 4;
        }

        // --- 8: LA TRAMPA. Desuscribirse desde adentro de la notificacion ----------
        PropertyChangeSupport p3 = new PropertyChangeSupport(fuente);
        Suicida s = new Suicida();
        s.pcs = p3;
        Contador despues = new Contador();
        p3.addPropertyChangeListener(s);
        p3.addPropertyChangeListener(despues);
        p3.firePropertyChange("p", 1, 2);
        // El que se borro recibio ESE evento, y el que venia detras tambien: la copia protege el
        // recorrido entero.
        if (s.veces != 1 || despues.veces != 1) {
            System.out.println("F8a s=" + s.veces + " despues=" + despues.veces);
            fallas |= 8;
        }
        // y en el siguiente disparo ya no esta
        p3.firePropertyChange("p", 2, 3);
        if (s.veces != 1 || despues.veces != 2) {
            System.out.println("F8b s=" + s.veces + " despues=" + despues.veces);
            fallas |= 8;
        }

        // --- 16: agregar un oyente durante la notificacion no le da ESE evento -----
        PropertyChangeSupport p4 = new PropertyChangeSupport(fuente);
        Reproductor r = new Reproductor();
        r.pcs = p4;
        p4.addPropertyChangeListener(r);
        p4.firePropertyChange("q", 1, 2);
        if (r.veces != 1 || r.hijo.veces != 0) {
            System.out.println("F16a r=" + r.veces + " hijo=" + r.hijo.veces);
            fallas |= 16;
        }
        p4.firePropertyChange("q", 2, 3);
        if (r.hijo.veces != 1) {
            System.out.println("F16b hijo=" + r.hijo.veces);
            fallas |= 16;
        }

        // --- 32: hasListeners ------------------------------------------------------
        PropertyChangeSupport p5 = new PropertyChangeSupport(fuente);
        if (p5.hasListeners("a")) { System.out.println("F32a"); fallas |= 32; }
        Contador c5 = new Contador();
        p5.addPropertyChangeListener("a", c5);
        if (!p5.hasListeners("a") || p5.hasListeners("b")) {
            System.out.println("F32b");
            fallas |= 32;
        }
        Contador g5 = new Contador();
        p5.addPropertyChangeListener(g5);
        // un oyente global hace que hasListeners diga true para cualquier nombre
        if (!p5.hasListeners("b")) { System.out.println("F32c"); fallas |= 32; }

        // --- 64: getPropertyChangeListeners() envuelve los de nombre en un proxy ---
        PropertyChangeSupport p6 = new PropertyChangeSupport(fuente);
        Contador gl = new Contador();
        Contador nm = new Contador();
        p6.addPropertyChangeListener(gl);
        p6.addPropertyChangeListener("a", nm);
        PropertyChangeListener[] todos = p6.getPropertyChangeListeners();
        int proxies = 0;
        int planos = 0;
        for (int i = 0; i < todos.length; i++) {
            if (todos[i] instanceof PropertyChangeListenerProxy) {
                proxies = proxies + 1;
                PropertyChangeListenerProxy px = (PropertyChangeListenerProxy) todos[i];
                if (!"a".equals(px.getPropertyName())) { System.out.println("F64a"); fallas |= 64; }
            } else {
                planos = planos + 1;
            }
        }
        if (todos.length != 2 || proxies != 1 || planos != 1) {
            System.out.println("F64b total=" + todos.length + " proxies=" + proxies);
            fallas |= 64;
        }

        // --- 128: evento indexado --------------------------------------------------
        PropertyChangeSupport p7 = new PropertyChangeSupport(fuente);
        final int[] visto = new int[] { -99 };
        p7.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e instanceof IndexedPropertyChangeEvent) {
                    visto[0] = ((IndexedPropertyChangeEvent) e).getIndex();
                }
            }
        });
        p7.fireIndexedPropertyChange("arr", 3, 1, 2);
        if (visto[0] != 3) {
            System.out.println("F128 indice=" + visto[0]);
            fallas |= 128;
        }

        // --- 256: el veto para el cambio y revierte a los que ya aceptaron ---------
        VetoableChangeSupport vcs = new VetoableChangeSupport(fuente);
        TestigoVeto t = new TestigoVeto();
        vcs.addVetoableChangeListener(t);
        vcs.addVetoableChangeListener(new Vetador());
        boolean vetado = false;
        try { vcs.fireVetoableChange("v", "a", "b"); }
        catch (PropertyVetoException e) { vetado = true; }
        if (!vetado) {
            System.out.println("F256a no veto");
            fallas |= 256;
        }
        // al testigo lo llamaron dos veces: la propuesta y la vuelta atras, con los valores dados
        // vuelta la segunda vez.
        if (t.veces != 2 || !"a".equals(t.ultimoNuevo)) {
            System.out.println("F256b veces=" + t.veces + " ultimoNuevo=" + t.ultimoNuevo);
            fallas |= 256;
        }

        // --- 512: sin vetador el cambio pasa --------------------------------------
        VetoableChangeSupport v2 = new VetoableChangeSupport(fuente);
        TestigoVeto t2 = new TestigoVeto();
        v2.addVetoableChangeListener(t2);
        v2.fireVetoableChange("v", "a", "b");
        if (t2.veces != 1 || !"b".equals(t2.ultimoNuevo)) {
            System.out.println("F512 veces=" + t2.veces);
            fallas |= 512;
        }

        // --- 1024: PropertyChangeEvent conserva lo suyo ---------------------------
        PropertyChangeEvent ev = new PropertyChangeEvent(fuente, "n", "v", "w");
        ev.setPropagationId("pid");
        if (ev.getSource() != fuente || !"n".equals(ev.getPropertyName())
            || !"v".equals(ev.getOldValue()) || !"w".equals(ev.getNewValue())
            || !"pid".equals(ev.getPropagationId())) {
            System.out.println("F1024 PropertyChangeEvent");
            fallas |= 1024;
        }

        return fallas == 0 ? -1 : fallas;
    }

    public static void main(String[] a) throws Exception { System.out.println(run()); }
}
