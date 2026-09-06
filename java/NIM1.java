import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.AbstractRegionPainter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.plaf.nimbus.NimbusStyle;
import javax.swing.plaf.nimbus.State;

/**
 * Checks {@code javax.swing.plaf.nimbus} against JDK 25.
 *
 * <h2>What can be compared</h2>
 *
 * <p>Everything that is arithmetic: the three-band grid {@code AbstractRegionPainter} translates
 * coordinates with, the derived colors, the scale constants and the look and feel's own data. All of
 * that answers the same whether or not the library has Nimbus's ninety painters.
 *
 * <p>What is not compared is drawing, because the concrete painters are private classes of the
 * package and this library does not have them.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1.
 */
public class NIM1 {

    static final String[] EXPECTED = {
        "data|Nimbus|Nimbus|Nimbus Look and Feel|true",
        "scales|1.15|0.857|0.714",
        "keys|large|small|mini",
        "state|MyState",
        "derived-same|-12814156",
        "derived-brighter|-11691289",
        "derived-less-saturated|-1684625996|155",
        "derived-no-base|true",
        "mid-0|0,0,0,255",
        "mid-1|200,100,50,255",
        "mid-half|100,50,25,255",
        "grid|0.0;3.5;7.0;50.0;93.0;96.5;100.0;|0.0;2.5;5.0;15.0;25.0;27.5;30.0;|10.0;23.0",
    };

    /** A test painter with known margins, so the coordinate translation can be measured. */
    static class Painter extends AbstractRegionPainter {

        private final PaintContext ctx = new PaintContext(
                new Insets(5, 7, 5, 7), new Dimension(100, 30), false);

        private String result = "";

        @Override
        protected PaintContext getPaintContext() {
            return ctx;
        }

        /** No rendering hints: what is being measured are the coordinates, not the drawing. */
        @Override
        protected void configureGraphics(Graphics2D g) {
        }

        @Override
        protected void doPaint(Graphics2D g, JComponent c, int w, int h, Object[] keys) {
            final float[] v = {0f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f};
            final StringBuilder b = new StringBuilder();
            for (int i = 0; i < v.length; i++) {
                b.append(decodeX(v[i])).append(';');
            }
            b.append('|');
            for (int i = 0; i < v.length; i++) {
                b.append(decodeY(v[i])).append(';');
            }
            b.append('|').append(decodeAnchorX(1f, 3f)).append(';').append(decodeAnchorY(2f, -2f));
            result = b.toString();
        }

        /** Translates a handful of coordinates with the component measuring that. */
        String measure(int w, int h) {
            // paint() is final and is the one that fixes the current size, so the measurement has to
            // happen inside it. The graphics context is not used for anything, but it has to exist.
            final Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    .createGraphics();
            paint(g, new Comp(w, h), w, h);
            g.dispose();
            return result;
        }
    }

    /** A component of a fixed size. */
    static class Comp extends JComponent {
        private static final long serialVersionUID = 1L;

        Comp(int w, int h) {
            setSize(w, h);
        }
    }

    /** A test state, to see that the name travels. */
    static class TestState extends State<JComponent> {
        TestState(String n) {
            super(n);
        }

        @Override
        protected boolean isInState(JComponent c) {
            return c != null;
        }
    }

    /** What the package does, one line per check. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        final NimbusLookAndFeel laf = new NimbusLookAndFeel();
        a.add("data|" + laf.getName() + "|" + laf.getID() + "|" + laf.getDescription()
                + "|" + laf.shouldUpdateStyleOnAncestorChanged());

        a.add("scales|" + NimbusStyle.LARGE_SCALE + "|" + NimbusStyle.SMALL_SCALE
                + "|" + NimbusStyle.MINI_SCALE);
        a.add("keys|" + NimbusStyle.LARGE_KEY + "|" + NimbusStyle.SMALL_KEY
                + "|" + NimbusStyle.MINI_KEY);

        a.add("state|" + new TestState("MyState"));

        // Derived colors are arithmetic over hue, saturation and brightness.
        UIManager.put("testBase", new Color(60, 120, 180));
        final Color d1 = laf.getDerivedColor("testBase", 0f, 0f, 0f, 0, false);
        a.add("derived-same|" + d1.getRGB());
        final Color d2 = laf.getDerivedColor("testBase", 0f, 0f, 0.2f, 0, false);
        a.add("derived-brighter|" + d2.getRGB());
        final Color d3 = laf.getDerivedColor("testBase", 0f, -0.5f, 0f, -100, false);
        a.add("derived-less-saturated|" + d3.getRGB() + "|" + d3.getAlpha());
        final Color d4 = laf.getDerivedColor("doesNotExist", 0f, 0f, 0f, 0, false);
        a.add("derived-no-base|" + (d4 != null));

        // The one halfway between two colors.
        final Color m0 = new Color(0, 0, 0, 0);
        final Color m1 = new Color(200, 100, 50, 255);
        a.add("mid-0|" + mid(m0, m1, 0f));
        a.add("mid-1|" + mid(m0, m1, 1f));
        a.add("mid-half|" + mid(m0, m1, 0.5f));

        // The grid: with margins of 7 on the sides and a component 100 wide, the middle band
        // measures 86. That is what keeps a corner at its own size.
        final Painter p = new Painter();
        a.add("grid|" + p.measure(100, 30));
        return a.toArray(new String[a.size()]);
    }

    /** {@code getDerivedColor(Color, Color, float)} is protected; it is reached by subclassing. */
    static String mid(Color c1, Color c2, float p) {
        return new Access().blend(c1, c2, p);
    }

    /** Only so the protected method can be called from the test. */
    static class Access extends NimbusLookAndFeel {
        String blend(Color c1, Color c2, float p) {
            final Color c = getDerivedColor(c1, c2, p);
            return c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha();
        }
    }

    /**
     * The index of the first answer that differs from the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk =" + EXPECTED[i]);
    }
}
