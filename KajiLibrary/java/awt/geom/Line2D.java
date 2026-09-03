package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

// java.awt.geom.Line2D de KajiLibrary -- un segmento de recta. Superficie completa.
//
// `linesIntersect` es de los metodos que mas se usan mal del paquete, asi que vale decir que hace
// exactamente: devuelve true si los dos **segmentos** (no las rectas infinitas) tienen algun punto
// en comun, tocarse en un extremo incluido, y tambien cuando son colineales y se superponen. La
// prueba es la de los cuatro `relativeCCW` cruzados; el caso colineal sale gratis porque
// `relativeCCW` no devuelve 0 para un punto colineal que cae **fuera** del segmento, sino ±1.
//
// Una Line2D no encierra area, asi que sus cuatro `contains` devuelven siempre false. No es un
// atajo: un segmento no contiene ningun punto en el sentido de "insideness" de Shape.
public abstract class Line2D implements Shape, Cloneable {

    // Segmento con coordenadas float.
    public static class Float extends Line2D implements java.io.Serializable {

        public float x1;
        public float y1;
        public float x2;
        public float y2;

        public Float() {
        }

        public Float(float x1, float y1, float x2, float y2) {
            setLine(x1, y1, x2, y2);
        }

        public Float(Point2D p1, Point2D p2) {
            setLine(p1, p2);
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

        public double getX2() {
            return (double) this.x2;
        }

        public double getY2() {
            return (double) this.y2;
        }

        public Point2D getP2() {
            return Point2D.newFloat(this.x2, this.y2);
        }

        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        public void setLine(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Rectangle2D getBounds2D() {
            float x;
            float y;
            float w;
            float h;
            if (this.x1 < this.x2) {
                x = this.x1;
                w = this.x2 - this.x1;
            } else {
                x = this.x2;
                w = this.x1 - this.x2;
            }
            if (this.y1 < this.y2) {
                y = this.y1;
                h = this.y2 - this.y1;
            } else {
                y = this.y2;
                h = this.y1 - this.y2;
            }
            return Rectangle2D.newFloat(x, y, w, h);
        }
    }

    // Segmento con coordenadas double.
    public static class Double extends Line2D implements java.io.Serializable {

        public double x1;
        public double y1;
        public double x2;
        public double y2;

        public Double() {
        }

        public Double(double x1, double y1, double x2, double y2) {
            setLine(x1, y1, x2, y2);
        }

        public Double(Point2D p1, Point2D p2) {
            setLine(p1, p2);
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

        public double getX2() {
            return this.x2;
        }

        public double getY2() {
            return this.y2;
        }

        public Point2D getP2() {
            return Point2D.newDouble(this.x2, this.y2);
        }

        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public Rectangle2D getBounds2D() {
            double x;
            double y;
            double w;
            double h;
            if (this.x1 < this.x2) {
                x = this.x1;
                w = this.x2 - this.x1;
            } else {
                x = this.x2;
                w = this.x1 - this.x2;
            }
            if (this.y1 < this.y2) {
                y = this.y1;
                h = this.y2 - this.y1;
            } else {
                y = this.y2;
                h = this.y1 - this.y2;
            }
            return Rectangle2D.newDouble(x, y, w, h);
        }
    }

    protected Line2D() {
    }

    public abstract double getX1();

    public abstract double getY1();

    public abstract Point2D getP1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract Point2D getP2();

    public abstract void setLine(double x1, double y1, double x2, double y2);

    public void setLine(Point2D p1, Point2D p2) {
        setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void setLine(Line2D l) {
        setLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    // Devuelve -1, 0 o 1 segun de que lado de la recta dirigida (x1,y1)->(x2,y2) cae el punto.
    // El 0 se reserva para los puntos colineales que caen **dentro** del segmento: uno colineal pero
    // pasado el extremo devuelve ±1 segun por que punta se paso. Esa distincion es la que hace que
    // `linesIntersect` acierte en los casos colineales sin tratarlos aparte.
    public static int relativeCCW(double x1, double y1, double x2, double y2,
                                  double px, double py) {
        x2 = x2 - x1;
        y2 = y2 - y1;
        px = px - x1;
        py = py - y1;
        double ccw = px * y2 - py * x2;
        if (ccw == 0.0) {
            // Colineal: se clasifica por la proyeccion sobre el segmento.
            ccw = px * x2 + py * y2;
            if (ccw > 0.0) {
                // Se repite la cuenta relativa al otro extremo. x2,y2 ya estan negados respecto de
                // ese origen, asi que alcanza con correr px,py.
                px = px - x2;
                py = py - y2;
                ccw = px * x2 + py * y2;
                if (ccw < 0.0) {
                    ccw = 0.0;
                }
            }
        }
        if (ccw < 0.0) {
            return -1;
        }
        if (ccw > 0.0) {
            return 1;
        }
        return 0;
    }

    public int relativeCCW(double px, double py) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public int relativeCCW(Point2D p) {
        return relativeCCW(getX1(), getY1(), getX2(), getY2(), p.getX(), p.getY());
    }

    public static boolean linesIntersect(double x1, double y1, double x2, double y2,
                                         double x3, double y3, double x4, double y4) {
        return ((relativeCCW(x1, y1, x2, y2, x3, y3) * relativeCCW(x1, y1, x2, y2, x4, y4) <= 0)
                && (relativeCCW(x3, y3, x4, y4, x1, y1)
                    * relativeCCW(x3, y3, x4, y4, x2, y2) <= 0));
    }

    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        return linesIntersect(x1, y1, x2, y2, getX1(), getY1(), getX2(), getY2());
    }

    public boolean intersectsLine(Line2D l) {
        return linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(),
                              getX1(), getY1(), getX2(), getY2());
    }

    // Distancia al **segmento**: si la proyeccion cae fuera, la distancia es a la punta mas cercana.
    // Con un segmento degenerado (los dos extremos iguales) da la distancia al punto, que es lo
    // correcto; la version de recta infinita, en cambio, devuelve NaN ahi -- una recta que no existe
    // no tiene distancia definida, y el JDK tampoco la inventa.
    public static double ptSegDistSq(double x1, double y1, double x2, double y2,
                                     double px, double py) {
        x2 = x2 - x1;
        y2 = y2 - y1;
        px = px - x1;
        py = py - y1;
        double dotprod = px * x2 + py * y2;
        double projlenSq;
        if (dotprod <= 0.0) {
            // El punto cae del lado de (x1,y1): la proyeccion recortada mide 0.
            projlenSq = 0.0;
        } else {
            // Se pasa a vectores medidos desde (x2,y2).
            px = x2 - px;
            py = y2 - py;
            dotprod = px * x2 + py * y2;
            if (dotprod <= 0.0) {
                projlenSq = 0.0;
            } else {
                projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
            }
        }
        double lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0) {
            lenSq = 0;
        }
        return lenSq;
    }

    public static double ptSegDist(double x1, double y1, double x2, double y2,
                                   double px, double py) {
        return Math.sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptSegDistSq(double px, double py) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDistSq(Point2D pt) {
        return ptSegDistSq(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    public double ptSegDist(double px, double py) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptSegDist(Point2D pt) {
        return ptSegDist(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    public static double ptLineDistSq(double x1, double y1, double x2, double y2,
                                      double px, double py) {
        x2 = x2 - x1;
        y2 = y2 - y1;
        px = px - x1;
        py = py - y1;
        double dotprod = px * x2 + py * y2;
        double projlenSq = dotprod * dotprod / (x2 * x2 + y2 * y2);
        double lenSq = px * px + py * py - projlenSq;
        if (lenSq < 0) {
            lenSq = 0;
        }
        return lenSq;
    }

    public static double ptLineDist(double x1, double y1, double x2, double y2,
                                    double px, double py) {
        return Math.sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
    }

    public double ptLineDistSq(double px, double py) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDistSq(Point2D pt) {
        return ptLineDistSq(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    public double ptLineDist(double px, double py) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), px, py);
    }

    public double ptLineDist(Point2D pt) {
        return ptLineDist(getX1(), getY1(), getX2(), getY2(), pt.getX(), pt.getY());
    }

    // Un segmento no encierra area: nunca contiene nada.
    public boolean contains(double x, double y) {
        return false;
    }

    public boolean contains(Point2D p) {
        return false;
    }

    public boolean contains(double x, double y, double w, double h) {
        return false;
    }

    public boolean contains(Rectangle2D r) {
        return false;
    }

    public boolean intersects(double x, double y, double w, double h) {
        return intersects(Rectangle2D.newDouble(x, y, w, h));
    }

    public boolean intersects(Rectangle2D r) {
        return r.intersectsLine(getX1(), getY1(), getX2(), getY2());
    }

    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new LineIterator(this, at);
    }

    // Un segmento ya es plano: el parametro de aplanado no cambia nada.
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new LineIterator(this, at);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
}
