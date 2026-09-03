package java.awt.geom;

import java.util.ArrayList;

// Trozo de borde monotono en Y (no es API). Es la pieza sobre la que trabaja AreaOp.
//
// Toda la maquinaria de Area se apoya en una sola invariante: **cada trozo es monotono en Y y se
// guarda de arriba hacia abajo**, con `dir` acordandose de si el trazo original iba hacia abajo
// (+1) o hacia arriba (-1). Esa invariante compra tres cosas que de otro modo serian casos
// especiales sueltos:
//
//   * `xForY(y)` esta bien definida --hay un solo x por cada y-- y se resuelve por biseccion en el
//     parametro. Sobre una funcion monotona la biseccion converge al ulp en 60 pasos y no necesita
//     resolver la cubica: no hay formula cerrada que sea mas exacta que eso, solo mas rapida.
//
//   * El conteo de cruces para el numero de vuelta es el de siempre --intervalo semiabierto
//     [ytop, ybot) y suma de `dir`-- y da el resultado exacto incluso cuando el rayo pasa justo por
//     un vertice: en un maximo o minimo local nacen dos trozos con el mismo `ytop` y `dir` opuesto,
//     que se cancelan; en un vertice de paso uno termina (no cuenta) y otro empieza (cuenta), o sea
//     una vez.
//
//   * Los tramos horizontales se descartan al construir. No aportan nada al numero de vuelta y son
//     la fuente clasica de divisiones por cero. AreaOp los vuelve a fabricar al final, ya
//     clasificados, para cerrar los lazos.
//
// El precio de la invariante es partir cada cuadratica y cada cubica en sus extremos de Y antes de
// guardarlas. Es una cuadratica que resolver (dy/dt = 0) y como mucho dos cortes.
final class AreaCurve {

    /** Trozo del operando izquierdo. */
    static final int LEFT = 0;

    /** Trozo del operando derecho. */
    static final int RIGHT = 1;

    /** Orden: 1 recta, 2 cuadratica, 3 cubica. */
    final int order;

    /** 2*(order+1) coordenadas, del extremo superior al inferior. */
    final double[] c;

    /** +1 si el trazo original bajaba, -1 si subia. */
    final int dir;

    /** LEFT o RIGHT. */
    final int tag;

    AreaCurve(int order, double[] c, int dir, int tag) {
        this.order = order;
        this.c = c;
        this.dir = dir;
        this.tag = tag;
    }

    double xtop() {
        return this.c[0];
    }

    double ytop() {
        return this.c[1];
    }

    double xbot() {
        return this.c[2 * this.order];
    }

    double ybot() {
        return this.c[2 * this.order + 1];
    }

    // --- evaluacion ------------------------------------------------------------------------------

    /** El punto de la curva en el parametro t, por de Casteljau. */
    double[] point(double t) {
        double[] p = copyOf(this.c);
        int level = this.order;
        while (level > 0) {
            int i = 0;
            while (i < level) {
                p[2 * i] = p[2 * i] + (p[2 * i + 2] - p[2 * i]) * t;
                p[2 * i + 1] = p[2 * i + 1] + (p[2 * i + 3] - p[2 * i + 1]) * t;
                i = i + 1;
            }
            level = level - 1;
        }
        double[] r = new double[2];
        r[0] = p[0];
        r[1] = p[1];
        return r;
    }

    // La X que le corresponde a esa Y. Fuera del rango devuelve el extremo, que es lo que quieren
    // los llamadores: la curva "vale" su punta cuando el rayo pasa justo por ella.
    double xForY(double y) {
        if (y <= ytop()) {
            return xtop();
        }
        if (y >= ybot()) {
            return xbot();
        }
        if (this.order == 1) {
            double dy = this.c[3] - this.c[1];
            double t = (y - this.c[1]) / dy;
            return this.c[0] + (this.c[2] - this.c[0]) * t;
        }
        // Biseccion: y(t) es monotona creciente por construccion, asi que el invariante
        // y(lo) <= y <= y(hi) se mantiene sin mirar derivadas.
        double lo = 0.0;
        double hi = 1.0;
        int i = 0;
        while (i < 60) {
            double m = (lo + hi) * 0.5;
            double[] p = point(m);
            if (p[1] < y) {
                lo = m;
            } else {
                hi = m;
            }
            i = i + 1;
        }
        double[] p = point((lo + hi) * 0.5);
        return p[0];
    }

    // --- subdivision -----------------------------------------------------------------------------

    static double[] copyOf(double[] a) {
        double[] r = new double[a.length];
        int i = 0;
        while (i < a.length) {
            r[i] = a[i];
            i = i + 1;
        }
        return r;
    }

    /** Puntos de control del trozo [0, t]. */
    static double[] leftPart(double[] c, int n, double t) {
        double[] p = copyOf(c);
        double[] out = new double[2 * (n + 1)];
        out[0] = p[0];
        out[1] = p[1];
        int level = n;
        int k = 1;
        while (level > 0) {
            int i = 0;
            while (i < level) {
                p[2 * i] = p[2 * i] + (p[2 * i + 2] - p[2 * i]) * t;
                p[2 * i + 1] = p[2 * i + 1] + (p[2 * i + 3] - p[2 * i + 1]) * t;
                i = i + 1;
            }
            out[2 * k] = p[0];
            out[2 * k + 1] = p[1];
            k = k + 1;
            level = level - 1;
        }
        return out;
    }

    /** Puntos de control del trozo [t, 1]. */
    static double[] rightPart(double[] c, int n, double t) {
        double[] p = copyOf(c);
        double[] out = new double[2 * (n + 1)];
        out[2 * n] = p[2 * n];
        out[2 * n + 1] = p[2 * n + 1];
        int level = n;
        int k = n - 1;
        while (level > 0) {
            int i = 0;
            while (i < level) {
                p[2 * i] = p[2 * i] + (p[2 * i + 2] - p[2 * i]) * t;
                p[2 * i + 1] = p[2 * i + 1] + (p[2 * i + 3] - p[2 * i + 1]) * t;
                i = i + 1;
            }
            out[2 * k] = p[2 * (level - 1)];
            out[2 * k + 1] = p[2 * (level - 1) + 1];
            k = k - 1;
            level = level - 1;
        }
        return out;
    }

    /** Puntos de control del trozo [t0, t1]. */
    static double[] subCurve(double[] c, int n, double t0, double t1) {
        double[] r = c;
        if (t1 < 1.0) {
            r = leftPart(r, n, t1);
        }
        if (t0 > 0.0) {
            double s = 0.0;
            if (t1 > 0.0) {
                s = t0 / t1;
            }
            if (s > 1.0) {
                s = 1.0;
            }
            r = rightPart(r, n, s);
        } else {
            r = copyOf(r);
        }
        return r;
    }

    // --- construccion desde un camino ------------------------------------------------------------

    static void appendPath(ArrayList<AreaCurve> out, PathIterator pi, int tag) {
        double[] coords = new double[6];
        double movx = 0.0;
        double movy = 0.0;
        double curx = 0.0;
        double cury = 0.0;
        boolean open = false;
        while (!pi.isDone()) {
            int seg = pi.currentSegment(coords);
            if (seg == PathIterator.SEG_MOVETO) {
                if (open) {
                    appendLine(out, curx, cury, movx, movy, tag);
                }
                movx = coords[0];
                movy = coords[1];
                curx = movx;
                cury = movy;
                open = true;
            } else if (seg == PathIterator.SEG_LINETO) {
                appendLine(out, curx, cury, coords[0], coords[1], tag);
                curx = coords[0];
                cury = coords[1];
            } else if (seg == PathIterator.SEG_QUADTO) {
                appendQuad(out, curx, cury, coords[0], coords[1], coords[2], coords[3], tag);
                curx = coords[2];
                cury = coords[3];
            } else if (seg == PathIterator.SEG_CUBICTO) {
                appendCubic(out, curx, cury, coords[0], coords[1], coords[2], coords[3],
                        coords[4], coords[5], tag);
                curx = coords[4];
                cury = coords[5];
            } else {
                appendLine(out, curx, cury, movx, movy, tag);
                curx = movx;
                cury = movy;
                open = false;
            }
            pi.next();
        }
        if (open) {
            // Un subcamino sin closePath se cierra igual: el area encerrada es la del camino
            // cerrado. Es lo que dice la spec de Shape y lo que hace el JDK.
            appendLine(out, curx, cury, movx, movy, tag);
        }
    }

    static void appendLine(ArrayList<AreaCurve> out, double x0, double y0,
                           double x1, double y1, int tag) {
        if (y0 == y1) {
            return;
        }
        double[] c = new double[4];
        c[0] = x0;
        c[1] = y0;
        c[2] = x1;
        c[3] = y1;
        appendMonotone(out, c, 1, tag);
    }

    static void appendQuad(ArrayList<AreaCurve> out, double x0, double y0,
                           double cx, double cy, double x1, double y1, int tag) {
        double[] c = new double[6];
        c[0] = x0;
        c[1] = y0;
        c[2] = cx;
        c[3] = cy;
        c[4] = x1;
        c[5] = y1;
        // dy/dt = 2*[(cy-y0) + t*(y0 - 2cy + y1)]
        double a = y0 - 2.0 * cy + y1;
        double b = cy - y0;
        double[] roots = new double[2];
        int n = 0;
        if (a != 0.0) {
            double t = -b / a;
            if (t > 0.0 && t < 1.0) {
                roots[n] = t;
                n = n + 1;
            }
        }
        splitAndAppend(out, c, 2, roots, n, tag);
    }

    static void appendCubic(ArrayList<AreaCurve> out, double x0, double y0,
                            double cx1, double cy1, double cx2, double cy2,
                            double x1, double y1, int tag) {
        double[] c = new double[8];
        c[0] = x0;
        c[1] = y0;
        c[2] = cx1;
        c[3] = cy1;
        c[4] = cx2;
        c[5] = cy2;
        c[6] = x1;
        c[7] = y1;
        // dy/dt / 3 = p*t^2 + q*t + r con las diferencias hacia adelante de la Y.
        double d0 = cy1 - y0;
        double d1 = cy2 - cy1;
        double d2 = y1 - cy2;
        double p = d0 - 2.0 * d1 + d2;
        double q = 2.0 * (d1 - d0);
        double r = d0;
        double[] roots = new double[2];
        int n = 0;
        if (p == 0.0) {
            if (q != 0.0) {
                double t = -r / q;
                if (t > 0.0 && t < 1.0) {
                    roots[n] = t;
                    n = n + 1;
                }
            }
        } else {
            double disc = q * q - 4.0 * p * r;
            if (disc >= 0.0) {
                double sq = Math.sqrt(disc);
                double ta = (-q - sq) / (2.0 * p);
                double tb = (-q + sq) / (2.0 * p);
                if (ta > tb) {
                    double tmp = ta;
                    ta = tb;
                    tb = tmp;
                }
                if (ta > 0.0 && ta < 1.0) {
                    roots[n] = ta;
                    n = n + 1;
                }
                if (tb > 0.0 && tb < 1.0 && (n == 0 || tb > roots[0])) {
                    roots[n] = tb;
                    n = n + 1;
                }
            }
        }
        splitAndAppend(out, c, 3, roots, n, tag);
    }

    private static void splitAndAppend(ArrayList<AreaCurve> out, double[] c, int n,
                                       double[] roots, int count, int tag) {
        double t0 = 0.0;
        int i = 0;
        while (i <= count) {
            double t1 = 1.0;
            if (i < count) {
                t1 = roots[i];
            }
            if (t1 > t0) {
                appendMonotone(out, subCurve(c, n, t0, t1), n, tag);
            }
            t0 = t1;
            i = i + 1;
        }
    }

    // Guarda el trozo ya monotono, girado de arriba hacia abajo. Los puntos de control interiores se
    // recortan al rango de Y: matematicamente ya estan adentro despues de cortar en los extremos, y
    // el recorte solo saca el ruido de coma flotante que le arruinaria la biseccion a `xForY`.
    static void appendMonotone(ArrayList<AreaCurve> out, double[] c, int n, int tag) {
        double y0 = c[1];
        double y1 = c[2 * n + 1];
        if (y0 == y1) {
            return;
        }
        int dir = 1;
        if (y0 > y1) {
            c = reverse(c, n);
            dir = -1;
        }
        double top = c[1];
        double bot = c[2 * n + 1];
        int i = 1;
        while (i < n) {
            if (c[2 * i + 1] < top) {
                c[2 * i + 1] = top;
            }
            if (c[2 * i + 1] > bot) {
                c[2 * i + 1] = bot;
            }
            i = i + 1;
        }
        out.add(new AreaCurve(n, c, dir, tag));
    }

    static double[] reverse(double[] c, int n) {
        double[] r = new double[2 * (n + 1)];
        int i = 0;
        while (i <= n) {
            r[2 * i] = c[2 * (n - i)];
            r[2 * i + 1] = c[2 * (n - i) + 1];
            i = i + 1;
        }
        return r;
    }

    // --- intersecciones --------------------------------------------------------------------------

    /**
     * Los pares (ta, tb) donde las dos curvas se tocan, agregados a `out`.
     *
     * Recta contra recta se resuelve en forma cerrada, incluido el caso colineal --dos bordes
     * pegados es lo normal, no lo raro: pasa en cuanto se unen dos rectangulos que comparten un
     * lado, y el metodo general de subdivision se le va al infinito ahi--. Los demas pares se
     * resuelven subdividiendo el par y descartando por cajas envolventes, que es exacto hasta donde
     * llega el double y no depende de resolver polinomios de grado 6.
     */
    static void intersections(AreaCurve a, AreaCurve b, ArrayList<double[]> out) {
        if (a.ytop() > b.ybot() || b.ytop() > a.ybot()) {
            return;
        }
        if (sameGeometry(a, b)) {
            // Coincidentes de punta a punta: no hay nada que cortar, AreaOp las agrupa.
            return;
        }
        if (a.order == 1 && b.order == 1) {
            lineLine(a, b, out);
            return;
        }
        subdivide(a.c, a.order, 0.0, 1.0, b.c, b.order, 0.0, 1.0, 0, out, scale(a, b));
    }

    static boolean sameGeometry(AreaCurve a, AreaCurve b) {
        if (a.order != b.order) {
            return false;
        }
        int i = 0;
        while (i < a.c.length) {
            if (a.c[i] != b.c[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static double scale(AreaCurve a, AreaCurve b) {
        double m = 1.0;
        int i = 0;
        while (i < a.c.length) {
            double v = Math.abs(a.c[i]);
            if (v > m) {
                m = v;
            }
            i = i + 1;
        }
        i = 0;
        while (i < b.c.length) {
            double v = Math.abs(b.c[i]);
            if (v > m) {
                m = v;
            }
            i = i + 1;
        }
        return m;
    }

    private static void lineLine(AreaCurve a, AreaCurve b, ArrayList<double[]> out) {
        double sc = scale(a, b);
        double tol = 1.0e-11 * sc;
        double ax = a.c[0];
        double ay = a.c[1];
        double adx = a.c[2] - ax;
        double ady = a.c[3] - ay;
        double bx = b.c[0];
        double by = b.c[1];
        double bdx = b.c[2] - bx;
        double bdy = b.c[3] - by;
        double den = adx * bdy - ady * bdx;
        double rx = bx - ax;
        double ry = by - ay;
        double len = Math.sqrt(adx * adx + ady * ady);
        if (Math.abs(den) <= tol * len) {
            // Paralelas. Solo importan si ademas son colineales y se pisan.
            double perp = Math.abs(rx * ady - ry * adx);
            if (len == 0.0 || perp > tol * len) {
                return;
            }
            double y0 = Math.max(a.ytop(), b.ytop());
            double y1 = Math.min(a.ybot(), b.ybot());
            if (y1 <= y0) {
                return;
            }
            addPair(out, (y0 - a.ytop()) / (a.ybot() - a.ytop()),
                    (y0 - b.ytop()) / (b.ybot() - b.ytop()));
            addPair(out, (y1 - a.ytop()) / (a.ybot() - a.ytop()),
                    (y1 - b.ytop()) / (b.ybot() - b.ytop()));
            return;
        }
        double ta = (rx * bdy - ry * bdx) / den;
        double tb = (rx * ady - ry * adx) / den;
        if (ta < 0.0 || ta > 1.0 || tb < 0.0 || tb > 1.0) {
            return;
        }
        addPair(out, ta, tb);
    }

    private static void addPair(ArrayList<double[]> out, double ta, double tb) {
        if (ta < 0.0) {
            ta = 0.0;
        }
        if (ta > 1.0) {
            ta = 1.0;
        }
        if (tb < 0.0) {
            tb = 0.0;
        }
        if (tb > 1.0) {
            tb = 1.0;
        }
        double[] p = new double[2];
        p[0] = ta;
        p[1] = tb;
        out.add(p);
    }

    private static void subdivide(double[] ca, int na, double ta0, double ta1,
                                  double[] cb, int nb, double tb0, double tb1,
                                  int depth, ArrayList<double[]> out, double sc) {
        if (out.size() > 64) {
            return;
        }
        double[] ba = bbox(ca, na);
        double[] bb = bbox(cb, nb);
        double tol = 1.0e-12 * sc;
        if (ba[2] < bb[0] - tol || bb[2] < ba[0] - tol
                || ba[3] < bb[1] - tol || bb[3] < ba[1] - tol) {
            return;
        }
        double da = Math.max(ba[2] - ba[0], ba[3] - ba[1]);
        double db = Math.max(bb[2] - bb[0], bb[3] - bb[1]);
        if (depth >= 60 || (da <= tol && db <= tol)) {
            addPair(out, (ta0 + ta1) * 0.5, (tb0 + tb1) * 0.5);
            return;
        }
        if (da >= db) {
            double tm = (ta0 + ta1) * 0.5;
            subdivide(leftPart(ca, na, 0.5), na, ta0, tm, cb, nb, tb0, tb1, depth + 1, out, sc);
            subdivide(rightPart(ca, na, 0.5), na, tm, ta1, cb, nb, tb0, tb1, depth + 1, out, sc);
        } else {
            double tm = (tb0 + tb1) * 0.5;
            subdivide(ca, na, ta0, ta1, leftPart(cb, nb, 0.5), nb, tb0, tm, depth + 1, out, sc);
            subdivide(ca, na, ta0, ta1, rightPart(cb, nb, 0.5), nb, tm, tb1, depth + 1, out, sc);
        }
    }

    /** {minx, miny, maxx, maxy} de la envolvente de los puntos de control. */
    static double[] bbox(double[] c, int n) {
        double x0 = c[0];
        double y0 = c[1];
        double x1 = c[0];
        double y1 = c[1];
        int i = 1;
        while (i <= n) {
            double x = c[2 * i];
            double y = c[2 * i + 1];
            if (x < x0) {
                x0 = x;
            }
            if (x > x1) {
                x1 = x;
            }
            if (y < y0) {
                y0 = y;
            }
            if (y > y1) {
                y1 = y;
            }
            i = i + 1;
        }
        double[] r = new double[4];
        r[0] = x0;
        r[1] = y0;
        r[2] = x1;
        r[3] = y1;
        return r;
    }
}
