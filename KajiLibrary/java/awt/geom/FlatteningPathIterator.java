package java.awt.geom;

import java.util.NoSuchElementException;

// java.awt.geom.FlatteningPathIterator de KajiLibrary -- envuelve otro PathIterator y reemplaza cada
// curva por una cadena de segmentos rectos. Superficie completa.
//
// Como funciona la pila: `hold` es un buffer que se llena **desde el final hacia el principio**.
// Cuando un trozo de curva todavia no es lo bastante plano se lo subdivide en dos y la mitad
// izquierda se escribe encima del original mientras la derecha se empuja hacia atras en el buffer;
// `holdIndex` apunta al trozo que se esta mirando y `holdEnd` al final de lo que queda pendiente.
// Es una pila explicita en vez de recursion, y por eso el buffer crece (`ensureHoldCapacity`) en vez
// de desbordar la de llamadas.
//
// Dos cosas del contrato que conviene tener presentes:
//
//   * El iterador aplanado **nunca** devuelve QUADTO ni CUBICTO. Devuelve MOVETO, LINETO y CLOSE, y
//     nada mas. Eso es lo que lo hace util: quien lo consume no necesita saber de curvas.
//
//   * `limit` es el numero maximo de subdivisiones **por curva**, no en total. Al agotarse se emite
//     el segmento aunque no haya llegado a la planitud pedida. Sin ese tope una curva con una cuspide
//     subdividiria para siempre, porque la planitud no baja de cierto punto.
//
// El constructor rechaza una planitud negativa y un limite negativo con IllegalArgumentException:
// no hay lectura sensata de "aplanar con tolerancia -1", y aceptarlo daria un bucle infinito mas
// tarde, lejos del error.
public class FlatteningPathIterator implements PathIterator {

    static final int GROW_SIZE = 24;

    PathIterator src;
    double squareflat;
    int limit;
    double[] hold = new double[14];
    double curx;
    double cury;
    double movx;
    double movy;
    int holdType;
    int holdEnd;
    int holdIndex;
    int[] levels;
    int levelIndex;
    boolean done;

    public FlatteningPathIterator(PathIterator src, double flatness) {
        this(src, flatness, 10);
    }

    public FlatteningPathIterator(PathIterator src, double flatness, int limit) {
        if (flatness < 0.0) {
            throw new IllegalArgumentException("flatness must be >= 0");
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        this.src = src;
        this.squareflat = flatness * flatness;
        this.limit = limit;
        this.levels = new int[limit + 1];
        next(false);
    }

    public double getFlatness() {
        return Math.sqrt(this.squareflat);
    }

    public int getRecursionLimit() {
        return this.limit;
    }

    public int getWindingRule() {
        return this.src.getWindingRule();
    }

    public boolean isDone() {
        return this.done;
    }

    void ensureHoldCapacity(int want) {
        if (this.holdIndex - want < 0) {
            int have = this.hold.length - this.holdIndex;
            int newsize = this.hold.length + GROW_SIZE;
            double[] newhold = new double[newsize];
            System.arraycopy(this.hold, this.holdIndex, newhold, this.holdIndex + GROW_SIZE, have);
            this.hold = newhold;
            this.holdIndex = this.holdIndex + GROW_SIZE;
            this.holdEnd = this.holdEnd + GROW_SIZE;
        }
    }

    public void next() {
        next(true);
    }

    private void next(boolean doNext) {
        if (this.holdIndex >= this.holdEnd) {
            if (doNext) {
                this.src.next();
            }
            if (this.src.isDone()) {
                this.done = true;
                return;
            }
            this.holdType = this.src.currentSegment(this.hold);
            this.levelIndex = 0;
            this.levels[0] = 0;
        }

        if (this.holdType == PathIterator.SEG_MOVETO || this.holdType == PathIterator.SEG_LINETO) {
            this.curx = this.hold[0];
            this.cury = this.hold[1];
            if (this.holdType == PathIterator.SEG_MOVETO) {
                this.movx = this.curx;
                this.movy = this.cury;
            }
            this.holdIndex = 0;
            this.holdEnd = 0;
            return;
        }
        if (this.holdType == PathIterator.SEG_CLOSE) {
            this.curx = this.movx;
            this.cury = this.movy;
            this.holdIndex = 0;
            this.holdEnd = 0;
            return;
        }

        int level;
        if (this.holdType == PathIterator.SEG_QUADTO) {
            if (this.holdIndex >= this.holdEnd) {
                // Primera vez que se ve esta curva: se copia al final del buffer, precedida por el
                // punto actual --que el iterador de origen no repite-- para tener los tres puntos de
                // control contiguos. Seis huecos: (x0,y0) (xc,yc) (x1,y1).
                this.holdIndex = this.hold.length - 6;
                this.holdEnd = this.hold.length - 2;
                this.hold[this.holdIndex + 0] = this.curx;
                this.hold[this.holdIndex + 1] = this.cury;
                this.hold[this.holdIndex + 2] = this.hold[0];
                this.hold[this.holdIndex + 3] = this.hold[1];
                this.curx = this.hold[2];
                this.cury = this.hold[3];
                this.hold[this.holdIndex + 4] = this.curx;
                this.hold[this.holdIndex + 5] = this.cury;
            }
            level = this.levels[this.levelIndex];
            while (level < this.limit) {
                if (QuadCurve2D.getFlatnessSq(this.hold, this.holdIndex) < this.squareflat) {
                    break;
                }
                ensureHoldCapacity(4);
                QuadCurve2D.subdivide(this.hold, this.holdIndex,
                                      this.hold, this.holdIndex - 4,
                                      this.hold, this.holdIndex);
                this.holdIndex = this.holdIndex - 4;
                // Quedaron dos curvas de un nivel mas: la izquierda en los huecos nuevos y la
                // derecha donde estaba la original. Las dos se marcan con el nivel siguiente.
                level = level + 1;
                this.levels[this.levelIndex] = level;
                this.levelIndex = this.levelIndex + 1;
                this.levels[this.levelIndex] = level;
            }
            // El trozo ya es plano (o se agoto el limite): su extremo, en holdIndex+4, es el final
            // del segmento que se va a emitir.
            this.holdIndex = this.holdIndex + 4;
            this.levelIndex = this.levelIndex - 1;
        } else {
            if (this.holdIndex >= this.holdEnd) {
                // Ocho huecos: (x0,y0) (xc0,yc0) (xc1,yc1) (x1,y1).
                this.holdIndex = this.hold.length - 8;
                this.holdEnd = this.hold.length - 2;
                this.hold[this.holdIndex + 0] = this.curx;
                this.hold[this.holdIndex + 1] = this.cury;
                this.hold[this.holdIndex + 2] = this.hold[0];
                this.hold[this.holdIndex + 3] = this.hold[1];
                this.hold[this.holdIndex + 4] = this.hold[2];
                this.hold[this.holdIndex + 5] = this.hold[3];
                this.curx = this.hold[4];
                this.cury = this.hold[5];
                this.hold[this.holdIndex + 6] = this.curx;
                this.hold[this.holdIndex + 7] = this.cury;
            }
            level = this.levels[this.levelIndex];
            while (level < this.limit) {
                if (CubicCurve2D.getFlatnessSq(this.hold, this.holdIndex) < this.squareflat) {
                    break;
                }
                ensureHoldCapacity(6);
                CubicCurve2D.subdivide(this.hold, this.holdIndex,
                                       this.hold, this.holdIndex - 6,
                                       this.hold, this.holdIndex);
                this.holdIndex = this.holdIndex - 6;
                level = level + 1;
                this.levels[this.levelIndex] = level;
                this.levelIndex = this.levelIndex + 1;
                this.levels[this.levelIndex] = level;
            }
            this.holdIndex = this.holdIndex + 6;
            this.levelIndex = this.levelIndex - 1;
        }
    }

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("flattening iterator out of bounds");
        }
        int type = this.holdType;
        if (type != PathIterator.SEG_CLOSE) {
            coords[0] = (float) this.hold[this.holdIndex + 0];
            coords[1] = (float) this.hold[this.holdIndex + 1];
            if (type != PathIterator.SEG_MOVETO) {
                type = PathIterator.SEG_LINETO;
            }
        }
        return type;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("flattening iterator out of bounds");
        }
        int type = this.holdType;
        if (type != PathIterator.SEG_CLOSE) {
            coords[0] = this.hold[this.holdIndex + 0];
            coords[1] = this.hold[this.holdIndex + 1];
            if (type != PathIterator.SEG_MOVETO) {
                type = PathIterator.SEG_LINETO;
            }
        }
        return type;
    }
}
