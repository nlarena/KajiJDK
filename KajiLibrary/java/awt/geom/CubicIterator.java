package java.awt.geom;

import java.util.NoSuchElementException;

// Iterador interno de CubicCurve2D: MOVETO al primer punto y una sola CUBICTO. Sin CLOSE.
class CubicIterator implements PathIterator {

    CubicCurve2D cubic;
    AffineTransform affine;
    int index;

    CubicIterator(CubicCurve2D q, AffineTransform at) {
        this.cubic = q;
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
            throw new NoSuchElementException("cubic iterator iterator out of bounds");
        }
        int type;
        if (this.index == 0) {
            coords[0] = (float) this.cubic.getX1();
            coords[1] = (float) this.cubic.getY1();
            type = PathIterator.SEG_MOVETO;
        } else {
            coords[0] = (float) this.cubic.getCtrlX1();
            coords[1] = (float) this.cubic.getCtrlY1();
            coords[2] = (float) this.cubic.getCtrlX2();
            coords[3] = (float) this.cubic.getCtrlY2();
            coords[4] = (float) this.cubic.getX2();
            coords[5] = (float) this.cubic.getY2();
            type = PathIterator.SEG_CUBICTO;
        }
        if (this.affine != null) {
            int n;
            if (this.index == 0) {
                n = 1;
            } else {
                n = 3;
            }
            this.affine.transform(coords, 0, coords, 0, n);
        }
        return type;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("cubic iterator iterator out of bounds");
        }
        int type;
        if (this.index == 0) {
            coords[0] = this.cubic.getX1();
            coords[1] = this.cubic.getY1();
            type = PathIterator.SEG_MOVETO;
        } else {
            coords[0] = this.cubic.getCtrlX1();
            coords[1] = this.cubic.getCtrlY1();
            coords[2] = this.cubic.getCtrlX2();
            coords[3] = this.cubic.getCtrlY2();
            coords[4] = this.cubic.getX2();
            coords[5] = this.cubic.getY2();
            type = PathIterator.SEG_CUBICTO;
        }
        if (this.affine != null) {
            int n;
            if (this.index == 0) {
                n = 1;
            } else {
                n = 3;
            }
            this.affine.transform(coords, 0, coords, 0, n);
        }
        return type;
    }
}
