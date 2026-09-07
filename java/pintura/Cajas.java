import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SizeRequirements;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * BoxLayout y Box contra el JDK: primero las cuentas de SizeRequirements a secas, despues una
 * caja vertical y una horizontal con separadores, pegamento y alineaciones distintas.
 *
 * Sin ventana no hay validate que valga —ni aca ni en el JDK—, asi que el maquetado se pide a
 * mano y en el mismo orden en los dos lados con maquetar().
 */
public class Cajas {
    static final int W = 150, H = 120;

    /** Maqueta el subarbol de arriba hacia abajo, que es lo que haria validate con ventana. */
    static void maquetar(Component c) {
        if (c instanceof Container) {
            Container k = (Container) c;
            k.doLayout();
            for (int i = 0; i < k.getComponentCount(); i++) {
                maquetar(k.getComponent(i));
            }
        }
    }

    static String caja(Component c) {
        return c.getX() + "," + c.getY() + " " + c.getWidth() + "x" + c.getHeight();
    }

    static void cuentas() {
        SizeRequirements[] hijos = {
            new SizeRequirements(10, 20, 40, 0.0f),
            new SizeRequirements(5, 30, 30, 0.5f),
            new SizeRequirements(0, 10, Short.MAX_VALUE, 1.0f),
        };
        System.out.println("//tiled  " + SizeRequirements.getTiledSizeRequirements(hijos));
        System.out.println("//align  " + SizeRequirements.getAlignedSizeRequirements(hijos));

        int[] off = new int[3];
        int[] span = new int[3];
        SizeRequirements total = SizeRequirements.getTiledSizeRequirements(hijos);
        for (int alojado : new int[] {15, 60, 100, 300}) {
            SizeRequirements.calculateTiledPositions(alojado, total, hijos, off, span);
            System.out.println("//reparte " + alojado + ": " + off[0] + "+" + span[0] + " "
                    + off[1] + "+" + span[1] + " " + off[2] + "+" + span[2]);
            SizeRequirements.calculateTiledPositions(alojado, total, hijos, off, span, false);
            System.out.println("//alreves " + alojado + ": " + off[0] + "+" + span[0] + " "
                    + off[1] + "+" + span[1] + " " + off[2] + "+" + span[2]);
        }
        SizeRequirements alineado = SizeRequirements.getAlignedSizeRequirements(hijos);
        for (int alojado : new int[] {20, 50}) {
            SizeRequirements.calculateAlignedPositions(alojado, alineado, hijos, off, span);
            System.out.println("//alinea " + alojado + ": " + off[0] + "+" + span[0] + " "
                    + off[1] + "+" + span[1] + " " + off[2] + "+" + span[2]);
            SizeRequirements.calculateAlignedPositions(alojado, alineado, hijos, off, span, false);
            System.out.println("//alinvert " + alojado + ": " + off[0] + "+" + span[0] + " "
                    + off[1] + "+" + span[1] + " " + off[2] + "+" + span[2]);
        }
        System.out.println("//adjust " + SizeRequirements.adjustSizes(10, hijos).length);
    }

    static JLabel etiqueta(String texto, Color fondo, int ancho, int alto, float alineacionX) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        l.setForeground(Color.black);
        l.setOpaque(true);
        l.setBackground(fondo);
        l.setPreferredSize(new Dimension(ancho, alto));
        l.setMinimumSize(new Dimension(10, alto));
        l.setMaximumSize(new Dimension(ancho, alto));
        l.setAlignmentX(alineacionX);
        return l;
    }

    public static int run() {
        cuentas();

        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBounds(0, 0, W, H);
        p.setOpaque(true);
        p.setBackground(Color.white);

        // Vertical: tres anchos y tres alineaciones distintas, con un separador en el medio.
        Box vertical = Box.createVerticalBox();
        vertical.setBorder(new LineBorder(Color.black, 1));
        JLabel a = etiqueta("A", new Color(0xCCCCFF), 40, 14, Component.LEFT_ALIGNMENT);
        JLabel b = etiqueta("B", new Color(0xCCFFCC), 26, 14, Component.CENTER_ALIGNMENT);
        JLabel c = etiqueta("C", new Color(0xFFCCCC), 34, 14, Component.RIGHT_ALIGNMENT);
        vertical.add(a);
        vertical.add(Box.createVerticalStrut(6));
        vertical.add(b);
        vertical.add(Box.createVerticalGlue());
        vertical.add(c);
        vertical.setBounds(2, 2, 60, 70);
        p.add(vertical);

        // Horizontal: dos botones separados por pegamento, que empuja al segundo contra el borde.
        Box horizontal = Box.createHorizontalBox();
        horizontal.setBorder(new LineBorder(Color.black, 1));
        JButton uno = new JButton("Ok");
        uno.setUI(new BasicButtonUI());
        uno.setFont(new Font("Dialog", Font.PLAIN, 12));
        JButton dos = new JButton("No");
        dos.setUI(new BasicButtonUI());
        dos.setFont(new Font("Dialog", Font.PLAIN, 12));
        horizontal.add(uno);
        horizontal.add(Box.createHorizontalGlue());
        horizontal.add(dos);
        horizontal.setBounds(2, 76, 146, 32);
        p.add(horizontal);

        maquetar(p);

        System.out.println("//vert pref=" + vertical.getPreferredSize().width + "x"
                + vertical.getPreferredSize().height + " min=" + vertical.getMinimumSize().width
                + "x" + vertical.getMinimumSize().height + " max=" + vertical.getMaximumSize().width
                + "x" + vertical.getMaximumSize().height + " alignX=" + vertical.getAlignmentX());
        System.out.println("//a " + caja(a) + " | b " + caja(b) + " | c " + caja(c));
        System.out.println("//separador " + caja(vertical.getComponent(1)) + " | pegamento "
                + caja(vertical.getComponent(3)));
        System.out.println("//horiz pref=" + horizontal.getPreferredSize().width + "x"
                + horizontal.getPreferredSize().height + " uno " + caja(uno) + " | dos "
                + caja(dos) + " | pegamento " + caja(horizontal.getComponent(1)));

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(Color.red);
        g.fillRect(0, 0, W, H);
        p.paint(g);

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
