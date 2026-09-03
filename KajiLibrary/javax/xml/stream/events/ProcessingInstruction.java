package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.ProcessingInstruction -- un
 * {@code <?destino datos?>}.
 *
 * <h2>El unico lugar donde XML deja pasar algo que no es XML</h2>
 *
 * <p>Una instruccion de procesamiento es un mensaje para una aplicacion concreta, metido en el
 * documento sin que el analizador tenga que entenderlo. El caso clasico es
 * {@code <?xml-stylesheet type="text/xsl" href="hoja.xsl"?>}: no dice nada sobre los datos, le dice
 * a quien los muestre como mostrarlos.
 *
 * <p>Lo que la hace distinta de un comentario es que <b>si</b> esta dirigida a alguien:
 * {@link #getTarget()} nombra a ese alguien, y {@link #getData()} devuelve el resto crudo, sin
 * interpretar. XML no define ninguna estructura para los datos --que parezcan atributos en el
 * ejemplo de arriba es una convencion, no una regla-- asi que la aplicacion los parsea como quiera.
 *
 * <p>La declaracion {@code <?xml version="1.0"?>} del principio del documento <b>no</b> es una
 * instruccion de procesamiento, aunque se le parezca: es sintaxis propia del prologo y llega como
 * {@link StartDocument}.
 */
public interface ProcessingInstruction extends XMLEvent {

    /**
     * A quien esta dirigida: el nombre que va inmediatamente despues de {@code <?}.
     *
     * @return el destino; nunca null y nunca vacio
     */
    String getTarget();

    /**
     * El resto de la instruccion, crudo.
     *
     * <p>Se descarta el espacio que separa el destino de los datos, y nada mas: no hay entidades
     * que resolver ni escapes que deshacer dentro de una instruccion de procesamiento.
     *
     * @return los datos, o null si la instruccion solo traia el destino
     */
    String getData();
}
