package java.awt.geom;

// java.awt.geom.Arc2D de KajiLibrary -- un sector de la elipse inscripta en el marco. Superficie
// completa.
//
// Convencion de angulos, que es la trampa de esta clase: se miden en **grados**, crecen en sentido
// antihorario y el 0 esta a la derecha; pero el eje y de la pantalla apunta hacia abajo, asi que
// para pasar a radianes hay que **negar** (`Math.toRadians(-angulo)`). Ademas, en una elipse no
// circular el angulo **no** es el angulo geometrico del punto: la spec lo define sobre el circulo
// unitario antes de estirar por el ancho y el alto. Por eso `setAngles` sesga los atan2 por w y h.
//
// El tipo (OPEN, CHORD, PIE) cambia que figura cierra el arco, y con eso cambian `contains`,
// `intersects` y el camino que emite el iterador.
public abstract class Arc2D extends RectangularShape {

    /** El arco queda abierto: no se agrega ningun segmento de cierre. */
    public static final int OPEN = 0;

    /** El arco se cierra con la cuerda que une sus dos extremos. */
    public static final int CHORD = 1;

    /** El arco se cierra pasando por el centro: una porcion de torta. */
    public static final int PIE = 2;

    // Arco con coordenadas float.
    public static class Float extends Arc2D implements java.io.Serializable {

        public float x;
        public float y;
        public float width;
        public float height;
        public float start;
        public float extent;

        public Float() {
            super(OPEN);
        }

        public Float(int type) {
            super(type);
        }

        public Float(float x, float y, float w, float h, float start, float extent, int type) {
            super(type);
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.start = start;
            this.extent = extent;
        }

        public Float(Rectangle2D ellipseBounds, float start, float extent, int type) {
            super(type);
            this.x = (float) ellipseBounds.getX();
            this.y = (float) ellipseBounds.getY();
            this.width = (float) ellipseBounds.getWidth();
            this.height = (float) ellipseBounds.getHeight();
            this.start = start;
            this.extent = extent;
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

        public double getAngleStart() {
            return (double) this.start;
        }

        public double getAngleExtent() {
            return (double) this.extent;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0 || this.height <= 0.0);
        }

        public void setArc(double x, double y, double w, double h,
                           double angSt, double angExt, int closure) {
            this.setArcType(closure);
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
            this.start = (float) angSt;
            this.extent = (float) angExt;
        }

        public void setAngleStart(double angSt) {
            this.start = (float) angSt;
        }

        public void setAngleExtent(double angExt) {
            this.extent = (float) angExt;
        }

        protected Rectangle2D makeBounds(double x, double y, double w, double h) {
            return Rectangle2D.newFloat((float) x, (float) y, (float) w, (float) h);
        }
    }

    // Arco con coordenadas double.
    public static class Double extends Arc2D implements java.io.Serializable {

        public double x;
        public double y;
        public double width;
        public double height;
        public double start;
        public double extent;

        public Double() {
            super(OPEN);
        }

        public Double(int type) {
            super(type);
        }

        public Double(double x, double y, double w, double h,
                      double start, double extent, int type) {
            super(type);
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.start = start;
            this.extent = extent;
        }

        public Double(Rectangle2D ellipseBounds, double start, double extent, int type) {
            super(type);
            this.x = ellipseBounds.getX();
            this.y = ellipseBounds.getY();
            this.width = ellipseBounds.getWidth();
            this.height = ellipseBounds.getHeight();
            this.start = start;
            this.extent = extent;
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

        public double getAngleStart() {
            return this.start;
        }

        public double getAngleExtent() {
            return this.extent;
        }

        public boolean isEmpty() {
            return (this.width <= 0.0 || this.height <= 0.0);
        }

        public void setArc(double x, double y, double w, double h,
                           double angSt, double angExt, int closure) {
            this.setArcType(closure);
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.start = angSt;
            this.extent = angExt;
        }

        public void setAngleStart(double angSt) {
            this.start = angSt;
        }

        public void setAngleExtent(double angExt) {
            this.extent = angExt;
        }

        protected Rectangle2D makeBounds(double x, double y, double w, double h) {
            return Rectangle2D.newDouble(x, y, w, h);
        }
    }

    private int type;

    protected Arc2D() {
        this(OPEN);
    }

    protected Arc2D(int type) {
        setArcType(type);
    }

    public abstract double getAngleStart();

    public abstract double getAngleExtent();

    public abstract void setAngleStart(double angSt);

    public abstract void setAngleExtent(double angExt);

    public abstract void setArc(double x, double y, double w, double h,
                                double angSt, double angExt, int closure);

    protected abstract Rectangle2D makeBounds(double x, double y, double w, double h);

    public int getArcType() {
        return this.type;
    }

    public void setArcType(int type) {
        if (type < OPEN || type > PIE) {
            throw new IllegalArgumentException("invalid type for Arc: " + type);
        }
        this.type = type;
    }

    public Point2D getStartPoint() {
        double angle = Math.toRadians(-getAngleStart());
        double x = getX() + (Math.cos(angle) * 0.5 + 0.5) * getWidth();
        double y = getY() + (Math.sin(angle) * 0.5 + 0.5) * getHeight();
        return Point2D.newDouble(x, y);
    }

    public Point2D getEndPoint() {
        double angle = Math.toRadians(-getAngleStart() - getAngleExtent());
        double x = getX() + (Math.cos(angle) * 0.5 + 0.5) * getWidth();
        double y = getY() + (Math.sin(angle) * 0.5 + 0.5) * getHeight();
        return Point2D.newDouble(x, y);
    }

    public void setArc(Point2D loc, Dimension2D size, double angSt, double angExt, int closure) {
        setArc(loc.getX(), loc.getY(), size.getWidth(), size.getHeight(), angSt, angExt, closure);
    }

    public void setArc(Rectangle2D rect, double angSt, double angExt, int closure) {
        setArc(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(),
               angSt, angExt, closure);
    }

    public void setArc(Arc2D a) {
        setArc(a.getX(), a.getY(), a.getWidth(), a.getHeight(),
               a.getAngleStart(), a.getAngleExtent(), a.type);
    }

    public void setArcByCenter(double x, double y, double radius,
                               double angSt, double angExt, int closure) {
        setArc(x - radius, y - radius, radius * 2.0, radius * 2.0, angSt, angExt, closure);
    }

    // Arco de radio dado tangente a los dos segmentos p1-p2 y p2-p3. El centro cae sobre la
    // bisectriz del angulo en p2, a distancia radius/sin(mitad del angulo).
    public void setArcByTangent(Point2D p1, Point2D p2, Point2D p3, double radius) {
        double ang1 = Math.atan2(p1.getY() - p2.getY(), p1.getX() - p2.getX());
        double ang2 = Math.atan2(p3.getY() - p2.getY(), p3.getX() - p2.getX());
        double diff = ang2 - ang1;
        if (diff > Math.PI) {
            ang2 = ang2 - Math.PI * 2.0;
        } else if (diff < -Math.PI) {
            ang2 = ang2 + Math.PI * 2.0;
        }
        double bisect = (ang1 + ang2) / 2.0;
        double theta = Math.abs(ang2 - bisect);
        double dist = radius / Math.sin(theta);
        double x = p2.getX() + dist * Math.cos(bisect);
        double y = p2.getY() + dist * Math.sin(bisect);
        // Los puntos de tangencia estan a 90 grados de las direcciones de los lados.
        if (ang1 < ang2) {
            ang1 = ang1 - Math.PI / 2.0;
            ang2 = ang2 + Math.PI / 2.0;
        } else {
            ang1 = ang1 + Math.PI / 2.0;
            ang2 = ang2 - Math.PI / 2.0;
        }
        ang1 = Math.toDegrees(-ang1);
        ang2 = Math.toDegrees(-ang2);
        setArcByCenter(x, y, radius, ang1, ang2 - ang1, this.type);
    }

    // El angulo se sesga por el alto y el ancho porque en la elipse el parametro no es el angulo
    // geometrico.
    public void setAngleStart(Point2D p) {
        double dx = getHeight() * (p.getX() - getCenterX());
        double dy = getWidth() * (p.getY() - getCenterY());
        setAngleStart(-Math.toDegrees(Math.atan2(dy, dx)));
    }

    public void setAngles(double x1, double y1, double x2, double y2) {
        double x = getCenterX();
        double y = getCenterY();
        double w = getWidth();
        double h = getHeight();
        // Se invierte la y para compensar el eje hacia abajo, y se sesga por w/h.
        double ang1 = Math.atan2(w * (y - y1), h * (x1 - x));
        double ang2 = Math.atan2(w * (y - y2), h * (x2 - x));
        ang2 = ang2 - ang1;
        if (ang2 <= 0.0) {
            ang2 = ang2 + Math.PI * 2.0;
        }
        setAngleStart(Math.toDegrees(ang1));
        setAngleExtent(Math.toDegrees(ang2));
    }

    public void setAngles(Point2D p1, Point2D p2) {
        setAngles(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public void setFrame(double x, double y, double w, double h) {
        setArc(x, y, w, h, getAngleStart(), getAngleExtent(), this.type);
    }

    // La caja del arco no es la del marco: hay que mirar solo los cuadrantes que el arco recorre,
    // mas sus dos extremos, mas el centro si es PIE.
    public Rectangle2D getBounds2D() {
        if (isEmpty()) {
            return makeBounds(getX(), getY(), getWidth(), getHeight());
        }
        double x1;
        double y1;
        double x2;
        double y2;
        if (getArcType() == PIE) {
            x1 = 0.0;
            y1 = 0.0;
            x2 = 0.0;
            y2 = 0.0;
        } else {
            x1 = 1.0;
            y1 = 1.0;
            x2 = -1.0;
            y2 = -1.0;
        }
        double angle = 0.0;
        int i = 0;
        while (i < 6) {
            if (i < 4) {
                // 0..3: los cuatro cuadrantes
                angle = angle + 90.0;
                if (!containsAngle(angle)) {
                    i = i + 1;
                    continue;
                }
            } else if (i == 4) {
                angle = getAngleStart();
            } else {
                angle = angle + getAngleExtent();
            }
            double rads = Math.toRadians(-angle);
            double xe = Math.cos(rads);
            double ye = Math.sin(rads);
            x1 = Math.min(x1, xe);
            y1 = Math.min(y1, ye);
            x2 = Math.max(x2, xe);
            y2 = Math.max(y2, ye);
            i = i + 1;
        }
        double w = getWidth();
        double h = getHeight();
        x2 = (x2 - x1) * 0.5 * w;
        y2 = (y2 - y1) * 0.5 * h;
        x1 = getX() + (x1 * 0.5 + 0.5) * w;
        y1 = getY() + (y1 * 0.5 + 0.5) * h;
        return makeBounds(x1, y1, x2, y2);
    }

    // Lleva un angulo al rango (-180, 180]. IEEEremainder puede devolver exactamente -180 para
    // ciertas entradas y ahi hay que corregirlo a mano.
    static double normalizeDegrees(double angle) {
        if (angle > 180.0) {
            if (angle <= (180.0 + 360.0)) {
                angle = angle - 360.0;
            } else {
                angle = Math.IEEEremainder(angle, 360.0);
                if (angle == -180.0) {
                    angle = 180.0;
                }
            }
        } else if (angle <= -180.0) {
            if (angle > (-180.0 - 360.0)) {
                angle = angle + 360.0;
            } else {
                angle = Math.IEEEremainder(angle, 360.0);
                if (angle == -180.0) {
                    angle = 180.0;
                }
            }
        }
        return angle;
    }

    public boolean containsAngle(double angle) {
        double angExt = getAngleExtent();
        boolean backwards = (angExt < 0.0);
        if (backwards) {
            angExt = -angExt;
        }
        if (angExt >= 360.0) {
            return true;
        }
        angle = normalizeDegrees(angle) - normalizeDegrees(getAngleStart());
        if (backwards) {
            angle = -angle;
        }
        if (angle < 0.0) {
            angle = angle + 360.0;
        }
        return (angle >= 0.0) && (angle < angExt);
    }

    public boolean contains(double x, double y) {
        // Primero: ¿esta dentro de la elipse? (normalizado a circulo de radio 0.5)
        double ellw = getWidth();
        if (ellw <= 0.0) {
            return false;
        }
        double normx = (x - getX()) / ellw - 0.5;
        double ellh = getHeight();
        if (ellh <= 0.0) {
            return false;
        }
        double normy = (y - getY()) / ellh - 0.5;
        double distSq = (normx * normx + normy * normy);
        if (distSq >= 0.25) {
            return false;
        }
        double angExt = Math.abs(getAngleExtent());
        if (angExt >= 360.0) {
            return true;
        }
        boolean inarc = containsAngle(-Math.toDegrees(Math.atan2(normy, normx)));
        if (this.type == PIE) {
            return inarc;
        }
        // CHORD y OPEN encierran la misma region: la limitada por la cuerda.
        if (inarc) {
            if (angExt >= 180.0) {
                return true;
            }
            // el punto tiene que quedar **afuera** del triangulo centro-extremos
        } else {
            if (angExt <= 180.0) {
                return false;
            }
            // el punto tiene que quedar **adentro** de ese triangulo
        }
        double angle = Math.toRadians(-getAngleStart());
        double x1 = Math.cos(angle);
        double y1 = Math.sin(angle);
        angle = angle + Math.toRadians(-getAngleExtent());
        double x2 = Math.cos(angle);
        double y2 = Math.sin(angle);
        boolean inside = (Line2D.relativeCCW(x1, y1, x2, y2, 2 * normx, 2 * normy)
                          * Line2D.relativeCCW(x1, y1, x2, y2, 0, 0) >= 0);
        if (inarc) {
            return !inside;
        }
        return inside;
    }

    public boolean intersects(double x, double y, double w, double h) {
        double aw = getWidth();
        double ah = getHeight();
        if (w <= 0 || h <= 0 || aw <= 0 || ah <= 0) {
            return false;
        }
        double ext = getAngleExtent();
        if (ext == 0) {
            return false;
        }
        double ax = getX();
        double ay = getY();
        double axw = ax + aw;
        double ayh = ay + ah;
        double xw = x + w;
        double yh = y + h;
        if (x >= axw || y >= ayh || xw <= ax || yh <= ay) {
            return false;
        }
        double axc = getCenterX();
        double ayc = getCenterY();
        Point2D sp = getStartPoint();
        Point2D ep = getEndPoint();
        double sx = sp.getX();
        double sy = sp.getY();
        double ex = ep.getX();
        double ey = ep.getY();

        // Casos que se escapan del rectangulo formado por el centro y los dos extremos: el arco
        // sobresale por uno de los cuatro puntos cardinales.
        if (ayc >= y && ayc <= yh) {
            if ((sx < xw && ex < xw && axc < xw && axw > x && containsAngle(0))
                || (sx > x && ex > x && axc > x && ax < xw && containsAngle(180))) {
                return true;
            }
        }
        if (axc >= x && axc <= xw) {
            if ((sy > y && ey > y && ayc > y && ay < yh && containsAngle(90))
                || (sy < yh && ey < yh && ayc < yh && ayh > y && containsAngle(270))) {
                return true;
            }
        }

        Rectangle2D rect = Rectangle2D.newDouble(x, y, w, h);
        if (this.type == PIE || Math.abs(ext) > 180) {
            // Con mas de media vuelta, o con PIE, la figura es concava: hay que probar los dos
            // radios y no la cuerda.
            if (rect.intersectsLine(axc, ayc, sx, sy) || rect.intersectsLine(axc, ayc, ex, ey)) {
                return true;
            }
        } else {
            if (rect.intersectsLine(sx, sy, ex, ey)) {
                return true;
            }
        }

        return contains(x, y) || contains(x + w, y)
                || contains(x, y + h) || contains(x + w, y + h);
    }

    public boolean contains(double x, double y, double w, double h) {
        return containsRect(x, y, w, h, null);
    }

    public boolean contains(Rectangle2D r) {
        return containsRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), r);
    }

    // Las cuatro esquinas adentro alcanzan mientras la figura sea convexa. Un PIE de mas de 180
    // grados es concavo: puede tener las cuatro esquinas adentro y aun asi un lado del rectangulo
    // cruzando la "cuña que falta", asi que hay que probar los dos radios aparte.
    private boolean containsRect(double x, double y, double w, double h, Rectangle2D origrect) {
        if (!(contains(x, y) && contains(x + w, y)
              && contains(x, y + h) && contains(x + w, y + h))) {
            return false;
        }
        if (this.type != PIE || Math.abs(getAngleExtent()) <= 180.0) {
            return true;
        }
        if (origrect == null) {
            origrect = Rectangle2D.newDouble(x, y, w, h);
        }
        double halfW = getWidth() / 2.0;
        double halfH = getHeight() / 2.0;
        double xc = getX() + halfW;
        double yc = getY() + halfH;
        double angle = Math.toRadians(-getAngleStart());
        double xe = xc + halfW * Math.cos(angle);
        double ye = yc + halfH * Math.sin(angle);
        if (origrect.intersectsLine(xc, yc, xe, ye)) {
            return false;
        }
        angle = angle + Math.toRadians(-getAngleExtent());
        xe = xc + halfW * Math.cos(angle);
        ye = yc + halfH * Math.sin(angle);
        return !origrect.intersectsLine(xc, yc, xe, ye);
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new ArcIterator(this, at);
    }

    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits + java.lang.Double.doubleToLongBits(getY()) * 37L;
        bits = bits + java.lang.Double.doubleToLongBits(getWidth()) * 43L;
        bits = bits + java.lang.Double.doubleToLongBits(getHeight()) * 47L;
        bits = bits + java.lang.Double.doubleToLongBits(getAngleStart()) * 53L;
        bits = bits + java.lang.Double.doubleToLongBits(getAngleExtent()) * 59L;
        bits = bits + getArcType() * 61L;
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Arc2D) {
            Arc2D a2d = (Arc2D) obj;
            return ((getX() == a2d.getX())
                    && (getY() == a2d.getY())
                    && (getWidth() == a2d.getWidth())
                    && (getHeight() == a2d.getHeight())
                    && (getAngleStart() == a2d.getAngleStart())
                    && (getAngleExtent() == a2d.getAngleExtent())
                    && (getArcType() == a2d.getArcType()));
        }
        return false;
    }
}
