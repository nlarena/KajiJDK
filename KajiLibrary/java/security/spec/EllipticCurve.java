package java.security.spec;

import java.math.BigInteger;

// La curva eliptica en si: y^2 = x^3 + a*x + b sobre un cuerpo, mas —opcionalmente— la semilla con
// la que se genero.
//
// La semilla no es decorativa. Las curvas de los estandares se derivan de un valor publico pasado
// por un hash, y publicarlo es lo que permite a cualquiera recalcular a y b y comprobar que no se
// eligieron a dedo para esconder una debilidad. Que sea opcional refleja que muchas curvas
// simplemente no la publican.
//
// `equals` **no** compara la semilla, y es correcto: dos curvas con los mismos a, b y cuerpo son la
// misma curva aunque una diga como se genero y la otra no. La semilla es procedencia, no identidad.
//
// La validacion de a y b es solo de rango, no de que la curva sea no singular: comprobar que el
// discriminante 4a^3 + 27b^2 no sea cero es barato en GF(p) pero no en GF(2^m), y el JDK no lo
// hace en ninguno de los dos. Un descriptor describe; quien opere sobre la curva decide si la
// acepta.
public class EllipticCurve {

    private final ECField field;
    private final BigInteger a;
    private final BigInteger b;
    private final byte[] seed;

    public EllipticCurve(ECField field, BigInteger a, BigInteger b) {
        this(field, a, b, null);
    }

    public EllipticCurve(ECField field, BigInteger a, BigInteger b, byte[] seed) {
        if (field == null) {
            throw new NullPointerException("field is null");
        }
        if (a == null) {
            throw new NullPointerException("first coefficient is null");
        }
        if (b == null) {
            throw new NullPointerException("second coefficient is null");
        }
        // Los coeficientes son elementos del cuerpo, asi que tienen que entrar en el. Como el
        // "entrar" se dice distinto en cada cuerpo, se pregunta por el tipo concreto; un cuerpo de
        // otra clase no se valida porque no hay forma de saber que significa ahi.
        if (field instanceof ECFieldFp) {
            BigInteger p = ((ECFieldFp) field).getP();
            if (p.compareTo(a) != 1) {
                throw new IllegalArgumentException("first coefficient is too large");
            }
            if (p.compareTo(b) != 1) {
                throw new IllegalArgumentException("second coefficient is too large");
            }
        } else if (field instanceof ECFieldF2m) {
            int m = ((ECFieldF2m) field).getM();
            if (a.bitLength() > m) {
                throw new IllegalArgumentException("first coefficient is too large");
            }
            if (b.bitLength() > m) {
                throw new IllegalArgumentException("second coefficient is too large");
            }
        }
        this.field = field;
        this.a = a;
        this.b = b;
        this.seed = copiar(seed);
    }

    private static byte[] copiar(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    public ECField getField() {
        return this.field;
    }

    public BigInteger getA() {
        return this.a;
    }

    public BigInteger getB() {
        return this.b;
    }

    // Copia de la semilla, o null si la curva no dice como se genero.
    public byte[] getSeed() {
        return copiar(this.seed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof EllipticCurve) {
            EllipticCurve otra = (EllipticCurve) obj;
            return this.field.equals(otra.field)
                && this.a.equals(otra.a)
                && this.b.equals(otra.b);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = this.field.hashCode();
        h = h * 31 + this.a.hashCode();
        h = h * 31 + this.b.hashCode();
        return h;
    }
}
