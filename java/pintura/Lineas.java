import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/** Solo primitivas deterministas: lineas, rectangulos, recorte, traslacion, copia. */
public class Lineas {
    static final int W = 44, H = 22;

    public static int run() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, W, H);

        g.setColor(Color.black);
        g.drawLine(0, 0, 43, 21);
        g.drawLine(43, 0, 0, 21);
        g.drawLine(0, 10, 43, 10);
        g.drawLine(22, 0, 22, 21);
        g.drawRect(2, 2, 10, 6);
        g.fillRect(30, 3, 8, 5);

        g.setColor(Color.gray);
        g.translate(5, 12);
        g.clipRect(0, 0, 12, 6);
        g.fillRect(-3, -3, 30, 30);
        g.setClip(null);
        g.translate(-5, -12);

        g.setColor(Color.black);
        g.copyArea(30, 3, 8, 5, -6, 14);

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
