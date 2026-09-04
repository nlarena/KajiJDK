import java.awt.AWTEvent;
import java.awt.AWTEventMulticaster;
import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * El núcleo de AWT: el árbol de componentes, los eventos y la cola.
 *
 * <p>El mismo archivo da -1 con el JDK 25 corriendo **sus** clases, sin pantalla.
 *
 * <p>No toca nada que necesite una ventana: usa `Container`, que es concreto y se construye igual sin
 * pantalla. Lo que se comprueba es lo que hace al árbol un árbol —que un hijo tenga un solo padre,
 * que agregar a un ancestro adentro de un descendiente esté prohibido, que el orden Z decida quién
 * recibe el clic— y el reparto de eventos de tres pasos.
 */
public class AwtCoreTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    /** Anota en una lista qué le llegó. */
    static class Anotador implements ContainerListener, ComponentListener, ActionListener {
        final List<String> visto = new ArrayList<String>();

        public void componentAdded(ContainerEvent e) {
            this.visto.add("added:" + e.getChild().getName());
        }

        public void componentRemoved(ContainerEvent e) {
            this.visto.add("removed:" + e.getChild().getName());
        }

        public void componentResized(ComponentEvent e) {
            this.visto.add("resized");
        }

        public void componentMoved(ComponentEvent e) {
            this.visto.add("moved");
        }

        public void componentShown(ComponentEvent e) {
            this.visto.add("shown");
        }

        public void componentHidden(ComponentEvent e) {
            this.visto.add("hidden");
        }

        public void actionPerformed(ActionEvent e) {
            this.visto.add("action:" + e.getActionCommand());
        }
    }

    /** Sólo oyente de acción: sin esto, `AWTEventMulticaster.add` no sabe cuál elegir. */
    static class SoloAccion implements ActionListener {
        final List<String> visto = new ArrayList<String>();

        public void actionPerformed(ActionEvent e) {
            this.visto.add("action:" + e.getActionCommand());
        }
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- el árbol
        Container raiz = new Container();
        raiz.setName("raiz");
        Container medio = new Container();
        medio.setName("medio");
        Container hoja = new Container();
        hoja.setName("hoja");

        Anotador an = new Anotador();
        raiz.addContainerListener(an);

        raiz.add(medio);
        medio.add(hoja);
        ok("agregar un hijo lo cuenta", raiz.getComponentCount() == 1);
        ok("y le pone padre", medio.getParent() == raiz);
        ok("el aviso llegó con el hijo", an.visto.contains("added:medio"));
        ok("pero no por el nieto, que se agregó a otro", !an.visto.contains("added:hoja"));

        ok("la raíz es ancestro del nieto", raiz.isAncestorOf(hoja));
        ok("y el nieto no lo es de la raíz", !hoja.isAncestorOf(raiz));

        // Un hijo tiene un solo padre: agregarlo a otro lo saca del primero.
        Container otro = new Container();
        otro.add(medio);
        ok("agregarlo a otro padre lo saca del primero", raiz.getComponentCount() == 0);
        ok("y ahora cuelga del nuevo", medio.getParent() == otro);
        ok("el aviso de salida también llegó", an.visto.contains("removed:medio"));

        // Un ciclo rompería el árbol y está prohibido.
        boolean cicloRechazado = false;
        try {
            hoja.add(otro);
        } catch (IllegalArgumentException e) {
            cicloRechazado = true;
        }
        ok("meter un ancestro adentro de un descendiente se rechaza", cicloRechazado);

        boolean seMismo = false;
        try {
            otro.add(otro);
        } catch (IllegalArgumentException e) {
            seMismo = true;
        }
        ok("y agregarse a sí mismo también", seMismo);

        // ---- orden Z
        Container caja = new Container();
        caja.setBounds(0, 0, 100, 100);
        Container a = new Container();
        a.setName("a");
        a.setBounds(0, 0, 50, 50);
        Container b = new Container();
        b.setName("b");
        b.setBounds(0, 0, 50, 50);
        caja.add(a);
        caja.add(b);
        ok("el primero agregado queda arriba", caja.getComponentZOrder(a) == 0);
        ok("y el segundo abajo", caja.getComponentZOrder(b) == 1);
        ok("el de arriba es el que recibiría el clic", caja.getComponentAt(10, 10) == a);
        caja.setComponentZOrder(b, 0);
        ok("subir el de abajo lo pone primero", caja.getComponentZOrder(b) == 0);
        ok("y ahora es él quien recibe el clic", caja.getComponentAt(10, 10) == b);

        // findComponentAt baja hasta la hoja; getComponentAt mira sólo a los hijos directos.
        Container adentro = new Container();
        adentro.setBounds(5, 5, 20, 20);
        a.add(adentro);
        caja.setComponentZOrder(a, 0);
        ok("getComponentAt se queda en el hijo directo", caja.getComponentAt(10, 10) == a);
        ok("findComponentAt baja hasta la hoja", caja.findComponentAt(10, 10) == adentro);

        // ---- herencia de color y fuente
        Container padre = new Container();
        Container hijo = new Container();
        padre.add(hijo);
        ok("sin color propio ni heredado, no hay color", hijo.getForeground() == null);
        padre.setForeground(Color.RED);
        ok("el hijo hereda el color del padre", hijo.getForeground() == Color.RED);
        ok("y no lo declara como propio", !hijo.isForegroundSet());
        hijo.setForeground(Color.BLUE);
        ok("con color propio, ése gana", hijo.getForeground() == Color.BLUE);
        ok("y ahora sí lo declara", hijo.isForegroundSet());

        Font f = new Font("Serif", Font.BOLD, 14);
        padre.setFont(f);
        ok("la fuente también se hereda", hijo.getFont() == f);

        // ---- las tres medidas
        Container med = new Container();
        med.setBounds(0, 0, 40, 30);
        ok("sin fijar, no está fijada", !med.isPreferredSizeSet());
        med.setPreferredSize(new Dimension(200, 100));
        ok("fijarla lo declara", med.isPreferredSizeSet());
        ok("y es la que se devuelve", med.getPreferredSize().equals(new Dimension(200, 100)));
        med.setPreferredSize(null);
        ok("ponerla en null la vuelve a soltar", !med.isPreferredSizeSet());

        // ---- validez
        Container v = new Container();
        ok("un contenedor nuevo es inválido", !v.isValid());
        // Validar un contenedor que no está en pantalla no hace nada: no hay nada que maquetar, y
        // marcarlo válido sería afirmar que se maquetó.
        v.validate();
        ok("validar sin pantalla no lo vuelve válido", !v.isValid());

        // Invalidar sí funciona, y se propaga hacia arriba.
        Container arriba = new Container();
        Container abajo = new Container();
        arriba.add(abajo);
        ok("agregar un hijo invalida al padre", !arriba.isValid());
        abajo.invalidate();
        ok("y invalidar al hijo también", !arriba.isValid());

        // ---- reparto de eventos
        Container esc = new Container();
        esc.setName("esc");
        Anotador an2 = new Anotador();
        esc.addComponentListener(an2);
        esc.dispatchEvent(new ComponentEvent(esc, ComponentEvent.COMPONENT_RESIZED));
        ok("un evento despachado llega al oyente", an2.visto.contains("resized"));
        an2.visto.clear();
        // Estos eventos los genera el sistema de ventanas al mover o redimensionar de verdad, no el
        // modelo al cambiar un número: un componente que nunca llega a la pantalla no dispara
        // ninguno por su cuenta.
        esc.setBounds(0, 0, 10, 10);
        esc.setLocation(5, 5);
        ok("sin pantalla, cambiar la geometría no dispara eventos", an2.visto.isEmpty());
        ok("pero la geometría sí cambió", esc.getX() == 5 && esc.getWidth() == 10);

        // ---- multicaster
        SoloAccion x = new SoloAccion();
        SoloAccion y = new SoloAccion();
        ActionListener juntos = AWTEventMulticaster.add(x, y);
        juntos.actionPerformed(new ActionEvent(esc, ActionEvent.ACTION_PERFORMED, "ir"));
        ok("los dos oyentes reciben", x.visto.contains("action:ir") && y.visto.contains("action:ir"));
        ActionListener soloY = AWTEventMulticaster.remove(juntos, x);
        x.visto.clear();
        y.visto.clear();
        soloY.actionPerformed(new ActionEvent(esc, ActionEvent.ACTION_PERFORMED, "ir"));
        ok("sacar uno deja al otro", !x.visto.contains("action:ir") && y.visto.contains("action:ir"));
        ok("y con uno solo no queda multicaster", soloY == y);
        ok("juntar con null devuelve el otro", AWTEventMulticaster.add(null, x) == x);

        ActionListener[] todos = AWTEventMulticaster.getListeners(juntos, ActionListener.class);
        ok("aplanar el árbol da los dos", todos.length == 2);

        // ---- oyentes registrados
        Container reg = new Container();
        ok("arranca sin oyentes", reg.getContainerListeners().length == 0);
        reg.addContainerListener(an);
        ok("se registra", reg.getContainerListeners().length == 1);
        reg.addContainerListener(an);
        ok("registrar dos veces al mismo cuenta dos",
                reg.getContainerListeners().length == 2);
        reg.removeContainerListener(an);
        ok("sacar uno deja el otro", reg.getContainerListeners().length == 1);
        reg.addContainerListener(null);
        ok("un null se ignora", reg.getContainerListeners().length == 1);

        // ---- teclas de recorrido
        Container tk = new Container();
        ok("sin fijar, no están fijadas", !tk.areFocusTraversalKeysSet(0));
        Set<AWTKeyStroke> adelante = tk.getFocusTraversalKeys(0);
        ok("las de fábrica incluyen el tabulador",
                adelante.contains(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0)));
        boolean sentidoMalo = false;
        try {
            tk.getFocusTraversalKeys(9);
        } catch (IllegalArgumentException e) {
            sentidoMalo = true;
        }
        ok("un sentido que no existe se rechaza", sentidoMalo);

        // ---- eventos: lo que traen
        MouseEvent me = new MouseEvent(esc, MouseEvent.MOUSE_PRESSED, 1000L,
                InputEvent.SHIFT_DOWN_MASK, 7, 9, 1, false, MouseEvent.BUTTON1);
        ok("el ratón trae su punto", me.getX() == 7 && me.getY() == 9);
        ok("y su punto como objeto", me.getPoint().equals(new Point(7, 9)));
        ok("dice qué botón cambió", me.getButton() == MouseEvent.BUTTON1);
        ok("y qué modificador estaba", me.isShiftDown());
        ok("no consumido de entrada", !me.isConsumed());
        me.consume();
        ok("consumirlo se nota", me.isConsumed());
        me.translatePoint(3, 1);
        ok("correrlo mueve el punto", me.getX() == 10 && me.getY() == 10);

        KeyEvent ke = new KeyEvent(esc, KeyEvent.KEY_PRESSED, 1000L, 0, KeyEvent.VK_F1,
                KeyEvent.CHAR_UNDEFINED);
        ok("una tecla de función no produce carácter", ke.getKeyChar() == KeyEvent.CHAR_UNDEFINED);
        ok("y es tecla de acción", ke.isActionKey());
        KeyEvent letra = new KeyEvent(esc, KeyEvent.KEY_TYPED, 1000L, 0,
                KeyEvent.VK_UNDEFINED, 'a');
        ok("un carácter tecleado no tiene código de tecla",
                letra.getKeyCode() == KeyEvent.VK_UNDEFINED);
        ok("y sí carácter", letra.getKeyChar() == 'a');

        // Un KEY_TYPED con código de tecla es una contradicción y se rechaza.
        boolean contradiccion = false;
        try {
            new KeyEvent(esc, KeyEvent.KEY_TYPED, 0L, 0, KeyEvent.VK_A, 'a');
        } catch (IllegalArgumentException e) {
            contradiccion = true;
        }
        ok("un KEY_TYPED con código de tecla se rechaza", contradiccion);

        // ---- atajos
        AWTKeyStroke k1 = AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK);
        AWTKeyStroke k2 = AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK);
        ok("dos atajos iguales son el mismo objeto", k1 == k2);
        ok("y son iguales", k1.equals(k2));
        ok("el atajo por tecla no tiene carácter", k1.getKeyChar() == KeyEvent.CHAR_UNDEFINED);
        ok("y dispara al apretar", !k1.isOnKeyRelease());
        ok("su tipo de evento es el de apretar", k1.getKeyEventType() == KeyEvent.KEY_PRESSED);

        AWTKeyStroke kc = AWTKeyStroke.getAWTKeyStroke('x');
        ok("el atajo por carácter no tiene tecla", kc.getKeyCode() == KeyEvent.VK_UNDEFINED);
        ok("y su tipo es el de tecleado", kc.getKeyEventType() == KeyEvent.KEY_TYPED);

        // ---- accesibilidad
        Container acc = new Container();
        acc.setName("elPanel");
        acc.add(new Container());
        // Un componente genérico **no** arma su contexto de accesibilidad solo: no sabe qué rol
        // tiene, y contestar "componente de AWT" a todo sería peor que no contestar. Lo arman las
        // subclases concretas.
        ok("un contenedor pelado no tiene contexto de accesibilidad",
                acc.getAccessibleContext() == null);

        // ---- la cola de eventos
        final int[] contador = new int[1];
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                contador[0] = contador[0] + 1;
            }
        });
        ok("invokeAndWait corrió la tarea y esperó", contador[0] == 1);
        ok("y no lo hizo en este hilo", !EventQueue.isDispatchThread());

        final boolean[] enElHilo = new boolean[1];
        EventQueue.invokeAndWait(new Runnable() {
            public void run() {
                enElHilo[0] = EventQueue.isDispatchThread();
            }
        });
        ok("la tarea corrió en el hilo de eventos", enElHilo[0]);

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("AwtCoreTest " + AwtCoreTest.run());
    }
}
