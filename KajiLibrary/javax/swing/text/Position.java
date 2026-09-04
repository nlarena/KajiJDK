package javax.swing.text;

/**
 * Una marca en el documento que <strong>se mueve sola</strong> cuando el texto cambia.
 *
 * <h2>Por que no alcanza con un {@code int}</h2>
 *
 * <p>Un desplazamiento es un numero, y un numero no sabe nada del documento: si alguien inserta diez
 * caracteres mas arriba, el numero sigue apuntando al mismo lugar del <em>texto viejo</em>, que ya
 * no es donde estaba lo que interesaba. Una {@code Position} la mantiene el documento y se corre con
 * las inserciones y los borrados.
 *
 * <p>Es lo que hace que un cursor, una seleccion o un marcador sobrevivan a que alguien edite arriba
 * de ellos. Todo el modelo de texto de Swing se apoya en esta distincion.
 */
public interface Position {

    /** Donde esta la marca ahora. */
    int getOffset();

    /**
     * De que lado del texto insertado se queda una marca.
     *
     * <p>La pregunta no es retorica: si se inserta exactamente en la posicion de la marca, no hay
     * respuesta obvia a si la marca queda antes o despues de lo insertado. Los dos comportamientos
     * se necesitan — un cursor quiere quedar despues de lo que acaba de escribir, y el final de un
     * resaltado quiere quedar antes.
     */
    public static final class Bias {

        /** La marca queda <em>despues</em> de lo insertado. */
        public static final Bias Forward = new Bias("Forward");

        /** La marca queda <em>antes</em> de lo insertado. */
        public static final Bias Backward = new Bias("Backward");

        private String nombre;

        private Bias(String nombre) {
            this.nombre = nombre;
        }

        public String toString() {
            return this.nombre;
        }
    }
}
