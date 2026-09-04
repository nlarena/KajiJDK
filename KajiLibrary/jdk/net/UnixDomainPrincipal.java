package jdk.net;

import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;

/**
 * Quien esta del otro lado de un socket de dominio Unix: su usuario y su grupo.
 *
 * <h2>Por que esto solo existe para sockets de dominio Unix</h2>
 *
 * <p>Sobre TCP, la identidad del par no es averiguable: lo unico que hay es una direccion, y una
 * direccion no dice quien corre el proceso que la usa. Un socket de dominio Unix vive dentro de una
 * sola maquina, asi que el nucleo <strong>si</strong> sabe que usuario abrio la otra punta y puede
 * contarlo — lo que convierte a estos sockets en un canal donde se puede autorizar sin credenciales
 * propias.
 *
 * <p>Se lee con la opcion {@link ExtendedSocketOptions#SO_PEERCRED}.
 *
 * <p>Es un {@code record} y no una clase con getters porque es exactamente eso: dos valores, sin
 * comportamiento, comparables por contenido.
 *
 * @param user el usuario que abrio la otra punta
 * @param group su grupo
 */
public record UnixDomainPrincipal(UserPrincipal user, GroupPrincipal group) {

    /**
     * @throws NullPointerException si alguno es {@code null} — un principal a medias no identifica
     *     a nadie, y dejarlo pasar solo cambia donde explota
     */
    // El JDK lo escribe en la forma COMPACTA (`public UnixDomainPrincipal {`), que nuestro parser
    // todavia no acepta: finding #403. La forma canonica completa es equivalente —el compilador
    // solo agrega las asignaciones que aca estan escritas— y es lo que ese finding registra como
    // rodeo.
    public UnixDomainPrincipal(UserPrincipal user, GroupPrincipal group) {
        if (user == null) {
            throw new NullPointerException("user");
        }
        if (group == null) {
            throw new NullPointerException("group");
        }
        this.user = user;
        this.group = group;
    }
}
