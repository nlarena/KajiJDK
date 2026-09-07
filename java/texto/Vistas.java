import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.TableView;
import javax.swing.text.View;

/**
 * La geometria del arbol de vistas contra el JDK.
 *
 * <p>Los hijos son vistas de tamano fijo, no de texto: asi la comparacion mide el maquetado de
 * {@link BoxView} y {@link TableView} y no la tipografia, que en esta biblioteca es una sola cara
 * de mapa de bits y por definicion no puede dar lo mismo que la del JDK.
 */
public class Vistas {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Una vista que mide lo que le digan y no mira el documento. */
    static class Fija extends View {

        private final float minX;
        private final float prefX;
        private final float maxX;
        private final float minY;
        private final float prefY;
        private final float maxY;
        private final float alinX;
        private final float alinY;

        Fija(Element elem, float minX, float prefX, float maxX,
                float minY, float prefY, float maxY, float alinX, float alinY) {
            super(elem);
            this.minX = minX;
            this.prefX = prefX;
            this.maxX = maxX;
            this.minY = minY;
            this.prefY = prefY;
            this.maxY = maxY;
            this.alinX = alinX;
            this.alinY = alinY;
        }

        public float getMinimumSpan(int axis) {
            return (axis == X_AXIS) ? minX : minY;
        }

        public float getPreferredSpan(int axis) {
            return (axis == X_AXIS) ? prefX : prefY;
        }

        public float getMaximumSpan(int axis) {
            return (axis == X_AXIS) ? maxX : maxY;
        }

        public float getAlignment(int axis) {
            return (axis == X_AXIS) ? alinX : alinY;
        }

        public void paint(java.awt.Graphics g, Shape allocation) {
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            return a;
        }

        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            bias[0] = Position.Bias.Forward;
            return getStartOffset();
        }
    }

    /**
     * Una tabla concreta: la del JDK es abstracta y no dice de que elementos salen las filas.
     *
     * <p>Las filas se piden por {@code createTableRow}, que tiene la misma firma de fuente en las
     * dos bibliotecas. Construirlas a mano no serviria para comparar: en el JDK la fila es una
     * clase interna y se escribe {@code tabla.new TableRow(e)}, y aca es anidada estatica.
     */
    static class Tabla extends TableView {

        Tabla(Element elem) {
            super(elem);
        }

        View fila(Element e) {
            return createTableRow(e);
        }
    }

    static String rect(Shape s) {
        if (s == null) {
            return "nulo";
        }
        Rectangle r = s.getBounds();
        return "(" + r.x + "," + r.y + " " + r.width + "x" + r.height + ")";
    }

    /** Los hijos de una vista, uno por linea, con el lugar que les toco. */
    static void reparto(String titulo, View v, int ancho, int alto) {
        v.setSize(ancho, alto);
        Rectangle alloc = new Rectangle(0, 0, ancho, alto);
        linea(titulo + " pref=" + (int) v.getPreferredSpan(View.X_AXIS)
                + "x" + (int) v.getPreferredSpan(View.Y_AXIS)
                + " min=" + (int) v.getMinimumSpan(View.X_AXIS)
                + "x" + (int) v.getMinimumSpan(View.Y_AXIS));
        for (int i = 0; i < v.getViewCount(); i++) {
            linea("  hijo " + i + " " + rect(v.getChildAllocation(i, alloc)));
        }
    }

    public static int run() {
        try {
            PlainDocument doc = new PlainDocument();
            doc.insertString(0, "uno\ndos\ntres\ncuatro\n", null);
            Element raiz = doc.getDefaultRootElement();
            linea("lineas=" + raiz.getElementCount() + " largo=" + doc.getLength());

            // Una caja vertical con tres hijos de distinto alto preferido.
            BoxView caja = new BoxView(raiz, View.Y_AXIS);
            View[] hijos = new View[3];
            hijos[0] = new Fija(raiz.getElement(0), 10, 40, 100, 5, 20, 60, 0f, 0f);
            hijos[1] = new Fija(raiz.getElement(1), 20, 60, 90, 10, 30, 30, 0.5f, 0.5f);
            hijos[2] = new Fija(raiz.getElement(2), 5, 30, 200, 8, 15, 400, 1f, 1f);
            caja.replace(0, 0, hijos);
            reparto("cajaY 200x100", caja, 200, 100);
            reparto("cajaY 200x40", caja, 200, 40);
            reparto("cajaY 200x400", caja, 200, 400);

            // La misma caja, ahora horizontal: cambia que eje se reparte y cual se impone.
            BoxView cajaX = new BoxView(raiz, View.X_AXIS);
            View[] hijos2 = new View[3];
            hijos2[0] = new Fija(raiz.getElement(0), 10, 40, 100, 5, 20, 60, 0f, 0f);
            hijos2[1] = new Fija(raiz.getElement(1), 20, 60, 90, 10, 30, 30, 0.5f, 0.5f);
            hijos2[2] = new Fija(raiz.getElement(2), 5, 30, 200, 8, 15, 400, 1f, 1f);
            cajaX.replace(0, 0, hijos2);
            reparto("cajaX 200x100", cajaX, 200, 100);
            reparto("cajaX 90x100", cajaX, 90, 100);

            // Una tabla de dos filas por tres columnas: los anchos los decide la tabla.
            Tabla tabla = new Tabla(raiz);
            View f0 = tabla.fila(raiz.getElement(0));
            View f1 = tabla.fila(raiz.getElement(1));
            View[] celdas0 = new View[3];
            celdas0[0] = new Fija(raiz.getElement(0), 10, 30, 500, 5, 20, 500, 0f, 0f);
            celdas0[1] = new Fija(raiz.getElement(0), 10, 70, 500, 5, 20, 500, 0f, 0f);
            celdas0[2] = new Fija(raiz.getElement(0), 10, 20, 500, 5, 40, 500, 0f, 0f);
            f0.replace(0, 0, celdas0);
            View[] celdas1 = new View[3];
            celdas1[0] = new Fija(raiz.getElement(1), 10, 50, 500, 5, 30, 500, 0f, 0f);
            celdas1[1] = new Fija(raiz.getElement(1), 10, 40, 500, 5, 10, 500, 0f, 0f);
            celdas1[2] = new Fija(raiz.getElement(1), 10, 60, 500, 5, 25, 500, 0f, 0f);
            f1.replace(0, 0, celdas1);
            View[] filas = new View[2];
            filas[0] = f0;
            filas[1] = f1;
            tabla.replace(0, 0, filas);

            reparto("tabla 300x200", tabla, 300, 200);
            for (int i = 0; i < tabla.getViewCount(); i++) {
                View fila = tabla.getView(i);
                Rectangle rf = tabla.getChildAllocation(i, new Rectangle(0, 0, 300, 200))
                        .getBounds();
                for (int j = 0; j < fila.getViewCount(); j++) {
                    linea("  fila " + i + " celda " + j + " "
                            + rect(fila.getChildAllocation(j, rf)));
                }
            }

            // La misma tabla mas angosta: se ve como se reparte el faltante.
            reparto("tabla 120x200", tabla, 120, 200);
            for (int i = 0; i < tabla.getViewCount(); i++) {
                View fila = tabla.getView(i);
                Rectangle rf = tabla.getChildAllocation(i, new Rectangle(0, 0, 120, 200))
                        .getBounds();
                for (int j = 0; j < fila.getViewCount(); j++) {
                    linea("  fila " + i + " celda " + j + " "
                            + rect(fila.getChildAllocation(j, rf)));
                }
            }

            // El indice de vista por posicion: la parte que usa el cursor para ubicarse.
            for (int p = 0; p <= doc.getLength(); p = p + 3) {
                linea("indice pos=" + p + " caja=" + caja.getViewIndex(p, Position.Bias.Forward)
                        + " tabla=" + tabla.getViewIndex(p, Position.Bias.Forward));
            }
        } catch (BadLocationException e) {
            linea("EXCEPCION " + e.getClass().getName() + " " + e.getMessage());
        }
        return 0;
    }
}
