package pq;

public class Constantes {

    /** El tipo anidado se nombra con la forma binaria, como en toda la biblioteca. */
    public static class Marca implements Conjunto$Atributo {

        private final String nombre;

        public Marca(String nombre) {
            this.nombre = nombre;
        }

        public String toString() {
            return nombre;
        }
    }

    public static final Conjunto$Atributo NEGRITA = new Marca("negrita");

    /** Control: el mismo tipo, con el nombre punteado, si resuelve en el lote. */
    public static class MarcaPunteada implements Conjunto.Atributo {
    }
}
