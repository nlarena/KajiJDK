package java.util;

import java.util.function.IntConsumer;

// Cuenta, suma, minimo, maximo y promedio de una corriente de `int`, en una sola pasada.
//
// Es un `IntConsumer`, y eso es todo su diseño: se le van dando valores con `accept` y el estado
// va quedando, sin guardar los elementos. Por eso `Collectors.summarizingInt` puede resumir una
// coleccion de cualquier tamaño con memoria constante.
//
// La suma es `long` aunque los elementos sean `int`: sumar dos mil millones de enteros medianos
// desborda un `int` mucho antes de agotar la corriente, y un resumen que se pasa de largo en
// silencio no sirve para nada.
public class IntSummaryStatistics implements IntConsumer {

    private long count;
    private long sum;
    private int min = 2147483647;   // Integer.MAX_VALUE
    private int max = -2147483648;  // Integer.MIN_VALUE

    // Un resumen vacio: cuenta y suma en cero, minimo en MAX_VALUE y maximo en MIN_VALUE.
    //
    // Los extremos arrancan invertidos a proposito, para que el primer `accept` los fije sin
    // necesitar un caso aparte. La consecuencia es que un resumen vacio devuelve MAX_VALUE por
    // `getMin()`, que es lo que hace el JDK y lo que hay que saber al leerlo.
    public IntSummaryStatistics() {
    }

    // Un resumen con valores ya calculados, para reconstruir uno guardado.
    public IntSummaryStatistics(long count, int min, int max, long sum) {
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

    // Suma un valor al resumen.
    public void accept(int value) {
        this.count = this.count + 1;
        this.sum = this.sum + value;
        this.min = Math.min(this.min, value);
        this.max = Math.max(this.max, value);
    }

    // Absorbe otro resumen. Sirve para juntar los parciales de una corriente dividida.
    public void combine(IntSummaryStatistics other) {
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

    // El minimo, o Integer.MAX_VALUE si no se acepto nada.
    public final int getMin() {
        return this.min;
    }

    // El maximo, o Integer.MIN_VALUE si no se acepto nada.
    public final int getMax() {
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
        args[3] = Integer.valueOf(this.min);
        args[4] = Double.valueOf(this.getAverage());
        args[5] = Integer.valueOf(this.max);
        return String.format("%s{count=%d, sum=%d, min=%d, average=%f, max=%d}", args);
    }
}
