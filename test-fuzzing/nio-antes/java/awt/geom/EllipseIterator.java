package java.awt.geom;

import java.util.NoSuchElementException;

// Ellipse2D's internal iterator: four Bezier cubics, one per quadrant.
//
// The circle is not exactly representable with Beziers; the constant CTRL_VAL = 4/3*(sqrt(2)-1) is
// the one that makes the approximated quarter circle touch both ends with the right tangent and
// stay within 0.03 % of the radius in the middle. It is the value everyone uses, the JDK included,
// and **the same** one has to be used for the coordinates to agree bit for bit.
//
// The walk starts at the midpoint of the right side (x+w, y+h/2) and goes right -> down -> left ->
// up, which with the screen's y axis pointing down is clockwise.
class EllipseIterator implements PathIterator {

    double x;
    double y;
    double w;
    double h;
    AffineTransform affine;
    int index;

    EllipseIterator(Ellipse2D e, AffineTransform at) {
        this.x = e.getX();
        this.y = e.getY();
        this.w = e.getWidth();
        this.h = e.getHeight();
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

    public static final double CTRL_VAL = 0.5522847498307933;

    private static final double PCV = 0.5 + CTRL_VAL * 0.5;
    private static final double NCV = 0.5 - CTRL_VAL * 0.5;

    // Each row is a cubic in normalized [0,1]x[0,1] coordinates: {c1x, c1y, c2x, c2y, px, py}.
    private static final double[][] CTRLPTS = {
        { 1.0, PCV, PCV, 1.0, 0.5, 1.0 },
        { NCV, 1.0, 0.0, PCV, 0.0, 0.5 },
        { 0.0, NCV, NCV, 0.0, 0.5, 0.0 },
        { PCV, 0.0, 1.0, NCV, 1.0, 0.5 }
    };

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("ellipse iterator out of bounds");
        }
        if (this.index == 5) {
            return PathIterator.SEG_CLOSE;
        }
        if (this.index == 0) {
            double[] ctrls = CTRLPTS[3];
            coords[0] = (float) (this.x + ctrls[4] * this.w);
            coords[1] = (float) (this.y + ctrls[5] * this.h);
            if (this.affine != null) {
                this.affine.transform(coords, 0, coords, 0, 1);
            }
            return PathIterator.SEG_MOVETO;
        }
        double[] ctrls = CTRLPTS[this.index - 1];
        coords[0] = (float) (this.x + ctrls[0] * this.w);
        coords[1] = (float) (this.y + ctrls[1] * this.h);
        coords[2] = (float) (this.x + ctrls[2] * this.w);
        coords[3] = (float) (this.y + ctrls[3] * this.h);
        coords[4] = (float) (this.x + ctrls[4] * this.w);
        coords[5] = (float) (this.y + ctrls[5] * this.h);
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 3);
        }
        return PathIterator.SEG_CUBICTO;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("ellipse iterator out of bounds");
        }
        if (this.index == 5) {
            return PathIterator.SEG_CLOSE;
        }
        if (this.index == 0) {
            double[] ctrls = CTRLPTS[3];
            coords[0] = this.x + ctrls[4] * this.w;
            coords[1] = this.y + ctrls[5] * this.h;
            if (this.affine != null) {
                this.affine.transform(coords, 0, coords, 0, 1);
            }
            return PathIterator.SEG_MOVETO;
        }
        double[] ctrls = CTRLPTS[this.index - 1];
        coords[0] = this.x + ctrls[0] * this.w;
        coords[1] = this.y + ctrls[1] * this.h;
        coords[2] = this.x + ctrls[2] * this.w;
        coords[3] = this.y + ctrls[3] * this.h;
        coords[4] = this.x + ctrls[4] * this.w;
        coords[5] = this.y + ctrls[5] * this.h;
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 3);
        }
        return PathIterator.SEG_CUBICTO;
    }
}
