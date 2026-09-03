package java.awt.geom;

import java.util.NoSuchElementException;

// Iterador interno de Rectangle2D. Seis segmentos: MOVETO, tres LINETO por las esquinas, un cuarto
// LINETO **de vuelta al origen** y recien ahi el CLOSE. El LINETO redundante no es un descuido: es
// lo que emite el JDK y lo que ve cualquiera que recorra el camino.
//
// Un rectangulo de ancho o alto negativo no representa nada: el iterador arranca ya terminado.
class RectIterator implements PathIterator {

    double x;
    double y;
    double w;
    double h;
    AffineTransform affine;
    int index;

    RectIterator(Rectangle2D r, AffineTransform at) {
        this.x = r.getX();
        this.y = r.getY();
        this.w = r.getWidth();
        this.h = r.getHeight();
        this.affine = at;
        if (this.w < 0 || this.h < 0) {
            this.index = 6;
        }
    }

    public int getWindingRule() {
        return PathIterator.WIND_NON_ZERO;
    }

    public boolean isDone() {
        return this.index > 5;
    }

    public void next() {
        this.index = this.index + 1;
    }

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("rect iterator out of bounds");
        }
        if (this.index == 5) {
            return PathIterator.SEG_CLOSE;
        }
        coords[0] = (float) this.x;
        coords[1] = (float) this.y;
        if (this.index == 1 || this.index == 2) {
            coords[0] = coords[0] + (float) this.w;
        }
        if (this.index == 2 || this.index == 3) {
            coords[1] = coords[1] + (float) this.h;
        }
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 1);
        }
        if (this.index == 0) {
            return PathIterator.SEG_MOVETO;
        }
        return PathIterator.SEG_LINETO;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("rect iterator out of bounds");
        }
        if (this.index == 5) {
            return PathIterator.SEG_CLOSE;
        }
        coords[0] = this.x;
        coords[1] = this.y;
        if (this.index == 1 || this.index == 2) {
            coords[0] = coords[0] + this.w;
        }
        if (this.index == 2 || this.index == 3) {
            coords[1] = coords[1] + this.h;
        }
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 1);
        }
        if (this.index == 0) {
            return PathIterator.SEG_MOVETO;
        }
        return PathIterator.SEG_LINETO;
    }
}
