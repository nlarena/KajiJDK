import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetContext;
import java.awt.event.InputEvent;
import java.util.Map;

import jdk.swing.interop.DispatcherWrapper;
import jdk.swing.interop.DragSourceContextWrapper;
import jdk.swing.interop.DropTargetContextWrapper;
import jdk.swing.interop.LightweightFrameWrapper;
import jdk.swing.interop.SwingInterOpUtils;

/**
 * Comprueba {@code jdk.swing.interop} contra el JDK 25.
 *
 * <h2>Que se puede comparar</h2>
 *
 * <p>La regla de teclas de {@code convertModifiersToDropAction}, que es aritmetica pura y no depende
 * de nada; la constante de la toma del mouse; que ceder el despacho de una cola de eventos tenga los
 * efectos que tiene que tener; y con que error falla cada cosa que necesita un sistema de ventanas.
 *
 * <p>Lo que no se compara es dibujar ni arrastrar de verdad: para eso hace falta la mitad nativa,
 * que ni esta biblioteca ni el JDK sin pantalla tienen.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1.
 */
public class IOP1 {

    static final String[] ESPERADO = {
        "mascara|-2147483648",
        "acciones|0;1;2;2;1073741824;2;2;2;0;0;2;2;0;2;2;2;0;1;0;1;0;1;1;1;0;0;0;0;1073741824;1073741824;1073741824;1073741824;0;1;2;2;1073741824;2;2;2;0;1;0;1;0;1;1;1;0;0;2;2;0;2;2;2;",
        "ungrab-nulo|false",
        "ungrab-otro|false",
        "grab|ok",
        "postEvent-nulo|ok",
        "despacho|hilo;programa;",
        "bucle|hilo;programa;bucle;|true",
        "apilar|RuntimeException",
        "instalar-nulo|NullPointerException",
        "cola-nula|NullPointerException",
        "origen|ok",
        "contexto|null",
        "fin-arrastre|IllegalArgumentException",
        "atar-nulo|NullPointerException",
        "reiniciar-nulo|NullPointerException",
        "atar|HeadlessException",
        "ventana|HeadlessException",
    };

    /** Un evento cualquiera, que es lo unico que se puede fabricar sin componentes. */
    static class Ev extends AWTEvent {
        private static final long serialVersionUID = 1L;

        Ev(Object fuente, int id) {
            super(fuente, id);
        }
    }

    /** Una cola que deja llamar a {@code dispatchEvent}, que es protegido. */
    static class Cola extends EventQueue {
        void despachar(AWTEvent e) {
            dispatchEvent(e);
        }
    }

    /** Un despachador que anota lo que le piden y no hace nada mas. */
    static class Desp extends DispatcherWrapper {
        final StringBuilder log = new StringBuilder();

        @Override
        public boolean isDispatchThread() {
            log.append("hilo;");
            return false;
        }

        @Override
        public void scheduleDispatch(Runnable r) {
            log.append("programa;");
        }

        @Override
        public SecondaryLoop createSecondaryLoop() {
            log.append("bucle;");
            return new Bucle();
        }
    }

    /** Un bucle secundario que no espera nada. */
    static class Bucle implements SecondaryLoop {
        public boolean enter() {
            return false;
        }

        public boolean exit() {
            return false;
        }
    }

    /** Un origen de arrastre concreto, para poder instanciarlo. */
    static class Origen extends DragSourceContextWrapper {
        Origen(java.awt.dnd.DragGestureEvent e) {
            super(e);
        }

        @Override
        protected void setNativeCursor(Cursor c, int tipo) {
        }

        @Override
        protected void startDrag(Transferable t, long[] formatos, Map<Long, DataFlavor> mapa) {
        }

        @Override
        public void startSecondaryEventLoop() {
        }

        @Override
        public void quitSecondaryEventLoop() {
        }
    }

    /** Un destino de arrastre concreto, para poder instanciarlo. */
    static class Destino extends DropTargetContextWrapper {
        public void setTargetActions(int a) {
        }

        public int getTargetActions() {
            return 0;
        }

        public DropTarget getDropTarget() {
            return null;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return null;
        }

        public Transferable getTransferable() {
            return null;
        }

        public boolean isTransferableJVMLocal() {
            return false;
        }

        public void acceptDrag(int a) {
        }

        public void rejectDrag() {
        }

        public void acceptDrop(int a) {
        }

        public void rejectDrop() {
        }

        public void dropComplete(boolean s) {
        }
    }

    /** Lo que hace el paquete, una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        a.add("mascara|" + SwingInterOpUtils.GRAB_EVENT_MASK);

        // La regla de teclas, sobre toda la grilla que importa.
        final int[] teclas = {
            0,
            InputEvent.SHIFT_DOWN_MASK,
            InputEvent.CTRL_DOWN_MASK,
            InputEvent.SHIFT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK,
            InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
        };
        final int[] permitidas = {0, 1, 2, 3, 1073741824, 1073741827, 1073741831, -1};
        final StringBuilder g = new StringBuilder();
        for (int i = 0; i < teclas.length; i++) {
            for (int j = 0; j < permitidas.length; j++) {
                g.append(DragSourceContextWrapper.convertModifiersToDropAction(
                        teclas[i], permitidas[j])).append(';');
            }
        }
        a.add("acciones|" + g);

        // Tomar el mouse no existe sin sistema de ventanas, ni aca ni en un JDK sin pantalla.
        a.add("ungrab-nulo|" + SwingInterOpUtils.isUngrabEvent(null));
        a.add("ungrab-otro|" + SwingInterOpUtils.isUngrabEvent(new Ev(new Object(), 9999)));
        a.add("grab|" + intentar(new Grab()));
        a.add("postEvent-nulo|" + intentar(new PostNulo()));

        // Ceder el despacho: la cola tiene que empezar a preguntarle al otro.
        final Cola cola = new Cola();
        final Desp d = new Desp();
        DispatcherWrapper.setFwDispatcher(cola, d);
        cola.despachar(new Ev(new Object(), 9999));
        a.add("despacho|" + d.log);
        final SecondaryLoop bucle = cola.createSecondaryLoop();
        a.add("bucle|" + d.log + "|" + (bucle != null));
        a.add("apilar|" + intentar(new Apilar(cola)));
        a.add("instalar-nulo|" + intentar(new InstalarNulo(cola)));
        a.add("cola-nula|" + intentar(new ColaNula(d)));

        // El origen de un arrastre que nunca arranco.
        a.add("origen|" + intentar(new Construir()));
        a.add("contexto|" + new Origen(null).getDragSourceContext());
        a.add("fin-arrastre|" + intentar(new FinArrastre()));

        // El destino.
        a.add("atar-nulo|" + intentar(new AtarNulo()));
        a.add("reiniciar-nulo|" + intentar(new ReiniciarNulo()));
        a.add("atar|" + intentar(new Atar()));

        // La ventana que no se ve necesita una pantalla que no hay.
        a.add("ventana|" + intentar(new Ventana()));

        return a.toArray(new String[a.size()]);
    }

    /** Corre eso y devuelve "ok" o el nombre simple de lo que haya tirado. */
    static String intentar(Runnable r) {
        try {
            r.run();
            return "ok";
        } catch (Throwable t) {
            final String n = t.getClass().getName();
            return n.substring(n.lastIndexOf('.') + 1);
        }
    }

    static class Grab implements Runnable {
        public void run() {
            SwingInterOpUtils.grab(null, null);
            SwingInterOpUtils.ungrab(null, null);
        }
    }

    static class PostNulo implements Runnable {
        public void run() {
            SwingInterOpUtils.postEvent(null, null);
        }
    }

    static class Apilar implements Runnable {
        private final EventQueue cola;

        Apilar(EventQueue cola) {
            this.cola = cola;
        }

        public void run() {
            cola.push(new EventQueue());
        }
    }

    static class InstalarNulo implements Runnable {
        private final EventQueue cola;

        InstalarNulo(EventQueue cola) {
            this.cola = cola;
        }

        public void run() {
            DispatcherWrapper.setFwDispatcher(cola, null);
        }
    }

    static class ColaNula implements Runnable {
        private final DispatcherWrapper d;

        ColaNula(DispatcherWrapper d) {
            this.d = d;
        }

        public void run() {
            DispatcherWrapper.setFwDispatcher(null, d);
        }
    }

    static class Construir implements Runnable {
        public void run() {
            new Origen(null);
        }
    }

    static class FinArrastre implements Runnable {
        public void run() {
            new Origen(null).dragDropFinished(true, 3, 10, 20);
        }
    }

    static class AtarNulo implements Runnable {
        public void run() {
            new Destino().setDropTargetContext(null, null);
        }
    }

    static class ReiniciarNulo implements Runnable {
        public void run() {
            new Destino().reset(null);
        }
    }

    static class Atar implements Runnable {
        public void run() {
            final DropTargetContext c = new DropTarget().getDropTargetContext();
            final Destino t = new Destino();
            t.setDropTargetContext(c, t);
            t.reset(c);
        }
    }

    static class Ventana implements Runnable {
        public void run() {
            new LightweightFrameWrapper();
        }
    }

    /**
     * El indice de la primera respuesta que no coincide con la del JDK, o -1.
     *
     * @return el indice, o -1
     */
    public static int donde() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != ESPERADO.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(ESPERADO[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = donde();
        System.out.println(i < 0 ? "sin diferencias"
                : i + ":\n  nuestro=" + a[i] + "\n  jdk    =" + ESPERADO[i]);
    }
}
