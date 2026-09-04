package java.security;

// La foto de los dominios de proteccion en juego en un momento dado.
//
// La idea que codifica es la del **privilegio minimo de la pila**: una operacion esta permitida
// solo si **todos** los dominios de la cadena de llamadas la permiten. Alcanza con que uno solo no
// la tenga para que se niegue, y por eso codigo confiable llamado desde codigo no confiable no
// puede hacer mas de lo que el llamador podia — que es lo que impide el ataque del diputado
// confundido.
//
// Guardar el contexto en un objeto permite recuperarlo despues: un trabajo que se encola y se
// ejecuta en otro hilo se corre con el contexto de quien lo encolo, no con el del hilo que lo
// saca de la cola, que podria ser mucho mas privilegiado.
//
// **`checkPermission` siempre tira.** Igual que `Permission.checkGuard` y por el mismo motivo: sin
// `SecurityManager` no hay quien conteste, y el JDK 25 responde negando —`AccessControlException:
// checking permissions is not supported`— en vez de dejar pasar. Verificado contra el JDK real.
@Deprecated
public final class AccessControlContext {

    private final ProtectionDomain[] context;

    private final DomainCombiner combiner;

    // El contexto formado por esos dominios.
    //
    // Se filtran los null y los repetidos: un dominio dos veces no restringe mas que una, y
    // dejarlos haria que dos contextos equivalentes no fueran iguales.
    public AccessControlContext(ProtectionDomain[] context) {
        if (context == null) {
            throw new NullPointerException("null context");
        }
        java.util.ArrayList<ProtectionDomain> unicos =
            new java.util.ArrayList<ProtectionDomain>();
        int i = 0;
        while (i < context.length) {
            ProtectionDomain pd = context[i];
            if (pd != null && !unicos.contains(pd)) {
                unicos.add(pd);
            }
            i = i + 1;
        }
        this.context = new ProtectionDomain[unicos.size()];
        int j = 0;
        while (j < unicos.size()) {
            this.context[j] = unicos.get(j);
            j = j + 1;
        }
        this.combiner = null;
    }

    // El mismo contexto, pero con un combinador de dominios asociado.
    public AccessControlContext(AccessControlContext acc, DomainCombiner combiner) {
        if (acc == null) {
            throw new NullPointerException("null context");
        }
        this.context = acc.context;
        this.combiner = combiner;
    }

    public DomainCombiner getDomainCombiner() {
        return this.combiner;
    }

    // Siempre tira. Ver la cabecera.
    public void checkPermission(Permission perm) throws AccessControlException {
        if (perm == null) {
            throw new NullPointerException("permission can't be null");
        }
        throw new AccessControlException("checking permissions is not supported", perm);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AccessControlContext)) {
            return false;
        }
        AccessControlContext that = (AccessControlContext) obj;
        if (this.combiner == null) {
            if (that.combiner != null) {
                return false;
            }
        } else if (!this.combiner.equals(that.combiner)) {
            return false;
        }
        if (this.context.length != that.context.length) {
            return false;
        }
        // Comparacion como conjunto, no como lista: el orden de la pila no cambia que permisos
        // resultan de la interseccion.
        int i = 0;
        while (i < this.context.length) {
            boolean hallado = false;
            int j = 0;
            while (j < that.context.length) {
                if (this.context[i].equals(that.context[j])) {
                    hallado = true;
                    j = that.context.length;
                } else {
                    j = j + 1;
                }
            }
            if (!hallado) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        int i = 0;
        while (i < this.context.length) {
            h = h + this.context[i].hashCode();
            i = i + 1;
        }
        return h;
    }
}
