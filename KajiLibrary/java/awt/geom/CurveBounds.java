package java.awt.geom;

// La caja **ajustada** de una curva de Bezier: la mas chica que la contiene, no la de su poligono de
// control.
//
// Las dos son cotas validas segun `Shape.getBounds2D`, asi que la eleccion no la fuerza el contrato
// -- la fuerza el JDK, que devuelve la ajustada. Se comprobo corriendo el mismo caso con `java` de
// verdad: para la cubica (0,0),(0,10),(10,10),(10,0) devuelve alto 7.5, no 10. La caja del poligono
// de control es mas facil --sale de cuatro minimos y cuatro maximos, sin resolver nada-- y fue lo
// que hubo aca hasta que la prueba de comportamiento no coincidio.
//
// Como se calcula: una coordenada de una Bezier es un polinomio en `t`, y sus extremos sobre [0,1]
// estan en los bordes o donde la derivada se anula. Asi que la caja son los dos extremos de la curva
// mas el valor en cada raiz de la derivada que caiga **dentro** del intervalo abierto (0,1). Las
// raices de afuera no cuentan: describen extremos de la curva prolongada, que no es esta curva.
final class CurveBounds {

    private CurveBounds() {
    }

    /**
     * El minimo y el maximo de una coordenada de una cubica, como un arreglo de dos.
     *
     * <p>La derivada de una cubica es una cuadratica: `at^2 + bt + c` con `a = 3(-p0+3p1-3p2+p3)`,
     * `b = 6(p0-2p1+p2)` y `c = 3(p1-p0)`.
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
     * Lo mismo para una cuadratica.
     *
     * <p>Su derivada es lineal, asi que hay a lo sumo un extremo interior: `t = (p0-p1)/(p0-2p1+p2)`.
     * El denominador es cero cuando los tres puntos estan alineados en esta coordenada, y ahi la
     * curva es monotona: los extremos son los bordes y no hay nada que agregar.
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

    // La forma de Bernstein, y no la potencia expandida, porque es la numericamente estable: cada
    // termino es un producto de factores acotados en [0,1] y no hay restas de numeros grandes.
    private static double cubicAt(double p0, double p1, double p2, double p3, double t) {
        double u = 1.0 - t;
        return u * u * u * p0 + 3.0 * u * u * t * p1 + 3.0 * u * t * t * p2 + t * t * t * p3;
    }

    private static double quadAt(double p0, double p1, double p2, double t) {
        double u = 1.0 - t;
        return u * u * p0 + 2.0 * u * t * p1 + t * t * p2;
    }

    // Las raices reales de `at^2 + bt + c`, sin ordenar.
    //
    // El caso `a == 0` no es una curiosidad: pasa siempre que la cubica degrada a una cuadratica, que
    // es una de las formas mas comunes de escribirla. Tratarlo como cuadratica dividiria por cero.
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
        // La forma estable: se calcula la raiz que **no** cancela y la otra sale del producto de las
        // raices (`c/a`). Con la formula de siempre, cuando `b` es grande frente a `4ac` una de las
        // dos resta dos numeros casi iguales y pierde casi todos los digitos.
        double sq = Math.sqrt(disc);
        double q = b >= 0.0 ? -0.5 * (b + sq) : -0.5 * (b - sq);
        return new double[] { q / a, c / q };
    }
}
