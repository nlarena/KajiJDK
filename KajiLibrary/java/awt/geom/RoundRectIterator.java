package java.awt.geom;

import java.util.NoSuchElementException;

// Iterador interno de RoundRectangle2D: cuatro lados rectos y cuatro esquinas en cubica.
//
// Cada punto se arma como (x + v0*w + v1*arcWidth, y + v2*h + v3*arcHeight); esa parametrizacion en
// cuatro coeficientes es la que permite escribir las esquinas sin repetir la aritmetica de los
// lados. `acv` es el analogo de la constante de la elipse para un cuarto de arco.
class RoundRectIterator implements PathIterator {

    double x;
    double y;
    double w;
    double h;
    double aw;
    double ah;
    AffineTransform affine;
    int index;

    RoundRectIterator(RoundRectangle2D rr, AffineTransform at) {
        this.x = rr.getX();
        this.y = rr.getY();
        this.w = rr.getWidth();
        this.h = rr.getHeight();
        this.aw = Math.min(this.w, Math.abs(rr.getArcWidth()));
        this.ah = Math.min(this.h, Math.abs(rr.getArcHeight()));
        this.affine = at;
        if (this.aw < 0 || this.ah < 0) {
            // No hay redondeo que dibujar: el camino queda vacio.
            this.index = CTRLPTS.length;
        }
    }

    public int getWindingRule() {
        return PathIterator.WIND_NON_ZERO;
    }

    public boolean isDone() {
        return this.index >= CTRLPTS.length;
    }

    public void next() {
        this.index = this.index + 1;
    }

    private static final double ANGLE = Math.PI / 4.0;
    private static final double A = 1.0 - Math.cos(ANGLE);
    private static final double B = Math.tan(ANGLE);
    private static final double C = Math.sqrt(1.0 + B * B) - 1.0 + Math.cos(ANGLE);
    private static final double CV = 4.0 / 3.0 * A * B / C;
    private static final double ACV = (1.0 - CV) / 2.0;

    // Cada punto son cuatro coeficientes {v0, v1, v2, v3}.
    private static final double[][] CTRLPTS = {
        { 0.0, 0.0, 0.0, 0.5 },
        { 0.0, 0.0, 1.0, -0.5 },
        { 0.0, 0.0, 1.0, -ACV,
          0.0, ACV, 1.0, 0.0,
          0.0, 0.5, 1.0, 0.0 },
        { 1.0, -0.5, 1.0, 0.0 },
        { 1.0, -ACV, 1.0, 0.0,
          1.0, 0.0, 1.0, -ACV,
          1.0, 0.0, 1.0, -0.5 },
        { 1.0, 0.0, 0.0, 0.5 },
        { 1.0, 0.0, 0.0, ACV,
          1.0, -ACV, 0.0, 0.0,
          1.0, -0.5, 0.0, 0.0 },
        { 0.0, 0.5, 0.0, 0.0 },
        { 0.0, ACV, 0.0, 0.0,
          0.0, 0.0, 0.0, ACV,
          0.0, 0.0, 0.0, 0.5 },
        {}
    };

    private static final int[] TYPES = {
        PathIterator.SEG_MOVETO,
        PathIterator.SEG_LINETO, PathIterator.SEG_CUBICTO,
        PathIterator.SEG_LINETO, PathIterator.SEG_CUBICTO,
        PathIterator.SEG_LINETO, PathIterator.SEG_CUBICTO,
        PathIterator.SEG_LINETO, PathIterator.SEG_CUBICTO,
        PathIterator.SEG_CLOSE
    };

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("roundrect iterator out of bounds");
        }
        double[] ctrls = CTRLPTS[this.index];
        int nc = 0;
        int i = 0;
        while (i < ctrls.length) {
            coords[nc] = (float) (this.x + ctrls[i + 0] * this.w + ctrls[i + 1] * this.aw);
            nc = nc + 1;
            coords[nc] = (float) (this.y + ctrls[i + 2] * this.h + ctrls[i + 3] * this.ah);
            nc = nc + 1;
            i = i + 4;
        }
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, nc / 2);
        }
        return TYPES[this.index];
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("roundrect iterator out of bounds");
        }
        double[] ctrls = CTRLPTS[this.index];
        int nc = 0;
        int i = 0;
        while (i < ctrls.length) {
            coords[nc] = this.x + ctrls[i + 0] * this.w + ctrls[i + 1] * this.aw;
            nc = nc + 1;
            coords[nc] = this.y + ctrls[i + 2] * this.h + ctrls[i + 3] * this.ah;
            nc = nc + 1;
            i = i + 4;
        }
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, nc / 2);
        }
        return TYPES[this.index];
    }
}
