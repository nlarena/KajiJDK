package java.awt.geom;

// A Bezier curve's **tight** box: the smallest one containing it, not its control polygon's.
//
// Both are valid bounds according to `Shape.getBounds2D`, so the contract does not force the choice
// -- the JDK does, and it returns the tight one. It was checked by running the same case with the
// real `java`: for the cubic (0,0),(0,10),(10,10),(10,0) it returns height 7.5, not 10. The control
// polygon's box is easier --it comes out of four minima and four maxima, solving nothing-- and it
// is what was here until the behaviour test did not match.
//
// How it is worked out: a Bezier's coordinate is a polynomial in `t`, and its extrema over [0,1]
// are at the ends or where the derivative vanishes. So the box is the curve's two ends plus the
// value at each root of the derivative falling **inside** the open interval (0,1). The roots
// outside do not count: they describe extrema of the extended curve, which is not this curve.
final class CurveBounds {

    private CurveBounds() {
    }

    /**
     * A cubic's coordinate minimum and maximum, as an array of two.
     *
     * <p>A cubic's derivative is a quadratic: `at^2 + bt + c` with `a = 3(-p0+3p1-3p2+p3)`,
     * `b = 6(p0-2p1+p2)` and `c = 3(p1-p0)`.
     */
    static double[] cubic(double p0, double p1, double p2, double p3) {
        double min = Math.min(p0, p3);
        double max = Math.max(p0, p3);
        double a = 3.0 * (-p0 + 3.0 * p1 - 3.0 * p2 + p3);
        double b = 6.0 * (p0 - 2.0 * p1 + p2);
        double c = 3.0 * (p1 - p0);
        double[] roots = CurveBounds.quadraticRoots(a, b, c);
        for (int i = 0; i < roots.length; i++) {
            double t = roots[i];
            if (t <= 0.0 || t >= 1.0) {
                continue;
            }
            double v = CurveBounds.cubicAt(p0, p1, p2, p3, t);
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return new double[] { min, max };
    }

    /**
     * The same for a quadratic.
     *
     * <p>Its derivative is linear, so there is at most one interior extremum:
     * `t = (p0-p1)/(p0-2p1+p2)`. The denominator is zero when the three points are aligned in this
     * coordinate, and there the curve is monotonic: the extrema are the ends and there is nothing to
     * add.
     */
    static double[] quad(double p0, double p1, double p2) {
        double min = Math.min(p0, p2);
        double max = Math.max(p0, p2);
        double den = p0 - 2.0 * p1 + p2;
        if (den != 0.0) {
            double t = (p0 - p1) / den;
            if (t > 0.0 && t < 1.0) {
                double v = CurveBounds.quadAt(p0, p1, p2, t);
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }
        return new double[] { min, max };
    }

    // The Bernstein form, and not the expanded power one, because it is the numerically stable
    // one: each term is a product of factors bounded in [0,1] and there are no subtractions of large
    // numbers.
    private static double cubicAt(double p0, double p1, double p2, double p3, double t) {
        double u = 1.0 - t;
        return u * u * u * p0 + 3.0 * u * u * t * p1 + 3.0 * u * t * t * p2 + t * t * t * p3;
    }

    private static double quadAt(double p0, double p1, double p2, double t) {
        double u = 1.0 - t;
        return u * u * p0 + 2.0 * u * t * p1 + t * t * p2;
    }

    // The real roots of `at^2 + bt + c`, unordered.
    //
    // The `a == 0` case is no curiosity: it happens whenever the cubic degrades to a quadratic,
    // which is one of the commonest ways of writing it. Treating it as a quadratic would divide by
    // zero.
    private static double[] quadraticRoots(double a, double b, double c) {
        if (a == 0.0) {
            if (b == 0.0) {
                return new double[0];
            }
            return new double[] { -c / b };
        }
        double disc = b * b - 4.0 * a * c;
        if (disc < 0.0) {
            return new double[0];
        }
        if (disc == 0.0) {
            return new double[] { -b / (2.0 * a) };
        }
        // The stable form: the root that does **not** cancel is worked out and the other comes
        // from the product of the roots (`c/a`). With the usual formula, when `b` is large next to
        // `4ac` one of the two subtracts two nearly equal numbers and loses nearly every digit.
        double sq = Math.sqrt(disc);
        double q = b >= 0.0 ? -0.5 * (b + sq) : -0.5 * (b - sq);
        return new double[] { q / a, c / q };
    }
}
