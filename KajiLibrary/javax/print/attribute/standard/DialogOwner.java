package javax.print.attribute.standard;

import javax.print.attribute.Attribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Quien es la ventana duenia del dialogo de impresion: sobre ella se centra el dialogo y a ella
 * bloquea mientras esta abierto.
 *
 * <p>Es el unico atributo del paquete que no es un dato puro. Los demas describen papel, tinta o
 * estado; este apunta a un objeto vivo de la interfaz grafica, y por eso es el unico que no se
 * puede implementar entero aca.
 *
 * <p>Esta clase decia que le faltaban {@code DialogOwner(java.awt.Window)} y {@code getOwner()}
 * porque {@code java.awt.Window} no existia en este arbol --no se puede declarar un metodo cuyo tipo
 * de retorno no existe-- y que el dia que apareciera eran tres lineas. Aparecio, y son estas.
 *
 * <h2>La ventana es {@code transient}, y eso no es un descuido</h2>
 *
 * <p>Un {@link javax.print.attribute.Attribute} es serializable, y una ventana viva no se puede
 * serializar de forma util: lo que se recupere en otra maquina, o en otra corrida, no seria la misma
 * ventana ni podria serlo. Al deserializar el duenio queda en null, que es exactamente lo que
 * significa el constructor sin argumentos --"la duenia es la ventana del propio dialogo"-- y por lo
 * tanto un estado legal y no un agujero.
 *
 * <p>{@code getOwner()} devuelve null tambien cuando se construyo con {@code null}, que el JDK
 * acepta sin quejarse; se comprobo contra el JDK 25.
 */
public final class DialogOwner implements PrintRequestAttribute {

    private static final long serialVersionUID = -1901909867156076547L;

    /** La ventana duenia, o null. `transient` por lo que dice la cabecera. */
    private final transient java.awt.Window owner;

    /** Sin ventana explicita: la duenia es la del propio dialogo. */
    public DialogOwner() {
        this.owner = null;
    }

    /**
     * Con esa ventana como duenia.
     *
     * @param window la ventana; {@code null} vale y equivale al constructor sin argumentos, que es
     *     lo que hace el JDK
     */
    public DialogOwner(java.awt.Window window) {
        this.owner = window;
    }

    /** La ventana duenia, o null si no se dio ninguna. */
    public java.awt.Window getOwner() {
        return this.owner;
    }

    public final Class<? extends Attribute> getCategory() {
        return DialogOwner.class;
    }

    public final String getName() {
        return "dialog-owner";
    }
}
