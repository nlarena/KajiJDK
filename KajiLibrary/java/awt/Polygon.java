package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Un poligono cerrado con vertices enteros.
 *
 * <p>Los tres campos que definen la figura --{@code npoints}, {@code xpoints}, {@code ypoints}--
 * son publicos y mutables, que es una decision de 1.0 con la que hay que convivir. La consecuencia
 * es que la caja envolvente no se puede cachear a ciegas: quien toca los arreglos por afuera tiene
 * que llamar a {@code invalidate()}, y por eso existe ese metodo.
 *
 * <p>Los arreglos pueden ser mas largos que {@code npoints}. {@code addPoint} los duplica cuando se
 * llenan --amortizado-- asi que armar un poligono punto por punto no es cuadratico.
 *
 * <h2>Como se decide si un punto esta adentro</h2>
 *
 * <p>Regla par-impar: se cuentan los cruces de una semirrecta horizontal con los lados y se mira la
 * paridad. La consecuencia visible es que en un poligono que se autointersecta el "adentro" alterna,
 * y que los bordes de arriba y de la izquierda cuentan como adentro pero los de abajo y la derecha
 * no. Ese semiabierto no es un descuido: es lo que hace que dos poligonos pegados no compartan
 * ningun pixel y no se pise el borde al rellenar los dos.
 *
 * <p>Las pruebas contra un rectangulo se delegan en {@code Path2D} con
 * {@code WIND_EVEN_ODD}, que es la misma maquinaria de cruces que usa el resto de
 * {@code java.awt.geom}. Escribir un contador de cruces propio para esto seria repetir --y poder
 * equivocar-- un algoritmo que ya esta y ya se probo.
 */
public class Polygon implements Shape, java.io.Serializable {

    private static final long serialVersionUID = -6460061437900069969L;

    private static final int MIN_LENGTH = 4;

    public int npoints;

    public int[] xpoints;

    public int[] ypoints;

    /** La caja envolvente cacheada, o null si hay que recalcularla. */
    protected Rectangle bounds;

    public Polygon() {
        xpoints = new int[MIN_LENGTH];
        ypoints = new int[MIN_LENGTH];
    }

    public Polygon(int[] xpoints, int[] ypoints, int npoints) {
        // El orden importa: primero el largo negativo --que es un error del que llama-- y despues
        // el desborde, para que el mensaje describa la causa y no el sintoma.
        if (npoints > xpoints.length || npoints > ypoints.length) {
            throw new IndexOutOfBoundsException(
                    "npoints > xpoints.length || npoints > ypoints.length");
        }
        if (npoints < 0) {
            throw new NegativeArraySizeException("npoints < 0");
        }
        this.npoints = npoints;
        this.xpoints = java.util.Arrays.copyOf(xpoints, npoints);
        this.ypoints = java.util.Arrays.copyOf(ypoints, npoints);
    }

    public void reset() {
        npoints = 0;
        bounds = null;
    }

    /** Hay que llamarlo si se tocaron los arreglos publicos: el cache no se entera solo. */
    public void invalidate() {
        bounds = null;
    }

    public void translate(int deltaX, int deltaY) {
        for (int i = 0; i < npoints; i++) {
            xpoints[i] += deltaX;
            ypoints[i] += deltaY;
        }
        if (bounds != null) {
            // Trasladar la caja es exacto y evita recorrer los puntos una segunda vez.
            bounds.translate(deltaX, deltaY);
        }
    }

    private void calculateBounds(int[] xpoints, int[] ypoints, int npoints) {
        int boundsMinX = Integer.MAX_VALUE;
        int boundsMinY = Integer.MAX_VALUE;
        int boundsMaxX = Integer.MIN_VALUE;
        int boundsMaxY = Integer.MIN_VALUE;

        for (int i = 0; i < npoints; i++) {
            int x = xpoints[i];
            boundsMinX = Math.min(boundsMinX, x);
            boundsMaxX = Math.max(boundsMaxX, x);
            int y = ypoints[i];
            boundsMinY = Math.min(boundsMinY, y);
            boundsMaxY = Math.max(boundsMaxY, y);
        }
        bounds = new Rectangle(boundsMinX, boundsMinY,
                boundsMaxX - boundsMinX, boundsMaxY - boundsMinY);
    }

    private void updateBounds(int x, int y) {
        if (x < bounds.x) {
            bounds.width = bounds.width + (bounds.x - x);
            bounds.x = x;
        } else {
            bounds.width = Math.max(bounds.width, x - bounds.x);
        }

        if (y < bounds.y) {
            bounds.height = bounds.height + (bounds.y - y);
            bounds.y = y;
        } else {
            bounds.height = Math.max(bounds.height, y - bounds.y);
        }
    }

    public void addPoint(int x, int y) {
        if (npoints >= xpoints.length || npoints >= ypoints.length) {
            int newLength = npoints * 2;
            if (newLength < MIN_LENGTH) {
                newLength = MIN_LENGTH;
            } else if ((newLength & (newLength - 1)) != 0) {
                newLength = Integer.highestOneBit(newLength);
            }
            xpoints = java.util.Arrays.copyOf(xpoints, newLength);
            ypoints = java.util.Arrays.copyOf(ypoints, newLength);
        }
        xpoints[npoints] = x;
        ypoints[npoints] = y;
        npoints++;
        if (bounds != null) {
            updateBounds(x, y);
        }
    }

    public Rectangle getBounds() {
        return getBoundingBox();
    }

    /** El nombre de 1.0. En el JDK es el que hace el trabajo y {@code getBounds()} delega. */
    public Rectangle getBoundingBox() {
        if (npoints == 0) {
            return new Rectangle();
        }
        if (bounds == null) {
            calculateBounds(xpoints, ypoints, npoints);
        }
        return bounds.getBounds();
    }

    public boolean contains(Point p) {
        return contains(p.x, p.y);
    }

    public boolean contains(int x, int y) {
        return contains((double) x, (double) y);
    }

    /** El nombre de 1.0. */
    public boolean inside(int x, int y) {
        return contains((double) x, (double) y);
    }

    public Rectangle2D getBounds2D() {
        return getBounds();
    }

    /**
     * Cuenta cruces con la semirrecta que sale del punto hacia la izquierda y mira la paridad.
     *
     * <p>Los lados horizontales se saltean: no aportan cruce y ademas dividir por su altura seria
     * dividir por cero. Los otros descartes tempranos --por caja, por lado enteramente a la
     * izquierda o a la derecha-- estan para que el caso comun no llegue nunca a la division.
     */
    public boolean contains(double x, double y) {
        if (npoints <= 2 || !getBoundingBox().contains(x, y)) {
            return false;
        }
        int hits = 0;

        int lastx = xpoints[npoints - 1];
        int lasty = ypoints[npoints - 1];
        int curx;
        int cury;

        for (int i = 0; i < npoints; lastx = curx, lasty = cury, i++) {
            curx = xpoints[i];
            cury = ypoints[i];

            if (cury == lasty) {
                continue;
            }

            int leftx;
            if (curx < lastx) {
                if (x >= lastx) {
                    continue;
                }
                leftx = curx;
            } else {
                if (x >= curx) {
                    continue;
                }
                leftx = lastx;
            }

            double test1;
            double test2;
            if (cury < lasty) {
                if (y < cury || y >= lasty) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - curx;
                test2 = y - cury;
            } else {
                if (y < lasty || y >= cury) {
                    continue;
                }
                if (x < leftx) {
                    hits++;
                    continue;
                }
                test1 = x - lastx;
                test2 = y - lasty;
            }

            if (test1 < (test2 / (lasty - cury) * (lastx - curx))) {
                hits++;
            }
        }

        return ((hits & 1) != 0);
    }

    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    /** El poligono como camino, para poder reusar los cruces contra rectangulo de Path2D. */
    private Path2D.Double comoCamino() {
        Path2D.Double camino = new Path2D.Double(Path2D.WIND_EVEN_ODD, npoints);
        camino.moveTo(xpoints[0], ypoints[0]);
        for (int i = 1; i < npoints; i++) {
            camino.lineTo(xpoints[i], ypoints[i]);
        }
        camino.closePath();
        return camino;
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (npoints <= 0 || !getBoundingBox().intersects(x, y, w, h)) {
            return false;
        }
        return comoCamino().intersects(x, y, w, h);
    }

    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean contains(double x, double y, double w, double h) {
        if (npoints <= 0 || !getBoundingBox().intersects(x, y, w, h)) {
            return false;
        }
        return comoCamino().contains(x, y, w, h);
    }

    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new PolygonPathIterator(this, at);
    }

    /**
     * La tolerancia de aplanado se ignora, y no es una omision: un poligono ya es una sucesion de
     * segmentos rectos, asi que aplanarlo no puede cambiar nada.
     */
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return getPathIterator(at);
    }

    private static class PolygonPathIterator implements PathIterator {

        private Polygon poly;

        private AffineTransform transform;

        private int index;

        PolygonPathIterator(Polygon pg, AffineTransform at) {
            poly = pg;
            transform = at;
            if (pg.npoints == 0) {
                // Un poligono vacio ya esta terminado: se salta hasta el final para que el
                // recorrido no emita un SEG_CLOSE que no cierra nada.
                index = 1;
            }
        }

        public int getWindingRule() {
            // Calificado a proposito: ver #469 en COMPILER_FINDINGS.md -- los campos de una
            // interfaz implementada no se heredan y sin el prefijo no compila.
            return PathIterator.WIND_EVEN_ODD;
        }

        public boolean isDone() {
            return index > poly.npoints;
        }

        public void next() {
            index++;
        }

        public int currentSegment(float[] coords) {
            if (index >= poly.npoints) {
                return PathIterator.SEG_CLOSE;
            }
            coords[0] = poly.xpoints[index];
            coords[1] = poly.ypoints[index];
            if (transform != null) {
                transform.transform(coords, 0, coords, 0, 1);
            }
            return (index == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO);
        }

        public int currentSegment(double[] coords) {
            if (index >= poly.npoints) {
                return PathIterator.SEG_CLOSE;
            }
            coords[0] = poly.xpoints[index];
            coords[1] = poly.ypoints[index];
            if (transform != null) {
                transform.transform(coords, 0, coords, 0, 1);
            }
            return (index == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO);
        }
    }
}
