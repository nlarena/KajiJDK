import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Pinta {
    static final int W = 40, H = 20;

    public static int run() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();

        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);
        g.setColor(Color.black);
        g.drawRect(1, 1, 20, 10);
        g.drawLine(0, 0, 39, 19);
        g.setColor(Color.gray);
        g.fillOval(24, 4, 12, 12);
        g.setColor(Color.black);
        int[] xs = { 4, 12, 8 };
        int[] ys = { 18, 18, 13 };
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
        System.out.println("checksum=" + suma);
        return (int) suma;
    }
}
