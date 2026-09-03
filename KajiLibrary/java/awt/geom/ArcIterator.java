package java.awt.geom;

import java.util.NoSuchElementException;

// Iterador interno de Arc2D. El arco se parte en tramos de a lo sumo 90 grados y cada tramo se
// aproxima con una cubica; `btan` da la longitud del brazo de control que hace que la cubica toque
// el arco en los extremos con la tangente correcta.
//
// El tipo del arco decide que va despues de las cubicas: OPEN no agrega nada, CHORD cierra (la
// cuerda sale sola del CLOSE) y PIE agrega un LINETO al centro y despues cierra.
class ArcIterator implements PathIterator {

    double x;
    double y;
    double w;
    double h;
    double angStRad;
    double increment;
    double cv;
    AffineTransform affine;
    int index;
    int arcSegs;
    int lineSegs;

    ArcIterator(Arc2D a, AffineTransform at) {
        this.w = a.getWidth() / 2;
        this.h = a.getHeight() / 2;
        this.x = a.getX() + this.w;
        this.y = a.getY() + this.h;
        this.angStRad = -Math.toRadians(a.getAngleStart());
        this.affine = at;
        double ext = -a.getAngleExtent();
        if (ext >= 360.0 || ext <= -360.0) {
            this.arcSegs = 4;
            this.increment = Math.PI / 2;
            // btan(PI/2) exacto, la misma constante que usa la elipse
            this.cv = 0.5522847498307933;
            if (ext < 0) {
                this.increment = -this.increment;
                this.cv = -this.cv;
            }
        } else {
            this.arcSegs = (int) Math.ceil(Math.abs(ext) / 90.0);
            this.increment = Math.toRadians(ext / this.arcSegs);
            this.cv = btan(this.increment);
            if (this.cv == 0) {
                this.arcSegs = 0;
            }
        }
        int t = a.getArcType();
        if (t == Arc2D.OPEN) {
            this.lineSegs = 0;
        } else if (t == Arc2D.CHORD) {
            this.lineSegs = 1;
        } else {
            this.lineSegs = 2;
        }
        if (this.w < 0 || this.h < 0) {
            this.arcSegs = -1;
            this.lineSegs = -1;
        }
    }

    public int getWindingRule() {
        return PathIterator.WIND_NON_ZERO;
    }

    public boolean isDone() {
        return this.index > this.arcSegs + this.lineSegs;
    }

    public void next() {
        this.index = this.index + 1;
    }

    // Largo del brazo de control (en unidades de radio) para una cubica que cubre `increment`
    // radianes de arco.
    private static double btan(double increment) {
        increment = increment / 2.0;
        return 4.0 / 3.0 * Math.sin(increment) / (1.0 + Math.cos(increment));
    }

    // Se calcula en double y se baja a float **antes** de transformar, no despues: la transformacion
    // de un float[] convierte a double, opera y vuelve a bajar, y el redondeo doble da otro numero.
    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("arc iterator out of bounds");
        }
        double angle = this.angStRad;
        if (this.index == 0) {
            coords[0] = (float) (this.x + Math.cos(angle) * this.w);
            coords[1] = (float) (this.y + Math.sin(angle) * this.h);
            if (this.affine != null) {
                this.affine.transform(coords, 0, coords, 0, 1);
            }
            return PathIterator.SEG_MOVETO;
        }
        if (this.index > this.arcSegs) {
            if (this.index == this.arcSegs + this.lineSegs) {
                return PathIterator.SEG_CLOSE;
            }
            coords[0] = (float) this.x;
            coords[1] = (float) this.y;
            if (this.affine != null) {
                this.affine.transform(coords, 0, coords, 0, 1);
            }
            return PathIterator.SEG_LINETO;
        }
        angle = angle + this.increment * (this.index - 1);
        double relx = Math.cos(angle);
        double rely = Math.sin(angle);
        coords[0] = (float) (this.x + (relx - this.cv * rely) * this.w);
        coords[1] = (float) (this.y + (rely + this.cv * relx) * this.h);
        angle = angle + this.increment;
        relx = Math.cos(angle);
        rely = Math.sin(angle);
        coords[2] = (float) (this.x + (relx + this.cv * rely) * this.w);
        coords[3] = (float) (this.y + (rely - this.cv * relx) * this.h);
        coords[4] = (float) (this.x + relx * this.w);
        coords[5] = (float) (this.y + rely * this.h);
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 3);
        }
        return PathIterator.SEG_CUBICTO;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("arc iterator out of bounds");
        }
        double angle = this.angStRad;
        if (this.index == 0) {
            coords[0] = this.x + Math.cos(angle) * this.w;
            coords[1] = this.y + Math.sin(angle) * this.h;
            if (this.affine != null) {
                this.affine.transform(coords, 0, coords, 0, 1);
            }
            return PathIterator.SEG_MOVETO;
        }
        if (this.index > this.arcSegs) {
            if (this.index == this.arcSegs + this.lineSegs) {
                return PathIterator.SEG_CLOSE;
            }
            coords[0] = this.x;
            coords[1] = this.y;
            if (this.affine != null) {
                this.affine.transform(coords, 0, coords, 0, 1);
            }
            return PathIterator.SEG_LINETO;
        }
        angle = angle + this.increment * (this.index - 1);
        double relx = Math.cos(angle);
        double rely = Math.sin(angle);
        coords[0] = this.x + (relx - this.cv * rely) * this.w;
        coords[1] = this.y + (rely + this.cv * relx) * this.h;
        angle = angle + this.increment;
        relx = Math.cos(angle);
        rely = Math.sin(angle);
        coords[2] = this.x + (relx + this.cv * rely) * this.w;
        coords[3] = this.y + (rely - this.cv * relx) * this.h;
        coords[4] = this.x + relx * this.w;
        coords[5] = this.y + rely * this.h;
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 3);
        }
        return PathIterator.SEG_CUBICTO;
    }
}
