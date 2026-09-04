import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

/**
 * Casillas, botones de radio y un boton con estado, en sus estados: normal, seleccionado,
 * apretado, deshabilitado y con el cursor encima.
 *
 * Es el modelo con estado, los iconos medidos de Ocean y los UI basicos contra el JDK. En los dos
 * lados se instala el UI basico a mano y una fuente explicita regular, como en Boton.
 */
public class Casillas {
    static final int W = 130, H = 126;

    static void ubicar(JPanel p, AbstractButton b, int x, int y) {
        b.setFont(new Font("Dialog", Font.PLAIN, 12));
        Dimension d = b.getPreferredSize();
        b.setBounds(x, y, d.width, d.height);
        p.add(b);
    }

    public static int run() {
        JPanel p = new JPanel();
        p.setLayout(null);
        p.setBounds(0, 0, W, H);
        p.setOpaque(true);
        p.setBackground(Color.white);

        JCheckBox c1 = new JCheckBox("Uno");
        c1.setUI(new BasicCheckBoxUI());
        ubicar(p, c1, 2, 2);

        JCheckBox c2 = new JCheckBox("Dos", true);
        c2.setUI(new BasicCheckBoxUI());
        ubicar(p, c2, 2, 26);

        JCheckBox c3 = new JCheckBox("Tres", true);
        c3.setUI(new BasicCheckBoxUI());
        c3.setEnabled(false);
        ubicar(p, c3, 2, 50);

        JCheckBox c4 = new JCheckBox("Ap");
        c4.setUI(new BasicCheckBoxUI());
        c4.getModel().setArmed(true);
        c4.getModel().setPressed(true);
        ubicar(p, c4, 2, 74);

        JCheckBox c5 = new JCheckBox("Ro", true);
        c5.setUI(new BasicCheckBoxUI());
        c5.getModel().setRollover(true);
        ubicar(p, c5, 2, 98);

        JRadioButton r1 = new JRadioButton("A");
        JRadioButton r2 = new JRadioButton("B");
        r1.setUI(new BasicRadioButtonUI());
        r2.setUI(new BasicRadioButtonUI());
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(r1);
        grupo.add(r2);
        r2.setSelected(true);
        ubicar(p, r1, 64, 2);
        ubicar(p, r2, 64, 26);

        JRadioButton r3 = new JRadioButton("C", true);
        r3.setUI(new BasicRadioButtonUI());
        r3.setEnabled(false);
        ubicar(p, r3, 64, 50);

        JRadioButton r4 = new JRadioButton("D");
        r4.setUI(new BasicRadioButtonUI());
        r4.getModel().setArmed(true);
        r4.getModel().setPressed(true);
        ubicar(p, r4, 64, 74);

        JToggleButton t = new JToggleButton("T", true);
        t.setUI(new BasicToggleButtonUI());
        ubicar(p, t, 64, 98);

        JToggleButton t2 = new JToggleButton("U");
        t2.setUI(new BasicToggleButtonUI());
        t2.getModel().setArmed(true);
        t2.getModel().setPressed(true);
        ubicar(p, t2, 96, 98);

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(Color.red);
        g.fillRect(0, 0, W, H);
        p.paint(g);

        System.out.println("//pref c1=" + c1.getWidth() + "x" + c1.getHeight() + " r1="
                + r1.getWidth() + "x" + r1.getHeight() + " t=" + t.getWidth() + "x" + t.getHeight()
                + " insets c1=" + c1.getInsets() + " t=" + t.getInsets()
                + " base c1=" + c1.getBaseline(c1.getWidth(), c1.getHeight())
                + " borde=" + c1.isBorderPainted() + " rollover c=" + c1.isRolloverEnabled()
                + " t=" + t.isRolloverEnabled() + " sel r1=" + r1.isSelected() + " r2="
                + r2.isSelected() + " hAlign=" + c1.getHorizontalAlignment());

        r1.doClick(0);
        System.out.println("//click r1: sel r1=" + r1.isSelected() + " r2=" + r2.isSelected()
                + " grupo=" + (grupo.getSelection() == r1.getModel()));
        c1.doClick(0);
        c1.doClick(0);
        c2.doClick(0);
        System.out.println("//click c: c1=" + c1.isSelected() + " c2=" + c2.isSelected());

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
