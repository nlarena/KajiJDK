package java.awt.geom;

// java.awt.geom.Rectangle2D de KajiLibrary -- un rectangulo alineado a los ejes. Superficie completa.
//
// Dos detalles que se suelen implementar mal y que aca estan a proposito:
//
//   * `intersect(a, b, dest)` **no normaliza**: si los rectangulos no se tocan, el destino queda con
//     ancho y/o alto negativos (por ejemplo x=5, w=-4). Eso es lo que hace el JDK y es util --
//     `isEmpty()` lo reconoce como vacio-- pero tienta a "arreglarlo" con setFrameFromDiagonal, que
//     lo normalizaria y daria otro rectangulo.
//
//   * `intersectsLine` recorta el segmento contra el marco con el algoritmo de outcodes de
//     Cohen-Sutherland en vez de probar las cuatro aristas. Es el metodo que mas se usa mal del
//     paquete: probar "¿alguna arista corta al segmento?" da falso cuando el segmento esta
//     enteramente adentro.
//
// Al final del archivo hay fabricas internas `newDouble`/`newFloat`. Existen por el mismo motivo que
// las de Point2D: el javac de esta casa no resuelve bien dos tipos con el mismo nombre simple en una
// unidad de compilacion, y `Arc2D`, `Ellipse2D`, `Line2D`, `Path2D` y `RoundRectangle2D` tienen sus
// **propias** clases anidadas `Double` y `Float`, asi que no pueden nombrar tambien a las de aca.
// Ver la nota larga en el encabezado de Point2D.java.
public abstract class Rectangle2D extends RectangularShape {

    /** El punto esta a la izquierda del rectangulo. */
    public static final int OUT_LEFT = 1;

    /** El punto esta por encima del rectangulo. */
    public static final int OUT_TOP = 2;

    /** El punto esta a la derecha del rectangulo. */
    public static final int OUT_RIGHT = 4;

    /** El punto esta por debajo del rectangulo. */
    public static final int OUT_BOTTOM = 8;

    // Rectangulo con coordenadas float.
    public static class Float extends Rectangle2D implements java.io.Serializable {

        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float w, float h) {
            setRect(x, y, w, h);
        }

        public double getX() {
            return (double) this.x;
        }

        public double getY() {
            return (double) this.y;
        }

        public double getWidth() {
            return (double) this.width;
        }

        public double getHeight() {
            return (double) this.height;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0f) || (this.height <= 0.0f);
        }

        public void setRect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public void setRect(double x, double y, double w, double h) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
        }

        public void setRect(Rectangle2D r) {
            this.x = (float) r.getX();
            this.y = (float) r.getY();
            this.width = (float) r.getWidth();
            this.height = (float) r.getHeight();
        }

        public int outcode(double x, double y) {
            int out = 0;
            if (this.width <= 0.0f) {
                out = out | OUT_LEFT | OUT_RIGHT;
            } else if (x < (double) this.x) {
                out = out | OUT_LEFT;
            } else if (x > (double) this.x + (double) this.width) {
                out = out | OUT_RIGHT;
            }
            if (this.height <= 0.0f) {
                out = out | OUT_TOP | OUT_BOTTOM;
            } else if (y < (double) this.y) {
                out = out | OUT_TOP;
            } else if (y > (double) this.y + (double) this.height) {
                out = out | OUT_BOTTOM;
            }
            return out;
        }

        public Rectangle2D getBounds2D() {
            return new Float(this.x, this.y, this.width, this.height);
        }

        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dest;
            if (r instanceof Float) {
                dest = new Float();
            } else {
                dest = new Double();
            }
            Rectangle2D.intersect(this, r, dest);
            return dest;
        }

        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest;
            if (r instanceof Float) {
                dest = new Float();
            } else {
                dest = new Double();
            }
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        public String toString() {
            return getClass().getName() + "[x=" + this.x + ",y=" + this.y
                    + ",w=" + this.width + ",h=" + this.height + "]";
        }
    }

    // Rectangulo con coordenadas double.
    public static class Double extends Rectangle2D implements java.io.Serializable {

        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            setRect(x, y, w, h);
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public double getWidth() {
            return this.width;
        }

        public double getHeight() {
            return this.height;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0) || (this.height <= 0.0);
        }

        public void setRect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public void setRect(Rectangle2D r) {
            this.x = r.getX();
            this.y = r.getY();
            this.width = r.getWidth();
            this.height = r.getHeight();
        }

        public int outcode(double x, double y) {
            int out = 0;
            if (this.width <= 0.0) {
                out = out | OUT_LEFT | OUT_RIGHT;
            } else if (x < this.x) {
                out = out | OUT_LEFT;
            } else if (x > this.x + this.width) {
                out = out | OUT_RIGHT;
            }
            if (this.height <= 0.0) {
                out = out | OUT_TOP | OUT_BOTTOM;
            } else if (y < this.y) {
                out = out | OUT_TOP;
            } else if (y > this.y + this.height) {
                out = out | OUT_BOTTOM;
            }
            return out;
        }

        public Rectangle2D getBounds2D() {
            return new Double(this.x, this.y, this.width, this.height);
        }

        public Rectangle2D createIntersection(Rectangle2D r) {
            Rectangle2D dest = new Double();
            Rectangle2D.intersect(this, r, dest);
            return dest;
        }

        public Rectangle2D createUnion(Rectangle2D r) {
            Rectangle2D dest = new Double();
            Rectangle2D.union(this, r, dest);
            return dest;
        }

        public String toString() {
            return getClass().getName() + "[x=" + this.x + ",y=" + this.y
                    + ",w=" + this.width + ",h=" + this.height + "]";
        }
    }

    protected Rectangle2D() {
    }

    public abstract void setRect(double x, double y, double w, double h);

    public void setRect(Rectangle2D r) {
        setRect(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public abstract int outcode(double x, double y);

    public int outcode(Point2D p) {
        return outcode(p.getX(), p.getY());
    }

    public abstract Rectangle2D createIntersection(Rectangle2D r);

    public abstract Rectangle2D createUnion(Rectangle2D r);

    public void setFrame(double x, double y, double w, double h) {
        setRect(x, y, w, h);
    }

    public Rectangle2D getBounds2D() {
        return (Rectangle2D) clone();
    }

    // Asimetria deliberada (§ "insideness"): el borde izquierdo y el superior pertenecen al
    // rectangulo, el derecho y el inferior no. Asi dos rectangulos pegados no comparten puntos.
    public boolean contains(double x, double y) {
        double x0 = getX();
        double y0 = getY();
        return (x >= x0 && y >= y0 && x < x0 + getWidth() && y < y0 + getHeight());
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return (x + w > x0 && y + h > y0 && x < x0 + getWidth() && y < y0 + getHeight());
    }

    public boolean contains(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double x0 = getX();
        double y0 = getY();
        return (x >= x0 && y >= y0 && (x + w) <= x0 + getWidth() && (y + h) <= y0 + getHeight());
    }

    // Cohen-Sutherland: se recorta el extremo (x1,y1) contra la arista que su outcode señale hasta
    // que caiga adentro (devolver true) o hasta que los dos extremos queden del mismo lado de una
    // misma arista (devolver false). Recortar --y no intersecar aristas-- es lo que hace que un
    // segmento enteramente contenido devuelva true.
    public boolean intersectsLine(double x1, double y1, double x2, double y2) {
        int out1;
        int out2 = outcode(x2, y2);
        if (out2 == 0) {
            return true;
        }
        out1 = outcode(x1, y1);
        while (out1 != 0) {
            if ((out1 & out2) != 0) {
                return false;
            }
            if ((out1 & (OUT_LEFT | OUT_RIGHT)) != 0) {
                double x = getX();
                if ((out1 & OUT_RIGHT) != 0) {
                    x = x + getWidth();
                }
                y1 = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
                x1 = x;
            } else {
                double y = getY();
                if ((out1 & OUT_BOTTOM) != 0) {
                    y = y + getHeight();
                }
                x1 = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                y1 = y;
            }
            out1 = outcode(x1, y1);
        }
        return true;
    }

    public boolean intersectsLine(Line2D l) {
        return intersectsLine(l.getX1(), l.getY1(), l.getX2(), l.getY2());
    }

    // Ojo: no normaliza. Si no hay interseccion, `dest` queda con dimensiones negativas.
    public static void intersect(Rectangle2D src1, Rectangle2D src2, Rectangle2D dest) {
        double x1 = Math.max(src1.getMinX(), src2.getMinX());
        double y1 = Math.max(src1.getMinY(), src2.getMinY());
        double x2 = Math.min(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.min(src1.getMaxY(), src2.getMaxY());
        dest.setFrame(x1, y1, x2 - x1, y2 - y1);
    }

    // Tampoco filtra los vacios: la union de un rectangulo de area cero con otro incluye al punto
    // degenerado. Es lo que hace el JDK.
    public static void union(Rectangle2D src1, Rectangle2D src2, Rectangle2D dest) {
        double x1 = Math.min(src1.getMinX(), src2.getMinX());
        double y1 = Math.min(src1.getMinY(), src2.getMinY());
        double x2 = Math.max(src1.getMaxX(), src2.getMaxX());
        double y2 = Math.max(src1.getMaxY(), src2.getMaxY());
        dest.setFrameFromDiagonal(x1, y1, x2, y2);
    }

    public void add(double newx, double newy) {
        double x1 = Math.min(getMinX(), newx);
        double x2 = Math.max(getMaxX(), newx);
        double y1 = Math.min(getMinY(), newy);
        double y2 = Math.max(getMaxY(), newy);
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    public void add(Point2D pt) {
        add(pt.getX(), pt.getY());
    }

    public void add(Rectangle2D r) {
        double x1 = Math.min(getMinX(), r.getMinX());
        double x2 = Math.max(getMaxX(), r.getMaxX());
        double y1 = Math.min(getMinY(), r.getMinY());
        double y2 = Math.max(getMaxY(), r.getMaxY());
        setRect(x1, y1, x2 - x1, y2 - y1);
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new RectIterator(this, at);
    }

    // Un rectangulo ya es plano: aplanarlo no cambia nada y no hace falta el FlatteningPathIterator.
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new RectIterator(this, at);
    }

    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits + java.lang.Double.doubleToLongBits(getY()) * 37L;
        bits = bits + java.lang.Double.doubleToLongBits(getWidth()) * 43L;
        bits = bits + java.lang.Double.doubleToLongBits(getHeight()) * 47L;
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Rectangle2D) {
            Rectangle2D r2d = (Rectangle2D) obj;
            return ((getX() == r2d.getX())
                    && (getY() == r2d.getY())
                    && (getWidth() == r2d.getWidth())
                    && (getHeight() == r2d.getHeight()));
        }
        return false;
    }

    // --- fabricas internas (no son API; ver la nota del encabezado) -------------------------------

    static Rectangle2D newDouble(double x, double y, double w, double h) {
        return new Double(x, y, w, h);
    }

    static Rectangle2D newFloat(float x, float y, float w, float h) {
        return new Float(x, y, w, h);
    }
}
