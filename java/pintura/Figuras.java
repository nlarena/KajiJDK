import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/** Solo figuras de barrido: ovalos, arcos, poligonos. */
public class Figuras {
    static final int W = 44, H = 22;

    public static int run() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);
        g.setColor(Color.black);
        g.fillOval(1, 1, 12, 12);
        g.drawOval(15, 1, 13, 13);
        g.setColor(Color.gray);
        g.fillArc(30, 1, 12, 12, 45, 180);
        g.setColor(Color.black);
        int[] xs = { 4, 18, 10 };
        int[] ys = { 21, 21, 15 };
        g.fillPolygon(xs, ys, 3);

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
