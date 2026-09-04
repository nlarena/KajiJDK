import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

/**
 * Tres JLabel en un JPanel: con icono, alineada a la derecha con borde, y deshabilitada.
 *
 * Es el algoritmo layoutCompoundLabel contra el JDK: alineaciones, separacion icono-texto,
 * insets del borde y el relieve del texto deshabilitado. Se imprimen ademas los tamanos
 * preferidos y la linea de base, que son los numeros que un layout usa.
 */
public class Etiqueta {
    static final int W = 84, H = 58;

    /** Un icono con nombre, no anonimo (#499): una caja gris con un punto negro. */
    static class Caja implements Icon {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.gray);
            g.fillRect(x, y, 12, 10);
            g.setColor(Color.black);
            g.fillRect(x + 2, y + 2, 2, 2);
        }
        public int getIconWidth() { return 12; }
        public int getIconHeight() { return 10; }
    }

    public static int run() {
        Font plana = new Font("Dialog", Font.PLAIN, 12);
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBounds(0, 0, W, H);
        p.setOpaque(true);
        p.setBackground(Color.white);

        JLabel a = new JLabel("Hola", new Caja(), JLabel.LEADING);
        a.setFont(plana);
        a.setForeground(Color.black);
        a.setBounds(2, 2, 60, 16);
        p.add(a);

        JLabel b = new JLabel("Derecha", JLabel.RIGHT);
        b.setFont(plana);
        b.setForeground(Color.black);
        b.setBorder(new LineBorder(Color.black, 1));
        b.setBounds(2, 20, 78, 18);
        p.add(b);

        JLabel c = new JLabel("Gris", JLabel.CENTER);
        c.setFont(plana);
        c.setEnabled(false);
        c.setBounds(2, 40, 78, 16);
        p.add(c);

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setColor(Color.red);
        g.fillRect(0, 0, W, H);
        p.paint(g);

        Dimension da = a.getPreferredSize();
        Dimension db = b.getPreferredSize();
        System.out.println("//pref a=" + da.width + "x" + da.height + " b=" + db.width + "x" + db.height
                + " base a=" + a.getBaseline(60, 16) + " b=" + b.getBaseline(78, 18)
                + " bg c=" + Integer.toHexString(c.getBackground().getRGB() & 0xFFFFFF));

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
        return (int) suma;
    }
}
