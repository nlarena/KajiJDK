package javax.swing;

/**
 * Los nombres de las nueve posiciones de un {@link JScrollPane} y las seis politicas de barra.
 *
 * <p>Estan en una interfaz aparte, y no como constantes de {@code JScrollPane}, porque las usan
 * tres clases que no se heredan entre si: el panel, su distribucion y su aspecto. Cada una las
 * implementa y las nombra sin calificar.
 *
 * <p>Las posiciones son cadenas y no enteros porque son restricciones de {@code add}: van al mismo
 * lugar que {@code BorderLayout.CENTER}. Las cuatro esquinas <em>iniciales</em> y <em>finales</em>
 * —{@code LEADING}, {@code TRAILING}— se resuelven segun la orientacion del panel, y son las que
 * hay que usar para que una esquina siga estando del lado correcto en un idioma que se lee de
 * derecha a izquierda.
 */
public interface ScrollPaneConstants {

    /** La ventana que muestra el contenido. */
    String VIEWPORT = "VIEWPORT";

    String VERTICAL_SCROLLBAR = "VERTICAL_SCROLLBAR";

    String HORIZONTAL_SCROLLBAR = "HORIZONTAL_SCROLLBAR";

    /** La franja fija de la izquierda, que se desplaza solo en vertical. */
    String ROW_HEADER = "ROW_HEADER";

    /** La franja fija de arriba, que se desplaza solo en horizontal. */
    String COLUMN_HEADER = "COLUMN_HEADER";

    String LOWER_LEFT_CORNER = "LOWER_LEFT_CORNER";

    String LOWER_RIGHT_CORNER = "LOWER_RIGHT_CORNER";

    String UPPER_LEFT_CORNER = "UPPER_LEFT_CORNER";

    String UPPER_RIGHT_CORNER = "UPPER_RIGHT_CORNER";

    /** La esquina de abajo del lado por el que empieza la linea; ver la nota de la interfaz. */
    String LOWER_LEADING_CORNER = "LOWER_LEADING_CORNER";

    String LOWER_TRAILING_CORNER = "LOWER_TRAILING_CORNER";

    String UPPER_LEADING_CORNER = "UPPER_LEADING_CORNER";

    String UPPER_TRAILING_CORNER = "UPPER_TRAILING_CORNER";

    String VERTICAL_SCROLLBAR_POLICY = "VERTICAL_SCROLLBAR_POLICY";

    String HORIZONTAL_SCROLLBAR_POLICY = "HORIZONTAL_SCROLLBAR_POLICY";

    /** La barra vertical aparece solo cuando el contenido no entra. */
    int VERTICAL_SCROLLBAR_AS_NEEDED = 20;

    int VERTICAL_SCROLLBAR_NEVER = 21;

    int VERTICAL_SCROLLBAR_ALWAYS = 22;

    /** La barra horizontal aparece solo cuando el contenido no entra. */
    int HORIZONTAL_SCROLLBAR_AS_NEEDED = 30;

    int HORIZONTAL_SCROLLBAR_NEVER = 31;

    int HORIZONTAL_SCROLLBAR_ALWAYS = 32;
}
