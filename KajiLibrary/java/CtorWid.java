public class CtorWid {
    public static int run() {
        Object a = new java.awt.geom.Rectangle2D.Double(5.0, 5.0, 1.0, 1.0);
        Object b = new java.awt.geom.Ellipse2D.Double(1.0, 2.0, 3.0, 4.0);
        if (!(a instanceof java.awt.geom.Rectangle2D)) return 0;
        if (!(b instanceof java.awt.geom.Ellipse2D)) return 1;
        return -1;
    }
}
