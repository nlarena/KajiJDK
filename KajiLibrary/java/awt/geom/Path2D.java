package java.awt.geom;

import java.awt.Rectangle;
import java.awt.Shape;

// java.awt.geom.Path2D de KajiLibrary -- un camino arbitrario hecho de moveTo/lineTo/quadTo/
// curveTo/closePath. Superficie completa.
//
// El camino se guarda en dos arreglos paralelos: `pointTypes` (un byte por segmento) y las
// coordenadas (float o double segun la subclase), aplanadas. `numTypes` y `numCoords` son los
// largos utiles; la capacidad crece de a saltos.
//
// Tres cosas que valen la pena saber antes de tocar esto:
//
//   * **`getBounds2D` usa los puntos de control, no la curva.** Una cubica queda dentro de la
//     envolvente convexa de sus cuatro puntos, asi que el rectangulo devuelto contiene al camino
//     pero puede ser mas grande que el ajustado. Es lo que especifica el JDK y lo que esperan los
//     llamadores; calcular el ajustado seria mas "lindo" y **distinto**, o sea observable.
//
//   * **`contains`/`intersects` no son lo mismo dado vuelta.** Se cuentan cruces del borde contra
//     el rectangulo con un centinela (Curve.RECT_INTERSECTS) que dice "el borde entra". `intersects`
//     acepta ese caso, `contains` lo rechaza. Ver el encabezado de Curve.
//
//   * **`moveTo` es obligatorio antes de todo lo demas.** Un `lineTo` sin `moveTo` previo tira
//     IllegalPathStateException; no se inventa un origen en (0,0).
public abstract class Path2D implements Shape, Cloneable {

    /** Regla par/impar para decidir el interior. */
    public static final int WIND_EVEN_ODD = PathIterator.WIND_EVEN_ODD;

    /** Regla no-cero para decidir el interior. */
    public static final int WIND_NON_ZERO = PathIterator.WIND_NON_ZERO;

    // --- estado interno (libre por la regla del contrato) -----------------------------------------

    static final byte SEG_MOVETO = (byte) PathIterator.SEG_MOVETO;
    static final byte SEG_LINETO = (byte) PathIterator.SEG_LINETO;
    static final byte SEG_QUADTO = (byte) PathIterator.SEG_QUADTO;
    static final byte SEG_CUBICTO = (byte) PathIterator.SEG_CUBICTO;
    static final byte SEG_CLOSE = (byte) PathIterator.SEG_CLOSE;

    static final int INIT_SIZE = 20;
    static final int EXPAND_MAX = 500;
    static final int EXPAND_MAX_COORDS = EXPAND_MAX * 2;

    transient byte[] pointTypes;
    transient int numTypes;
    transient int numCoords;
    transient int windingRule;

    Path2D() {
    }

    Path2D(int rule, int initialTypes) {
        setWindingRule(rule);
        this.pointTypes = new byte[initialTypes];
    }

    abstract float[] cloneCoordsFloat(AffineTransform at);

    abstract double[] cloneCoordsDouble(AffineTransform at);

    abstract void append(float x, float y);

    abstract void append(double x, double y);

    abstract Point2D getPoint(int coordindex);

    abstract void needRoom(boolean needMove, int newCoords);

    abstract int pointCrossings(double px, double py);

    abstract int rectCrossings(double rxmin, double rymin, double rxmax, double rymax);

    // Acceso al arreglo de coordenadas para los iteradores internos. Devuelve el arreglo **vivo**,
    // no una copia: los iteradores solo leen, y copiar el camino entero para recorrerlo seria
    // exactamente lo que getPathIterator promete no hacer. Uno de los dos siempre devuelve null,
    // segun la precision de la subclase.
    abstract float[] floatCoordsRef();

    abstract double[] doubleCoordsRef();

    // Crece al doble hasta EXPAND_MAX y de ahi en adelante de a saltos fijos: duplicar un arreglo
    // de millones de puntos para agregar dos coordenadas no tiene sentido.
    static byte[] expandPointTypes(byte[] oldPointTypes, int needed) {
        int oldSize = oldPointTypes.length;
        int newSizeMin = oldSize + needed;
        if (newSizeMin < oldSize) {
            throw new ArrayIndexOutOfBoundsException("pointTypes exceeds maximum capacity !");
        }
        int grow = oldSize;
        if (grow > EXPAND_MAX) {
            grow = EXPAND_MAX;
        } else if (grow == 0) {
            grow = INIT_SIZE;
        }
        int newSize = oldSize + grow;
        if (newSize < newSizeMin) {
            newSize = java.lang.Integer.MAX_VALUE;
        }
        byte[] out = new byte[newSize];
        System.arraycopy(oldPointTypes, 0, out, 0, oldSize);
        return out;
    }

    static double[] expandCoordsDouble(double[] oldCoords, int needed) {
        int oldSize = oldCoords.length;
        int newSizeMin = oldSize + needed;
        if (newSizeMin < oldSize) {
            throw new ArrayIndexOutOfBoundsException("coords exceeds maximum capacity !");
        }
        int grow = oldSize;
        if (grow > EXPAND_MAX_COORDS) {
            grow = EXPAND_MAX_COORDS;
        } else if (grow == 0) {
            grow = INIT_SIZE * 2;
        }
        int newSize = oldSize + grow;
        if (newSize < newSizeMin) {
            newSize = java.lang.Integer.MAX_VALUE;
        }
        double[] out = new double[newSize];
        System.arraycopy(oldCoords, 0, out, 0, oldSize);
        return out;
    }

    static float[] expandCoordsFloat(float[] oldCoords, int needed) {
        int oldSize = oldCoords.length;
        int newSizeMin = oldSize + needed;
        if (newSizeMin < oldSize) {
            throw new ArrayIndexOutOfBoundsException("coords exceeds maximum capacity !");
        }
        int grow = oldSize;
        if (grow > EXPAND_MAX_COORDS) {
            grow = EXPAND_MAX_COORDS;
        } else if (grow == 0) {
            grow = INIT_SIZE * 2;
        }
        int newSize = oldSize + grow;
        if (newSize < newSizeMin) {
            newSize = java.lang.Integer.MAX_VALUE;
        }
        float[] out = new float[newSize];
        System.arraycopy(oldCoords, 0, out, 0, oldSize);
        return out;
    }

    // --- camino con coordenadas float ------------------------------------------------------------

    public static class Float extends Path2D implements java.io.Serializable {

        transient float[] floatCoords;

        public Float() {
            this(WIND_NON_ZERO, INIT_SIZE);
        }

        public Float(int rule) {
            this(rule, INIT_SIZE);
        }

        public Float(int rule, int initialCapacity) {
            super(rule, initialCapacity);
            this.floatCoords = new float[initialCapacity * 2];
        }

        public Float(Shape s) {
            this(s, null);
        }

        public Float(Shape s, AffineTransform at) {
            if (s instanceof Path2D) {
                Path2D p2d = (Path2D) s;
                setWindingRule(p2d.windingRule);
                this.numTypes = p2d.numTypes;
                this.pointTypes = copyOf(p2d.pointTypes, p2d.pointTypes.length);
                this.numCoords = p2d.numCoords;
                this.floatCoords = p2d.cloneCoordsFloat(at);
            } else {
                PathIterator pi = s.getPathIterator(at);
                setWindingRule(pi.getWindingRule());
                this.pointTypes = new byte[INIT_SIZE];
                this.floatCoords = new float[INIT_SIZE * 2];
                append(pi, false);
            }
        }

        float[] cloneCoordsFloat(AffineTransform at) {
            float[] ret;
            if (at == null) {
                ret = copyOf(this.floatCoords, this.floatCoords.length);
            } else {
                ret = new float[this.floatCoords.length];
                at.transform(this.floatCoords, 0, ret, 0, this.numCoords / 2);
            }
            return ret;
        }

        double[] cloneCoordsDouble(AffineTransform at) {
            double[] ret = new double[this.floatCoords.length];
            if (at == null) {
                int i = 0;
                while (i < this.numCoords) {
                    ret[i] = (double) this.floatCoords[i];
                    i = i + 1;
                }
            } else {
                at.transform(this.floatCoords, 0, ret, 0, this.numCoords / 2);
            }
            return ret;
        }

        void append(float x, float y) {
            this.floatCoords[this.numCoords] = x;
            this.numCoords = this.numCoords + 1;
            this.floatCoords[this.numCoords] = y;
            this.numCoords = this.numCoords + 1;
        }

        void append(double x, double y) {
            this.floatCoords[this.numCoords] = (float) x;
            this.numCoords = this.numCoords + 1;
            this.floatCoords[this.numCoords] = (float) y;
            this.numCoords = this.numCoords + 1;
        }

        Point2D getPoint(int coordindex) {
            return Point2D.newFloat(this.floatCoords[coordindex],
                    this.floatCoords[coordindex + 1]);
        }

        void needRoom(boolean needMove, int newCoords) {
            if ((this.numTypes == 0) && needMove) {
                throw new IllegalPathStateException("missing initial moveto "
                        + "in path definition");
            }
            if (this.numTypes >= this.pointTypes.length) {
                this.pointTypes = expandPointTypes(this.pointTypes, 1);
            }
            if (this.numCoords > (this.floatCoords.length - newCoords)) {
                this.floatCoords = expandCoordsFloat(this.floatCoords, newCoords);
            }
        }

        public final synchronized void moveTo(double x, double y) {
            if (this.numTypes > 0 && this.pointTypes[this.numTypes - 1] == SEG_MOVETO) {
                // Dos moveTo seguidos: el segundo pisa al primero, no deja un subcamino vacio.
                this.floatCoords[this.numCoords - 2] = (float) x;
                this.floatCoords[this.numCoords - 1] = (float) y;
            } else {
                needRoom(false, 2);
                this.pointTypes[this.numTypes] = SEG_MOVETO;
                this.numTypes = this.numTypes + 1;
                this.floatCoords[this.numCoords] = (float) x;
                this.numCoords = this.numCoords + 1;
                this.floatCoords[this.numCoords] = (float) y;
                this.numCoords = this.numCoords + 1;
            }
        }

        public final synchronized void moveTo(float x, float y) {
            if (this.numTypes > 0 && this.pointTypes[this.numTypes - 1] == SEG_MOVETO) {
                this.floatCoords[this.numCoords - 2] = x;
                this.floatCoords[this.numCoords - 1] = y;
            } else {
                needRoom(false, 2);
                this.pointTypes[this.numTypes] = SEG_MOVETO;
                this.numTypes = this.numTypes + 1;
                this.floatCoords[this.numCoords] = x;
                this.numCoords = this.numCoords + 1;
                this.floatCoords[this.numCoords] = y;
                this.numCoords = this.numCoords + 1;
            }
        }

        public final synchronized void lineTo(double x, double y) {
            needRoom(true, 2);
            this.pointTypes[this.numTypes] = SEG_LINETO;
            this.numTypes = this.numTypes + 1;
            append((float) x, (float) y);
        }

        public final synchronized void lineTo(float x, float y) {
            needRoom(true, 2);
            this.pointTypes[this.numTypes] = SEG_LINETO;
            this.numTypes = this.numTypes + 1;
            append(x, y);
        }

        public final synchronized void quadTo(double x1, double y1, double x2, double y2) {
            needRoom(true, 4);
            this.pointTypes[this.numTypes] = SEG_QUADTO;
            this.numTypes = this.numTypes + 1;
            append((float) x1, (float) y1);
            append((float) x2, (float) y2);
        }

        public final synchronized void quadTo(float x1, float y1, float x2, float y2) {
            needRoom(true, 4);
            this.pointTypes[this.numTypes] = SEG_QUADTO;
            this.numTypes = this.numTypes + 1;
            append(x1, y1);
            append(x2, y2);
        }

        public final synchronized void curveTo(double x1, double y1,
                                               double x2, double y2,
                                               double x3, double y3) {
            needRoom(true, 6);
            this.pointTypes[this.numTypes] = SEG_CUBICTO;
            this.numTypes = this.numTypes + 1;
            append((float) x1, (float) y1);
            append((float) x2, (float) y2);
            append((float) x3, (float) y3);
        }

        public final synchronized void curveTo(float x1, float y1,
                                               float x2, float y2,
                                               float x3, float y3) {
            needRoom(true, 6);
            this.pointTypes[this.numTypes] = SEG_CUBICTO;
            this.numTypes = this.numTypes + 1;
            append(x1, y1);
            append(x2, y2);
            append(x3, y3);
        }

        int pointCrossings(double px, double py) {
            if (this.numTypes == 0) {
                return 0;
            }
            float[] c = this.floatCoords;
            double movx = c[0];
            double movy = c[1];
            double curx = movx;
            double cury = movy;
            double endx;
            double endy;
            int crossings = 0;
            int ci = 2;
            int i = 1;
            while (i < this.numTypes) {
                byte t = this.pointTypes[i];
                if (t == SEG_MOVETO) {
                    if (cury != movy) {
                        crossings = crossings
                                + Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy);
                    }
                    movx = c[ci];
                    curx = c[ci];
                    ci = ci + 1;
                    movy = c[ci];
                    cury = c[ci];
                    ci = ci + 1;
                } else if (t == SEG_LINETO) {
                    endx = c[ci];
                    endy = c[ci + 1];
                    ci = ci + 2;
                    crossings = crossings
                            + Curve.pointCrossingsForLine(px, py, curx, cury, endx, endy);
                    curx = endx;
                    cury = endy;
                } else if (t == SEG_QUADTO) {
                    endx = c[ci + 2];
                    endy = c[ci + 3];
                    crossings = crossings + Curve.pointCrossingsForQuad(px, py, curx, cury,
                            c[ci], c[ci + 1], endx, endy, 0);
                    ci = ci + 4;
                    curx = endx;
                    cury = endy;
                } else if (t == SEG_CUBICTO) {
                    endx = c[ci + 4];
                    endy = c[ci + 5];
                    crossings = crossings + Curve.pointCrossingsForCubic(px, py, curx, cury,
                            c[ci], c[ci + 1], c[ci + 2], c[ci + 3], endx, endy, 0);
                    ci = ci + 6;
                    curx = endx;
                    cury = endy;
                } else {
                    if (cury != movy) {
                        crossings = crossings
                                + Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy);
                    }
                    curx = movx;
                    cury = movy;
                }
                i = i + 1;
            }
            if (cury != movy) {
                crossings = crossings
                        + Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy);
            }
            return crossings;
        }

        int rectCrossings(double rxmin, double rymin, double rxmax, double rymax) {
            if (this.numTypes == 0) {
                return 0;
            }
            float[] c = this.floatCoords;
            double movx = c[0];
            double movy = c[1];
            double curx = movx;
            double cury = movy;
            double endx;
            double endy;
            int crossings = 0;
            int ci = 2;
            int i = 1;
            while (crossings != Curve.RECT_INTERSECTS && i < this.numTypes) {
                byte t = this.pointTypes[i];
                if (t == SEG_MOVETO) {
                    if (curx != movx || cury != movy) {
                        crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin,
                                rxmax, rymax, curx, cury, movx, movy);
                    }
                    movx = c[ci];
                    curx = c[ci];
                    ci = ci + 1;
                    movy = c[ci];
                    cury = c[ci];
                    ci = ci + 1;
                } else if (t == SEG_LINETO) {
                    endx = c[ci];
                    endy = c[ci + 1];
                    ci = ci + 2;
                    crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin,
                            rxmax, rymax, curx, cury, endx, endy);
                    curx = endx;
                    cury = endy;
                } else if (t == SEG_QUADTO) {
                    endx = c[ci + 2];
                    endy = c[ci + 3];
                    crossings = Curve.rectCrossingsForQuad(crossings, rxmin, rymin, rxmax, rymax,
                            curx, cury, c[ci], c[ci + 1], endx, endy, 0);
                    ci = ci + 4;
                    curx = endx;
                    cury = endy;
                } else if (t == SEG_CUBICTO) {
                    endx = c[ci + 4];
                    endy = c[ci + 5];
                    crossings = Curve.rectCrossingsForCubic(crossings, rxmin, rymin, rxmax, rymax,
                            curx, cury, c[ci], c[ci + 1], c[ci + 2], c[ci + 3], endx, endy, 0);
                    ci = ci + 6;
                    curx = endx;
                    cury = endy;
                } else {
                    if (curx != movx || cury != movy) {
                        crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin,
                                rxmax, rymax, curx, cury, movx, movy);
                    }
                    curx = movx;
                    cury = movy;
                }
                i = i + 1;
            }
            if (crossings != Curve.RECT_INTERSECTS && (curx != movx || cury != movy)) {
                crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin,
                        rxmax, rymax, curx, cury, movx, movy);
            }
            return crossings;
        }

        public final void append(PathIterator pi, boolean connect) {
            double[] coords = new double[6];
            while (!pi.isDone()) {
                int t = pi.currentSegment(coords);
                if (t == SEG_MOVETO) {
                    if (!connect || this.numTypes < 1 || this.numCoords < 1) {
                        moveTo(coords[0], coords[1]);
                    } else if (this.pointTypes[this.numTypes - 1] != SEG_CLOSE
                            && this.floatCoords[this.numCoords - 2] == ((float) coords[0])
                            && this.floatCoords[this.numCoords - 1] == ((float) coords[1])) {
                        // Pegar en el mismo punto donde termina el camino: se saltea el moveTo.
                        connect = false;
                    } else {
                        if (this.pointTypes[this.numTypes - 1] == SEG_CLOSE) {
                            moveTo(coords[0], coords[1]);
                        } else {
                            lineTo(coords[0], coords[1]);
                        }
                        connect = false;
                    }
                } else if (t == SEG_LINETO) {
                    lineTo(coords[0], coords[1]);
                    connect = false;
                } else if (t == SEG_QUADTO) {
                    quadTo(coords[0], coords[1], coords[2], coords[3]);
                    connect = false;
                } else if (t == SEG_CUBICTO) {
                    curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    connect = false;
                } else {
                    closePath();
                    connect = false;
                }
                pi.next();
            }
        }

        public final synchronized void transform(AffineTransform at) {
            at.transform(this.floatCoords, 0, this.floatCoords, 0, this.numCoords / 2);
        }

        public final synchronized Rectangle2D getBounds2D() {
            double x1;
            double y1;
            double x2;
            double y2;
            int i = this.numCoords;
            if (i > 0) {
                i = i - 1;
                y2 = this.floatCoords[i];
                y1 = y2;
                i = i - 1;
                x2 = this.floatCoords[i];
                x1 = x2;
                while (i > 0) {
                    i = i - 1;
                    double y = this.floatCoords[i];
                    i = i - 1;
                    double x = this.floatCoords[i];
                    if (x < x1) {
                        x1 = x;
                    }
                    if (y < y1) {
                        y1 = y;
                    }
                    if (x > x2) {
                        x2 = x;
                    }
                    if (y > y2) {
                        y2 = y;
                    }
                }
            } else {
                x1 = 0.0;
                y1 = 0.0;
                x2 = 0.0;
                y2 = 0.0;
            }
            return Rectangle2D.newDouble(x1, y1, x2 - x1, y2 - y1);
        }

        float[] floatCoordsRef() {
            return this.floatCoords;
        }

        double[] doubleCoordsRef() {
            return null;
        }

        public final PathIterator getPathIterator(AffineTransform at) {
            return new FloatPathIterator(this, at);
        }

        public final synchronized void trimToSize() {
            if (this.numTypes < this.pointTypes.length) {
                this.pointTypes = copyOf(this.pointTypes, this.numTypes);
            }
            if (this.numCoords < this.floatCoords.length) {
                this.floatCoords = copyOf(this.floatCoords, this.numCoords);
            }
        }

        public final Object clone() {
            return new Float(this);
        }

        static byte[] copyOf(byte[] src, int len) {
            byte[] out = new byte[len];
            int n = src.length;
            if (n > len) {
                n = len;
            }
            System.arraycopy(src, 0, out, 0, n);
            return out;
        }

        static float[] copyOf(float[] src, int len) {
            float[] out = new float[len];
            int n = src.length;
            if (n > len) {
                n = len;
            }
            System.arraycopy(src, 0, out, 0, n);
            return out;
        }
    }

    // --- camino con coordenadas double -----------------------------------------------------------

    public static class Double extends Path2D implements java.io.Serializable {

        transient double[] doubleCoords;

        public Double() {
            this(WIND_NON_ZERO, INIT_SIZE);
        }

        public Double(int rule) {
            this(rule, INIT_SIZE);
        }

        public Double(int rule, int initialCapacity) {
            super(rule, initialCapacity);
            this.doubleCoords = new double[initialCapacity * 2];
        }

        public Double(Shape s) {
            this(s, null);
        }

        public Double(Shape s, AffineTransform at) {
            if (s instanceof Path2D) {
                Path2D p2d = (Path2D) s;
                setWindingRule(p2d.windingRule);
                this.numTypes = p2d.numTypes;
                this.pointTypes = copyOf(p2d.pointTypes, p2d.pointTypes.length);
                this.numCoords = p2d.numCoords;
                this.doubleCoords = p2d.cloneCoordsDouble(at);
            } else {
                PathIterator pi = s.getPathIterator(at);
                setWindingRule(pi.getWindingRule());
                this.pointTypes = new byte[INIT_SIZE];
                this.doubleCoords = new double[INIT_SIZE * 2];
                append(pi, false);
            }
        }

        float[] cloneCoordsFloat(AffineTransform at) {
            float[] ret = new float[this.doubleCoords.length];
            if (at == null) {
                int i = 0;
                while (i < this.numCoords) {
                    ret[i] = (float) this.doubleCoords[i];
                    i = i + 1;
                }
            } else {
                at.transform(this.doubleCoords, 0, ret, 0, this.numCoords / 2);
            }
            return ret;
        }

        double[] cloneCoordsDouble(AffineTransform at) {
            double[] ret;
            if (at == null) {
                ret = copyOf(this.doubleCoords, this.doubleCoords.length);
            } else {
                ret = new double[this.doubleCoords.length];
                at.transform(this.doubleCoords, 0, ret, 0, this.numCoords / 2);
            }
            return ret;
        }

        void append(float x, float y) {
            this.doubleCoords[this.numCoords] = (double) x;
            this.numCoords = this.numCoords + 1;
            this.doubleCoords[this.numCoords] = (double) y;
            this.numCoords = this.numCoords + 1;
        }

        void append(double x, double y) {
            this.doubleCoords[this.numCoords] = x;
            this.numCoords = this.numCoords + 1;
            this.doubleCoords[this.numCoords] = y;
            this.numCoords = this.numCoords + 1;
        }

        Point2D getPoint(int coordindex) {
            return Point2D.newDouble(this.doubleCoords[coordindex],
                    this.doubleCoords[coordindex + 1]);
        }

        void needRoom(boolean needMove, int newCoords) {
            if ((this.numTypes == 0) && needMove) {
                throw new IllegalPathStateException("missing initial moveto "
                        + "in path definition");
            }
            if (this.numTypes >= this.pointTypes.length) {
                this.pointTypes = expandPointTypes(this.pointTypes, 1);
            }
            if (this.numCoords > (this.doubleCoords.length - newCoords)) {
                this.doubleCoords = expandCoordsDouble(this.doubleCoords, newCoords);
            }
        }

        public final synchronized void moveTo(double x, double y) {
            if (this.numTypes > 0 && this.pointTypes[this.numTypes - 1] == SEG_MOVETO) {
                this.doubleCoords[this.numCoords - 2] = x;
                this.doubleCoords[this.numCoords - 1] = y;
            } else {
                needRoom(false, 2);
                this.pointTypes[this.numTypes] = SEG_MOVETO;
                this.numTypes = this.numTypes + 1;
                this.doubleCoords[this.numCoords] = x;
                this.numCoords = this.numCoords + 1;
                this.doubleCoords[this.numCoords] = y;
                this.numCoords = this.numCoords + 1;
            }
        }

        public final synchronized void lineTo(double x, double y) {
            needRoom(true, 2);
            this.pointTypes[this.numTypes] = SEG_LINETO;
            this.numTypes = this.numTypes + 1;
            append(x, y);
        }

        public final synchronized void quadTo(double x1, double y1, double x2, double y2) {
            needRoom(true, 4);
            this.pointTypes[this.numTypes] = SEG_QUADTO;
            this.numTypes = this.numTypes + 1;
            append(x1, y1);
            append(x2, y2);
        }

        public final synchronized void curveTo(double x1, double y1,
                                               double x2, double y2,
                                               double x3, double y3) {
            needRoom(true, 6);
            this.pointTypes[this.numTypes] = SEG_CUBICTO;
            this.numTypes = this.numTypes + 1;
            append(x1, y1);
            append(x2, y2);
            append(x3, y3);
        }

        int pointCrossings(double px, double py) {
            if (this.numTypes == 0) {
                return 0;
            }
            double[] c = this.doubleCoords;
            double movx = c[0];
            double movy = c[1];
            double curx = movx;
            double cury = movy;
            double endx;
            double endy;
            int crossings = 0;
            int ci = 2;
            int i = 1;
            while (i < this.numTypes) {
                byte t = this.pointTypes[i];
                if (t == SEG_MOVETO) {
                    if (cury != movy) {
                        crossings = crossings
                                + Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy);
                    }
                    movx = c[ci];
                    curx = c[ci];
                    ci = ci + 1;
                    movy = c[ci];
                    cury = c[ci];
                    ci = ci + 1;
                } else if (t == SEG_LINETO) {
                    endx = c[ci];
                    endy = c[ci + 1];
                    ci = ci + 2;
                    crossings = crossings
                            + Curve.pointCrossingsForLine(px, py, curx, cury, endx, endy);
                    curx = endx;
                    cury = endy;
                } else if (t == SEG_QUADTO) {
                    endx = c[ci + 2];
                    endy = c[ci + 3];
                    crossings = crossings + Curve.pointCrossingsForQuad(px, py, curx, cury,
                            c[ci], c[ci + 1], endx, endy, 0);
                    ci = ci + 4;
                    curx = endx;
                    cury = endy;
                } else if (t == SEG_CUBICTO) {
                    endx = c[ci + 4];
                    endy = c[ci + 5];
                    crossings = crossings + Curve.pointCrossingsForCubic(px, py, curx, cury,
                            c[ci], c[ci + 1], c[ci + 2], c[ci + 3], endx, endy, 0);
                    ci = ci + 6;
                    curx = endx;
                    cury = endy;
                } else {
                    if (cury != movy) {
                        crossings = crossings
                                + Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy);
                    }
                    curx = movx;
                    cury = movy;
                }
                i = i + 1;
            }
            if (cury != movy) {
                crossings = crossings
                        + Curve.pointCrossingsForLine(px, py, curx, cury, movx, movy);
            }
            return crossings;
        }

        int rectCrossings(double rxmin, double rymin, double rxmax, double rymax) {
            if (this.numTypes == 0) {
                return 0;
            }
            double[] c = this.doubleCoords;
            double movx = c[0];
            double movy = c[1];
            double curx = movx;
            double cury = movy;
            double endx;
            double endy;
            int crossings = 0;
            int ci = 2;
            int i = 1;
            while (crossings != Curve.RECT_INTERSECTS && i < this.numTypes) {
                byte t = this.pointTypes[i];
                if (t == SEG_MOVETO) {
                    if (curx != movx || cury != movy) {
                        crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin,
                                rxmax, rymax, curx, cury, movx, movy);
                    }
                    movx = c[ci];
                    curx = c[ci];
                    ci = ci + 1;
                    movy = c[ci];
                    cury = c[ci];
                    ci = ci + 1;
                } else if (t == SEG_LINETO) {
                    endx = c[ci];
                    endy = c[ci + 1];
                    ci = ci + 2;
                    crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin,
                            rxmax, rymax, curx, cury, endx, endy);
                    curx = endx;
                    cury = endy;
                } else if (t == SEG_QUADTO) {
                    endx = c[ci + 2];
                    endy = c[ci + 3];
                    crossings = Curve.rectCrossingsForQuad(crossings, rxmin, rymin, rxmax, rymax,
                            curx, cury, c[ci], c[ci + 1], endx, endy, 0);
                    ci = ci + 4;
                    curx = endx;
                    cury = endy;
                } else if (t == SEG_CUBICTO) {
                    endx = c[ci + 4];
                    endy = c[ci + 5];
                    crossings = Curve.rectCrossingsForCubic(crossings, rxmin, rymin, rxmax, rymax,
                            curx, cury, c[ci], c[ci + 1], c[ci + 2], c[ci + 3], endx, endy, 0);
                    ci = ci + 6;
                    curx = endx;
                    cury = endy;
                } else {
                    if (curx != movx || cury != movy) {
                        crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin,
                                rxmax, rymax, curx, cury, movx, movy);
                    }
                    curx = movx;
                    cury = movy;
                }
                i = i + 1;
            }
            if (crossings != Curve.RECT_INTERSECTS && (curx != movx || cury != movy)) {
                crossings = Curve.rectCrossingsForLine(crossings, rxmin, rymin,
                        rxmax, rymax, curx, cury, movx, movy);
            }
            return crossings;
        }

        public final void append(PathIterator pi, boolean connect) {
            double[] coords = new double[6];
            while (!pi.isDone()) {
                int t = pi.currentSegment(coords);
                if (t == SEG_MOVETO) {
                    if (!connect || this.numTypes < 1 || this.numCoords < 1) {
                        moveTo(coords[0], coords[1]);
                    } else if (this.pointTypes[this.numTypes - 1] != SEG_CLOSE
                            && this.doubleCoords[this.numCoords - 2] == coords[0]
                            && this.doubleCoords[this.numCoords - 1] == coords[1]) {
                        connect = false;
                    } else {
                        if (this.pointTypes[this.numTypes - 1] == SEG_CLOSE) {
                            moveTo(coords[0], coords[1]);
                        } else {
                            lineTo(coords[0], coords[1]);
                        }
                        connect = false;
                    }
                } else if (t == SEG_LINETO) {
                    lineTo(coords[0], coords[1]);
                    connect = false;
                } else if (t == SEG_QUADTO) {
                    quadTo(coords[0], coords[1], coords[2], coords[3]);
                    connect = false;
                } else if (t == SEG_CUBICTO) {
                    curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
                    connect = false;
                } else {
                    closePath();
                    connect = false;
                }
                pi.next();
            }
        }

        public final synchronized void transform(AffineTransform at) {
            at.transform(this.doubleCoords, 0, this.doubleCoords, 0, this.numCoords / 2);
        }

        public final synchronized Rectangle2D getBounds2D() {
            double x1;
            double y1;
            double x2;
            double y2;
            int i = this.numCoords;
            if (i > 0) {
                i = i - 1;
                y2 = this.doubleCoords[i];
                y1 = y2;
                i = i - 1;
                x2 = this.doubleCoords[i];
                x1 = x2;
                while (i > 0) {
                    i = i - 1;
                    double y = this.doubleCoords[i];
                    i = i - 1;
                    double x = this.doubleCoords[i];
                    if (x < x1) {
                        x1 = x;
                    }
                    if (y < y1) {
                        y1 = y;
                    }
                    if (x > x2) {
                        x2 = x;
                    }
                    if (y > y2) {
                        y2 = y;
                    }
                }
            } else {
                x1 = 0.0;
                y1 = 0.0;
                x2 = 0.0;
                y2 = 0.0;
            }
            return Rectangle2D.newDouble(x1, y1, x2 - x1, y2 - y1);
        }

        float[] floatCoordsRef() {
            return null;
        }

        double[] doubleCoordsRef() {
            return this.doubleCoords;
        }

        public final PathIterator getPathIterator(AffineTransform at) {
            return new DoublePathIterator(this, at);
        }

        public final synchronized void trimToSize() {
            if (this.numTypes < this.pointTypes.length) {
                this.pointTypes = copyOf(this.pointTypes, this.numTypes);
            }
            if (this.numCoords < this.doubleCoords.length) {
                this.doubleCoords = copyOf(this.doubleCoords, this.numCoords);
            }
        }

        public final Object clone() {
            return new Double(this);
        }

        static byte[] copyOf(byte[] src, int len) {
            byte[] out = new byte[len];
            int n = src.length;
            if (n > len) {
                n = len;
            }
            System.arraycopy(src, 0, out, 0, n);
            return out;
        }

        static double[] copyOf(double[] src, int len) {
            double[] out = new double[len];
            int n = src.length;
            if (n > len) {
                n = len;
            }
            System.arraycopy(src, 0, out, 0, n);
            return out;
        }
    }

    // --- superficie comun -------------------------------------------------------------------------

    public abstract void moveTo(double x, double y);

    public abstract void lineTo(double x, double y);

    public abstract void quadTo(double x1, double y1, double x2, double y2);

    public abstract void curveTo(double x1, double y1, double x2, double y2,
                                 double x3, double y3);

    public final synchronized void closePath() {
        if (this.numTypes == 0 || this.pointTypes[this.numTypes - 1] != SEG_CLOSE) {
            needRoom(true, 0);
            this.pointTypes[this.numTypes] = SEG_CLOSE;
            this.numTypes = this.numTypes + 1;
        }
    }

    public final void append(Shape s, boolean connect) {
        append(s.getPathIterator(null), connect);
    }

    public abstract void append(PathIterator pi, boolean connect);

    public final synchronized int getWindingRule() {
        return this.windingRule;
    }

    public final void setWindingRule(int rule) {
        if (rule != WIND_EVEN_ODD && rule != WIND_NON_ZERO) {
            throw new IllegalArgumentException("winding rule must be "
                    + "WIND_EVEN_ODD or "
                    + "WIND_NON_ZERO");
        }
        this.windingRule = rule;
    }

    // El ultimo punto **dibujado**. Despues de un CLOSE es el punto del moveTo que abrio el
    // subcamino, no el ultimo lineTo: el camino volvio ahi.
    public final synchronized Point2D getCurrentPoint() {
        int index = this.numCoords;
        if (this.numTypes < 1 || index < 1) {
            return null;
        }
        if (this.pointTypes[this.numTypes - 1] == SEG_CLOSE) {
            int i = this.numTypes - 2;
            boolean buscando = true;
            while (i > 0 && buscando) {
                byte t = this.pointTypes[i];
                if (t == SEG_MOVETO) {
                    buscando = false;
                } else if (t == SEG_LINETO) {
                    index = index - 2;
                } else if (t == SEG_QUADTO) {
                    index = index - 4;
                } else if (t == SEG_CUBICTO) {
                    index = index - 6;
                }
                if (buscando) {
                    i = i - 1;
                }
            }
        }
        return getPoint(index - 2);
    }

    public final synchronized void reset() {
        this.numTypes = 0;
        this.numCoords = 0;
    }

    public abstract void transform(AffineTransform at);

    public final synchronized Shape createTransformedShape(AffineTransform at) {
        Path2D p2d = (Path2D) clone();
        if (at != null) {
            p2d.transform(at);
        }
        return p2d;
    }

    public abstract Object clone();

    public abstract void trimToSize();

    // --- preguntas geometricas --------------------------------------------------------------------

    public static boolean contains(PathIterator pi, double x, double y) {
        // x*0.0 da NaN si x es infinito o NaN: filtra los dos casos de una.
        if (x * 0.0 + y * 0.0 == 0.0) {
            int mask;
            if (pi.getWindingRule() == WIND_NON_ZERO) {
                mask = -1;
            } else {
                mask = 1;
            }
            int cross = Curve.pointCrossingsForPath(pi, x, y);
            return ((cross & mask) != 0);
        }
        return false;
    }

    public static boolean contains(PathIterator pi, Point2D p) {
        return contains(pi, p.getX(), p.getY());
    }

    public final boolean contains(double x, double y) {
        if (x * 0.0 + y * 0.0 == 0.0) {
            if (this.numTypes < 2) {
                return false;
            }
            int mask;
            if (this.windingRule == WIND_NON_ZERO) {
                mask = -1;
            } else {
                mask = 1;
            }
            return ((pointCrossings(x, y) & mask) != 0);
        }
        return false;
    }

    public final boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    public static boolean contains(PathIterator pi, double x, double y, double w, double h) {
        if (java.lang.Double.isNaN(x + w) || java.lang.Double.isNaN(y + h)) {
            // Un rectangulo con NaN no tiene interior; ni contains ni intersects son ciertos.
            return false;
        }
        if (w <= 0 || h <= 0) {
            return false;
        }
        int mask;
        if (pi.getWindingRule() == WIND_NON_ZERO) {
            mask = -1;
        } else {
            mask = 2;
        }
        int crossings = Curve.rectCrossingsForPath(pi, x, y, x + w, y + h);
        return (crossings != Curve.RECT_INTERSECTS && (crossings & mask) != 0);
    }

    public static boolean contains(PathIterator pi, Rectangle2D r) {
        return contains(pi, r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public final boolean contains(double x, double y, double w, double h) {
        if (java.lang.Double.isNaN(x + w) || java.lang.Double.isNaN(y + h)) {
            return false;
        }
        if (w <= 0 || h <= 0) {
            return false;
        }
        int mask;
        if (this.windingRule == WIND_NON_ZERO) {
            mask = -1;
        } else {
            mask = 2;
        }
        int crossings = rectCrossings(x, y, x + w, y + h);
        return (crossings != Curve.RECT_INTERSECTS && (crossings & mask) != 0);
    }

    public final boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public static boolean intersects(PathIterator pi, double x, double y, double w, double h) {
        if (java.lang.Double.isNaN(x + w) || java.lang.Double.isNaN(y + h)) {
            return false;
        }
        if (w <= 0 || h <= 0) {
            return false;
        }
        int mask;
        if (pi.getWindingRule() == WIND_NON_ZERO) {
            mask = -1;
        } else {
            mask = 2;
        }
        int crossings = Curve.rectCrossingsForPath(pi, x, y, x + w, y + h);
        return (crossings == Curve.RECT_INTERSECTS || (crossings & mask) != 0);
    }

    public static boolean intersects(PathIterator pi, Rectangle2D r) {
        return intersects(pi, r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public final boolean intersects(double x, double y, double w, double h) {
        if (java.lang.Double.isNaN(x + w) || java.lang.Double.isNaN(y + h)) {
            return false;
        }
        if (w <= 0 || h <= 0) {
            return false;
        }
        int mask;
        if (this.windingRule == WIND_NON_ZERO) {
            mask = -1;
        } else {
            mask = 2;
        }
        int crossings = rectCrossings(x, y, x + w, y + h);
        return (crossings == Curve.RECT_INTERSECTS || (crossings & mask) != 0);
    }

    public final boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public final PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new FlatteningPathIterator(getPathIterator(at), flatness);
    }

    public final Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    // Fabrica interna (no es API). Ver la nota del encabezado de Point2D.java: AffineTransform no
    // puede nombrar a `Path2D.Double` porque en esa unidad de compilacion `Double` ya es
    // java.lang.Double.
    static Path2D newDouble(Shape s, AffineTransform at) {
        return new Double(s, at);
    }

    static Path2D newDouble(int rule) {
        return new Double(rule);
    }
}
