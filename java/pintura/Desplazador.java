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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;
import javax.swing.plaf.basic.BasicViewportUI;

/**
 * JScrollPane contra el JDK: las dos barras apareciendo cuando hacen falta, el borde de Metal, la
 * sincronizacion entre las barras y la ventana, y un contenido Scrollable que decide sus propios
 * escalones y sigue al ancho de la ventana.
 *
 * Los aspectos basicos se instalan a mano en los dos lados: por omision el JDK pondria los de
 * Metal en las barras, que pintan un degradado que esta VM no tiene.
 */
public class Desplazador {
    static final int W = 210, H = 120;

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

    /** Un contenido que opina: sigue al ancho de la ventana y mueve de a diez. */
    static class Opinadora extends JComponent implements Scrollable {
        public void paintComponent(Graphics g) {
            g.setColor(new Color(0xFFEECC));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.black);
            for (int y = 0; y < getHeight(); y = y + 10) {
                g.drawLine(0, y, getWidth() - 1, y);
            }
        }
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(40, 30); }
        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return 10;
        }
        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return 25;
        }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    static String caja(Rectangle r) {
        return r.x + "," + r.y + " " + r.width + "x" + r.height;
    }

    static void basico(JScrollPane sp) {
        sp.setUI(new BasicScrollPaneUI());
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new BasicScrollBarUI());
        sp.getViewport().setUI(new BasicViewportUI());
    }

    public static int run() {
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBounds(0, 0, W, H);
        p.setOpaque(true);
        p.setBackground(Color.white);

        // Contenido mas grande que la ventana: aparecen las dos barras.
        Rayada grande = new Rayada();
        grande.setPreferredSize(new Dimension(90, 80));
        JScrollPane sp = new JScrollPane(grande);
        basico(sp);
        sp.setBounds(2, 2, 62, 52);
        p.add(sp);

        // Contenido chico: no aparece ninguna barra.
        JLabel chica = new JLabel("Hola");
        chica.setFont(new Font("Dialog", Font.PLAIN, 12));
        chica.setForeground(Color.black);
        chica.setOpaque(true);
        chica.setBackground(new Color(0xCCCCFF));
        JScrollPane sp2 = new JScrollPane(chica);
        basico(sp2);
        sp2.setBounds(70, 2, 60, 30);
        p.add(sp2);

        // Contenido que opina: sigue al ancho, asi que solo aparece la vertical.
        Opinadora opina = new Opinadora();
        opina.setPreferredSize(new Dimension(200, 90));
        JScrollPane sp3 = new JScrollPane(opina);
        basico(sp3);
        sp3.setBounds(136, 2, 70, 52);
        p.add(sp3);

        // Politicas fijas: la vertical siempre, la horizontal nunca.
        Rayada otra = new Rayada();
        otra.setPreferredSize(new Dimension(90, 20));
        JScrollPane sp4 = new JScrollPane(otra, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        basico(sp4);
        sp4.setBounds(2, 60, 62, 52);
        p.add(sp4);

        maquetar(p);

        JScrollBar vsb = sp.getVerticalScrollBar();
        JScrollBar hsb = sp.getHorizontalScrollBar();
        System.out.println("//sp pref=" + sp.getPreferredSize().width + "x"
                + sp.getPreferredSize().height + " insets=" + sp.getInsets() + " borde="
                + (sp.getBorder() == null ? "null" : sp.getBorder().getClass().getName())
                + " validateRoot=" + sp.isValidateRoot() + " opaco=" + sp.isOpaque()
                + " piezas=" + sp.getComponentCount());
        System.out.println("//sp vp=" + caja(sp.getViewport().getBounds()) + " vsb="
                + caja(vsb.getBounds()) + " visible=" + vsb.isVisible() + " hsb="
                + caja(hsb.getBounds()) + " visible=" + hsb.isVisible() + " bordeVentana="
                + caja(sp.getViewportBorderBounds()));
        System.out.println("//sp vsb val=" + vsb.getValue() + " ext=" + vsb.getVisibleAmount()
                + " max=" + vsb.getMaximum() + " unidad=" + vsb.getUnitIncrement(1) + " bloque="
                + vsb.getBlockIncrement(1) + " | hsb ext=" + hsb.getVisibleAmount() + " max="
                + hsb.getMaximum() + " bloque=" + hsb.getBlockIncrement(1));

        // Mover la barra tiene que mover la ventana, y viceversa.
        vsb.setValue(25);
        System.out.println("//tras barra: pos=" + sp.getViewport().getViewPosition().y
                + " vista en " + grande.getY());
        sp.getViewport().setViewPosition(new Point(12, 40));
        System.out.println("//tras ventana: vsb=" + vsb.getValue() + " hsb=" + hsb.getValue());
        maquetar(sp);

        System.out.println("//sp2 vsb visible=" + sp2.getVerticalScrollBar().isVisible()
                + " hsb visible=" + sp2.getHorizontalScrollBar().isVisible() + " vp="
                + caja(sp2.getViewport().getBounds()) + " vista="
                + chica.getWidth() + "x" + chica.getHeight());

        JScrollBar vsb3 = sp3.getVerticalScrollBar();
        System.out.println("//sp3 vsb visible=" + vsb3.isVisible() + " hsb visible="
                + sp3.getHorizontalScrollBar().isVisible() + " vista="
                + opina.getWidth() + "x" + opina.getHeight() + " unidad="
                + vsb3.getUnitIncrement(1) + " bloque=" + vsb3.getBlockIncrement(1) + " pref="
                + sp3.getPreferredSize().width + "x" + sp3.getPreferredSize().height);

        System.out.println("//sp4 vsb visible=" + sp4.getVerticalScrollBar().isVisible()
                + " hsb visible=" + sp4.getHorizontalScrollBar().isVisible() + " vp="
                + caja(sp4.getViewport().getBounds()));

        // La linea de base sale de la cabecera de columnas, no del contenido.
        JLabel cab = new JLabel("Cab");
        cab.setFont(new Font("Dialog", Font.PLAIN, 12));
        cab.setForeground(Color.black);
        cab.setOpaque(true);
        cab.setBackground(new Color(0xDDDDDD));
        // Fuente explicita y plana: la de omision es negrita, que este rasterizador no distingue.
        JLabel dentro = new JLabel("x");
        dentro.setFont(new Font("Dialog", Font.PLAIN, 12));
        dentro.setForeground(Color.black);
        JScrollPane sp5 = new JScrollPane(dentro);
        basico(sp5);
        sp5.setColumnHeaderView(cab);
        sp5.setBounds(136, 60, 70, 52);
        p.add(sp5);
        maquetar(sp5);

        System.out.println("//base sp2=" + sp2.getBaseline(60, 30) + " sp="
                + sp.getBaseline(62, 52) + " sp5=" + sp5.getBaseline(70, 52)
                + " comportamiento=" + sp2.getBaselineResizeBehavior()
                + " cabecera=" + caja(sp5.getColumnHeader().getBounds()));

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
