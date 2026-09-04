import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Cuatro JButton en un JPanel: uno con el borde por omision, uno con icono y borde del usuario,
 * uno apretado y uno deshabilitado.
 *
 * Es BasicButtonUI contra el JDK: el margen a traves del MarginBorder, el borde de Ocean en sus
 * tres estados, la eleccion de icono y el texto deshabilitado en relieve. En los dos lados se
 * instala BasicButtonUI a mano, para comparar el aspecto basico y no Metal, que ademas pinta un
 * degradado. La fuente es explicita y regular porque la negrita por omision no esta en el
 * rasterizador de esta VM.
 */
public class Boton {
    static final int W = 100, H = 84;

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

    static JButton boton(String texto, Icon icono, Font f) {
        JButton b = new JButton(texto, icono);
        b.setUI(new BasicButtonUI());
        b.setFont(f);
        return b;
    }

    public static int run() {
        Font plana = new Font("Dialog", Font.PLAIN, 12);
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBounds(0, 0, W, H);
        p.setOpaque(true);
        p.setBackground(Color.white);

        JButton a = boton("Ok", null, plana);
        Dimension da = a.getPreferredSize();
        a.setBounds(2, 2, da.width, da.height);
        p.add(a);

        JButton b = boton("Hola", new Caja(), plana);
        b.setBorder(new LineBorder(Color.black, 1));
        b.setBounds(2, 32, 60, 22);
        p.add(b);

        JButton c = boton("P", null, plana);
        c.getModel().setArmed(true);
        c.getModel().setPressed(true);
        c.setBounds(64, 32, 34, 22);
        p.add(c);

        JButton d = boton("Gris", null, plana);
        d.setEnabled(false);
        d.setBounds(2, 58, 60, 24);
        p.add(d);

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setColor(Color.red);
        g.fillRect(0, 0, W, H);
        p.paint(g);

        Dimension db = b.getPreferredSize();
        System.out.println("//pref a=" + da.width + "x" + da.height + " b=" + db.width + "x" + db.height
                + " base a=" + a.getBaseline(da.width, da.height)
                + " insets a=" + a.getInsets() + " margin a=" + a.getMargin()
                + " opaque=" + a.isOpaque() + " rollover=" + a.isRolloverEnabled()
                + " cmd=" + a.getActionCommand());

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
