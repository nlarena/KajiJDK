package java.awt.geom;

import java.util.NoSuchElementException;

// Iterador interno de Path2D.Float (no es API). Recorre el arreglo plano de tipos y el de
// coordenadas en paralelo, y aplica la transformacion --si la hay-- al vuelo, sin copiar el camino.
//
// El indice `typeIdx` avanza de a un tipo y `pointIdx` de a tantas coordenadas como consuma ese
// tipo, que es de donde sale la tabla `CURVESIZE`. Un CLOSE no consume coordenadas: por eso su
// entrada es 0 y por eso `currentSegment` no toca el arreglo del llamador cuando devuelve CLOSE.
//
// Detalle que se ve raro y no lo es: el campo es un `Path2D` y las coordenadas se piden por
// `floatCoordsRef()`/`doubleCoordsRef()` en vez de tipar el campo como la subclase anidada. El javac
// de esta casa no resuelve la relacion de herencia de un `Outer.Inner` mientras Outer no tenga
// .class, y este paquete tiene que compilarse de una sola invocacion por los ciclos de tipos --asi
// que ningun archivo puede depender de que `Path2D.Float` sea un `Path2D`. Con el tipo declarante no
// hay busqueda por herencia y resuelve.
class FloatPathIterator implements PathIterator {

    // Cuantas coordenadas escribe cada tipo de segmento: MOVETO 2, LINETO 2, QUADTO 4, CUBICTO 6,
    // CLOSE 0. Indexada por el valor de la constante SEG_*.
    static final int[] CURVESIZE = {2, 2, 4, 6, 0};

    Path2D path;
    float[] coordsRef;
    AffineTransform affine;
    int typeIdx;
    int pointIdx;

    FloatPathIterator(Path2D p2df, AffineTransform at) {
        this.path = p2df;
        this.coordsRef = p2df.floatCoordsRef();
        this.affine = at;
    }

    public int getWindingRule() {
        return this.path.getWindingRule();
    }

    public boolean isDone() {
        return (this.typeIdx >= this.path.numTypes);
    }

    public void next() {
        int type = this.path.pointTypes[this.typeIdx];
        this.typeIdx = this.typeIdx + 1;
        this.pointIdx = this.pointIdx + CURVESIZE[type];
    }

    public int currentSegment(float[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("path iterator out of bounds");
        }
        int type = this.path.pointTypes[this.typeIdx];
        int numCoords = CURVESIZE[type];
        if (numCoords > 0) {
            if (this.affine == null) {
                System.arraycopy(this.coordsRef, this.pointIdx, coords, 0, numCoords);
            } else {
                this.affine.transform(this.coordsRef, this.pointIdx, coords, 0, numCoords / 2);
            }
        }
        return type;
    }

    public int currentSegment(double[] coords) {
        if (isDone()) {
            throw new NoSuchElementException("path iterator out of bounds");
        }
        int type = this.path.pointTypes[this.typeIdx];
        int numCoords = CURVESIZE[type];
        if (numCoords > 0) {
            if (this.affine == null) {
                int i = 0;
                while (i < numCoords) {
                    coords[i] = (double) this.coordsRef[this.pointIdx + i];
                    i = i + 1;
                }
            } else {
                // Se amplia a double **dentro** de la transformacion, no antes: transformar en float
                // y ampliar despues redondearia dos veces y daria otro punto.
                this.affine.transform(this.coordsRef, this.pointIdx, coords, 0, numCoords / 2);
            }
        }
        return type;
    }
}
