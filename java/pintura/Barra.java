import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.SwingConstants;

/**
 * JScrollBar con BasicScrollBarUI contra el JDK: la geometria de los botones, la pista y el
 * pulgar, el relieve de las tres piezas, y las flechas en los cuatro sentidos.
 *
 * La barra deshabilitada y la que no tiene nada que desplazar entran a proposito: son los dos
 * casos donde el pulgar cambia de tamano o desaparece.
 */
public class Barra {
    static final int W = 160, H = 76;

    /** Deja ver los rectangulos del aspecto, que son protegidos (#504). */
    static class UIAbierto extends BasicScrollBarUI {
        Rectangle pulgar() { return getThumbBounds(); }
        Rectangle pista() { return getTrackBounds(); }
    }

    /** Maqueta el subarbol, que es lo que haria validate con ventana. */
    static void maquetar(Component c) {
        if (c instanceof Container) {
            Container k = (Container) c;
            k.doLayout();
            for (int i = 0; i < k.getComponentCount(); i++) {
                maquetar(k.getComponent(i));
            }
        }
    }

    static int ajustes = 0;

    /** Cuenta los eventos de ajuste; nombrado y no anonimo (#499). */
    static class Contador implements AdjustmentListener {
        public void adjustmentValueChanged(AdjustmentEvent e) {
            ajustes++;
        }
    }

    static String caja(Rectangle r) {
        return r.x + "," + r.y + " " + r.width + "x" + r.height;
    }

    public static int run() {
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBounds(0, 0, W, H);
        p.setOpaque(true);
        p.setBackground(Color.white);

        JScrollBar v = new JScrollBar(JScrollBar.VERTICAL, 20, 30, 0, 100);
        UIAbierto uiv = new UIAbierto();
        v.setUI(uiv);
        v.addAdjustmentListener(new Contador());
        v.setBounds(2, 2, 17, 60);
        p.add(v);

        JScrollBar h = new JScrollBar(JScrollBar.HORIZONTAL, 0, 50, 0, 100);
        h.setUI(new BasicScrollBarUI());
        h.setBounds(24, 2, 60, 17);
        p.add(h);

        JScrollBar lleno = new JScrollBar(JScrollBar.VERTICAL, 0, 100, 0, 100);
        lleno.setUI(new BasicScrollBarUI());
        lleno.setBounds(90, 2, 17, 40);
        p.add(lleno);

        JScrollBar desh = new JScrollBar(JScrollBar.VERTICAL, 30, 20, 0, 100);
        desh.setUI(new BasicScrollBarUI());
        desh.setEnabled(false);
        desh.setBounds(112, 2, 17, 40);
        p.add(desh);

        // Las cuatro flechas sueltas, con los colores por omision del aspecto.
        int[] sentidos = {SwingConstants.NORTH, SwingConstants.SOUTH, SwingConstants.EAST,
            SwingConstants.WEST};
        for (int i = 0; i < sentidos.length; i++) {
            BasicArrowButton b = new BasicArrowButton(sentidos[i]);
            b.setBounds(24 + i * 18, 24, 17, 17);
            p.add(b);
        }
        BasicArrowButton apretada = new BasicArrowButton(SwingConstants.SOUTH);
        apretada.getModel().setArmed(true);
        apretada.getModel().setPressed(true);
        apretada.setBounds(24, 46, 17, 17);
        p.add(apretada);

        BasicArrowButton deshabilitada = new BasicArrowButton(SwingConstants.EAST);
        deshabilitada.setEnabled(false);
        deshabilitada.setBounds(44, 46, 17, 17);
        p.add(deshabilitada);

        maquetar(p);

        System.out.println("//vert pref=" + v.getPreferredSize().width + "x"
                + v.getPreferredSize().height + " min=" + v.getMinimumSize().width + "x"
                + v.getMinimumSize().height + " max=" + v.getMaximumSize().width + "x"
                + v.getMaximumSize().height + " opaca=" + v.isOpaque() + " borde=" + v.getBorder()
                + " hijos=" + v.getComponentCount());
        System.out.println("//vert pulgar=" + caja(uiv.pulgar()) + " pista=" + caja(uiv.pista())
                + " dec=" + caja(v.getComponent(1).getBounds()) + " inc="
                + caja(v.getComponent(0).getBounds()));

        v.setValue(70);
        maquetar(v);
        System.out.println("//al fondo valor=" + v.getValue() + " pulgar=" + caja(uiv.pulgar()));
        v.setValue(0);
        maquetar(v);
        System.out.println("//al tope valor=" + v.getValue() + " pulgar=" + caja(uiv.pulgar()));
        v.setValue(20);
        maquetar(v);

        System.out.println("//bloque=" + v.getBlockIncrement(1) + " unidad="
                + v.getUnitIncrement(1) + " visible=" + v.getVisibleAmount() + " ajustes="
                + (ajustes > 0));

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(Color.red);
        g.fillRect(0, 0, W, H);
        p.paint(g);

        StringBuilder sb = new StringBuilder();
        long suma = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                suma = suma * 31 + rgb;
                sb.append(rgb == 0xFFFFFF ? '.' : (rgb == 0 ? '#' : 'o'));
            }
            sb.append('\n');
        }
        System.out.print(sb);
        System.out.println("//hash=" + suma);
        return (int) suma;
    }
}
