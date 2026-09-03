package java.awt.geom;

// java.awt.geom.RoundRectangle2D de KajiLibrary -- rectangulo con esquinas redondeadas.
// Superficie completa.
//
// El ancho y el alto del arco se recortan al ancho y alto del marco (`Math.min`) y se toman en valor
// absoluto: un arco mas grande que el rectangulo no tiene sentido y uno negativo tampoco.
public abstract class RoundRectangle2D extends RectangularShape {

    // Rectangulo redondeado con coordenadas float.
    public static class Float extends RoundRectangle2D implements java.io.Serializable {

        public float x;
        public float y;
        public float width;
        public float height;
        public float arcwidth;
        public float archeight;

        public Float() {
        }

        public Float(float x, float y, float w, float h, float arcw, float arch) {
            setRoundRect(x, y, w, h, arcw, arch);
        }

        public double getX() {
            return (double) this.x;
        }

        public double getY() {
            return (double) this.y;
        }

        public double getWidth() {
            return (double) this.width;
        }

        public double getHeight() {
            return (double) this.height;
        }

        public double getArcWidth() {
            return (double) this.arcwidth;
        }

        public double getArcHeight() {
            return (double) this.archeight;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0f) || (this.height <= 0.0f);
        }

        public void setRoundRect(float x, float y, float w, float h, float arcw, float arch) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
        }

        public void setRoundRect(double x, double y, double w, double h,
                                 double arcw, double arch) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
            this.arcwidth = (float) arcw;
            this.archeight = (float) arch;
        }

        public void setRoundRect(RoundRectangle2D rr) {
            this.x = (float) rr.getX();
            this.y = (float) rr.getY();
            this.width = (float) rr.getWidth();
            this.height = (float) rr.getHeight();
            this.arcwidth = (float) rr.getArcWidth();
            this.archeight = (float) rr.getArcHeight();
        }

        public Rectangle2D getBounds2D() {
            return Rectangle2D.newFloat(this.x, this.y, this.width, this.height);
        }
    }

    // Rectangulo redondeado con coordenadas double.
    public static class Double extends RoundRectangle2D implements java.io.Serializable {

        public double x;
        public double y;
        public double width;
        public double height;
        public double arcwidth;
        public double archeight;

        public Double() {
        }

        public Double(double x, double y, double w, double h, double arcw, double arch) {
            setRoundRect(x, y, w, h, arcw, arch);
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public double getWidth() {
            return this.width;
        }

        public double getHeight() {
            return this.height;
        }

        public double getArcWidth() {
            return this.arcwidth;
        }

        public double getArcHeight() {
            return this.archeight;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0) || (this.height <= 0.0);
        }

        public void setRoundRect(double x, double y, double w, double h,
                                 double arcw, double arch) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.arcwidth = arcw;
            this.archeight = arch;
        }

        public void setRoundRect(RoundRectangle2D rr) {
            this.x = rr.getX();
            this.y = rr.getY();
            this.width = rr.getWidth();
            this.height = rr.getHeight();
            this.arcwidth = rr.getArcWidth();
            this.archeight = rr.getArcHeight();
        }

        public Rectangle2D getBounds2D() {
            return Rectangle2D.newDouble(this.x, this.y, this.width, this.height);
        }
    }

    protected RoundRectangle2D() {
    }

    public abstract double getArcWidth();

    public abstract double getArcHeight();

    public abstract void setRoundRect(double x, double y, double w, double h,
                                      double arcWidth, double arcHeight);

    public void setRoundRect(RoundRectangle2D rr) {
        setRoundRect(rr.getX(), rr.getY(), rr.getWidth(), rr.getHeight(),
                     rr.getArcWidth(), rr.getArcHeight());
    }

    // Cambiar el marco conserva el redondeo.
    public void setFrame(double x, double y, double w, double h) {
        setRoundRect(x, y, w, h, getArcWidth(), getArcHeight());
    }

    // Rechazo rapido por el marco; si el punto cae en la banda central (horizontal o vertical) esta
    // adentro sin mas; si no, cae en una esquina y hay que probarlo contra el cuarto de elipse.
    public boolean contains(double x, double y) {
        if (isEmpty()) {
            return false;
        }
        double rrx0 = getX();
        double rry0 = getY();
        double rrx1 = rrx0 + getWidth();
        double rry1 = rry0 + getHeight();
        if (x < rrx0 || y < rry0 || x >= rrx1 || y >= rry1) {
            return false;
        }
        double aw = Math.min(getWidth(), Math.abs(getArcWidth())) / 2.0;
        double ah = Math.min(getHeight(), Math.abs(getArcHeight())) / 2.0;
        double cx;
        double cy;
        double xLo = rrx0 + aw;
        double xHi = rrx1 - aw;
        if (x < xLo) {
            cx = xLo;
        } else if (x < xHi) {
            return true;
        } else {
            cx = xHi;
        }
        double yLo = rry0 + ah;
        double yHi = rry1 - ah;
        if (y < yLo) {
            cy = yLo;
        } else if (y < yHi) {
            return true;
        } else {
            cy = yHi;
        }
        double nx = (x - cx) / aw;
        double ny = (y - cy) / ah;
        return (nx * nx + ny * ny <= 1.0);
    }

    // Ubica una coordenada en una de cinco franjas: 0 = antes del marco, 1 = en el arco de este
    // lado, 2 = en el rectangulo interior, 3 = en el arco del otro lado, 4 = pasado el marco.
    private int classify(double coord, double left, double right, double arcsize) {
        if (coord < left) {
            return 0;
        }
        if (coord < left + arcsize) {
            return 1;
        }
        if (coord < right - arcsize) {
            return 2;
        }
        if (coord < right) {
            return 3;
        }
        return 4;
    }

    public boolean intersects(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        double rrx0 = getX();
        double rry0 = getY();
        double rrx1 = rrx0 + getWidth();
        double rry1 = rry0 + getHeight();
        if (x + w <= rrx0 || x >= rrx1 || y + h <= rry0 || y >= rry1) {
            return false;
        }
        double aw = Math.min(getWidth(), Math.abs(getArcWidth())) / 2.0;
        double ah = Math.min(getHeight(), Math.abs(getArcHeight())) / 2.0;
        int x0class = classify(x, rrx0, rrx1, aw);
        int x1class = classify(x + w, rrx0, rrx1, aw);
        int y0class = classify(y, rry0, rry1, ah);
        int y1class = classify(y + h, rry0, rry1, ah);
        // Si algun borde cae en el rectangulo interior, hay interseccion seguro.
        if (x0class == 2 || x1class == 2 || y0class == 2 || y1class == 2) {
            return true;
        }
        // O si algun lado lo cruza de lado a lado.
        if ((x0class < 2 && x1class > 2) || (y0class < 2 && y1class > 2)) {
            return true;
        }
        // Si no, alguna esquina del rectangulo cae en alguna esquina redondeada: se prueba el punto
        // mas cercano contra el cuarto de elipse.
        double nx;
        double ny;
        if (x1class == 1) {
            nx = x + w - (rrx0 + aw);
        } else {
            nx = x - (rrx1 - aw);
        }
        if (y1class == 1) {
            ny = y + h - (rry0 + ah);
        } else {
            ny = y - (rry1 - ah);
        }
        nx = nx / aw;
        ny = ny / ah;
        return (nx * nx + ny * ny <= 1.0);
    }

    // La figura es convexa: alcanza con las cuatro esquinas.
    public boolean contains(double x, double y, double w, double h) {
        if (isEmpty() || w <= 0 || h <= 0) {
            return false;
        }
        return (contains(x, y)
                && contains(x + w, y)
                && contains(x, y + h)
                && contains(x + w, y + h));
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new RoundRectIterator(this, at);
    }

    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits + java.lang.Double.doubleToLongBits(getY()) * 37L;
        bits = bits + java.lang.Double.doubleToLongBits(getWidth()) * 43L;
        bits = bits + java.lang.Double.doubleToLongBits(getHeight()) * 47L;
        bits = bits + java.lang.Double.doubleToLongBits(getArcWidth()) * 53L;
        bits = bits + java.lang.Double.doubleToLongBits(getArcHeight()) * 59L;
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RoundRectangle2D) {
            RoundRectangle2D rr2d = (RoundRectangle2D) obj;
            return ((getX() == rr2d.getX())
                    && (getY() == rr2d.getY())
                    && (getWidth() == rr2d.getWidth())
                    && (getHeight() == rr2d.getHeight())
                    && (getArcWidth() == rr2d.getArcWidth())
                    && (getArcHeight() == rr2d.getArcHeight()));
        }
        return false;
    }
}
