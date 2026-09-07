package java.awt.geom;

import java.util.NoSuchElementException;

// KajiLibrary's java.awt.geom.FlatteningPathIterator -- it wraps another PathIterator and replaces
// each curve with a chain of straight segments. The surface is complete.
//
// How the stack works: `hold` is a buffer filled **from the end towards the start**. When a piece of
// curve is not flat enough yet it is subdivided in two and the left half is written over the
// original while the right one is pushed back into the buffer; `holdIndex` points at the piece being
// looked at and `holdEnd` at the end of what is still pending. It is an explicit stack instead of
// recursion, and that is why the buffer grows (`ensureHoldCapacity`) instead of overflowing the call
// stack.
//
// Two things about the contract worth keeping in mind:
//
//   * The flattened iterator **never** returns QUADTO or CUBICTO. It returns MOVETO, LINETO and
//     CLOSE, and nothing else. That is what makes it useful: whoever consumes it needs to know
//     nothing about curves.
//
//   * `limit` is the maximum number of subdivisions **per curve**, not in total. Once exhausted the
//     segment is emitted even if it has not reached the flatness asked for. Without that cap a curve
//     with a cusp would subdivide for ever, because the flatness stops going down past a point.
//
// The constructor rejects a negative flatness and a negative limit with IllegalArgumentException:
// there is no sensible reading of "flatten with tolerance -1", and accepting it would give an
// infinite loop later, far from the mistake.
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
                // First time this curve is seen: it is copied to the end of the buffer, preceded
                // by the current point --which the source iterator does not repeat-- so as to have
                // the three control points contiguous. Six slots: (x0,y0) (xc,yc) (x1,y1).
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
                // Two curves one level deeper are left: the left one in the new slots and the
                // right one where the original was. Both are marked with the next level.
                level = level + 1;
                this.levels[this.levelIndex] = level;
                this.levelIndex = this.levelIndex + 1;
                this.levels[this.levelIndex] = level;
            }
            // The piece is flat already (or the limit ran out): its end, at holdIndex+4, is the
            // end of the segment about to be emitted.
            this.holdIndex = this.holdIndex + 4;
            this.levelIndex = this.levelIndex - 1;
        } else {
            if (this.holdIndex >= this.holdEnd) {
                // Eight slots: (x0,y0) (xc0,yc0) (xc1,yc1) (x1,y1).
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
