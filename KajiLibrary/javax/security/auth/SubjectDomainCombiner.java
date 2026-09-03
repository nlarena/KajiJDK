package javax.security.auth;

import java.security.DomainCombiner;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.Set;

/**
 * KajiLibrary's javax.security.auth.SubjectDomainCombiner -- le pega las identidades de un
 * {@link Subject} a los dominios de proteccion de la pila.
 *
 * <p>Un {@code ProtectionDomain} dice de donde salio el codigo y que puede hacer. Este combinador
 * agrega la otra mitad de la pregunta: <b>en nombre de quien</b> esta corriendo. Sin el, una politica
 * solo puede decidir por origen del codigo; con el, puede decir "este jar puede leer ese archivo
 * <i>solo si</i> lo esta corriendo juan".
 *
 * <p>El trabajo es un recorrido: por cada dominio de la pila actual se arma uno nuevo con el mismo
 * origen, el mismo cargador y los mismos permisos, mas los principals del Subject; despues se le
 * pegan atras los dominios ya asignados.
 *
 * <p>Con una excepcion que hay que reproducir porque es observable: un dominio de <b>permisos
 * estaticos</b> --el que se construyo con la coleccion de permisos ya cerrada-- se devuelve
 * <b>tal cual</b>, la misma instancia. La razon no es ahorrar un objeto: un dominio estatico dice
 * "estos permisos y nada mas, para siempre", asi que agregarle identidades no cambiaria ninguna
 * respuesta -- su {@code implies} ya no consulta la politica --. Los dinamicos, en cambio, si se
 * rehacen, porque ahi las identidades entran en la consulta.
 *
 * <p>Nota sobre para que sirve hoy: el gestor de seguridad ya no se puede habilitar, asi que nada de
 * la biblioteca llama a este combinador. La clase existe porque su forma es parte del API y porque
 * el calculo que hace es puro -- no depende de que haya un gestor -- y se puede mirar y probar.
 */
public class SubjectDomainCombiner implements DomainCombiner {

    private final Subject subject;

    public SubjectDomainCombiner(Subject subject) {
        this.subject = subject;
    }

    /** El Subject cuyas identidades se pegan. La misma instancia que se paso. */
    public Subject getSubject() {
        return this.subject;
    }

    /**
     * Los dominios de la pila con las identidades del Subject encima, seguidos de los ya asignados.
     *
     * <p>Si no hay dominios actuales devuelve los asignados <b>tal cual</b> --incluido null--: no hay
     * nada a que pegarle las identidades, y armar un arreglo vacio seria decir algo distinto de "no
     * habia nada".
     */
    public ProtectionDomain[] combine(ProtectionDomain[] currentDomains,
            ProtectionDomain[] assignedDomains) {
        if (currentDomains == null || currentDomains.length == 0) {
            return assignedDomains;
        }
        Principal[] principalsOf = principalsOf();
        int assignedCount = assignedDomains == null ? 0 : assignedDomains.length;
        ProtectionDomain[] out =
            new ProtectionDomain[currentDomains.length + assignedCount];
        int i = 0;
        while (i < currentDomains.length) {
            ProtectionDomain d = currentDomains[i];
            // Ver la nota de la clase: al estatico no le cambia nada tener identidades encima.
            out[i] = d.staticPermissionsOnly() ? d
                : new ProtectionDomain(d.getCodeSource(), d.getPermissions(),
                    d.getClassLoader(), principalsOf);
            i = i + 1;
        }
        int j = 0;
        while (j < assignedCount) {
            out[currentDomains.length + j] = assignedDomains[j];
            j = j + 1;
        }
        return out;
    }

    private Principal[] principalsOf() {
        if (this.subject == null) {
            return new Principal[0];
        }
        Set<Principal> ps = this.subject.getPrincipals();
        Principal[] arr = new Principal[ps.size()];
        int i = 0;
        java.util.Iterator<Principal> it = ps.iterator();
        while (it.hasNext()) {
            arr[i] = it.next();
            i = i + 1;
        }
        return arr;
    }
}
