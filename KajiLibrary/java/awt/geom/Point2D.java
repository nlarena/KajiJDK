package java.awt.geom;

// java.awt.geom.Point2D de KajiLibrary -- un punto (x,y) en coma flotante. Superficie completa.
//
// Ojo al leer este archivo: dentro del cuerpo de Point2D, `Double` y `Float` son las clases
// anidadas de aca, no las de java.lang. Por eso todo uso del envoltorio va escrito completo
// (`java.lang.Double.doubleToLongBits`). No es manía: es la regla de sombreado de §6.5.5 y el
// compilador la aplica.
//
// Y por eso existen las fabricas `newDouble`/`newFloat`/`newLike` del final, que son internas y no
// API. El resto del paquete las usa en vez de escribir `new Point2D.Double(...)` porque el javac de
// esta casa **no puede tener los dos nombres `Double` vivos en la misma unidad de compilacion**:
// mantiene un solo mapa nombre-simple -> tipo por archivo, asi que un archivo que nombre a la vez
// `Point2D.Double` y `java.lang.Double` resuelve mal uno de los dos (da "tipo incompatible" o
// "no se encuentra el campo MIN_VALUE"). Ese choque afectaba a AffineTransform, Arc2D, Line2D y
// Path2D, que necesitan las dos cosas. Aca adentro no hay choque --`Double` a secas ya es la
// anidada-- asi que la construccion vive aca y los demas archivos nunca nombran a `Point2D.Double`.
// Es un rodeo por un bug del compilador, no por la especificacion del lenguaje: contra el javac del
// JDK las dos formas son igual de validas.
public abstract class Point2D implements Cloneable {

    // Punto con coordenadas float. Las guarda en float pero las expone en double, que es lo que
    // pide el contrato de la clase base.
    public static class Float extends Point2D implements java.io.Serializable {

        public float x;
        public float y;

        public Float() {
        }

        public Float(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return (double) this.x;
        }

        public double getY() {
            return (double) this.y;
        }

        public void setLocation(double x, double y) {
            this.x = (float) x;
            this.y = (float) y;
        }

        public void setLocation(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return "Point2D.Float[" + this.x + ", " + this.y + "]";
        }
    }

    // Punto con coordenadas double.
    public static class Double extends Point2D implements java.io.Serializable {

        public double x;
        public double y;

        public Double() {
        }

        public Double(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return this.x;
        }

        public double getY() {
            return this.y;
        }

        public void setLocation(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return "Point2D.Double[" + this.x + ", " + this.y + "]";
        }
    }

    protected Point2D() {
    }

    public abstract double getX();

    public abstract double getY();

    public abstract void setLocation(double x, double y);

    public void setLocation(Point2D p) {
        setLocation(p.getX(), p.getY());
    }

    // La distancia al cuadrado existe aparte de la distancia a proposito: evita la raiz cuando solo
    // se van a comparar distancias entre si, y ahi el resultado es exacto si los operandos lo son.
    public static double distanceSq(double x1, double y1, double x2, double y2) {
        x1 = x1 - x2;
        y1 = y1 - y2;
        return (x1 * x1 + y1 * y1);
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        x1 = x1 - x2;
        y1 = y1 - y2;
        return Math.sqrt(x1 * x1 + y1 * y1);
    }

    public double distanceSq(double px, double py) {
        px = px - getX();
        py = py - getY();
        return (px * px + py * py);
    }

    public double distanceSq(Point2D pt) {
        double px = pt.getX() - getX();
        double py = pt.getY() - getY();
        return (px * px + py * py);
    }

    public double distance(double px, double py) {
        px = px - getX();
        py = py - getY();
        return Math.sqrt(px * px + py * py);
    }

    public double distance(Point2D pt) {
        double px = pt.getX() - getX();
        double py = pt.getY() - getY();
        return Math.sqrt(px * px + py * py);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }

    // Mezcla de los bits de las dos coordenadas. Se replica la formula del JDK y no otra: hashCode
    // es observable y dos implementaciones que "hashean bien" pero distinto son distinguibles.
    public int hashCode() {
        long bits = java.lang.Double.doubleToLongBits(getX());
        bits = bits ^ (java.lang.Double.doubleToLongBits(getY()) * 31L);
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    public boolean equals(Object obj) {
        if (obj instanceof Point2D) {
            Point2D p2d = (Point2D) obj;
            return (getX() == p2d.getX()) && (getY() == p2d.getY());
        }
        return super.equals(obj);
    }

    // --- fabricas internas (no son API; ver la nota del encabezado) -------------------------------

    static Point2D newDouble(double x, double y) {
        return new Double(x, y);
    }

    static Point2D newFloat(float x, float y) {
        return new Float(x, y);
    }

    // Un punto vacio de la misma precision que `src`. Es la regla del JDK para el destino implicito
    // de AffineTransform.transform(src, null): Double solo si el origen ya era Double, Float en
    // cualquier otro caso --incluida una subclase de Point2D escrita afuera.
    static Point2D newLike(Point2D src) {
        if (src instanceof Double) {
            return new Double();
        }
        return new Float();
    }
}
