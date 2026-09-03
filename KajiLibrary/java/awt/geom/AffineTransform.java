package java.awt.geom;

import java.awt.Shape;

// java.awt.geom.AffineTransform de KajiLibrary -- una transformacion afin 2D. Superficie completa.
//
//     [ x' ]   [ m00 m01 m02 ] [ x ]
//     [ y' ] = [ m10 m11 m12 ] [ y ]
//     [ 1  ]   [  0   0   1  ] [ 1 ]
//
// Dos cosas que se confunden facil y que aca estan escritas a proposito:
//
//   * `concatenate(Tx)` deja `this = this ∘ Tx`: Tx se aplica **primero** a cada punto. Es lo que
//     uno quiere cuando va acumulando transformaciones "hacia adentro" de un dibujo.
//     `preConcatenate(Tx)` deja `this = Tx ∘ this`: Tx se aplica **al final**, sobre el resultado.
//     La composicion no conmuta, asi que elegir mal no da un error, da otra figura.
//
//   * `invert()`/`createInverse()` **tienen que fallar** con NoninvertibleTransformException cuando
//     el determinante es cero. Devolver "algo" (una matriz con infinitos, por ejemplo) seria mentir:
//     la transformacion no tiene inversa y el llamador tiene que enterarse.
//
// Sobre la aritmetica: el estado interno (`state`) recuerda que celdas son **estructuralmente** 0 o
// 1, y las operaciones saltean los terminos que las involucran en vez de multiplicar por cero. No es
// una optimizacion: es lo que hace que `0.0` no se convierta en `-0.0` al componer con una escala
// negativa, y que un `y` infinito no ensucie una traslacion pura con un NaN. El JDK hace exactamente
// lo mismo y por el mismo motivo; sin eso, los resultados divergen en los bordes.
public class AffineTransform implements Cloneable, java.io.Serializable {

    /** La transformacion no cambia nada. */
    public static final int TYPE_IDENTITY = 0;

    /** Hay una traslacion. */
    public static final int TYPE_TRANSLATION = 1;

    /** Escala igual en las dos direcciones. */
    public static final int TYPE_UNIFORM_SCALE = 2;

    /** Escala distinta en cada direccion. */
    public static final int TYPE_GENERAL_SCALE = 4;

    /** Mascara de los dos bits de escala. */
    public static final int TYPE_MASK_SCALE = TYPE_UNIFORM_SCALE | TYPE_GENERAL_SCALE;

    /** Invierte la orientacion (reflexion). */
    public static final int TYPE_FLIP = 64;

    /** Rotacion de un multiplo de 90 grados. */
    public static final int TYPE_QUADRANT_ROTATION = 8;

    /** Rotacion de un angulo arbitrario. */
    public static final int TYPE_GENERAL_ROTATION = 16;

    /** Mascara de los dos bits de rotacion. */
    public static final int TYPE_MASK_ROTATION = TYPE_QUADRANT_ROTATION | TYPE_GENERAL_ROTATION;

    /** Transformacion que no cae en ninguna de las categorias de arriba. */
    public static final int TYPE_GENERAL_TRANSFORM = 32;

    // --- estado interno (libre por la regla del contrato) -------------------------------------

    static final int APPLY_IDENTITY = 0;
    static final int APPLY_TRANSLATE = 1;
    static final int APPLY_SCALE = 2;
    static final int APPLY_SHEAR = 4;

    private static final int TYPE_UNKNOWN = -1;

    double m00;
    double m10;
    double m01;
    double m11;
    double m02;
    double m12;

    transient int state;
    private transient int type;

    public AffineTransform() {
        this.m00 = 1.0;
        this.m11 = 1.0;
        // el resto queda en 0.0
        this.state = APPLY_IDENTITY;
        this.type = TYPE_IDENTITY;
    }

    public AffineTransform(AffineTransform Tx) {
        this.m00 = Tx.m00;
        this.m10 = Tx.m10;
        this.m01 = Tx.m01;
        this.m11 = Tx.m11;
        this.m02 = Tx.m02;
        this.m12 = Tx.m12;
        this.state = Tx.state;
        this.type = Tx.type;
    }

    // Ojo al orden de los parametros: es **por columnas** (m00, m10, m01, m11, m02, m12), no por
    // filas. Es la trampa numero uno de esta clase.
    public AffineTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
        this.m00 = (double) m00;
        this.m10 = (double) m10;
        this.m01 = (double) m01;
        this.m11 = (double) m11;
        this.m02 = (double) m02;
        this.m12 = (double) m12;
        updateState();
    }

    public AffineTransform(float[] flatmatrix) {
        this.m00 = (double) flatmatrix[0];
        this.m10 = (double) flatmatrix[1];
        this.m01 = (double) flatmatrix[2];
        this.m11 = (double) flatmatrix[3];
        if (flatmatrix.length > 5) {
            this.m02 = (double) flatmatrix[4];
            this.m12 = (double) flatmatrix[5];
        }
        updateState();
    }

    public AffineTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        updateState();
    }

    public AffineTransform(double[] flatmatrix) {
        this.m00 = flatmatrix[0];
        this.m10 = flatmatrix[1];
        this.m01 = flatmatrix[2];
        this.m11 = flatmatrix[3];
        if (flatmatrix.length > 5) {
            this.m02 = flatmatrix[4];
            this.m12 = flatmatrix[5];
        }
        updateState();
    }

    // --- fabricas ------------------------------------------------------------------------------

    public static AffineTransform getTranslateInstance(double tx, double ty) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToTranslation(tx, ty);
        return Tx;
    }

    public static AffineTransform getRotateInstance(double theta) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(theta);
        return Tx;
    }

    public static AffineTransform getRotateInstance(double theta, double anchorx, double anchory) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(theta, anchorx, anchory);
        return Tx;
    }

    public static AffineTransform getRotateInstance(double vecx, double vecy) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(vecx, vecy);
        return Tx;
    }

    public static AffineTransform getRotateInstance(double vecx, double vecy,
                                                    double anchorx, double anchory) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToRotation(vecx, vecy, anchorx, anchory);
        return Tx;
    }

    public static AffineTransform getQuadrantRotateInstance(int numquadrants) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToQuadrantRotation(numquadrants);
        return Tx;
    }

    public static AffineTransform getQuadrantRotateInstance(int numquadrants,
                                                            double anchorx, double anchory) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToQuadrantRotation(numquadrants, anchorx, anchory);
        return Tx;
    }

    public static AffineTransform getScaleInstance(double sx, double sy) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToScale(sx, sy);
        return Tx;
    }

    public static AffineTransform getShearInstance(double shx, double shy) {
        AffineTransform Tx = new AffineTransform();
        Tx.setToShear(shx, shy);
        return Tx;
    }

    // --- estado y tipo -------------------------------------------------------------------------

    // Recalcula `state` desde las celdas. Se llama despues de cada mutacion que no sepa dejar el
    // estado a mano; los predicados son los mismos que usa el JDK, asi que los dos coinciden en que
    // consideran "estructuralmente cero".
    void updateState() {
        if (m01 == 0.0 && m10 == 0.0) {
            if (m00 == 1.0 && m11 == 1.0) {
                if (m02 == 0.0 && m12 == 0.0) {
                    this.state = APPLY_IDENTITY;
                    this.type = TYPE_IDENTITY;
                } else {
                    this.state = APPLY_TRANSLATE;
                    this.type = TYPE_TRANSLATION;
                }
            } else {
                if (m02 == 0.0 && m12 == 0.0) {
                    this.state = APPLY_SCALE;
                } else {
                    this.state = APPLY_SCALE | APPLY_TRANSLATE;
                }
                this.type = TYPE_UNKNOWN;
            }
        } else {
            if (m00 == 0.0 && m11 == 0.0) {
                if (m02 == 0.0 && m12 == 0.0) {
                    this.state = APPLY_SHEAR;
                } else {
                    this.state = APPLY_SHEAR | APPLY_TRANSLATE;
                }
            } else {
                if (m02 == 0.0 && m12 == 0.0) {
                    this.state = APPLY_SHEAR | APPLY_SCALE;
                } else {
                    this.state = APPLY_SHEAR | APPLY_SCALE | APPLY_TRANSLATE;
                }
            }
            this.type = TYPE_UNKNOWN;
        }
    }

    public int getType() {
        if (this.type == TYPE_UNKNOWN) {
            calculateType();
        }
        return this.type;
    }

    // La clasificacion en categorias. Se sigue al pie la del JDK porque `getType()` es observable y
    // dos clasificaciones "razonables" pero distintas se distinguen desde afuera.
    private void calculateType() {
        int ret = TYPE_IDENTITY;
        boolean sgn0;
        boolean sgn1;
        double M0;
        double M1;
        double M2;
        double M3;
        int st = this.state;

        if ((st & APPLY_TRANSLATE) != 0) {
            ret = TYPE_TRANSLATION;
        }
        int linear = st & (APPLY_SHEAR | APPLY_SCALE);

        if (linear == (APPLY_SHEAR | APPLY_SCALE)) {
            M0 = m00;
            M2 = m01;
            M3 = m10;
            M1 = m11;
            if (M0 * M2 + M3 * M1 != 0.0) {
                // Los vectores unitarios transformados no quedan perpendiculares: no hay forma de
                // describirlo como rotacion + escala.
                this.type = TYPE_GENERAL_TRANSFORM;
                return;
            }
            sgn0 = (M0 >= 0.0);
            sgn1 = (M1 >= 0.0);
            if (sgn0 == sgn1) {
                // sgn(M0) == sgn(M1), luego sgn(M2) == -sgn(M3): rotacion sin reflexion.
                if (M0 != M1 || M2 != -M3) {
                    ret = ret | (TYPE_GENERAL_ROTATION | TYPE_GENERAL_SCALE);
                } else if (M0 * M1 - M2 * M3 != 1.0) {
                    ret = ret | (TYPE_GENERAL_ROTATION | TYPE_UNIFORM_SCALE);
                } else {
                    ret = ret | TYPE_GENERAL_ROTATION;
                }
            } else {
                // signos cruzados: hay reflexion.
                if (M0 == -M1 && M2 == M3) {
                    if (M0 * M1 - M2 * M3 != -1.0) {
                        ret = ret | (TYPE_GENERAL_ROTATION | TYPE_UNIFORM_SCALE | TYPE_FLIP);
                    } else {
                        ret = ret | (TYPE_GENERAL_ROTATION | TYPE_FLIP);
                    }
                } else {
                    ret = ret | (TYPE_GENERAL_ROTATION | TYPE_GENERAL_SCALE | TYPE_FLIP);
                }
            }
        } else if (linear == APPLY_SHEAR) {
            M0 = m01;
            M1 = m10;
            sgn0 = (M0 >= 0.0);
            sgn1 = (M1 >= 0.0);
            if (sgn0 != sgn1) {
                // signos distintos: giro de 90 grados limpio
                if (M0 != -M1) {
                    ret = ret | (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE);
                } else if (M0 != 1.0 && M0 != -1.0) {
                    ret = ret | (TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE);
                } else {
                    ret = ret | TYPE_QUADRANT_ROTATION;
                }
            } else {
                // mismos signos: giro de 90 grados mas una reflexion
                if (M0 == M1) {
                    ret = ret | (TYPE_QUADRANT_ROTATION | TYPE_FLIP | TYPE_UNIFORM_SCALE);
                } else {
                    ret = ret | (TYPE_QUADRANT_ROTATION | TYPE_FLIP | TYPE_GENERAL_SCALE);
                }
            }
        } else if (linear == APPLY_SCALE) {
            M0 = m00;
            M1 = m11;
            sgn0 = (M0 >= 0.0);
            sgn1 = (M1 >= 0.0);
            if (sgn0 == sgn1) {
                if (sgn0) {
                    // las dos escalas no negativas: escala pura
                    if (M0 == M1) {
                        ret = ret | TYPE_UNIFORM_SCALE;
                    } else {
                        ret = ret | TYPE_GENERAL_SCALE;
                    }
                } else {
                    // las dos negativas: giro de 180 grados
                    if (M0 != M1) {
                        ret = ret | (TYPE_QUADRANT_ROTATION | TYPE_GENERAL_SCALE);
                    } else if (M0 != -1.0) {
                        ret = ret | (TYPE_QUADRANT_ROTATION | TYPE_UNIFORM_SCALE);
                    } else {
                        ret = ret | TYPE_QUADRANT_ROTATION;
                    }
                }
            } else {
                // signos distintos: reflexion sobre alguno de los ejes
                if (M0 == -M1) {
                    if (M0 == 1.0 || M0 == -1.0) {
                        ret = ret | TYPE_FLIP;
                    } else {
                        ret = ret | (TYPE_FLIP | TYPE_UNIFORM_SCALE);
                    }
                } else {
                    ret = ret | (TYPE_FLIP | TYPE_GENERAL_SCALE);
                }
            }
        }
        this.type = ret;
    }

    // El determinante se calcula salteando los terminos estructuralmente nulos, igual que el JDK:
    // en una matriz de puro shear `m00 * m11` es 0*0 y sumarlo cambiaria el signo del cero.
    public double getDeterminant() {
        int linear = this.state & (APPLY_SHEAR | APPLY_SCALE);
        if (linear == (APPLY_SHEAR | APPLY_SCALE)) {
            return m00 * m11 - m01 * m10;
        }
        if (linear == APPLY_SHEAR) {
            return -(m01 * m10);
        }
        if (linear == APPLY_SCALE) {
            return m00 * m11;
        }
        return 1.0;
    }

    public double getScaleX() {
        return m00;
    }

    public double getScaleY() {
        return m11;
    }

    public double getShearX() {
        return m01;
    }

    public double getShearY() {
        return m10;
    }

    public double getTranslateX() {
        return m02;
    }

    public double getTranslateY() {
        return m12;
    }

    public void getMatrix(double[] flatmatrix) {
        flatmatrix[0] = m00;
        flatmatrix[1] = m10;
        flatmatrix[2] = m01;
        flatmatrix[3] = m11;
        if (flatmatrix.length > 5) {
            flatmatrix[4] = m02;
            flatmatrix[5] = m12;
        }
    }

    public boolean isIdentity() {
        return (this.state == APPLY_IDENTITY || (getType() == TYPE_IDENTITY));
    }

    // --- setTo* --------------------------------------------------------------------------------

    public void setToIdentity() {
        m00 = 1.0;
        m11 = 1.0;
        m10 = 0.0;
        m01 = 0.0;
        m02 = 0.0;
        m12 = 0.0;
        this.state = APPLY_IDENTITY;
        this.type = TYPE_IDENTITY;
    }

    public void setToTranslation(double tx, double ty) {
        m00 = 1.0;
        m10 = 0.0;
        m01 = 0.0;
        m11 = 1.0;
        m02 = tx;
        m12 = ty;
        if (tx != 0.0 || ty != 0.0) {
            this.state = APPLY_TRANSLATE;
            this.type = TYPE_TRANSLATION;
        } else {
            this.state = APPLY_IDENTITY;
            this.type = TYPE_IDENTITY;
        }
    }

    public void setToScale(double sx, double sy) {
        m00 = sx;
        m10 = 0.0;
        m01 = 0.0;
        m11 = sy;
        m02 = 0.0;
        m12 = 0.0;
        if (sx != 1.0 || sy != 1.0) {
            this.state = APPLY_SCALE;
            this.type = TYPE_UNKNOWN;
        } else {
            this.state = APPLY_IDENTITY;
            this.type = TYPE_IDENTITY;
        }
    }

    public void setToShear(double shx, double shy) {
        m00 = 1.0;
        m01 = shx;
        m10 = shy;
        m11 = 1.0;
        m02 = 0.0;
        m12 = 0.0;
        if (shx != 0.0 || shy != 0.0) {
            this.state = APPLY_SHEAR | APPLY_SCALE;
            this.type = TYPE_UNKNOWN;
        } else {
            this.state = APPLY_IDENTITY;
            this.type = TYPE_IDENTITY;
        }
    }

    // El redondeo de sin/cos a 0 y ±1 exactos en los cuadrantes no es cosmetico: sin el, una
    // rotacion de 90 grados deja un 6.1e-17 en la diagonal y la matriz deja de ser reconocible como
    // TYPE_QUADRANT_ROTATION.
    public void setToRotation(double theta) {
        double sin = Math.sin(theta);
        double cos;
        if (sin == 1.0 || sin == -1.0) {
            cos = 0.0;
            this.state = APPLY_SHEAR;
            this.type = TYPE_QUADRANT_ROTATION;
        } else {
            cos = Math.cos(theta);
            if (cos == -1.0) {
                sin = 0.0;
                this.state = APPLY_SCALE;
                this.type = TYPE_QUADRANT_ROTATION;
            } else if (cos == 1.0) {
                sin = 0.0;
                this.state = APPLY_IDENTITY;
                this.type = TYPE_IDENTITY;
            } else {
                this.state = APPLY_SHEAR | APPLY_SCALE;
                this.type = TYPE_UNKNOWN;
            }
        }
        m00 = cos;
        m10 = sin;
        m01 = -sin;
        m11 = cos;
        m02 = 0.0;
        m12 = 0.0;
    }

    public void setToRotation(double theta, double anchorx, double anchory) {
        setToRotation(theta);
        double sin = m10;
        double oneMinusCos = 1.0 - m00;
        m02 = anchorx * oneMinusCos + anchory * sin;
        m12 = anchory * oneMinusCos - anchorx * sin;
        if (m02 != 0.0 || m12 != 0.0) {
            this.state = this.state | APPLY_TRANSLATE;
            this.type = this.type | TYPE_TRANSLATION;
        }
    }

    public void setToRotation(double vecx, double vecy) {
        double sin;
        double cos;
        if (vecy == 0.0) {
            sin = 0.0;
            if (vecx < 0.0) {
                cos = -1.0;
                this.state = APPLY_SCALE;
                this.type = TYPE_QUADRANT_ROTATION;
            } else {
                cos = 1.0;
                this.state = APPLY_IDENTITY;
                this.type = TYPE_IDENTITY;
            }
        } else if (vecx == 0.0) {
            cos = 0.0;
            if (vecy > 0.0) {
                sin = 1.0;
            } else {
                sin = -1.0;
            }
            this.state = APPLY_SHEAR;
            this.type = TYPE_QUADRANT_ROTATION;
        } else {
            double len = Math.sqrt(vecx * vecx + vecy * vecy);
            cos = vecx / len;
            sin = vecy / len;
            this.state = APPLY_SHEAR | APPLY_SCALE;
            this.type = TYPE_UNKNOWN;
        }
        m00 = cos;
        m10 = sin;
        m01 = -sin;
        m11 = cos;
        m02 = 0.0;
        m12 = 0.0;
    }

    public void setToRotation(double vecx, double vecy, double anchorx, double anchory) {
        setToRotation(vecx, vecy);
        double sin = m10;
        double oneMinusCos = 1.0 - m00;
        m02 = anchorx * oneMinusCos + anchory * sin;
        m12 = anchory * oneMinusCos - anchorx * sin;
        if (m02 != 0.0 || m12 != 0.0) {
            this.state = this.state | APPLY_TRANSLATE;
            this.type = this.type | TYPE_TRANSLATION;
        }
    }

    public void setToQuadrantRotation(int numquadrants) {
        int q = numquadrants & 3;
        if (q == 0) {
            m00 = 1.0;
            m10 = 0.0;
            m01 = 0.0;
            m11 = 1.0;
            m02 = 0.0;
            m12 = 0.0;
            this.state = APPLY_IDENTITY;
            this.type = TYPE_IDENTITY;
        } else if (q == 1) {
            m00 = 0.0;
            m10 = 1.0;
            m01 = -1.0;
            m11 = 0.0;
            m02 = 0.0;
            m12 = 0.0;
            this.state = APPLY_SHEAR;
            this.type = TYPE_QUADRANT_ROTATION;
        } else if (q == 2) {
            m00 = -1.0;
            m10 = 0.0;
            m01 = 0.0;
            m11 = -1.0;
            m02 = 0.0;
            m12 = 0.0;
            this.state = APPLY_SCALE;
            this.type = TYPE_QUADRANT_ROTATION;
        } else {
            m00 = 0.0;
            m10 = -1.0;
            m01 = 1.0;
            m11 = 0.0;
            m02 = 0.0;
            m12 = 0.0;
            this.state = APPLY_SHEAR;
            this.type = TYPE_QUADRANT_ROTATION;
        }
    }

    public void setToQuadrantRotation(int numquadrants, double anchorx, double anchory) {
        int q = numquadrants & 3;
        if (q == 0) {
            m00 = 1.0;
            m10 = 0.0;
            m01 = 0.0;
            m11 = 1.0;
            m02 = 0.0;
            m12 = 0.0;
            this.state = APPLY_IDENTITY;
            this.type = TYPE_IDENTITY;
            return;
        }
        if (q == 1) {
            m00 = 0.0;
            m10 = 1.0;
            m01 = -1.0;
            m11 = 0.0;
            m02 = anchorx + anchory;
            m12 = anchory - anchorx;
            this.state = APPLY_SHEAR;
            this.type = TYPE_QUADRANT_ROTATION;
        } else if (q == 2) {
            m00 = -1.0;
            m10 = 0.0;
            m01 = 0.0;
            m11 = -1.0;
            m02 = anchorx + anchorx;
            m12 = anchory + anchory;
            this.state = APPLY_SCALE;
            this.type = TYPE_QUADRANT_ROTATION;
        } else {
            m00 = 0.0;
            m10 = -1.0;
            m01 = 1.0;
            m11 = 0.0;
            m02 = anchorx - anchory;
            m12 = anchory + anchorx;
            this.state = APPLY_SHEAR;
            this.type = TYPE_QUADRANT_ROTATION;
        }
        if (m02 != 0.0 || m12 != 0.0) {
            this.state = this.state | APPLY_TRANSLATE;
            this.type = this.type | TYPE_TRANSLATION;
        }
    }

    public void setTransform(AffineTransform Tx) {
        this.m00 = Tx.m00;
        this.m10 = Tx.m10;
        this.m01 = Tx.m01;
        this.m11 = Tx.m11;
        this.m02 = Tx.m02;
        this.m12 = Tx.m12;
        this.state = Tx.state;
        this.type = Tx.type;
    }

    public void setTransform(double m00, double m10, double m01, double m11,
                             double m02, double m12) {
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
        updateState();
    }

    // --- mutadores por composicion -------------------------------------------------------------

    public void translate(double tx, double ty) {
        int st = this.state;
        double nx;
        double ny;
        if ((st & APPLY_SHEAR) != 0) {
            if ((st & APPLY_SCALE) != 0) {
                nx = tx * m00 + ty * m01;
                ny = tx * m10 + ty * m11;
            } else {
                // shear puro: m00 y m11 son cero estructural, no se multiplican
                nx = ty * m01;
                ny = tx * m10;
            }
        } else {
            if ((st & APPLY_SCALE) != 0) {
                nx = tx * m00;
                ny = ty * m11;
            } else {
                nx = tx;
                ny = ty;
            }
        }
        if ((st & APPLY_TRANSLATE) != 0) {
            m02 = nx + m02;
            m12 = ny + m12;
        } else {
            m02 = nx;
            m12 = ny;
        }
        updateState();
    }

    // Giro de 90 grados por permutacion de celdas: sin multiplicaciones no hay error de redondeo ni
    // ceros con signo inventados.
    private void rotate90() {
        double M0 = m00;
        m00 = m01;
        m01 = -M0;
        M0 = m10;
        m10 = m11;
        m11 = -M0;
        updateState();
    }

    private void rotate180() {
        m00 = -m00;
        m11 = -m11;
        if ((this.state & APPLY_SHEAR) != 0) {
            m01 = -m01;
            m10 = -m10;
        }
        // Sin shear, m01/m10 valen 0.0: negarlos daria -0.0 sin motivo.
        updateState();
    }

    private void rotate270() {
        double M0 = m00;
        m00 = -m01;
        m01 = M0;
        M0 = m10;
        m10 = -m11;
        m11 = M0;
        updateState();
    }

    public void rotate(double theta) {
        double sin = Math.sin(theta);
        if (sin == 1.0) {
            rotate90();
        } else if (sin == -1.0) {
            rotate270();
        } else {
            double cos = Math.cos(theta);
            if (cos == -1.0) {
                rotate180();
            } else if (cos != 1.0) {
                double M0;
                double M1;
                M0 = m00;
                M1 = m01;
                m00 = cos * M0 + sin * M1;
                m01 = -sin * M0 + cos * M1;
                M0 = m10;
                M1 = m11;
                m10 = cos * M0 + sin * M1;
                m11 = -sin * M0 + cos * M1;
                updateState();
            }
            // cos == 1.0 (y sin != ±1): la rotacion es la identidad, no se toca nada.
        }
    }

    public void rotate(double theta, double anchorx, double anchory) {
        translate(anchorx, anchory);
        rotate(theta);
        translate(-anchorx, -anchory);
    }

    public void rotate(double vecx, double vecy) {
        if (vecy == 0.0) {
            if (vecx < 0.0) {
                rotate180();
            }
            // vecx > 0: no hay rotacion. vecx == 0: vector nulo, angulo indefinido, no se rota.
        } else if (vecx == 0.0) {
            if (vecy > 0.0) {
                rotate90();
            } else {
                rotate270();
            }
        } else {
            double len = Math.sqrt(vecx * vecx + vecy * vecy);
            double sin = vecy / len;
            double cos = vecx / len;
            double M0;
            double M1;
            M0 = m00;
            M1 = m01;
            m00 = cos * M0 + sin * M1;
            m01 = -sin * M0 + cos * M1;
            M0 = m10;
            M1 = m11;
            m10 = cos * M0 + sin * M1;
            m11 = -sin * M0 + cos * M1;
            updateState();
        }
    }

    public void rotate(double vecx, double vecy, double anchorx, double anchory) {
        translate(anchorx, anchory);
        rotate(vecx, vecy);
        translate(-anchorx, -anchory);
    }

    public void quadrantRotate(int numquadrants) {
        int q = numquadrants & 3;
        if (q == 1) {
            rotate90();
        } else if (q == 2) {
            rotate180();
        } else if (q == 3) {
            rotate270();
        }
    }

    public void quadrantRotate(int numquadrants, double anchorx, double anchory) {
        int q = numquadrants & 3;
        if (q == 0) {
            return;
        }
        translate(anchorx, anchory);
        if (q == 1) {
            rotate90();
        } else if (q == 2) {
            rotate180();
        } else {
            rotate270();
        }
        translate(-anchorx, -anchory);
    }

    public void scale(double sx, double sy) {
        int st = this.state;
        if ((st & APPLY_SCALE) != 0) {
            m00 = m00 * sx;
            m11 = m11 * sy;
        } else if ((st & APPLY_SHEAR) == 0) {
            // diagonal estructuralmente 1: se asigna en vez de multiplicar
            m00 = sx;
            m11 = sy;
        }
        // (SHEAR sin SCALE: la diagonal es cero estructural y no se toca)
        if ((st & APPLY_SHEAR) != 0) {
            m01 = m01 * sy;
            m10 = m10 * sx;
        }
        updateState();
    }

    public void shear(double shx, double shy) {
        int st = this.state;
        int linear = st & (APPLY_SHEAR | APPLY_SCALE);
        if (linear == (APPLY_SHEAR | APPLY_SCALE)) {
            double M0;
            double M1;
            M0 = m00;
            M1 = m01;
            m00 = M0 + M1 * shy;
            m01 = M0 * shx + M1;
            M0 = m10;
            M1 = m11;
            m10 = M0 + M1 * shy;
            m11 = M0 * shx + M1;
        } else if (linear == APPLY_SHEAR) {
            m00 = m01 * shy;
            m11 = m10 * shx;
        } else if (linear == APPLY_SCALE) {
            m01 = m00 * shx;
            m10 = m11 * shy;
        } else {
            m01 = shx;
            m10 = shy;
        }
        updateState();
    }

    // `this = this ∘ Tx`. Tx se aplica primero.
    public void concatenate(AffineTransform Tx) {
        int mystate = this.state;
        int txstate = Tx.state;
        double T00 = Tx.m00;
        double T01 = Tx.m01;
        double T10 = Tx.m10;
        double T11 = Tx.m11;
        double T02 = Tx.m02;
        double T12 = Tx.m12;

        // Parte lineal: C = A * B, salteando todo termino con un factor estructuralmente nulo.
        double C00 = product2(m00, T00, diagZero(mystate), diagZero(txstate),
                              m01, T10, offZero(mystate), offZero(txstate));
        double C01 = product2(m00, T01, diagZero(mystate), offZero(txstate),
                              m01, T11, offZero(mystate), diagZero(txstate));
        double C10 = product2(m10, T00, offZero(mystate), diagZero(txstate),
                              m11, T10, diagZero(mystate), offZero(txstate));
        double C11 = product2(m10, T01, offZero(mystate), offZero(txstate),
                              m11, T11, diagZero(mystate), diagZero(txstate));

        // Parte de traslacion: el desplazamiento de Tx pasa por la parte lineal **vieja** de this.
        double C02 = m02;
        double C12 = m12;
        if ((txstate & APPLY_TRANSLATE) != 0) {
            double nx = product2(T02, m00, false, diagZero(mystate),
                                 T12, m01, false, offZero(mystate));
            double ny = product2(T02, m10, false, offZero(mystate),
                                 T12, m11, false, diagZero(mystate));
            if ((mystate & APPLY_TRANSLATE) != 0) {
                C02 = nx + m02;
                C12 = ny + m12;
            } else {
                C02 = nx;
                C12 = ny;
            }
        }

        m00 = C00;
        m01 = C01;
        m10 = C10;
        m11 = C11;
        m02 = C02;
        m12 = C12;
        updateState();
    }

    // `this = Tx ∘ this`. Tx se aplica al final, sobre el resultado de this.
    public void preConcatenate(AffineTransform Tx) {
        int mystate = this.state;
        int txstate = Tx.state;
        double T00 = Tx.m00;
        double T01 = Tx.m01;
        double T10 = Tx.m10;
        double T11 = Tx.m11;
        double T02 = Tx.m02;
        double T12 = Tx.m12;

        // C = B * A (B = Tx)
        double C00 = product2(T00, m00, diagZero(txstate), diagZero(mystate),
                              T01, m10, offZero(txstate), offZero(mystate));
        double C01 = product2(T00, m01, diagZero(txstate), offZero(mystate),
                              T01, m11, offZero(txstate), diagZero(mystate));
        double C10 = product2(T10, m00, offZero(txstate), diagZero(mystate),
                              T11, m10, diagZero(txstate), offZero(mystate));
        double C11 = product2(T10, m01, offZero(txstate), offZero(mystate),
                              T11, m11, diagZero(txstate), diagZero(mystate));

        // La traslacion de this pasa por la parte lineal de Tx, y despues se suma la de Tx.
        double C02;
        double C12;
        if ((mystate & APPLY_TRANSLATE) != 0) {
            C02 = product2(T00, m02, diagZero(txstate), false,
                           T01, m12, offZero(txstate), false);
            C12 = product2(T10, m02, offZero(txstate), false,
                           T11, m12, diagZero(txstate), false);
            if ((txstate & APPLY_TRANSLATE) != 0) {
                C02 = C02 + T02;
                C12 = C12 + T12;
            }
        } else {
            C02 = T02;
            C12 = T12;
        }

        m00 = C00;
        m01 = C01;
        m10 = C10;
        m11 = C11;
        m02 = C02;
        m12 = C12;
        updateState();
    }

    // ¿La diagonal (m00/m11) de una matriz en este estado es cero estructural? Lo es exactamente
    // cuando hay shear y no hay escala.
    private static boolean diagZero(int st) {
        return (st & APPLY_SHEAR) != 0 && (st & APPLY_SCALE) == 0;
    }

    // ¿La antidiagonal (m01/m10) es cero estructural? Lo es cuando no hay shear.
    private static boolean offZero(int st) {
        return (st & APPLY_SHEAR) == 0;
    }

    // a*b + c*d, salteando el termino cuyo factor sea cero estructural. Si los dos se saltean el
    // resultado es 0.0 literal -- nunca -0.0 ni NaN heredado de un factor infinito del otro lado.
    private static double product2(double a, double b, boolean aZero, boolean bZero,
                                   double c, double d, boolean cZero, boolean dZero) {
        boolean t1 = !(aZero || bZero);
        boolean t2 = !(cZero || dZero);
        if (t1 && t2) {
            return a * b + c * d;
        }
        if (t1) {
            return a * b;
        }
        if (t2) {
            return c * d;
        }
        return 0.0;
    }

    public AffineTransform createInverse() throws NoninvertibleTransformException {
        AffineTransform inv = new AffineTransform(this);
        inv.invert();
        return inv;
    }

    // Falla en vez de devolver basura: si el determinante es cero la transformacion colapsa el plano
    // sobre una recta o un punto y no hay inversa que valga.
    public void invert() throws NoninvertibleTransformException {
        int linear = this.state & (APPLY_SHEAR | APPLY_SCALE);
        boolean hasTranslate = (this.state & APPLY_TRANSLATE) != 0;
        double M00;
        double M01;
        double M02;
        double M10;
        double M11;
        double M12;
        double det;

        if (linear == (APPLY_SHEAR | APPLY_SCALE)) {
            M00 = m00;
            M01 = m01;
            M10 = m10;
            M11 = m11;
            det = M00 * M11 - M01 * M10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is " + det);
            }
            m00 = M11 / det;
            m10 = -M10 / det;
            m01 = -M01 / det;
            m11 = M00 / det;
            if (hasTranslate) {
                M02 = m02;
                M12 = m12;
                m02 = (M01 * M12 - M11 * M02) / det;
                m12 = (M10 * M02 - M00 * M12) / det;
            }
        } else if (linear == APPLY_SHEAR) {
            M01 = m01;
            M10 = m10;
            if (M01 == 0.0 || M10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            m10 = 1.0 / M01;
            m01 = 1.0 / M10;
            if (hasTranslate) {
                M02 = m02;
                M12 = m12;
                m02 = -M12 / M10;
                m12 = -M02 / M01;
            }
        } else if (linear == APPLY_SCALE) {
            M00 = m00;
            M11 = m11;
            if (M00 == 0.0 || M11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            m00 = 1.0 / M00;
            m11 = 1.0 / M11;
            if (hasTranslate) {
                M02 = m02;
                M12 = m12;
                m02 = -M02 / M00;
                m12 = -M12 / M11;
            }
        } else {
            if (hasTranslate) {
                m02 = -m02;
                m12 = -m12;
            }
            // identidad: nada que invertir
        }
        updateState();
    }

    // --- aplicacion ----------------------------------------------------------------------------

    // Componente x de la parte lineal aplicada a (x,y), sin la traslacion.
    private double deltaX(double x, double y) {
        int st = this.state;
        if ((st & APPLY_SHEAR) != 0) {
            if ((st & APPLY_SCALE) != 0) {
                return m00 * x + m01 * y;
            }
            return m01 * y;
        }
        if ((st & APPLY_SCALE) != 0) {
            return m00 * x;
        }
        return x;
    }

    private double deltaY(double x, double y) {
        int st = this.state;
        if ((st & APPLY_SHEAR) != 0) {
            if ((st & APPLY_SCALE) != 0) {
                return m10 * x + m11 * y;
            }
            return m10 * x;
        }
        if ((st & APPLY_SCALE) != 0) {
            return m11 * y;
        }
        return y;
    }

    private double fullX(double x, double y) {
        double r = deltaX(x, y);
        if ((this.state & APPLY_TRANSLATE) != 0) {
            return r + m02;
        }
        return r;
    }

    private double fullY(double x, double y) {
        double r = deltaY(x, y);
        if ((this.state & APPLY_TRANSLATE) != 0) {
            return r + m12;
        }
        return r;
    }

    public Point2D transform(Point2D ptSrc, Point2D ptDst) {
        if (ptDst == null) {
            ptDst = Point2D.newLike(ptSrc);
        }
        double x = ptSrc.getX();
        double y = ptSrc.getY();
        ptDst.setLocation(fullX(x, y), fullY(x, y));
        return ptDst;
    }

    public void transform(Point2D[] ptSrc, int srcOff, Point2D[] ptDst, int dstOff, int numPts) {
        while (numPts > 0) {
            numPts = numPts - 1;
            Point2D src = ptSrc[srcOff];
            srcOff = srcOff + 1;
            double x = src.getX();
            double y = src.getY();
            Point2D dst = ptDst[dstOff];
            if (dst == null) {
                dst = Point2D.newLike(src);
                ptDst[dstOff] = dst;
            }
            dst.setLocation(fullX(x, y), fullY(x, y));
            dstOff = dstOff + 1;
        }
    }

    public void transform(double[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        if (dstPts == srcPts && dstOff > srcOff && dstOff < srcOff + numPts * 2) {
            // Los rangos se pisan con el destino mas adelante: si transformamos en orden nos
            // comeriamos coordenadas de origen todavia sin leer. Copiar primero lo resuelve.
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            srcOff = dstOff;
        }
        while (numPts > 0) {
            numPts = numPts - 1;
            double x = srcPts[srcOff];
            double y = srcPts[srcOff + 1];
            srcOff = srcOff + 2;
            dstPts[dstOff] = fullX(x, y);
            dstPts[dstOff + 1] = fullY(x, y);
            dstOff = dstOff + 2;
        }
    }

    public void transform(float[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
        if (dstPts == srcPts && dstOff > srcOff && dstOff < srcOff + numPts * 2) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            srcOff = dstOff;
        }
        while (numPts > 0) {
            numPts = numPts - 1;
            double x = (double) srcPts[srcOff];
            double y = (double) srcPts[srcOff + 1];
            srcOff = srcOff + 2;
            dstPts[dstOff] = (float) fullX(x, y);
            dstPts[dstOff + 1] = (float) fullY(x, y);
            dstOff = dstOff + 2;
        }
    }

    public void transform(float[] srcPts, int srcOff, double[] dstPts, int dstOff, int numPts) {
        while (numPts > 0) {
            numPts = numPts - 1;
            double x = (double) srcPts[srcOff];
            double y = (double) srcPts[srcOff + 1];
            srcOff = srcOff + 2;
            dstPts[dstOff] = fullX(x, y);
            dstPts[dstOff + 1] = fullY(x, y);
            dstOff = dstOff + 2;
        }
    }

    public void transform(double[] srcPts, int srcOff, float[] dstPts, int dstOff, int numPts) {
        while (numPts > 0) {
            numPts = numPts - 1;
            double x = srcPts[srcOff];
            double y = srcPts[srcOff + 1];
            srcOff = srcOff + 2;
            dstPts[dstOff] = (float) fullX(x, y);
            dstPts[dstOff + 1] = (float) fullY(x, y);
            dstOff = dstOff + 2;
        }
    }

    // La transformacion "delta" ignora la traslacion: transforma vectores, no puntos.
    public Point2D deltaTransform(Point2D ptSrc, Point2D ptDst) {
        if (ptDst == null) {
            ptDst = Point2D.newLike(ptSrc);
        }
        double x = ptSrc.getX();
        double y = ptSrc.getY();
        ptDst.setLocation(deltaX(x, y), deltaY(x, y));
        return ptDst;
    }

    public void deltaTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff,
                               int numPts) {
        if (dstPts == srcPts && dstOff > srcOff && dstOff < srcOff + numPts * 2) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            srcOff = dstOff;
        }
        while (numPts > 0) {
            numPts = numPts - 1;
            double x = srcPts[srcOff];
            double y = srcPts[srcOff + 1];
            srcOff = srcOff + 2;
            dstPts[dstOff] = deltaX(x, y);
            dstPts[dstOff + 1] = deltaY(x, y);
            dstOff = dstOff + 2;
        }
    }

    public Point2D inverseTransform(Point2D ptSrc, Point2D ptDst)
            throws NoninvertibleTransformException {
        if (ptDst == null) {
            ptDst = Point2D.newLike(ptSrc);
        }
        double x = ptSrc.getX();
        double y = ptSrc.getY();
        int linear = this.state & (APPLY_SHEAR | APPLY_SCALE);
        if ((this.state & APPLY_TRANSLATE) != 0) {
            x = x - m02;
            y = y - m12;
        }
        if (linear == (APPLY_SHEAR | APPLY_SCALE)) {
            double det = m00 * m11 - m01 * m10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is " + det);
            }
            ptDst.setLocation((x * m11 - y * m01) / det, (y * m00 - x * m10) / det);
        } else if (linear == APPLY_SHEAR) {
            if (m01 == 0.0 || m10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            ptDst.setLocation(y / m10, x / m01);
        } else if (linear == APPLY_SCALE) {
            if (m00 == 0.0 || m11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
            ptDst.setLocation(x / m00, y / m11);
        } else {
            ptDst.setLocation(x, y);
        }
        return ptDst;
    }

    public void inverseTransform(double[] srcPts, int srcOff, double[] dstPts, int dstOff,
                                 int numPts) throws NoninvertibleTransformException {
        if (dstPts == srcPts && dstOff > srcOff && dstOff < srcOff + numPts * 2) {
            System.arraycopy(srcPts, srcOff, dstPts, dstOff, numPts * 2);
            srcOff = dstOff;
        }
        int linear = this.state & (APPLY_SHEAR | APPLY_SCALE);
        boolean hasTranslate = (this.state & APPLY_TRANSLATE) != 0;
        double det = 0.0;
        if (linear == (APPLY_SHEAR | APPLY_SCALE)) {
            det = m00 * m11 - m01 * m10;
            if (Math.abs(det) <= Double.MIN_VALUE) {
                throw new NoninvertibleTransformException("Determinant is " + det);
            }
        } else if (linear == APPLY_SHEAR) {
            if (m01 == 0.0 || m10 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
        } else if (linear == APPLY_SCALE) {
            if (m00 == 0.0 || m11 == 0.0) {
                throw new NoninvertibleTransformException("Determinant is 0");
            }
        }
        while (numPts > 0) {
            numPts = numPts - 1;
            double x = srcPts[srcOff];
            double y = srcPts[srcOff + 1];
            srcOff = srcOff + 2;
            if (hasTranslate) {
                x = x - m02;
                y = y - m12;
            }
            if (linear == (APPLY_SHEAR | APPLY_SCALE)) {
                dstPts[dstOff] = (x * m11 - y * m01) / det;
                dstPts[dstOff + 1] = (y * m00 - x * m10) / det;
            } else if (linear == APPLY_SHEAR) {
                dstPts[dstOff] = y / m10;
                dstPts[dstOff + 1] = x / m01;
            } else if (linear == APPLY_SCALE) {
                dstPts[dstOff] = x / m00;
                dstPts[dstOff + 1] = y / m11;
            } else {
                dstPts[dstOff] = x;
                dstPts[dstOff + 1] = y;
            }
            dstOff = dstOff + 2;
        }
    }

    public Shape createTransformedShape(Shape pSrc) {
        if (pSrc == null) {
            return null;
        }
        return Path2D.newDouble(pSrc, this);
    }

    // --- Object --------------------------------------------------------------------------------

    // Se redondea a 15 digitos significativos igual que el JDK: sin eso una rotacion imprime
    // 0.7071067811865476 y 0.7071067811865475 en celdas que deberian verse iguales.
    private static double matround(double matval) {
        return Math.rint(matval * 1E15) / 1E15;
    }

    public String toString() {
        return "AffineTransform[[" + matround(m00) + ", " + matround(m01) + ", " + matround(m02)
                + "], [" + matround(m10) + ", " + matround(m11) + ", " + matround(m12) + "]]";
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }

    public int hashCode() {
        long bits = Double.doubleToLongBits(m00);
        bits = bits * 31L + Double.doubleToLongBits(m01);
        bits = bits * 31L + Double.doubleToLongBits(m02);
        bits = bits * 31L + Double.doubleToLongBits(m10);
        bits = bits * 31L + Double.doubleToLongBits(m11);
        bits = bits * 31L + Double.doubleToLongBits(m12);
        return ((int) bits) ^ ((int) (bits >> 32));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof AffineTransform)) {
            return false;
        }
        AffineTransform a = (AffineTransform) obj;
        return ((m00 == a.m00) && (m01 == a.m01) && (m02 == a.m02)
                && (m10 == a.m10) && (m11 == a.m11) && (m12 == a.m12));
    }
}
