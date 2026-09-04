import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/** Un borde de javax.swing.border dibujado de verdad, sin componente. */
public class Borde {
    static final int W = 30, H = 12;

    public static int run() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);

        Border linea = new LineBorder(Color.black, 2);
        linea.paintBorder(null, g, 1, 1, 12, 9);

        Border bisel = new BevelBorder(BevelBorder.RAISED,
                Color.gray, Color.gray, Color.black, Color.black);
        bisel.paintBorder(null, g, 16, 1, 12, 9);

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
