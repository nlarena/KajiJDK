import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

/**
 * Un TitledBorder: el unico borde que lleva texto, y el que necesitaba FontMetrics para existir.
 *
 * Pide un Component de verdad para las metricas y la fuente; un Panel de AWT alcanza y no arrastra
 * Swing. Los insets tambien se imprimen: es el numero que un layout usa, y tiene que coincidir con
 * lo que se pinta.
 */
public class Titulo {
    static final int W = 64, H = 26;

    public static int run() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);

        Panel c = new Panel();
        c.setFont(new Font("Dialog", Font.PLAIN, 12));
        c.setBackground(Color.white);
        c.setForeground(Color.black);

        // Fuente plana explicita: sin ella el JDK usa TitledBorder.font de UIManager, que es
        // negrita, y esta VM tiene una sola cara. Asi la comparacion mide colocacion, no cara.
        TitledBorder tb = new TitledBorder(new LineBorder(Color.black, 1), "Datos",
                TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                new Font("Dialog", Font.PLAIN, 12));
        Insets in = tb.getBorderInsets(c);
        System.out.println("//insets " + in.top + " " + in.left + " " + in.bottom + " " + in.right);
        tb.paintBorder(c, g, 1, 1, 60, 23);

        StringBuilder sb = new StringBuilder();
        long suma = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                suma = suma * 31 + rgb;
                // Tres niveles, no dos: un impresor que solo distingue blanco de "otra cosa" dio por
                // identico un titulo gris (51,51,51) contra uno negro. El gris tiene que verse.
                sb.append(rgb == 0xFFFFFF ? '.' : (rgb == 0 ? '#' : 'o'));
            }
            sb.append('\n');
        }
        System.out.print(sb);
        return (int) suma;
    }
}
