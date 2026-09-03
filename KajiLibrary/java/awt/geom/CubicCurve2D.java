package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

// java.awt.geom.CubicCurve2D de KajiLibrary -- un segmento de curva de Bezier cubica. Superficie
// completa.
//
// La curva va de (x1,y1) a (x2,y2) con dos puntos de control que **no se tocan**: la curva pasa por
// los extremos y no por los controles. De ahi salen dos cosas que sorprenden si uno espera que el
// cuadrilatero de control sea la figura:
//
//   * `getBounds2D` devuelve la caja **ajustada** de la curva, no la del poligono de control. Las
//     dos son cotas validas segun `Shape.getBounds2D`, asi que esto no lo decide el contrato sino
//     el JDK, que devuelve la ajustada -- comprobado con `java` de verdad. Aca hubo la del poligono
//     de control hasta que la prueba de comportamiento no coincidio. Ver `CurveBounds`.
//
//   * `contains` mide la region encerrada por la curva **mas la cuerda** que une sus extremos, con
//     la regla par/impar. Una curva abierta no encierra nada por si sola; cerrarla con la cuerda es
//     la unica lectura que le da sentido a "adentro", y es la del JDK.
//
// Sobre `solveCubic`: se resuelve por el metodo trigonometrico/de Cardano de Numerical Recipes (5.6)
// --el mismo que usa el JDK-- y despues se pulen las raices con dos pasos de Newton. El pulido no es
// decorativo: la formula cerrada pierde precision cuando dos raices estan cerca, y sin el las
// raices dobles salen con un error de 1e-8 en vez de 1e-15. El **orden** en que quedan las raices no
// esta especificado por el contrato ni aca ni en el JDK, asi que compararlas contra el JDK exige
// ordenarlas primero; la prueba CgeomCubicTest lo hace.
public abstract class CubicCurve2D implements Shape, Cloneable {

    // Curva con coordenadas float.
    public static class Float extends CubicCurve2D implements java.io.Serializable {

        public float x1;
        public float y1;
        public float ctrlx1;
        public float ctrly1;
        public float ctrlx2;
        public float ctrly2;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1,
                     float ctrlx1, float ctrly1,
                     float ctrlx2, float ctrly2,
                     float x2, float y2) {
            setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
        }

        public double getX1() {
            return (double) this.x1;
        }

        public double getY1() {
            return (double) this.y1;
        }

        public Point2D getP1() {
            return Point2D.newFloat(this.x1, this.y1);
        }

        public double getCtrlX1() {
            return (double) this.ctrlx1;
        }

        public double getCtrlY1() {
            return (double) this.ctrly1;
        }

        public Point2D getCtrlP1() {
            return Point2D.newFloat(this.ctrlx1, this.ctrly1);
        }

        public double getCtrlX2() {
            return (double) this.ctrlx2;
        }

        public double getCtrlY2() {
            return (double) this.ctrly2;
        }

        public Point2D getCtrlP2() {
            return Point2D.newFloat(this.ctrlx2, this.ctrly2);
        }

        public double getX2() {
            return (double) this.x2;
        }

        public double getY2() {
            return (double) this.y2;
        }

        public Point2D getP2() {
            return Point2D.newFloat(this.x2, this.y2);
        }

        public void setCurve(double x1, double y1,
                             double ctrlx1, double ctrly1,
                             double ctrlx2, double ctrly2,
                             double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.ctrlx1 = (float) ctrlx1;
            this.ctrly1 = (float) ctrly1;
            this.ctrlx2 = (float) ctrlx2;
            this.ctrly2 = (float) ctrly2;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setCurve(float x1, float y1,
                             float ctrlx1, float ctrly1,
                             float ctrlx2, float ctrly2,
                             float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    // Curva con coordenadas double.
    public static class Double extends CubicCurve2D implements java.io.Serializable {

        public double x1;
        public double y1;
        public double ctrlx1;
        public double ctrly1;
        public double ctrlx2;
        public double ctrly2;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1,
                      double ctrlx1, double ctrly1,
                      double ctrlx2, double ctrly2,
                      double x2, double y2) {
            setCurve(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
        }

        public double getX1() {
            return this.x1;
        }

        public double getY1() {
            return this.y1;
        }

        public Point2D getP1() {
            return Point2D.newDouble(this.x1, this.y1);
        }

        public double getCtrlX1() {
            return this.ctrlx1;
        }

        public double getCtrlY1() {
            return this.ctrly1;
        }

        public Point2D getCtrlP1() {
            return Point2D.newDouble(this.ctrlx1, this.ctrly1);
        }

        public double getCtrlX2() {
            return this.ctrlx2;
        }

        public double getCtrlY2() {
            return this.ctrly2;
        }

        public Point2D getCtrlP2() {
            return Point2D.newDouble(this.ctrlx2, this.ctrly2);
        }

        public double getX2() {
            return this.x2;
        }

        public double getY2() {
            return this.y2;
        }

        public Point2D getP2() {
            return Point2D.newDouble(this.x2, this.y2);
        }

        public void setCurve(double x1, double y1,
                             double ctrlx1, double ctrly1,
                             double ctrlx2, double ctrly2,
                             double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.ctrlx1 = ctrlx1;
            this.ctrly1 = ctrly1;
            this.ctrlx2 = ctrlx2;
            this.ctrly2 = ctrly2;
            this.x2 = x2;
            this.y2 = y2;
        }
    }

    protected CubicCurve2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract Point2D getP1();

    public abstract double getCtrlX1();

    public abstract double getCtrlY1();

    public abstract Point2D getCtrlP1();

    public abstract double getCtrlX2();

    public abstract double getCtrlY2();

    public abstract Point2D getCtrlP2();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP2();

    public abstract void setCurve(double x1, double y1,
                                  double ctrlx1, double ctrly1,
                                  double ctrlx2, double ctrly2,
                                  double x2, double y2);

    public void setCurve(double[] coords, int offset) {
        setCurve(coords[offset + 0], coords[offset + 1],
                 coords[offset + 2], coords[offset + 3],
                 coords[offset + 4], coords[offset + 5],
                 coords[offset + 6], coords[offset + 7]);
    }

    public void setCurve(Point2D p1, Point2D cp1, Point2D cp2, Point2D p2) {
        setCurve(p1.getX(), p1.getY(), cp1.getX(), cp1.getY(),
                 cp2.getX(), cp2.getY(), p2.getX(), p2.getY());
    }

    public void setCurve(Point2D[] pts, int offset) {
        setCurve(pts[offset + 0].getX(), pts[offset + 0].getY(),
                 pts[offset + 1].getX(), pts[offset + 1].getY(),
                 pts[offset + 2].getX(), pts[offset + 2].getY(),
                 pts[offset + 3].getX(), pts[offset + 3].getY());
    }

    public void setCurve(CubicCurve2D c) {
        setCurve(c.getX1(), c.getY1(), c.getCtrlX1(), c.getCtrlY1(),
                 c.getCtrlX2(), c.getCtrlY2(), c.getX2(), c.getY2());
    }

    // "Planitud": la mayor de las dos distancias de los puntos de control a la cuerda, al cuadrado.
    // Es la medida con la que el aplanador decide si un trozo ya se puede dibujar como un segmento.
    public static double getFlatnessSq(double x1, double y1,
                                       double ctrlx1, double ctrly1,
                                       double ctrlx2, double ctrly2,
                                       double x2, double y2) {
        return Math.max(Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx1, ctrly1),
                        Line2D.ptSegDistSq(x1, y1, x2, y2, ctrlx2, ctrly2));
    }

    public static double getFlatness(double x1, double y1,
                                     double ctrlx1, double ctrly1,
                                     double ctrlx2, double ctrly2,
                                     double x2, double y2) {
        return Math.sqrt(getFlatnessSq(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2));
    }

    public static double getFlatnessSq(double[] coords, int offset) {
        return getFlatnessSq(coords[offset + 0], coords[offset + 1],
                             coords[offset + 2], coords[offset + 3],
                             coords[offset + 4], coords[offset + 5],
                             coords[offset + 6], coords[offset + 7]);
    }

    public static double getFlatness(double[] coords, int offset) {
        return Math.sqrt(getFlatnessSq(coords, offset));
    }

    public double getFlatnessSq() {
        return getFlatnessSq(getX1(), getY1(), getCtrlX1(), getCtrlY1(),
                             getCtrlX2(), getCtrlY2(), getX2(), getY2());
    }

    public double getFlatness() {
        return Math.sqrt(getFlatnessSq());
    }

    public void subdivide(CubicCurve2D left, CubicCurve2D right) {
        subdivide(this, left, right);
    }

    // Corte de De Casteljau en t=0.5. Se hace solo con sumas y una division por 2, que es exacta en
    // binario: subdividir no introduce error de redondeo propio.
    public static void subdivide(CubicCurve2D src, CubicCurve2D left, CubicCurve2D right) {
        double x1 = src.getX1();
        double y1 = src.getY1();
        double ctrlx1 = src.getCtrlX1();
        double ctrly1 = src.getCtrlY1();
        double ctrlx2 = src.getCtrlX2();
        double ctrly2 = src.getCtrlY2();
        double x2 = src.getX2();
        double y2 = src.getY2();
        double centerx = (ctrlx1 + ctrlx2) / 2.0;
        double centery = (ctrly1 + ctrly2) / 2.0;
        ctrlx1 = (x1 + ctrlx1) / 2.0;
        ctrly1 = (y1 + ctrly1) / 2.0;
        ctrlx2 = (x2 + ctrlx2) / 2.0;
        ctrly2 = (y2 + ctrly2) / 2.0;
        double ctrlx12 = (ctrlx1 + centerx) / 2.0;
        double ctrly12 = (ctrly1 + centery) / 2.0;
        double ctrlx21 = (ctrlx2 + centerx) / 2.0;
        double ctrly21 = (ctrly2 + centery) / 2.0;
        centerx = (ctrlx12 + ctrlx21) / 2.0;
        centery = (ctrly12 + ctrly21) / 2.0;
        if (left != null) {
            left.setCurve(x1, y1, ctrlx1, ctrly1, ctrlx12, ctrly12, centerx, centery);
        }
        if (right != null) {
            right.setCurve(centerx, centery, ctrlx21, ctrly21, ctrlx2, ctrly2, x2, y2);
        }
    }

    public static void subdivide(double[] src, int srcoff,
                                 double[] left, int leftoff,
                                 double[] right, int rightoff) {
        double x1 = src[srcoff + 0];
        double y1 = src[srcoff + 1];
        double ctrlx1 = src[srcoff + 2];
        double ctrly1 = src[srcoff + 3];
        double ctrlx2 = src[srcoff + 4];
        double ctrly2 = src[srcoff + 5];
        double x2 = src[srcoff + 6];
        double y2 = src[srcoff + 7];
        if (left != null) {
            left[leftoff + 0] = x1;
            left[leftoff + 1] = y1;
        }
        if (right != null) {
            right[rightoff + 6] = x2;
            right[rightoff + 7] = y2;
        }
        x1 = (x1 + ctrlx1) / 2.0;
        y1 = (y1 + ctrly1) / 2.0;
        x2 = (x2 + ctrlx2) / 2.0;
        y2 = (y2 + ctrly2) / 2.0;
        double centerx = (ctrlx1 + ctrlx2) / 2.0;
        double centery = (ctrly1 + ctrly2) / 2.0;
        ctrlx1 = (x1 + centerx) / 2.0;
        ctrly1 = (y1 + centery) / 2.0;
        ctrlx2 = (x2 + centerx) / 2.0;
        ctrly2 = (y2 + centery) / 2.0;
        centerx = (ctrlx1 + ctrlx2) / 2.0;
        centery = (ctrly1 + ctrly2) / 2.0;
        if (left != null) {
            left[leftoff + 2] = x1;
            left[leftoff + 3] = y1;
            left[leftoff + 4] = ctrlx1;
            left[leftoff + 5] = ctrly1;
            left[leftoff + 6] = centerx;
            left[leftoff + 7] = centery;
        }
        if (right != null) {
            right[rightoff + 0] = centerx;
            right[rightoff + 1] = centery;
            right[rightoff + 2] = ctrlx2;
            right[rightoff + 3] = ctrly2;
            right[rightoff + 4] = x2;
            right[rightoff + 5] = y2;
        }
    }

    public static int solveCubic(double[] eqn) {
        return solveCubic(eqn, eqn);
    }

    // eqn = {c, b, a, d} con d*t^3 + a*t^2 + b*t + c = 0. Devuelve cuantas raices reales hay y las
    // deja en `res`. Devolver -1 significa "infinitas" (la ecuacion es 0 = 0).
    public static int solveCubic(double[] eqn, double[] res) {
        double d = eqn[3];
        if (d == 0.0) {
            return QuadCurve2D.solveQuadratic(eqn, res);
        }
        double a = eqn[2] / d;
        double b = eqn[1] / d;
        double c = eqn[0] / d;
        // Se copian los coeficientes originales antes de escribir en `res`, que puede ser el mismo
        // arreglo: el pulido de Newton los necesita intactos.
        double[] orig = new double[4];
        orig[0] = eqn[0];
        orig[1] = eqn[1];
        orig[2] = eqn[2];
        orig[3] = eqn[3];

        int roots = 0;
        double q = (a * a - 3.0 * b) / 9.0;
        double r = (2.0 * a * a * a - 9.0 * a * b + 27.0 * c) / 54.0;
        double r2 = r * r;
        double q3 = q * q * q;
        double a3 = a / 3.0;
        if (r2 < q3) {
            // Tres raices reales distintas: forma trigonometrica.
            double theta = Math.acos(r / Math.sqrt(q3));
            double m = -2.0 * Math.sqrt(q);
            res[0] = m * Math.cos(theta / 3.0) - a3;
            res[1] = m * Math.cos((theta + Math.PI * 2.0) / 3.0) - a3;
            res[2] = m * Math.cos((theta - Math.PI * 2.0) / 3.0) - a3;
            roots = 3;
        } else {
            // Una raiz real: forma de Cardano.
            boolean neg = (r < 0.0);
            double s = Math.sqrt(r2 - q3);
            double rr = r;
            if (neg) {
                rr = -rr;
            }
            double aa = Math.pow(rr + s, 1.0 / 3.0);
            if (!neg) {
                aa = -aa;
            }
            double bb;
            if (aa == 0.0) {
                bb = 0.0;
            } else {
                bb = q / aa;
            }
            res[0] = (aa + bb) - a3;
            roots = 1;
        }
        for (int i = 0; i < roots; i = i + 1) {
            res[i] = refine(orig, res[i]);
        }
        return roots;
    }

    // Dos pasos de Newton sobre el polinomio original. Se aborta si la derivada es cero o si el paso
    // no mejora: pulir de mas puede alejar la raiz cuando ya se llego al limite del double.
    private static double refine(double[] eqn, double t) {
        for (int k = 0; k < 2; k = k + 1) {
            double f = ((eqn[3] * t + eqn[2]) * t + eqn[1]) * t + eqn[0];
            double df = (3.0 * eqn[3] * t + 2.0 * eqn[2]) * t + eqn[1];
            if (df == 0.0) {
                return t;
            }
            double next = t - f / df;
            if (next == t) {
                return t;
            }
            double fnext = ((eqn[3] * next + eqn[2]) * next + eqn[1]) * next + eqn[0];
            if (Math.abs(fnext) > Math.abs(f)) {
                return t;
            }
            t = next;
        }
        return t;
    }

    // "Adentro" es la region que encierran la curva y la cuerda entre sus extremos, con la regla
    // par/impar. Un x o y infinito o NaN da false: no hay punto que examinar.
    public boolean contains(double x, double y) {
        if (!(x * 0.0 + y * 0.0 == 0.0)) {
            return false;
        }
        double x1 = getX1();
        double y1 = getY1();
        double x2 = getX2();
        double y2 = getY2();
        int crossings = Curve.pointCrossingsForLine(x, y, x1, y1, x2, y2)
                + Curve.pointCrossingsForCubic(x, y, x1, y1,
                                               getCtrlX1(), getCtrlY1(),
                                               getCtrlX2(), getCtrlY2(),
                                               x2, y2, 0);
        return ((crossings & 1) == 1);
    }

    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        int numCrossings = rectCrossings(x, y, w, h);
        // Basta con que la cuenta no sea cero: RECT_INTERSECTS tampoco lo es, y las dos cosas
        // --borde tocado o interior no vacio-- significan que hay interseccion.
        return numCrossings != 0;
    }

    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean contains(double x, double y, double w, double h) {
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        int numCrossings = rectCrossings(x, y, w, h);
        // Aca si hace falta separar: RECT_INTERSECTS quiere decir que el borde de la curva entra al
        // rectangulo, y entonces el rectangulo no esta contenido aunque los cruces no sean cero.
        return !(numCrossings == 0 || numCrossings == Curve.RECT_INTERSECTS);
    }

    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    private int rectCrossings(double x, double y, double w, double h) {
        int crossings = 0;
        if (!(getX1() == getX2() && getY1() == getY2())) {
            crossings = Curve.rectCrossingsForLine(crossings, x, y, x + w, y + h,
                                                   getX1(), getY1(), getX2(), getY2());
            if (crossings == Curve.RECT_INTERSECTS) {
                return crossings;
            }
        }
        // La curva se recorre al reves para que su sentido cierre con el de la cuerda, que ya se
        // conto en la direccion contraria. Con los dos en el mismo sentido los cruces se cancelan y
        // `contains` daria false para todo.
        return Curve.rectCrossingsForCubic(crossings, x, y, x + w, y + h,
                                           getX2(), getY2(),
                                           getCtrlX2(), getCtrlY2(),
                                           getCtrlX1(), getCtrlY1(),
                                           getX1(), getY1(), 0);
    }

    /**
     * La caja **ajustada** de la curva: la mas chica que la contiene.
     *
     * <p>No es la del poligono de control, que seria una cota valida y mas facil de calcular. Es lo
     * que devuelve el JDK, comprobado corriendo el mismo caso con `java` de verdad. Ver
     * {@link CurveBounds}, que resuelve las derivadas.
     *
     * <p>El JDK declara este metodo concreto aca y no en las subclases anidadas, asi que `Float`
     * tambien devuelve un `Rectangle2D.Double`; se respeta.
     */
    public Rectangle2D getBounds2D() {
        double[] xs = CurveBounds.cubic(getX1(), getCtrlX1(), getCtrlX2(), getX2());
        double[] ys = CurveBounds.cubic(getY1(), getCtrlY1(), getCtrlY2(), getY2());
        return Rectangle2D.newDouble(xs[0], ys[0], xs[1] - xs[0], ys[1] - ys[0]);
    }

    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new CubicIterator(this, at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
}
