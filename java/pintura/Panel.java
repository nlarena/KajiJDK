import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * Un JPanel opaco con TitledBorder y un hijo que pinta su area: la tuberia entera de JComponent.
 *
 * paintComponent (fondo opaco) -> paintBorder (el titulo) -> paintChildren (el hijo), volcada a
 * imagen con paint(g) y comparada contra el JDK. Sin ventana ni EDT: es la forma headless canonica.
 */
public class Panel {
    static final int W = 64, H = 30;

    /** Un hijo con nombre, no anonimo: ver #499. Pinta su area entera de gris. */
    static class Caja extends JComponent {
        protected void paintComponent(Graphics g) {
            g.setColor(Color.gray);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public static int run() {
        Font plana = new Font("Dialog", Font.PLAIN, 12);
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBounds(0, 0, W, H);
        p.setOpaque(true);
        p.setBackground(Color.white);
        p.setForeground(Color.black);
        p.setFont(plana);
        p.setBorder(new TitledBorder(new LineBorder(Color.black, 1), "Datos",
                TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, plana));

        Caja hijo = new Caja();
        hijo.setBounds(8, 18, 20, 6);
        p.add(hijo);

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setColor(Color.red);
        g.fillRect(0, 0, W, H);
        p.paint(g);

        System.out.println("//insets " + p.getInsets().top + " " + p.getInsets().left
                + " opaque=" + p.isOpaque() + " hijos=" + p.getComponentCount());

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
