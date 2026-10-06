// Repro del finding #567 -- ABIERTO. Paso 1 de 2.
//
// Una interfaz ANIDADA, que el paso 2 va a leer desde su `.class` y no desde esta fuente.
public class Fuera {
    public interface Pide {
        int cuanto();
    }
}
