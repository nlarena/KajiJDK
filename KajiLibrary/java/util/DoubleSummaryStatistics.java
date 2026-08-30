package java.util;

import java.util.function.DoubleConsumer;

// Cuenta, suma, minimo, maximo y promedio de una corriente de `double`, en una sola pasada.
//
// La suma NO es un `sum += value` a secas: usa **suma compensada de Kahan**, y esa es la unica
// parte de esta clase que no es obvia. Al sumar muchos valores de magnitudes distintas, cada
// suma en punto flotante pierde los bits bajos del sumando mas chico; sobre un millon de
// elementos ese error se acumula y el resultado puede estar mal en las primeras cifras. Kahan
// lleva aparte lo que se perdio en cada paso y lo devuelve al siguiente.
//
// El JDK lleva ademas `simpleSum`, la suma ingenua, **solo** para un caso de borde: si la
// compensada da NaN (puede pasar sumando infinitos de signos opuestos) pero la ingenua dio un
// infinito, el infinito es la respuesta correcta y es la que se devuelve.
public class DoubleSummaryStatistics implements DoubleConsumer {

    private long count;

    // La suma compensada, y lo que quedo pendiente de compensar.
    private double sum;
    private double sumCompensation;

    // La suma ingenua, solo para desempatar el caso NaN/infinito de `getSum`.
    private double simpleSum;

    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    // Un resumen vacio. Los extremos arrancan en los infinitos opuestos, por lo mismo que en las
    // versiones enteras: para que el primer `accept` los fije sin un caso aparte.
    public DoubleSummaryStatistics() {
    }

    // Un resumen con valores ya calculados, para reconstruir uno guardado.
    public DoubleSummaryStatistics(long count, double min, double max, double sum) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative count value");
        }
        if (count > 0) {
            if (min > max) {
                throw new IllegalArgumentException("Minimum greater than maximum");
            }
            if (!Double.isNaN(min) && !Double.isNaN(max) && !Double.isNaN(sum)) {
                double promedio = sum / count;
                if (promedio < min || promedio > max) {
                    throw new IllegalArgumentException("Average is out of range");
                }
            }
        }
        this.count = count;
        this.sum = sum;
        this.simpleSum = sum;
        this.sumCompensation = 0.0d;
        this.min = min;
        this.max = max;
    }

    // Suma un valor al resumen.
    public void accept(double value) {
        this.count = this.count + 1;
        this.simpleSum = this.simpleSum + value;
        this.sumaCompensada(value);
        this.min = Math.min(this.min, value);
        this.max = Math.max(this.max, value);
    }

    // Un paso de Kahan: `sumCompensation` guarda lo que la suma anterior no pudo representar, se
    // lo descuenta al sumando nuevo, y despues se recalcula cuanto quedo pendiente esta vez.
    private void sumaCompensada(double value) {
        double ajustado = value - this.sumCompensation;
        double nueva = this.sum + ajustado;
        this.sumCompensation = (nueva - this.sum) - ajustado;
        this.sum = nueva;
    }

    // Absorbe otro resumen.
    //
    // Se suman las dos partes de Kahan del otro por separado —la suma y, con signo cambiado, su
    // pendiente— para no perder la compensacion que el otro venia acarreando.
    public void combine(DoubleSummaryStatistics other) {
        this.count = this.count + other.count;
        this.simpleSum = this.simpleSum + other.simpleSum;
        this.sumaCompensada(other.sum);
        this.sumaCompensada(-other.sumCompensation);
        this.min = Math.min(this.min, other.min);
        this.max = Math.max(this.max, other.max);
    }

    public final long getCount() {
        return this.count;
    }

    // La suma, compensada.
    //
    // El desempate: si la compensada dio NaN pero la ingenua dio infinito, gana la ingenua. Es el
    // caso de sumar infinitos de signos opuestos, donde la correccion de Kahan produce un NaN que
    // no describe el resultado.
    public final double getSum() {
        double compensada = this.sum - this.sumCompensation;
        if (Double.isNaN(compensada) && Double.isInfinite(this.simpleSum)) {
            return this.simpleSum;
        }
        return compensada;
    }

    // El minimo, o POSITIVE_INFINITY si no se acepto nada. NaN si algun valor lo era.
    public final double getMin() {
        return this.min;
    }

    // El maximo, o NEGATIVE_INFINITY si no se acepto nada. NaN si algun valor lo era.
    public final double getMax() {
        return this.max;
    }

    // El promedio, o 0.0 si no se acepto nada.
    public final double getAverage() {
        if (this.count > 0) {
            return this.getSum() / this.count;
        }
        return 0.0d;
    }

    public String toString() {
        Object[] args = new Object[6];
        args[0] = this.getClass().getSimpleName();
        args[1] = Long.valueOf(this.count);
        args[2] = Double.valueOf(this.getSum());
        args[3] = Double.valueOf(this.getMin());
        args[4] = Double.valueOf(this.getAverage());
        args[5] = Double.valueOf(this.getMax());
        return String.format("%s{count=%d, sum=%f, min=%f, average=%f, max=%f}", args);
    }
}
