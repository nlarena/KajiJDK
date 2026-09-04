import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D$Double;
import java.awt.image.BufferedImage;
import javax.swing.border.Border;
import javax.swing.border.StrokeBorder;

/** Graphics2D: transformaciones, draw/fill de figuras, grosor de trazo. */
public class Dos {
    static final int W = 46, H = 24;

    public static int run() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);

        // fill de una figura, sin transformar
        g.setColor(Color.black);
        g.fill(new Rectangle2D$Double(2, 2, 9, 5));

        // la misma figura, escalada y trasladada
        AffineTransform viejo = g.getTransform();
        g.translate(16, 2);
        g.scale(2.0, 1.0);
        g.setColor(Color.gray);
        g.fill(new Rectangle2D$Double(0, 0, 6, 5));
        g.setTransform(viejo);

        // draw con grosor
        g.setColor(Color.black);
        g.setStroke(new BasicStroke(2.0f));
        g.draw(new Rectangle2D$Double(2, 10, 14, 8));

        // un StrokeBorder, que necesita Graphics2D
        g.setStroke(new BasicStroke(1.0f));
        Border sb = new StrokeBorder(new BasicStroke(1.0f), Color.gray);
        sb.paintBorder(null, g, 24, 10, 18, 10);

        StringBuilder sb2 = new StringBuilder();
        long suma = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                suma = suma * 31 + rgb;
                sb2.append(rgb == 0xFFFFFF ? '.' : (rgb == 0 ? '#' : 'o'));
            }
            sb2.append('\n');
        }
        System.out.print(sb2);
        return (int) suma;
    }
}
