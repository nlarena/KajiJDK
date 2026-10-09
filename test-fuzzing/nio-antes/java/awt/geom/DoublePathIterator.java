package java.awt.geom;

import java.util.NoSuchElementException;

// Path2D.Double's internal iterator (not API). Same mechanics as FloatPathIterator; the
// explanation of the size table and of why the field is typed as Path2D lives there.
class DoublePathIterator implements PathIterator {

    Path2D path;
    double[] coordsRef;
    AffineTransform affine;
    int typeIdx;
    int pointIdx;

    DoublePathIterator(Path2D p2dd, AffineTransform at) {
        this.path = p2dd;
        this.coordsRef = p2dd.doubleCoordsRef();
        this.affine = at;
    }

    public int getWindingRule() {
        return this.path.getWindingRule();
    }

    public boolean isDone() {
        return (this.typeIdx >= this.path.numTypes);
    }

    public void next() {
        int type = this.path.pointTypes[this.typeIdx];
        this.typeIdx = this.typeIdx + 1;
        this.pointIdx = this.pointIdx + FloatPathIterator.CURVESIZE[type];
    }

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("path iterator out of bounds");
        }
        int type = this.path.pointTypes[this.typeIdx];
        int numCoords = FloatPathIterator.CURVESIZE[type];
        if (numCoords > 0) {
            if (this.affine == null) {
                int i = 0;
                while (i < numCoords) {
                    coords[i] = (float) this.coordsRef[this.pointIdx + i];
                    i = i + 1;
                }
            } else {
                // It is transformed in double and the transform narrows to float on writing: the
                // other way round would round twice.
                this.affine.transform(this.coordsRef, this.pointIdx, coords, 0, numCoords / 2);
            }
        }
        return type;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("path iterator out of bounds");
        }
        int type = this.path.pointTypes[this.typeIdx];
        int numCoords = FloatPathIterator.CURVESIZE[type];
        if (numCoords > 0) {
            if (this.affine == null) {
                System.arraycopy(this.coordsRef, this.pointIdx, coords, 0, numCoords);
            } else {
                this.affine.transform(this.coordsRef, this.pointIdx, coords, 0, numCoords / 2);
            }
        }
        return type;
    }
}
