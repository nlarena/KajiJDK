import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Texto: la misma cadena en las dos VMs.
 *
 * En el JDK real se apagan el antialias y las metricas fraccionarias, que es exactamente la
 * configuracion de la que se extrajeron los glifos; nuestra VM guarda esas sugerencias sin
 * aplicarlas porque ya dibuja asi.
 */
public class Texto {
    static final int W = 70, H = 20;

    public static int run() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setFont(new Font("Dialog", Font.PLAIN, 12));
        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);
        g.setColor(Color.black);
        g.drawString("Hola AWT: g?", 2, 14);

        FontMetrics fm = g.getFontMetrics();
        System.out.println("//ancho=" + fm.stringWidth("Hola AWT: g?")
                + " asc=" + fm.getAscent() + " desc=" + fm.getDescent()
                + " alto=" + fm.getHeight());

        StringBuilder sb = new StringBuilder();
        long suma = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                suma = suma * 31 + rgb;
                sb.append(rgb == 0xFFFFFF ? '.' : '#');
            }
            sb.append('\n');
        }
        System.out.print(sb);
        return (int) suma;
    }
}
