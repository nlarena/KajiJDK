import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * JViewport contra el JDK: la vista mas grande que el agujero, el recorte, la posicion cambiada
 * de signo, scrollRectToVisible y los avisos de cambio. Ademas las cuentas de
 * DefaultBoundedRangeModel, que es el modelo que despues usa la barra.
 */
public class Vista {
    static final int W = 120, H = 100;

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

    /** Una vista con rayas, para que se vea que pedazo quedo adentro. */
    static class Rayada extends JComponent {
        public void paintComponent(Graphics g) {
            g.setColor(new Color(0xCCFFCC));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.black);
            for (int y = 0; y < getHeight(); y = y + 10) {
                g.drawLine(0, y, getWidth() - 1, y);
            }
            for (int x = 0; x < getWidth(); x = x + 20) {
                g.drawLine(x, 0, x, getHeight() - 1);
            }
        }
    }

    /**
     * Una ventana que deja ver computeBlit, que es protegido.
     *
     * <p>Llamarlo desde afuera compila con nuestro javac y no con el del JDK (hallazgo #504); la
     * subclase es la forma que Java admite, y la que deja comparar los dos lados.
     */
    static class VistaAbierta extends JViewport {
        boolean blit(int dx, int dy, Point desde, Point hasta, Dimension tam, Rectangle pinta) {
            return computeBlit(dx, dy, desde, hasta, tam, pinta);
        }
    }

    static int avisos = 0;

    /** Cuenta los avisos de cambio; nombrado y no anonimo (#499). */
    static class Contador implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            avisos++;
        }
    }

    static void modelo() {
        DefaultBoundedRangeModel m = new DefaultBoundedRangeModel(20, 30, 0, 100);
        System.out.println("//modelo " + m.getValue() + " " + m.getExtent() + " " + m.getMinimum()
                + " " + m.getMaximum());
        m.setValue(90);
        System.out.println("//valor 90 -> " + m.getValue() + " ext " + m.getExtent());
        m.setValue(-5);
        System.out.println("//valor -5 -> " + m.getValue());
        m.setExtent(200);
        System.out.println("//ext 200 -> " + m.getExtent() + " val " + m.getValue());
        m.setMaximum(50);
        System.out.println("//max 50 -> " + m.getValue() + " " + m.getExtent() + " "
                + m.getMinimum() + " " + m.getMaximum());
        m.setMinimum(40);
        System.out.println("//min 40 -> " + m.getValue() + " " + m.getExtent() + " "
                + m.getMinimum() + " " + m.getMaximum());
        m.setRangeProperties(5, 10, 0, 12, true);
        System.out.println("//rango -> " + m.getValue() + " " + m.getExtent() + " "
                + m.getMinimum() + " " + m.getMaximum() + " " + m.getValueIsAdjusting());
        System.out.println("//toString " + m.toString());
    }

    public static int run() {
        modelo();

        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBounds(0, 0, W, H);
        p.setOpaque(true);
        p.setBackground(Color.white);

        VistaAbierta vp = new VistaAbierta();
        vp.setBackground(new Color(0xEEEEEE));
        vp.addChangeListener(new Contador());
        Rayada vista = new Rayada();
        vista.setPreferredSize(new Dimension(90, 80));
        vp.setView(vista);
        vp.setBounds(4, 4, 50, 40);
        p.add(vp);

        // Una segunda ventana con una etiqueta chica: la vista se estira hasta llenar.
        JViewport chica = new JViewport();
        chica.setBackground(new Color(0xFFEECC));
        JLabel etiqueta = new JLabel("Hola");
        etiqueta.setFont(new Font("Dialog", Font.PLAIN, 12));
        etiqueta.setForeground(Color.black);
        etiqueta.setOpaque(true);
        etiqueta.setBackground(new Color(0xCCCCFF));
        chica.setView(etiqueta);
        chica.setBounds(60, 4, 56, 24);
        p.add(chica);

        JPanel marco = new JPanel();
        marco.setLayout(null);
        marco.setOpaque(false);
        marco.setBorder(new LineBorder(Color.black, 1));
        marco.setBounds(3, 3, 52, 42);
        p.add(marco);

        maquetar(p);
        int avisosTrasMaquetar = avisos;

        System.out.println("//vista tam=" + vp.getViewSize().width + "x" + vp.getViewSize().height
                + " pos=" + vp.getViewPosition().x + "," + vp.getViewPosition().y + " ext="
                + vp.getExtentSize().width + "x" + vp.getExtentSize().height + " insets="
                + vp.getInsets() + " opaca=" + vp.isOpaque() + " hijos=" + vp.getComponentCount());
        System.out.println("//vista bounds=" + vista.getX() + "," + vista.getY() + " "
                + vista.getWidth() + "x" + vista.getHeight() + " chica="
                + etiqueta.getWidth() + "x" + etiqueta.getHeight());

        vp.setViewPosition(new Point(25, 30));
        System.out.println("//tras mover pos=" + vp.getViewPosition().x + ","
                + vp.getViewPosition().y + " vista en " + vista.getX() + "," + vista.getY()
                + " rect=" + vp.getViewRect());

        // Un rectangulo de la vista que ya se ve entero: no mueve nada.
        vp.scrollRectToVisible(new Rectangle(30, 35, 10, 10));
        System.out.println("//visible ya: " + vp.getViewPosition().x + ","
                + vp.getViewPosition().y);
        // Uno mas arriba: se alinea por el borde de arriba.
        vp.scrollRectToVisible(new Rectangle(0, -20, 10, 10));
        System.out.println("//visible arriba: " + vp.getViewPosition().x + ","
                + vp.getViewPosition().y);
        // Uno mas abajo del final: se pega al fondo.
        vp.scrollRectToVisible(new Rectangle(0, 100, 10, 10));
        System.out.println("//visible abajo: " + vp.getViewPosition().x + ","
                + vp.getViewPosition().y);

        vp.setViewPosition(new Point(18, 22));

        Point blitDesde = new Point();
        Point blitHasta = new Point();
        Dimension blitTam = new Dimension();
        Rectangle blitPinta = new Rectangle();
        boolean sirve = vp.blit(0, 12, blitDesde, blitHasta, blitTam, blitPinta);
        System.out.println("//blit " + sirve + " desde=" + blitDesde.y + " hasta=" + blitHasta.y
                + " tam=" + blitTam.width + "x" + blitTam.height + " pinta=" + blitPinta);
        sirve = vp.blit(9, 4, blitDesde, blitHasta, blitTam, blitPinta);
        System.out.println("//blit diagonal " + sirve);

        System.out.println("//avisos tras maquetar=" + (avisosTrasMaquetar > 0) + " total="
                + (avisos > avisosTrasMaquetar));

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
