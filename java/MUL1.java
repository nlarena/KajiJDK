import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.multi.MultiButtonUI;
import javax.swing.plaf.multi.MultiLookAndFeel;

/**
 * Comprueba {@code javax.swing.plaf.multi} contra el JDK 25.
 *
 * <h2>Que arma</h2>
 *
 * <p>Un aspecto grafico principal de mentira y otro auxiliar, cada uno con su tabla que apunta a una
 * interfaz grafica que anota lo que le piden. Despues pide la interfaz de un componente y mira que
 * salga: con un solo aspecto tiene que salir la de ese, sin envolver; con dos, un multiplexor que le
 * pasa cada llamada a las dos y devuelve lo que dijo la primera.
 *
 * <p>Los aspectos son de mentira a proposito. Esta biblioteca no tiene ninguno implementado, y con
 * los del JDK la prueba mediria el aspecto Metal en vez del mecanismo de reparto.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1.
 */
public class MUL1 {


    static final String[] ESPERADO = {
        "datos|Multiplexing Look and Feel|Multiplex|Allows multiple UI instances per component instance|false|true",
        "sin-clase|null",
        "uno-solo|MUL1x",
        "con-auxiliar|javax.swing.plaf.multi.MultiButtonUI",
        "cuantas|2",
        "installUI|1|1",
        "getPreferredSize|10|1|1",
        "contains|true|1|1",
        "tras-sacar|true",
        "uno-de-nuevo|MUL1x",
    };

    /** El componente que se le pasa a la interfaz grafica; lo unico que importa es su identificador. */
    static class Comp extends JComponent {
        private static final long serialVersionUID = 1L;

        @Override
        public String getUIClassID() {
            return "ButtonUI";
        }
    }

    /** Un componente cuyo identificador no esta en ninguna tabla. */
    static class Huerfano extends JComponent {
        private static final long serialVersionUID = 1L;

        @Override
        public String getUIClassID() {
            return "NoExisteUI";
        }
    }

    /** Un aspecto grafico de mentira, con una tabla que apunta a la clase que se le diga. */
    static class Falso extends LookAndFeel {
        private final String nombre;
        private final UIDefaults tabla;

        Falso(String nombre, String claseUI) {
            this.nombre = nombre;
            this.tabla = new UIDefaults();
            this.tabla.put("ButtonUI", claseUI);
        }

        @Override
        public String getName() {
            return nombre;
        }

        @Override
        public String getID() {
            return nombre;
        }

        @Override
        public String getDescription() {
            return nombre;
        }

        @Override
        public boolean isNativeLookAndFeel() {
            return false;
        }

        @Override
        public boolean isSupportedLookAndFeel() {
            return true;
        }

        @Override
        public UIDefaults getDefaults() {
            return tabla;
        }
    }

    /** Lo que hace el paquete, una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        final MultiLookAndFeel m = new MultiLookAndFeel();
        a.add("datos|" + m.getName() + "|" + m.getID() + "|" + m.getDescription()
                + "|" + m.isNativeLookAndFeel() + "|" + m.isSupportedLookAndFeel());

        UIManager.setLookAndFeel(new Falso("principal", "MUL1x"));
        final Comp c = new Comp();

        // Sin clase para ese componente no hay de que repartir, y eso no se tapa con un multiplexor
        // vacio: se devuelve null y el problema queda donde esta. Se prueba con un identificador que
        // no esta en ninguna tabla, y no arrancando sin aspecto, porque el JDK arranca con Metal
        // puesto y esta biblioteca no tiene ninguno: eso no se puede comparar.
        a.add("sin-clase|" + MultiButtonUI.createUI(new Huerfano()));

        // Con uno solo, la interfaz sale sin envolver.
        final ComponentUI sola = MultiButtonUI.createUI(c);
        a.add("uno-solo|" + sola.getClass().getName());

        // Con un auxiliar, sale el multiplexor.
        UIManager.addAuxiliaryLookAndFeel(new Falso("auxiliar", "MUL1y"));
        final ComponentUI multi = MultiButtonUI.createUI(c);
        a.add("con-auxiliar|" + multi.getClass().getName());
        a.add("cuantas|" + ((MultiButtonUI) multi).getUIs().length);

        MUL1x.llamadasA = 0;
        MUL1x.llamadasB = 0;
        multi.installUI(c);
        a.add("installUI|" + MUL1x.llamadasA + "|" + MUL1x.llamadasB);

        // Lo que devuelve tiene que ser lo que dijo la primera, y las dos tienen que haberse
        // enterado: es toda la razon de ser de este paquete.
        MUL1x.llamadasA = 0;
        MUL1x.llamadasB = 0;
        final Dimension d = multi.getPreferredSize(c);
        a.add("getPreferredSize|" + d.width + "|" + MUL1x.llamadasA + "|" + MUL1x.llamadasB);

        MUL1x.llamadasA = 0;
        MUL1x.llamadasB = 0;
        a.add("contains|" + multi.contains(c, 1, 1) + "|" + MUL1x.llamadasA + "|" + MUL1x.llamadasB);

        // Y al sacar el auxiliar se vuelve al caso simple.
        UIManager.removeAuxiliaryLookAndFeel(UIManager.getAuxiliaryLookAndFeels()[0]);
        a.add("tras-sacar|" + (UIManager.getAuxiliaryLookAndFeels() == null));
        a.add("uno-de-nuevo|" + MultiButtonUI.createUI(c).getClass().getName());
        return a.toArray(new String[a.size()]);
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
