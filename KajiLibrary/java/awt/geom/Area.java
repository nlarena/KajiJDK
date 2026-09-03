package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;

// java.awt.geom.Area de KajiLibrary -- una region del plano cerrada bajo union, interseccion,
// diferencia y diferencia simetrica. Superficie completa.
//
// Lo que distingue a Area de Path2D no son las cuatro operaciones booleanas sino la **forma
// normal**: un Area no guarda el camino que le dieron, guarda el borde de la region que ese camino
// encierra, con la regla no-cero, sin cruces y sin tramos que se pisen. Por eso `new Area(shape)`
// ya hace trabajo aunque no se opere nada, y por eso `isEmpty`, `isPolygonal`, `isRectangular`,
// `isSingular` y `equals` significan algo: preguntan sobre la region, no sobre como venia escrita.
// Un camino en ocho que se anula a si mismo da un Area vacia; dos rectangulos pegados dan un solo
// lazo sin el lado compartido.
//
// La normalizacion y las cuatro operaciones son el mismo calculo con distinta funcion booleana, y
// viven en AreaOp. El encabezado de ese archivo explica por que esta hecho en tres pasadas y no
// como el barrido del JDK.
//
// Dos cosas observables que conviene saber:
//
//   * **La orientacion de los lazos no es la del JDK.** Aca el interior queda a la derecha del
//     trozo recorrido de arriba hacia abajo, o sea vuelta +1; el JDK elige la contraria. Las dos
//     rellenan la misma region con la regla no-cero --que es lo unico que el contrato promete-- y
//     nada de la API expone el signo. Se anota porque un `getPathIterator` comparado coordenada por
//     coordenada contra el JDK va a diferir en el orden de recorrido.
//
//   * **`equals(Area)` no es `equals(Object)`.** No hay override de `equals(Object)`, igual que en
//     el JDK: `unArea.equals((Object) otra)` cae en la identidad de Object. Es una rareza heredada
//     y esta respetada a proposito, porque un `List.contains` o un `HashSet` que empezara a usar
//     igualdad geometrica seria un cambio de comportamiento observable.
//
// Nada quedo afuera de esta clase.
public class Area implements Shape, Cloneable {

    // El borde ya normalizado. Siempre WIND_NON_ZERO y siempre con todos los subcaminos cerrados.
    private Path2D path;

    /** Una region vacia. */
    public Area() {
        this.path = Path2D.newDouble(PathIterator.WIND_NON_ZERO);
    }

    /**
     * La region encerrada por `s`, normalizada.
     *
     * Los subcaminos abiertos se cierran implicitamente: el area encerrada por un camino sin
     * `closePath` es la del camino cerrado, que es lo que dice la spec de Shape.
     */
    public Area(Shape s) {
        if (s == null) {
            throw new NullPointerException("s");
        }
        if (s instanceof Area) {
            // Ya esta normalizada: copiarla es mas barato y da exactamente lo mismo.
            this.path = Path2D.newDouble(((Area) s).path, null);
        } else {
            ArrayList<AreaCurve> curves = new ArrayList<AreaCurve>();
            PathIterator pi = s.getPathIterator(null);
            int rule = pi.getWindingRule();
            AreaCurve.appendPath(curves, pi, AreaCurve.LEFT);
            this.path = AreaOp.compute(curves, rule, PathIterator.WIND_NON_ZERO, AreaOp.ADD);
        }
    }

    // --- operaciones booleanas -------------------------------------------------------------------

    /** Union con `rhs`. */
    public void add(Area rhs) {
        applyOp(rhs, AreaOp.ADD);
    }

    /** Resta de `rhs`. */
    public void subtract(Area rhs) {
        applyOp(rhs, AreaOp.SUB);
    }

    /** Interseccion con `rhs`. */
    public void intersect(Area rhs) {
        applyOp(rhs, AreaOp.INT);
    }

    /** Diferencia simetrica con `rhs`: lo que esta en una y no en la otra. */
    public void exclusiveOr(Area rhs) {
        applyOp(rhs, AreaOp.XOR);
    }

    private void applyOp(Area rhs, int op) {
        if (rhs == null) {
            throw new NullPointerException("rhs");
        }
        this.path = combine(this, rhs, op);
    }

    private static Path2D combine(Area left, Area right, int op) {
        ArrayList<AreaCurve> curves = new ArrayList<AreaCurve>();
        AreaCurve.appendPath(curves, left.path.getPathIterator(null), AreaCurve.LEFT);
        AreaCurve.appendPath(curves, right.path.getPathIterator(null), AreaCurve.RIGHT);
        return AreaOp.compute(curves, PathIterator.WIND_NON_ZERO,
                PathIterator.WIND_NON_ZERO, op);
    }

    /** Deja la region vacia. */
    public void reset() {
        this.path = Path2D.newDouble(PathIterator.WIND_NON_ZERO);
    }

    // --- preguntas sobre la region ---------------------------------------------------------------

    /** Si la region no encierra nada. */
    public boolean isEmpty() {
        return this.path.getPathIterator(null).isDone();
    }

    /** Si el borde es todo de segmentos rectos. */
    public boolean isPolygonal() {
        PathIterator pi = this.path.getPathIterator(null);
        double[] coords = new double[6];
        while (!pi.isDone()) {
            int seg = pi.currentSegment(coords);
            if (seg == PathIterator.SEG_QUADTO || seg == PathIterator.SEG_CUBICTO) {
                return false;
            }
            pi.next();
        }
        return true;
    }

    /**
     * Si la region es un rectangulo con los lados paralelos a los ejes.
     *
     * Se comprueba contra el rectangulo envolvente y no contando vertices: un borde poligonal puede
     * tener vertices de mas en el medio de un lado --los deja la union de dos rectangulos que
     * comparten parte de un lado-- y seguir siendo geometricamente un rectangulo. Comparar las dos
     * regiones responde la pregunta que el nombre del metodo hace.
     */
    public boolean isRectangular() {
        if (isEmpty()) {
            return true;
        }
        if (!isPolygonal() || !isSingular()) {
            return false;
        }
        return new Area(getBounds2D()).equals(this);
    }

    /** Si la region es de una sola pieza sin agujeros, o sea un solo lazo. */
    public boolean isSingular() {
        PathIterator pi = this.path.getPathIterator(null);
        double[] coords = new double[6];
        int loops = 0;
        while (!pi.isDone()) {
            if (pi.currentSegment(coords) == PathIterator.SEG_MOVETO) {
                loops = loops + 1;
                if (loops > 1) {
                    return false;
                }
            }
            pi.next();
        }
        return true;
    }

    /**
     * Si `other` encierra exactamente la misma region.
     *
     * Se calcula la diferencia simetrica y se pregunta si quedo vacia. La comparacion directa de
     * los dos bordes vale como atajo --las dos formas normales de la misma region coinciden cuando
     * se llego a ellas por el mismo camino-- pero no como respuesta: dos regiones iguales pueden
     * tener el borde partido en distintos trozos segun con quien se hayan operado antes.
     */
    public boolean equals(Area other) {
        if (other == this) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (sameOutline(this.path, other.path)) {
            return true;
        }
        return combine(this, other, AreaOp.XOR).getPathIterator(null).isDone();
    }

    private static boolean sameOutline(Path2D a, Path2D b) {
        PathIterator pa = a.getPathIterator(null);
        PathIterator pb = b.getPathIterator(null);
        double[] ca = new double[6];
        double[] cb = new double[6];
        while (!pa.isDone() && !pb.isDone()) {
            int sa = pa.currentSegment(ca);
            int sb = pb.currentSegment(cb);
            if (sa != sb) {
                return false;
            }
            int n = 0;
            if (sa == PathIterator.SEG_MOVETO || sa == PathIterator.SEG_LINETO) {
                n = 2;
            } else if (sa == PathIterator.SEG_QUADTO) {
                n = 4;
            } else if (sa == PathIterator.SEG_CUBICTO) {
                n = 6;
            }
            int i = 0;
            while (i < n) {
                if (ca[i] != cb[i]) {
                    return false;
                }
                i = i + 1;
            }
            pa.next();
            pb.next();
        }
        return pa.isDone() && pb.isDone();
    }

    // --- transformacion --------------------------------------------------------------------------

    /**
     * Aplica la transformacion en el lugar.
     *
     * Se renormaliza despues de transformar y no solo se mueven los puntos: una reflexion da vuelta
     * el sentido de recorrido de todos los lazos, y una matriz singular aplasta la region contra
     * una recta y la deja sin area. Los dos casos los resuelve la normalizacion, que es la misma de
     * `new Area(Shape)`.
     */
    public void transform(AffineTransform t) {
        if (t == null) {
            throw new NullPointerException("transform");
        }
        this.path.transform(t);
        ArrayList<AreaCurve> curves = new ArrayList<AreaCurve>();
        AreaCurve.appendPath(curves, this.path.getPathIterator(null), AreaCurve.LEFT);
        this.path = AreaOp.compute(curves, PathIterator.WIND_NON_ZERO,
                PathIterator.WIND_NON_ZERO, AreaOp.ADD);
    }

    /** Una copia transformada, sin tocar esta. */
    public Area createTransformedArea(AffineTransform t) {
        Area a = new Area(this);
        a.transform(t);
        return a;
    }

    public Object clone() {
        return new Area(this);
    }

    // --- Shape -----------------------------------------------------------------------------------

    public Rectangle getBounds() {
        return this.path.getBounds();
    }

    public Rectangle2D getBounds2D() {
        return this.path.getBounds2D();
    }

    public boolean contains(double x, double y) {
        return this.path.contains(x, y);
    }

    public boolean contains(Point2D p) {
        if (p == null) {
            throw new NullPointerException("p");
        }
        return contains(p.getX(), p.getY());
    }

    public boolean contains(double x, double y, double w, double h) {
        return this.path.contains(x, y, w, h);
    }

    public boolean contains(Rectangle2D r) {
        if (r == null) {
            throw new NullPointerException("r");
        }
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean intersects(double x, double y, double w, double h) {
        return this.path.intersects(x, y, w, h);
    }

    public boolean intersects(Rectangle2D r) {
        if (r == null) {
            throw new NullPointerException("r");
        }
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return this.path.getPathIterator(at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(this.path.getPathIterator(at), flatness);
    }
}
