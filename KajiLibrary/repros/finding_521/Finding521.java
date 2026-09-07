package repros.finding_521;

/**
 * Hallazgo #521: {@code "texto" + unInteger} desempaqueta en vez de usar
 * {@code String.valueOf(Object)}, asi que un envoltorio nulo revienta.
 *
 * <h2>Que se espera</h2>
 *
 * <p>La especificacion dice que en una concatenacion de cadenas un operando de tipo referencia se
 * convierte con {@code String.valueOf(Object)}, y esa conversion contesta {@code "null"} para el
 * nulo. Nunca se desempaqueta: desempaquetar es lo que se hace para <em>aritmetica</em>, no para
 * concatenar.
 *
 * <h2>Que pasa</h2>
 *
 * <p>Con un {@code Integer} nulo --venga de una variable o de un metodo-- sale
 * {@code NullPointerException}. Con el <em>mismo</em> valor guardado en un {@code Object} anda y
 * escribe {@code "null"}, que es la prueba de que el problema es la eleccion de la sobrecarga y no
 * el nulo en si.
 *
 * <h2>Como se encontro</h2>
 *
 * <p>Escribiendo {@code BasicSliderUI}: {@code getLowestValue()} devuelve {@code Integer} y es nulo
 * cuando el deslizador no tiene tabla de etiquetas, que es el caso comun. La prueba diferencial
 * imprimia ese valor y reventaba.
 *
 * <h2>Como correrlo</h2>
 *
 * <pre>
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_521/Finding521.java
 *   target/release/run-headless.exe KajiLibrary/repros/finding_521/Finding521.class run
 * </pre>
 *
 * <p>Esperado: cuatro lineas {@code null}. Lo que sale: las dos primeras revientan.
 */
public class Finding521 {

    static Integer nulo() {
        return null;
    }

    public static int run() {
        Integer i = null;
        try {
            System.out.println("//variable=" + i);
        } catch (Throwable e) {
            System.out.println("//variable revienta: " + e.getClass().getName());
        }
        try {
            System.out.println("//metodo=" + nulo());
        } catch (Throwable e) {
            System.out.println("//metodo revienta: " + e.getClass().getName());
        }
        // Los dos que si andan, y que muestran que el problema es la sobrecarga elegida.
        System.out.println("//valueOf=" + String.valueOf((Object) nulo()));
        Object o = nulo();
        System.out.println("//por Object=" + o);
        return 0;
    }
}
