package java.awt.geom;

import java.util.NoSuchElementException;

// Iterador interno de QuadCurve2D: MOVETO al primer punto y una sola QUADTO. Sin CLOSE.
class QuadIterator implements PathIterator {

    QuadCurve2D quad;
    AffineTransform affine;
    int index;

    QuadIterator(QuadCurve2D q, AffineTransform at) {
        this.quad = q;
        this.affine = at;
    }

    public int getWindingRule() {
        return PathIterator.WIND_NON_ZERO;
    }

    public boolean isDone() {
        return (this.index > 1);
    }

    public void next() {
        this.index = this.index + 1;
    }

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("quad iterator iterator out of bounds");
        }
        int type;
        if (this.index == 0) {
            coords[0] = (float) this.quad.getX1();
            coords[1] = (float) this.quad.getY1();
            type = PathIterator.SEG_MOVETO;
        } else {
            coords[0] = (float) this.quad.getCtrlX();
            coords[1] = (float) this.quad.getCtrlY();
            coords[2] = (float) this.quad.getX2();
            coords[3] = (float) this.quad.getY2();
            type = PathIterator.SEG_QUADTO;
        }
        if (this.affine != null) {
            int n;
            if (this.index == 0) {
                n = 1;
            } else {
                n = 2;
            }
            this.affine.transform(coords, 0, coords, 0, n);
        }
        return type;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("quad iterator iterator out of bounds");
        }
        int type;
        if (this.index == 0) {
            coords[0] = this.quad.getX1();
            coords[1] = this.quad.getY1();
            type = PathIterator.SEG_MOVETO;
        } else {
            coords[0] = this.quad.getCtrlX();
            coords[1] = this.quad.getCtrlY();
            coords[2] = this.quad.getX2();
            coords[3] = this.quad.getY2();
            type = PathIterator.SEG_QUADTO;
        }
        if (this.affine != null) {
            int n;
            if (this.index == 0) {
                n = 1;
            } else {
                n = 2;
            }
            this.affine.transform(coords, 0, coords, 0, n);
        }
        return type;
    }
}
