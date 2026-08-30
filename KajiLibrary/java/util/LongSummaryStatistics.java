package java.util;

import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

// Cuenta, suma, minimo, maximo y promedio de una corriente de `long`, en una sola pasada.
//
// Implementa **las dos** interfaces, `LongConsumer` e `IntConsumer`, y no es un descuido del JDK:
// un `int` entra en un `long` sin perder nada, asi que el mismo resumen sirve para una corriente
// de enteros sin obligar al llamador a convertir. `accept(int)` delega en `accept(long)`.
//
// A diferencia de IntSummaryStatistics, aca la suma **puede** desbordar: es `long` igual que los
// elementos. El JDK acepta ese limite en vez de cargar un acumulador mas ancho, y se replica.
public class LongSummaryStatistics implements LongConsumer, IntConsumer {

    private long count;
    private long sum;
    private long min = 9223372036854775807L;   // Long.MAX_VALUE
    private long max = -9223372036854775808L;  // Long.MIN_VALUE

    // Un resumen vacio, con los extremos invertidos para que el primer `accept` los fije.
    public LongSummaryStatistics() {
    }

    // Un resumen con valores ya calculados, para reconstruir uno guardado.
    public LongSummaryStatistics(long count, long min, long max, long sum) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative count value");
        }
        if (count > 0) {
            if (min > max) {
                throw new IllegalArgumentException("Minimum greater than maximum");
            }
            long promedio = sum / count;
            if (promedio < min || promedio > max) {
                throw new IllegalArgumentException("Average is out of range");
            }
        }
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    // Suma un `int`, ensanchado a `long`.
    public void accept(int value) {
        this.accept((long) value);
    }

    // Suma un valor al resumen.
    public void accept(long value) {
        this.count = this.count + 1;
        this.sum = this.sum + value;
        this.min = Math.min(this.min, value);
        this.max = Math.max(this.max, value);
    }

    // Absorbe otro resumen.
    public void combine(LongSummaryStatistics other) {
        this.count = this.count + other.count;
        this.sum = this.sum + other.sum;
        this.min = Math.min(this.min, other.min);
        this.max = Math.max(this.max, other.max);
    }

    public final long getCount() {
        return this.count;
    }

    public final long getSum() {
        return this.sum;
    }

    // El minimo, o Long.MAX_VALUE si no se acepto nada.
    public final long getMin() {
        return this.min;
    }

    // El maximo, o Long.MIN_VALUE si no se acepto nada.
    public final long getMax() {
        return this.max;
    }

    // El promedio, o 0.0 si no se acepto nada.
    public final double getAverage() {
        if (this.count > 0) {
            return (double) this.sum / this.count;
        }
        return 0.0d;
    }

    public String toString() {
        Object[] args = new Object[6];
        args[0] = this.getClass().getSimpleName();
        args[1] = Long.valueOf(this.count);
        args[2] = Long.valueOf(this.sum);
        args[3] = Long.valueOf(this.min);
        args[4] = Double.valueOf(this.getAverage());
        args[5] = Long.valueOf(this.max);
        return String.format("%s{count=%d, sum=%d, min=%d, average=%f, max=%d}", args);
    }
}
