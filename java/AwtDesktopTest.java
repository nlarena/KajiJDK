import java.awt.AWTException;
import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.Desktop;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.MediaTracker;
import java.awt.MouseInfo;
import java.awt.PopupMenu;
import java.awt.Robot;
import java.awt.SplashScreen;
import java.awt.SystemTray;
import java.awt.Taskbar;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;

/**
 * Lo que queda de `java.awt` afuera del árbol de componentes: el gestor del foco, el entorno
 * gráfico, y los cinco servicios del escritorio.
 *
 * <p>El mismo archivo da -1 con el JDK 25 corriendo **sus** clases, sin pantalla
 * (`-Djava.awt.headless=true`). Esa aclaración importa más acá que en las otras pruebas: casi todo
 * lo que se comprueba es precisamente **qué hace cada cosa cuando no hay pantalla**, así que
 * correrlo con pantalla mediría otra cosa.
 */
public class AwtDesktopTest {

    static int failures = 0;

    static void ok(String que, boolean bien) {
        if (!bien) {
            failures = failures + 1;
            System.out.println("FALLA: " + que);
        }
    }

    /** Un componente concreto que se puede meter en un contenedor. */
    static class Hoja extends Container {
        private final String nombre;

        Hoja(String nombre) {
            this.nombre = nombre;
        }

        public String toString() {
            return this.nombre;
        }
    }

    /**
     * La política de orden de contenedor, pero aceptando a cualquiera que admita el foco.
     *
     * <p>Sin esto no se puede probar el **orden**: la de base exige que el componente sea mostrable,
     * y sin pantalla no lo es ninguno, así que todo daría `null` y no se estaría midiendo el
     * recorrido sino la falta de pantalla.
     */
    static class PoliticaLaxa extends ContainerOrderFocusTraversalPolicy {
        protected boolean accept(Component c) {
            return c.isFocusable();
        }
    }

    public static int run() throws Exception {
        gestorDeFoco();
        recorrido();
        entornoGrafico();
        mouse();
        seguidorDeMedios();
        servicios();
        iconoDeBandeja();
        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    static void gestorDeFoco() {
        KeyboardFocusManager k = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        ok("hay un gestor de foco", k != null);
        ok("y es siempre el mismo", k == KeyboardFocusManager.getCurrentKeyboardFocusManager());

        ok("los cuatro sentidos son 0..3",
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS == 0
                        && KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS == 1
                        && KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS == 2
                        && KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS == 3);

        Set<AWTKeyStroke> ade = k.getDefaultFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        ok("hacia adelante hay dos teclas", ade.size() == 2);
        ok("y una es Tab", ade.contains(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB, 0)));
        Set<AWTKeyStroke> atras = k.getDefaultFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
        ok("hacia atrás hay dos", atras.size() == 2);
        ok("y una es Shift+Tab", atras.contains(AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_TAB,
                java.awt.event.InputEvent.SHIFT_DOWN_MASK)));
        ok("los dos ciclos no tienen ninguna",
                k.getDefaultFocusTraversalKeys(
                        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS).isEmpty()
                        && k.getDefaultFocusTraversalKeys(
                                KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS).isEmpty());

        boolean tiro = false;
        try {
            k.getDefaultFocusTraversalKeys(9);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok("un sentido inventado tira", tiro);

        boolean tiroNull = false;
        try {
            k.setDefaultFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        } catch (IllegalArgumentException e) {
            tiroNull = true;
        }
        ok("un conjunto null tira", tiroNull);

        boolean tiroPolitica = false;
        try {
            k.setDefaultFocusTraversalPolicy(null);
        } catch (IllegalArgumentException e) {
            tiroPolitica = true;
        }
        ok("una política null tira", tiroPolitica);
        ok("hay una política por omisión", k.getDefaultFocusTraversalPolicy() != null);

        FocusTraversalPolicy p = new PoliticaLaxa();
        k.setDefaultFocusTraversalPolicy(p);
        ok("cambiar la política la cambia", k.getDefaultFocusTraversalPolicy() == p);

        // Los repartidores y posprocesadores: sin ninguno, la lista es null, no vacía.
        MiGestor mio = new MiGestor();
        ok("sin repartidores no hay lista", mio.repartidores() == null);
        KeyEventDispatcher d = new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                return false;
            }
        };
        mio.addKeyEventDispatcher(d);
        ok("agregado, la lista tiene uno", mio.repartidores().size() == 1);
        mio.removeKeyEventDispatcher(d);
        // Sacado el último, la lista queda **vacía**, no vuelve a ser null: lo que el null informa es
        // que nunca hubo cadena, no que ahora esté sin elementos.
        ok("sacado, la lista queda vacía", mio.repartidores() != null
                && mio.repartidores().isEmpty());
        mio.addKeyEventDispatcher(null);
        ok("agregar null no agrega nada", mio.repartidores().isEmpty());

        KeyEventPostProcessor pp = new KeyEventPostProcessor() {
            public boolean postProcessKeyEvent(KeyEvent e) {
                return false;
            }
        };
        mio.addKeyEventPostProcessor(pp);
        ok("un posprocesador se registra", mio.posprocesadores().size() == 1);
        mio.removeKeyEventPostProcessor(pp);
        ok("y se saca", mio.posprocesadores() != null && mio.posprocesadores().isEmpty());

        // Cambiar el gestor de turno y volver: es global, así que se deja como estaba.
        KeyboardFocusManager.setCurrentKeyboardFocusManager(mio);
        ok("el gestor se puede cambiar",
                KeyboardFocusManager.getCurrentKeyboardFocusManager() == mio);
        KeyboardFocusManager.setCurrentKeyboardFocusManager(k);
        ok("y volver al anterior",
                KeyboardFocusManager.getCurrentKeyboardFocusManager() == k);

        // Sin sistema de ventanas, las cinco propiedades globales arrancan vacías.
        ok("nadie tiene el foco", k.getFocusOwner() == null);
        ok("ni el permanente", k.getPermanentFocusOwner() == null);
        ok("no hay ventana con foco", k.getFocusedWindow() == null);
        ok("ni ventana activa", k.getActiveWindow() == null);
        ok("ni raíz de ciclo", k.getCurrentFocusCycleRoot() == null);

        final int[] avisos = new int[1];
        PropertyChangeListener pcl = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                avisos[0] = avisos[0] + 1;
            }
        };
        k.addPropertyChangeListener("currentFocusCycleRoot", pcl);
        ok("el oyente quedó puesto",
                k.getPropertyChangeListeners("currentFocusCycleRoot").length == 1);
        Container raiz = new Container();
        k.setGlobalCurrentFocusCycleRoot(raiz);
        ok("la raíz de ciclo se fijó", k.getCurrentFocusCycleRoot() == raiz);
        ok("y avisó una vez", avisos[0] == 1);
        k.setGlobalCurrentFocusCycleRoot(raiz);
        ok("fijar lo mismo no vuelve a avisar", avisos[0] == 1);
        k.setGlobalCurrentFocusCycleRoot(null);
        k.removePropertyChangeListener("currentFocusCycleRoot", pcl);
        ok("y se saca", k.getPropertyChangeListeners("currentFocusCycleRoot").length == 0);
    }

    /**
     * Un gestor propio, para poder mirar lo que el de base guarda `protected`.
     *
     * <p>`getKeyEventDispatchers` y `getKeyEventPostProcessors` son protegidos, y un `protected` se ve
     * desde una subclase pero **sobre sí misma**, no sobre otra instancia. Así que la forma de
     * mirarlos no es espiar al gestor de turno sino ser uno.
     */
    static class MiGestor extends java.awt.DefaultKeyboardFocusManager {
        java.util.List<KeyEventDispatcher> repartidores() {
            return this.getKeyEventDispatchers();
        }

        java.util.List<KeyEventPostProcessor> posprocesadores() {
            return this.getKeyEventPostProcessors();
        }
    }

    static void recorrido() {
        PoliticaLaxa p = new PoliticaLaxa();
        ok("entra en los ciclos por omisión", p.getImplicitDownCycleTraversal());
        p.setImplicitDownCycleTraversal(false);
        ok("se puede apagar el descenso implícito", !p.getImplicitDownCycleTraversal());
        p.setImplicitDownCycleTraversal(true);

        Container raiz = new Container();
        raiz.setFocusCycleRoot(true);
        Hoja a = new Hoja("a");
        Hoja b = new Hoja("b");
        raiz.add(a);
        raiz.add(b);

        // Sin pantalla nada es mostrable, y un ciclo que no se ve no tiene a quién darle el foco.
        // No es un error: los cinco métodos contestan `null`.
        ok("un contenedor no mostrable no tiene primero", p.getFirstComponent(raiz) == null);
        ok("ni último", p.getLastComponent(raiz) == null);
        ok("ni componente de arranque", p.getDefaultComponent(raiz) == null);
        ok("ni siguiente", p.getComponentAfter(raiz, a) == null);
        ok("ni anterior", p.getComponentBefore(raiz, b) == null);

        boolean tiro = false;
        try {
            p.getFirstComponent(null);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok("un contenedor null tira", tiro);

        boolean tiroDos = false;
        try {
            p.getComponentAfter(raiz, null);
        } catch (IllegalArgumentException e) {
            tiroDos = true;
        }
        ok("un componente null también", tiroDos);

        // Preguntar sobre un contenedor que no es raíz de ciclo ni proveedor de política no tiene
        // respuesta: no se recorre un contenedor cualquiera sino un ciclo.
        Container suelto = new Container();
        Hoja c = new Hoja("c");
        suelto.add(c);
        boolean tiroTres = false;
        try {
            p.getComponentAfter(suelto, c);
        } catch (IllegalArgumentException e) {
            tiroTres = true;
        }
        ok("un contenedor que no es raíz de ciclo tira", tiroTres);
        ok("pero pedirle el primero no tira, sólo da null",
                p.getFirstComponent(suelto) == null);

        // Un proveedor de política sí se puede recorrer aunque no sea raíz de ciclo.
        suelto.setFocusTraversalPolicyProvider(true);
        ok("un proveedor de política sí se acepta", p.getComponentAfter(suelto, c) == null);
        suelto.setFocusTraversalPolicyProvider(false);

        // Un componente que pertenece a **otro** ciclo tampoco tiene respuesta acá.
        boolean tiroCuatro = false;
        try {
            p.getComponentAfter(raiz, c);
        } catch (IllegalArgumentException e) {
            tiroCuatro = true;
        }
        ok("un componente de otro ciclo tira", tiroCuatro);

        // La política de fábrica es una de orden de contenedor con otro `accept`.
        java.awt.DefaultFocusTraversalPolicy dfp = new java.awt.DefaultFocusTraversalPolicy();
        ok("la de fábrica es una de orden de contenedor",
                dfp instanceof ContainerOrderFocusTraversalPolicy);
        ok("y hereda el descenso implícito", dfp.getImplicitDownCycleTraversal());
        ok("con las mismas precondiciones", dfp.getFirstComponent(raiz) == null);
    }

    static void entornoGrafico() {
        ok("no hay pantalla", GraphicsEnvironment.isHeadless());
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ok("hay un entorno gráfico", ge != null);
        ok("y es siempre el mismo", ge == GraphicsEnvironment.getLocalGraphicsEnvironment());
        ok("el entorno también es sin pantalla", ge.isHeadlessInstance());

        boolean tiro = false;
        try {
            ge.getScreenDevices();
        } catch (HeadlessException e) {
            tiro = true;
        }
        ok("pedir las pantallas tira", tiro);

        boolean tiroDefault = false;
        try {
            ge.getDefaultScreenDevice();
        } catch (HeadlessException e) {
            tiroDefault = true;
        }
        ok("pedir la principal también", tiroDefault);

        boolean tiroCentro = false;
        try {
            ge.getCenterPoint();
        } catch (HeadlessException e) {
            tiroCentro = true;
        }
        ok("el centro de la pantalla también", tiroCentro);

        boolean tiroMax = false;
        try {
            ge.getMaximumWindowBounds();
        } catch (HeadlessException e) {
            tiroMax = true;
        }
        ok("y el área maximizable", tiroMax);

        ok("una tipografía no creada no se registra",
                !ge.registerFont(new Font("Dialog", Font.PLAIN, 12)));
        boolean tiroFuente = false;
        try {
            ge.registerFont(null);
        } catch (NullPointerException e) {
            tiroFuente = true;
        }
        ok("registrar null tira", tiroFuente);

        ok("las tipografías se pueden pedir sin pantalla", ge.getAllFonts() != null);
        ok("y las familias también", ge.getAvailableFontFamilyNames() != null);
        // No hacen nada, pero no rompen: son preferencias.
        ge.preferLocaleFonts();
        ge.preferProportionalFonts();
    }

    static void mouse() {
        boolean tiro = false;
        try {
            MouseInfo.getPointerInfo();
        } catch (HeadlessException e) {
            tiro = true;
        }
        ok("preguntar dónde está el puntero tira", tiro);

        boolean tiroBotones = false;
        try {
            MouseInfo.getNumberOfButtons();
        } catch (HeadlessException e) {
            tiroBotones = true;
        }
        ok("preguntar cuántos botones tiene también", tiroBotones);
    }

    static void seguidorDeMedios() throws Exception {
        ok("las cuatro banderas son potencias de dos",
                MediaTracker.LOADING == 1 && MediaTracker.ABORTED == 2
                        && MediaTracker.ERRORED == 4 && MediaTracker.COMPLETE == 8);

        MediaTracker t = new MediaTracker(new Container());
        ok("un seguidor vacío ya terminó", t.checkAll());
        ok("y sin cargar nada", t.checkAll(false));
        ok("no falló nada", !t.isErrorAny());
        ok("y no hay errores que listar", t.getErrorsAny() == null);
        ok("el estado de todo es cero", t.statusAll(false) == 0);
        ok("el grupo 0 también terminó", t.checkID(0));
        ok("sin errores", !t.isErrorID(0) && t.getErrorsID(0) == null);
        // Esperar a un seguidor vacío contesta `false`: la pregunta es si están todas cargadas.
        ok("esperar a nada da false", !t.waitForAll(10));

        Image img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        t.addImage(img, 0);
        ok("una imagen ya cargada cuenta como terminada", t.checkAll(true));
        ok("y su estado es COMPLETE", (t.statusID(0, true) & MediaTracker.COMPLETE) != 0);
        ok("no falló", !t.isErrorAny());

        ok("y esperarla da true", t.waitForAll(10));

        t.removeImage(img);
        ok("sacada, vuelve a estar vacío", t.statusAll(false) == 0);
        // Un tiempo negativo es tiempo ya vencido, no un error.
        ok("un tiempo negativo contesta con lo que haya", !t.waitForAll(-1));
    }

    static void servicios() {
        boolean tiroRobot = false;
        try {
            new Robot();
        } catch (AWTException e) {
            tiroRobot = true;
        }
        ok("no se puede armar un Robot sin pantalla", tiroRobot);

        boolean tiroBienvenida = false;
        try {
            SplashScreen.getSplashScreen();
        } catch (HeadlessException e) {
            tiroBienvenida = true;
        }
        ok("pedir la pantalla de bienvenida tira", tiroBienvenida);

        ok("no hay bandeja del sistema", !SystemTray.isSupported());
        boolean tiroBandeja = false;
        try {
            SystemTray.getSystemTray();
        } catch (UnsupportedOperationException e) {
            // `HeadlessException` es una `UnsupportedOperationException`, así que este catch
            // atrapa las dos: la falta de bandeja y la falta de pantalla.
            tiroBandeja = true;
        }
        ok("pedirla tira", tiroBandeja);

        ok("no hay barra de tareas", !Taskbar.isTaskbarSupported());
        boolean tiroBarra = false;
        try {
            Taskbar.getTaskbar();
        } catch (UnsupportedOperationException e) {
            tiroBarra = true;
        }
        ok("pedirla tira", tiroBarra);

        ok("no hay escritorio", !Desktop.isDesktopSupported());
        boolean tiroEscritorio = false;
        try {
            Desktop.getDesktop();
        } catch (UnsupportedOperationException e) {
            tiroEscritorio = true;
        }
        ok("pedirlo tira", tiroEscritorio);

        // La barra de menú del sistema: el único método de `java.awt` que nombra un tipo de Swing.
        // Se comprueba que se pueda **nombrar** y que tire como los demás, que es todo lo que hace.
        boolean tiroMenu = false;
        try {
            Desktop.getDesktop().setDefaultMenuBar(new javax.swing.JMenuBar());
        } catch (UnsupportedOperationException e) {
            tiroMenu = true;
        }
        ok("poner la barra de menú del sistema tira", tiroMenu);
        ok("y un JMenuBar es un JComponent",
                new javax.swing.JMenuBar() instanceof javax.swing.JComponent);
        ok("y un JComponent es un Container",
                new javax.swing.JMenuBar() instanceof java.awt.Container);

        ok("las acciones del escritorio son 24", Desktop.Action.values().length == 24);
        ok("y se buscan por nombre",
                Desktop.Action.valueOf("MOVE_TO_TRASH") == Desktop.Action.MOVE_TO_TRASH);
        ok("la barra de tareas tiene diez funciones", Taskbar.Feature.values().length == 10);
        ok("y cinco estados de progreso", Taskbar.State.values().length == 5);
        ok("los avisos de bandeja son cuatro", TrayIcon.MessageType.values().length == 4);
    }

    static void iconoDeBandeja() {
        Image img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);

        // La falta de pantalla se comprueba primero: ni siquiera se llega a mirar el dibujo, así que
        // un `null` tira por la bandeja que falta y no por la imagen que falta.
        boolean tiroImagen = false;
        try {
            new TrayIcon(null);
        } catch (HeadlessException e) {
            tiroImagen = true;
        } catch (IllegalArgumentException e) {
            tiroImagen = false;
        }
        ok("sin pantalla, ni se mira el dibujo", tiroImagen);

        boolean tiroUno = false;
        try {
            new TrayIcon(img);
        } catch (HeadlessException e) {
            tiroUno = true;
        }
        ok("no se puede armar un ícono sin pantalla", tiroUno);

        boolean tiroDos = false;
        try {
            new TrayIcon(img, "hola");
        } catch (HeadlessException e) {
            tiroDos = true;
        }
        ok("tampoco con texto emergente", tiroDos);

        boolean tiroTres = false;
        try {
            new TrayIcon(img, "hola", new PopupMenu());
        } catch (HeadlessException e) {
            tiroTres = true;
        }
        ok("ni con menú", tiroTres);

        // Lo que sí se puede mirar sin instancia son sus avisos.
        ok("los avisos son cuatro", TrayIcon.MessageType.values().length == 4);
        ok("y se buscan por nombre",
                TrayIcon.MessageType.valueOf("WARNING") == TrayIcon.MessageType.WARNING);
        ok("el de menor peso es NONE",
                TrayIcon.MessageType.NONE.ordinal() == 3);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("AwtDesktopTest " + AwtDesktopTest.run());
    }
}
