package java.awt.geom;

// java.awt.geom.Ellipse2D de KajiLibrary -- una elipse inscripta en su marco. Superficie completa.
//
// Todas las pruebas geometricas se hacen normalizando a un circulo de radio 0.5 centrado en el
// origen: se divide por el ancho y el alto del marco y se resta 0.5. Asi `contains` es un solo
// `x²+y² < 0.25` en vez de la ecuacion general de la elipse, y no hay que tratar aparte el caso
// circular. El `<` estricto es el que corresponde: un punto exactamente sobre el borde **no** esta
// contenido.
public abstract class Ellipse2D extends RectangularShape {

    // Elipse con coordenadas float.
    public static class Float extends Ellipse2D implements java.io.Serializable {

        public float x;
        public float y;
        public float width;
        public float height;

        public Float() {
        }

        public Float(float x, float y, float w, float h) {
            setFrame(x, y, w, h);
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

        public boolean isEmpty() {
            return (this.width <= 0.0f || this.height <= 0.0f);
        }

        public void setFrame(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public void setFrame(double x, double y, double w, double h) {
            this.x = (float) x;
            this.y = (float) y;
            this.width = (float) w;
            this.height = (float) h;
        }

        public Rectangle2D getBounds2D() {
            return Rectangle2D.newFloat(this.x, this.y, this.width, this.height);
        }
    }

    // Elipse con coordenadas double.
    public static class Double extends Ellipse2D implements java.io.Serializable {

        public double x;
        public double y;
        public double width;
        public double height;

        public Double() {
        }

        public Double(double x, double y, double w, double h) {
            setFrame(x, y, w, h);
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

        public boolean isEmpty() {
            return (this.width <= 0.0 || this.height <= 0.0);
        }

        public void setFrame(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }

        public Rectangle2D getBounds2D() {
            return Rectangle2D.newDouble(this.x, this.y, this.width, this.height);
        }
    }

    protected Ellipse2D() {
    }

    public boolean contains(double x, double y) {
        // Se lleva todo a un circulo de radio 0.5 centrado en el origen.
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
        return (normx * normx + normy * normy) < 0.25;
    }

    // Se busca el punto del rectangulo mas cercano al centro de la elipse; si ese punto cae dentro
    // del circulo unitario normalizado, hay interseccion. Probar solo las esquinas seria incorrecto:
    // un rectangulo puede cruzar la elipse sin que ninguna esquina este adentro.
    public boolean intersects(double x, double y, double w, double h) {
        if (w <= 0.0 || h <= 0.0) {
            return false;
        }
        double ellw = getWidth();
        if (ellw <= 0.0) {
            return false;
        }
        double normx0 = (x - getX()) / ellw - 0.5;
        double normx1 = normx0 + w / ellw;
        double ellh = getHeight();
        if (ellh <= 0.0) {
            return false;
        }
        double normy0 = (y - getY()) / ellh - 0.5;
        double normy1 = normy0 + h / ellh;
        double nearx;
        double neary;
        if (normx0 > 0.0) {
            // el centro queda a la izquierda del rectangulo
            nearx = normx0;
        } else if (normx1 < 0.0) {
            // el centro queda a la derecha
            nearx = normx1;
        } else {
            nearx = 0.0;
        }
        if (normy0 > 0.0) {
            neary = normy0;
        } else if (normy1 < 0.0) {
            neary = normy1;
        } else {
            neary = 0.0;
        }
        return (nearx * nearx + neary * neary) < 0.25;
    }

    // La elipse es convexa, asi que alcanza con las cuatro esquinas.
    public boolean contains(double x, double y, double w, double h) {
        return (contains(x, y)
                && contains(x + w, y)
                && contains(x, y + h)
                && contains(x + w, y + h));
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return new EllipseIterator(this, at);
    }

    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits + java.lang.Double.doubleToLongBits(getY()) * 37L;
        bits = bits + java.lang.Double.doubleToLongBits(getWidth()) * 43L;
        bits = bits + java.lang.Double.doubleToLongBits(getHeight()) * 47L;
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Ellipse2D) {
            Ellipse2D e2d = (Ellipse2D) obj;
            return ((getX() == e2d.getX())
                    && (getY() == e2d.getY())
                    && (getWidth() == e2d.getWidth())
                    && (getHeight() == e2d.getHeight()));
        }
        return false;
    }
}
