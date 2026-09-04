package java.security;

// Sabe combinar dos listas de dominios de proteccion en la que finalmente se usa.
//
// Existe para el caso en que los permisos efectivos no son simplemente la interseccion de la pila
// de llamadas: el ejemplo canonico es JAAS, donde a los dominios del codigo hay que agregarles los
// del sujeto autenticado. Sin este gancho, "quien sos" no podria influir sobre "que podes hacer".
@Deprecated
public interface DomainCombiner {

    // Combina los dominios de la ejecucion actual con los heredados del contexto.
    ProtectionDomain[] combine(ProtectionDomain[] currentDomains,
                               ProtectionDomain[] assignedDomains);
}
