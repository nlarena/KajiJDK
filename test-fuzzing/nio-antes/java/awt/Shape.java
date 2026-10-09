package java.awt;

import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// KajiLibrary's java.awt.Shape.
//
// It lives in `java.awt` and not in `java.awt.geom`, but the whole geom package needs it: it is the
// type `AffineTransform.createTransformedShape` and `Path2D.createTransformedShape` return, and the
// one `new Area(Shape)` and `new GeneralPath(Shape)` take. It was written here for that dependency.
// (This note added that the rest of the parent package --Component, Graphics, Toolkit...-- had not
// been started; it has been since.)
//
// A note on `contains`: the spec's definition is "insideness" -- a point on the left or top edge
// belongs to the shape, one on the right or bottom edge does not. That asymmetry is deliberate (it
// makes adjacent shapes tile without overlapping) and it is respected throughout the package.
public interface Shape {

    public abstract Rectangle getBounds();

    public abstract Rectangle2D getBounds2D();

    public abstract boolean contains(double x, double y);

    public abstract boolean contains(Point2D p);

    public abstract boolean intersects(double x, double y, double w, double h);

    public abstract boolean intersects(Rectangle2D r);

    public abstract boolean contains(double x, double y, double w, double h);

    public abstract boolean contains(Rectangle2D r);

    public abstract PathIterator getPathIterator(AffineTransform at);

    public abstract PathIterator getPathIterator(AffineTransform at, double flatness);
}
