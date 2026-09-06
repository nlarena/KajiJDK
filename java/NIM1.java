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
 * Comprueba {@code javax.swing.plaf.nimbus} contra el JDK 25.
 *
 * <h2>Que se puede comparar</h2>
 *
 * <p>Lo que es cuenta: la grilla de tres bandas con que {@code AbstractRegionPainter} traduce las
 * coordenadas, los colores derivados, las constantes de escala y los datos del aspecto. Todo eso da
 * lo mismo tenga o no la biblioteca los noventa pintores de Nimbus.
 *
 * <p>Lo que no se compara es dibujar, porque los pintores concretos son clases privadas del paquete
 * y esta biblioteca no los tiene.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1.
 */
public class NIM1 {

    static final String[] ESPERADO = {
        "datos|Nimbus|Nimbus|Nimbus Look and Feel|true",
        "escalas|1.15|0.857|0.714",
        "claves|large|small|mini",
        "estado|MiEstado",
        "derivado-igual|-12814156",
        "derivado-mas-claro|-11691289",
        "derivado-menos-saturado|-1684625996|155",
        "derivado-sin-base|true",
        "medio-0|0,0,0,255",
        "medio-1|200,100,50,255",
        "medio-mitad|100,50,25,255",
        "grilla|0.0;3.5;7.0;50.0;93.0;96.5;100.0;|0.0;2.5;5.0;15.0;25.0;27.5;30.0;|10.0;23.0",
    };

    /** Un pintor de prueba con margenes conocidos, para poder medir la traduccion de coordenadas. */
    static class Pintor extends AbstractRegionPainter {

        private final PaintContext ctx = new PaintContext(
                new Insets(5, 7, 5, 7), new Dimension(100, 30), false);

        private String resultado = "";

        @Override
        protected PaintContext getPaintContext() {
            return ctx;
        }

        /** Sin motor grafico: lo que se esta midiendo son las coordenadas, no el dibujo. */
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
            resultado = b.toString();
        }

        /** Traduce unas cuantas coordenadas con el componente medido asi. */
        String medir(int w, int h) {
            // paint() es final y es quien fija el tamano actual, asi que la medicion tiene que
            // pasar por adentro. El motor grafico no se usa para nada, pero tiene que existir.
            final Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    .createGraphics();
            paint(g, new Comp(w, h), w, h);
            g.dispose();
            return resultado;
        }
    }

    /** Un componente de tamano fijo. */
    static class Comp extends JComponent {
        private static final long serialVersionUID = 1L;

        Comp(int w, int h) {
            setSize(w, h);
        }
    }

    /** Un estado de prueba, para ver que el nombre viaja. */
    static class Estado extends State<JComponent> {
        Estado(String n) {
            super(n);
        }

        @Override
        protected boolean isInState(JComponent c) {
            return c != null;
        }
    }

    /** Lo que hace el paquete, una linea por comprobacion. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();

        final NimbusLookAndFeel laf = new NimbusLookAndFeel();
        a.add("datos|" + laf.getName() + "|" + laf.getID() + "|" + laf.getDescription()
                + "|" + laf.shouldUpdateStyleOnAncestorChanged());

        a.add("escalas|" + NimbusStyle.LARGE_SCALE + "|" + NimbusStyle.SMALL_SCALE
                + "|" + NimbusStyle.MINI_SCALE);
        a.add("claves|" + NimbusStyle.LARGE_KEY + "|" + NimbusStyle.SMALL_KEY
                + "|" + NimbusStyle.MINI_KEY);

        a.add("estado|" + new Estado("MiEstado"));

        // Los colores derivados son aritmetica sobre tono, saturacion y brillo.
        UIManager.put("pruebaBase", new Color(60, 120, 180));
        final Color d1 = laf.getDerivedColor("pruebaBase", 0f, 0f, 0f, 0, false);
        a.add("derivado-igual|" + d1.getRGB());
        final Color d2 = laf.getDerivedColor("pruebaBase", 0f, 0f, 0.2f, 0, false);
        a.add("derivado-mas-claro|" + d2.getRGB());
        final Color d3 = laf.getDerivedColor("pruebaBase", 0f, -0.5f, 0f, -100, false);
        a.add("derivado-menos-saturado|" + d3.getRGB() + "|" + d3.getAlpha());
        final Color d4 = laf.getDerivedColor("noExiste", 0f, 0f, 0f, 0, false);
        a.add("derivado-sin-base|" + (d4 != null));

        // El intermedio entre dos colores.
        final Color m0 = new Color(0, 0, 0, 0);
        final Color m1 = new Color(200, 100, 50, 255);
        a.add("medio-0|" + medio(m0, m1, 0f));
        a.add("medio-1|" + medio(m0, m1, 1f));
        a.add("medio-mitad|" + medio(m0, m1, 0.5f));

        // La grilla: con margenes de 7 a los lados y un componente de 100 de ancho, la banda del
        // medio mide 86. Es lo que hace que una esquina conserve su tamano.
        final Pintor p = new Pintor();
        a.add("grilla|" + p.medir(100, 30));
        return a.toArray(new String[a.size()]);
    }

    /** {@code getDerivedColor(Color, Color, float)} es protegido; se lo alcanza heredando. */
    static String medio(Color c1, Color c2, float p) {
        return new Acceso().mezcla(c1, c2, p);
    }

    /** Solo para poder llamar al metodo protegido desde la prueba. */
    static class Acceso extends NimbusLookAndFeel {
        String mezcla(Color c1, Color c2, float p) {
            final Color c = getDerivedColor(c1, c2, p);
            return c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha();
        }
    }

    /**
     * El indice de la primera respuesta que no coincide con la del JDK, o -1.
     *
     * @return el indice, o -1
     */
    public static int donde() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != ESPERADO.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(ESPERADO[i])) {
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
        final int i = donde();
        System.out.println(i < 0 ? "sin diferencias"
                : i + ":\n  nuestro=" + a[i] + "\n  jdk    =" + ESPERADO[i]);
    }
}
