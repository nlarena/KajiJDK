package java.awt.geom;

// java.awt.geom.PathIterator de KajiLibrary -- el protocolo con el que toda Shape se recorre
// segmento por segmento. La superficie esta completa: cinco metodos y siete constantes.
//
// El contrato que hay que respetar al implementarla: `currentSegment` escribe en `coords` tantos
// pares (x,y) como pida el tipo devuelto -- 1 para MOVETO/LINETO, 2 para QUADTO, 3 para CUBICTO,
// 0 para CLOSE -- y no toca el resto del arreglo.
public interface PathIterator {

    /** Regla par/impar: un punto esta dentro si lo cruza un numero impar de segmentos. */
    public static final int WIND_EVEN_ODD = 0;

    /** Regla no-cero: un punto esta dentro si la suma de cruces con signo no es cero. */
    public static final int WIND_NON_ZERO = 1;

    public static final int SEG_MOVETO = 0;
    public static final int SEG_LINETO = 1;
    public static final int SEG_QUADTO = 2;
    public static final int SEG_CUBICTO = 3;
    public static final int SEG_CLOSE = 4;

    public abstract int getWindingRule();

    public abstract boolean isDone();

    public abstract void next();

    public abstract int currentSegment(float[] coords);

    public abstract int currentSegment(double[] coords);
}
