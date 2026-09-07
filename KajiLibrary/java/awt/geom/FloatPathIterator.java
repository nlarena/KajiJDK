package java.awt.geom;

import java.util.NoSuchElementException;

// Path2D.Float's internal iterator (not API). It walks the flat array of types and the one of
// coordinates in parallel, and applies the transform --if there is one-- on the fly, without
// copying the path.
//
// The `typeIdx` index advances one type at a time and `pointIdx` by as many coordinates as that
// type consumes, which is where the `CURVESIZE` table comes from. A CLOSE consumes no coordinates:
// that is why its entry is 0 and why `currentSegment` does not touch the caller's array when it
// returns CLOSE.
//
// A detail that looks odd and is not: the field is a `Path2D` and the coordinates are asked for
// through `floatCoordsRef()`/`doubleCoordsRef()` instead of typing the field as the nested subclass.
// This house's javac does not resolve an `Outer.Inner`'s inheritance relation while Outer has no
// .class, and this package has to be compiled in a single invocation because of the type cycles
// --so no file may depend on `Path2D.Float` being a `Path2D`. With the declaring type there is no
// lookup through inheritance and it resolves.
class FloatPathIterator implements PathIterator {

    // How many coordinates each segment type writes: MOVETO 2, LINETO 2, QUADTO 4, CUBICTO 6,
    // CLOSE 0. Indexed by the SEG_* constant's value.
    static final int[] CURVESIZE = {2, 2, 4, 6, 0};

    Path2D path;
    float[] coordsRef;
    AffineTransform affine;
    int typeIdx;
    int pointIdx;

    FloatPathIterator(Path2D p2df, AffineTransform at) {
        this.path = p2df;
        this.coordsRef = p2df.floatCoordsRef();
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
        this.pointIdx = this.pointIdx + CURVESIZE[type];
    }

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("path iterator out of bounds");
        }
        int type = this.path.pointTypes[this.typeIdx];
        int numCoords = CURVESIZE[type];
        if (numCoords > 0) {
            if (this.affine == null) {
                System.arraycopy(this.coordsRef, this.pointIdx, coords, 0, numCoords);
            } else {
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
        int numCoords = CURVESIZE[type];
        if (numCoords > 0) {
            if (this.affine == null) {
                int i = 0;
                while (i < numCoords) {
                    coords[i] = (double) this.coordsRef[this.pointIdx + i];
                    i = i + 1;
                }
            } else {
                // It is widened to double **inside** the transform, not before: transforming in
                // float and widening afterwards would round twice and give another point.
                this.affine.transform(this.coordsRef, this.pointIdx, coords, 0, numCoords / 2);
            }
        }
        return type;
    }
}
