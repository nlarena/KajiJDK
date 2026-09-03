package java.awt.geom;

import java.util.NoSuchElementException;

// Iterador interno de Ellipse2D: cuatro cubicas de Bezier, una por cuadrante.
//
// El circulo no es representable exactamente con Beziers; la constante CTRL_VAL = 4/3*(sqrt(2)-1)
// es la que hace que el cuarto de circulo aproximado toque los dos extremos con la tangente
// correcta y quede a menos de 0.03 % del radio en el medio. Es el valor que usa todo el mundo,
// incluido el JDK, y hay que usar **el mismo** para que las coordenadas coincidan bit a bit.
//
// El recorrido arranca en el punto medio del lado derecho (x+w, y+h/2) y va derecha -> abajo ->
// izquierda -> arriba, que con el eje y hacia abajo de la pantalla es el sentido horario.
class EllipseIterator implements PathIterator {

    double x;
    double y;
    double w;
    double h;
    AffineTransform affine;
    int index;

    EllipseIterator(Ellipse2D e, AffineTransform at) {
        this.x = e.getX();
        this.y = e.getY();
        this.w = e.getWidth();
        this.h = e.getHeight();
        this.affine = at;
        if (this.w < 0 || this.h < 0) {
            this.index = 6;
        }
    }

    public int getWindingRule() {
        return PathIterator.WIND_NON_ZERO;
    }

    public boolean isDone() {
        return this.index > 5;
    }

    public void next() {
        this.index = this.index + 1;
    }

    public static final double CTRL_VAL = 0.5522847498307933;

    private static final double PCV = 0.5 + CTRL_VAL * 0.5;
    private static final double NCV = 0.5 - CTRL_VAL * 0.5;

    // Cada fila es una cubica en coordenadas normalizadas [0,1]x[0,1]: {c1x, c1y, c2x, c2y, px, py}.
    private static final double[][] CTRLPTS = {
        { 1.0, PCV, PCV, 1.0, 0.5, 1.0 },
        { NCV, 1.0, 0.0, PCV, 0.0, 0.5 },
        { 0.0, NCV, NCV, 0.0, 0.5, 0.0 },
        { PCV, 0.0, 1.0, NCV, 1.0, 0.5 }
    };

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("ellipse iterator out of bounds");
        }
        if (this.index == 5) {
            return PathIterator.SEG_CLOSE;
        }
        if (this.index == 0) {
            double[] ctrls = CTRLPTS[3];
            coords[0] = (float) (this.x + ctrls[4] * this.w);
            coords[1] = (float) (this.y + ctrls[5] * this.h);
            if (this.affine != null) {
                this.affine.transform(coords, 0, coords, 0, 1);
            }
            return PathIterator.SEG_MOVETO;
        }
        double[] ctrls = CTRLPTS[this.index - 1];
        coords[0] = (float) (this.x + ctrls[0] * this.w);
        coords[1] = (float) (this.y + ctrls[1] * this.h);
        coords[2] = (float) (this.x + ctrls[2] * this.w);
        coords[3] = (float) (this.y + ctrls[3] * this.h);
        coords[4] = (float) (this.x + ctrls[4] * this.w);
        coords[5] = (float) (this.y + ctrls[5] * this.h);
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 3);
        }
        return PathIterator.SEG_CUBICTO;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("ellipse iterator out of bounds");
        }
        if (this.index == 5) {
            return PathIterator.SEG_CLOSE;
        }
        if (this.index == 0) {
            double[] ctrls = CTRLPTS[3];
            coords[0] = this.x + ctrls[4] * this.w;
            coords[1] = this.y + ctrls[5] * this.h;
            if (this.affine != null) {
                this.affine.transform(coords, 0, coords, 0, 1);
            }
            return PathIterator.SEG_MOVETO;
        }
        double[] ctrls = CTRLPTS[this.index - 1];
        coords[0] = this.x + ctrls[0] * this.w;
        coords[1] = this.y + ctrls[1] * this.h;
        coords[2] = this.x + ctrls[2] * this.w;
        coords[3] = this.y + ctrls[3] * this.h;
        coords[4] = this.x + ctrls[4] * this.w;
        coords[5] = this.y + ctrls[5] * this.h;
        if (this.affine != null) {
            this.affine.transform(coords, 0, coords, 0, 3);
        }
        return PathIterator.SEG_CUBICTO;
    }
}
