package java.awt.geom;

import java.util.NoSuchElementException;

// Line2D's internal iterator: MOVETO to the first end, LINETO to the second, and that is that. No
// CLOSE: a segment is an open shape.
class LineIterator implements PathIterator {

    Line2D line;
    AffineTransform affine;
    int index;

    LineIterator(Line2D l, AffineTransform at) {
        this.line = l;
        this.affine = at;
    }

    public int getWindingRule() {
        return PathIterator.WIND_NON_ZERO;
    }

    public boolean isDone() {
        return (index > 1);
    }

    public void next() {
        this.index = this.index + 1;
    }

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("line iterator out of bounds");
        }
        int type;
        if (this.index == 0) {
            coords[0] = (float) this.line.getX1();
            coords[1] = (float) this.line.getY1();
            type = PathIterator.SEG_MOVETO;
        } else {
            coords[0] = (float) this.line.getX2();
            coords[1] = (float) this.line.getY2();
            type = PathIterator.SEG_LINETO;
        }
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 1);
        }
        return type;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("line iterator out of bounds");
        }
        int type;
        if (this.index == 0) {
            coords[0] = this.line.getX1();
            coords[1] = this.line.getY1();
            type = PathIterator.SEG_MOVETO;
        } else {
            coords[0] = this.line.getX2();
            coords[1] = this.line.getY2();
            type = PathIterator.SEG_LINETO;
        }
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 1);
        }
        return type;
    }
}
