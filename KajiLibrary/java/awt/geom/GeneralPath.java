package java.awt.geom;

import java.awt.Shape;

// KajiLibrary's java.awt.geom.GeneralPath -- a float-precision path. The public surface is
// complete: four constructors and nothing else.
//
// It adds no behaviour: it is exactly `Path2D.Float` under another name. It exists because it
// predates Path2D (it comes from Java 1.2, Path2D from 6) and there is code that names it. It is
// left `final` as in the JDK.
//
// The package-private constructor `GeneralPath(int, byte[], int, float[], int)` the JDK lists has
// nothing to honour: it is neither public nor protected, so it is not contract -- it is the door
// the internal drawing pipeline uses to build a path without copying, and that pipeline is not
// here.
// The `extends` carries the full name because of finding #465: written `Path2D.Float`, the bytecode
// generator resolves `Float` against `java.lang` instead of against `Path2D`'s members, believes the
// superclass is `java.lang.Float` and cannot find its `(int, int)` constructor. The type checker
// does resolve it right --`--check` passes and `--emit` does not-- so the file looks healthy until
// one tries to emit it. With the whole qualified name it works.
public final class GeneralPath extends java.awt.geom.Path2D.Float {

    public GeneralPath() {
        super(Path2D.WIND_NON_ZERO, Path2D.INIT_SIZE);
    }

    public GeneralPath(int rule) {
        super(rule, Path2D.INIT_SIZE);
    }

    public GeneralPath(int rule, int initialCapacity) {
        super(rule, initialCapacity);
    }

    public GeneralPath(Shape s) {
        super(s, null);
    }
}
