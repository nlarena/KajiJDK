package java.rmi;

/**
 * KajiLibrary's java.rmi.Remote -- esta interfaz se puede llamar desde otra maquina virtual.
 *
 * <p>No declara nada. Es una <b>marca</b>: lo unico que hace es decirle a RMI que las interfaces que
 * la extienden describen objetos remotos.
 *
 * <h2>Por que hace falta marcar</h2>
 *
 * <p>Porque una llamada remota no se comporta como una local, y el codigo tiene que poder distinguir:
 *
 * <ul>
 *   <li>los argumentos y el resultado se copian por serializacion, no se pasan por referencia --salvo
 *       que sean a su vez objetos remotos--;
 *   <li>cualquier llamada puede fallar por la red, y por eso <b>todos</b> los metodos de una interfaz
 *       remota tienen que declarar {@link RemoteException};
 *   <li>{@code equals}, {@code hashCode} y {@code toString} sobre una referencia remota hablan del
 *       talon local, no del objeto de alla.
 * </ul>
 *
 * <p>Una interfaz que no declare {@code RemoteException} en algun metodo no se puede exportar, y ese
 * error se descubre al exportar y no al compilar. Es el tropiezo mas comun de RMI.
 */
public interface Remote {
}
