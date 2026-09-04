import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.beans.AppletInitializer;
import java.beans.BeanInfo;
import java.beans.Beans;
import java.beans.Introspector;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.beans.SimpleBeanInfo;
import java.beans.beancontext.BeanContext;
import java.net.URL;

/**
 * Los diez miembros de `java.beans` que dependen de `java.awt` y `java.applet`, y el paquete
 * `java.applet` entero.
 *
 * <p>El mismo archivo da -1 con el JDK 25 corriendo **sus** clases, sin pantalla
 * (`-Djava.awt.headless=true`). Casi todo lo que se comprueba es qué hace cada cosa cuando no hay
 * pantalla, así que correrlo con pantalla mediría otra cosa.
 */
public class BeansAwtTest {

    static int failures = 0;

    static void ok(String que, boolean bien) {
        if (!bien) {
            failures = failures + 1;
            System.out.println("FALLA: " + que);
        }
    }

    /** Un bean cualquiera, para que el introspector arme un BeanInfo. */
    public static class Cosa {
        private int valor;

        public int getValor() {
            return this.valor;
        }

        public void setValor(int valor) {
            this.valor = valor;
        }
    }

    /** Un editor que dice que sabe dibujarse. */
    static class EditorPintor extends PropertyEditorSupport {
        int pintadas = 0;

        public boolean isPaintable() {
            return true;
        }

        public void paintValue(java.awt.Graphics g, Rectangle r) {
            this.pintadas = this.pintadas + 1;
        }
    }

    public static int run() throws Exception {
        iconos();
        editores();
        applet();
        instanciar();
        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    static void iconos() throws Exception {
        ok("las cuatro clases de ícono son 1..4",
                BeanInfo.ICON_COLOR_16x16 == 1 && BeanInfo.ICON_COLOR_32x32 == 2
                        && BeanInfo.ICON_MONO_16x16 == 3 && BeanInfo.ICON_MONO_32x32 == 4);

        SimpleBeanInfo simple = new SimpleBeanInfo();
        ok("el BeanInfo vacío no ofrece ícono", simple.getIcon(BeanInfo.ICON_COLOR_16x16) == null);
        ok("ni de ninguna clase", simple.getIcon(BeanInfo.ICON_MONO_32x32) == null);
        ok("un recurso que no existe no da imagen", simple.loadImage("NoExiste.gif") == null);

        // El BeanInfo que arma el introspector tampoco inventa un ícono.
        BeanInfo deducido = Introspector.getBeanInfo(Cosa.class);
        ok("el introspector devuelve un BeanInfo", deducido != null);
        ok("y sin ícono", deducido.getIcon(BeanInfo.ICON_COLOR_16x16) == null);
    }

    static void editores() {
        PropertyEditorSupport e = new PropertyEditorSupport();
        ok("el editor de base no sabe dibujarse", !e.isPaintable());
        ok("ni tiene panel propio", !e.supportsCustomEditor());
        ok("y getCustomEditor lo confirma", e.getCustomEditor() == null);
        // Pintar con el de base no hace nada, y sobre todo no rompe: `null` de Graphics incluido.
        e.paintValue(null, new Rectangle(0, 0, 10, 10));

        EditorPintor p = new EditorPintor();
        ok("una subclase puede decir que se dibuja", p.isPaintable());
        p.paintValue(null, new Rectangle(0, 0, 10, 10));
        ok("y la llamada le llega", p.pintadas == 1);
        PropertyEditor comoInterfaz = p;
        ok("también a través de la interfaz", comoInterfaz.getCustomEditor() == null);
    }

    static void applet() throws Exception {
        boolean tiro = false;
        try {
            new Applet();
        } catch (HeadlessException e) {
            tiro = true;
        }
        ok("no se puede armar un applet sin pantalla", tiro);

        // Un clip es vago: no mira la dirección hasta que alguien lo reproduce, así que `null` se
        // acepta y el error, si lo hay, sale después.
        ok("un clip sin dirección se arma igual", Applet.newAudioClip(null) != null);

        AudioClip clip = Applet.newAudioClip(new URL("file:///no/existe.wav"));
        ok("un clip con dirección se arma aunque no haya pantalla", clip != null);
        // Parar un clip que nunca sonó no es un error en ninguna implementación.
        clip.stop();
    }

    static void instanciar() throws Exception {
        final int[] llamadas = new int[1];
        AppletInitializer init = new AppletInitializer() {
            public void initialize(Applet a, BeanContext c) {
                llamadas[0] = llamadas[0] + 1;
            }

            public void activate(Applet a) {
                llamadas[0] = llamadas[0] + 1;
            }
        };
        Object bean = Beans.instantiate(null, "java.util.ArrayList", null, init);
        ok("la forma con inicializador trae el bean", bean instanceof java.util.ArrayList);
        ok("y a un bean que no es applet no lo prepara", llamadas[0] == 0);

        Object otro = Beans.instantiate(null, "java.util.HashMap", null, null);
        ok("sin inicializador también anda", otro instanceof java.util.HashMap);

        boolean tiroNombre = false;
        try {
            Beans.instantiate(null, null, null, init);
        } catch (NullPointerException e) {
            tiroNombre = true;
        }
        ok("un nombre null tira", tiroNombre);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("BeansAwtTest " + BeansAwtTest.run());
    }
}
