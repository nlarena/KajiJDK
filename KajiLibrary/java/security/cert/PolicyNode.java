package java.security.cert;

import java.util.Iterator;
import java.util.Set;

// Un nodo del arbol de politicas que produce la validacion PKIX.
//
// Las politicas de certificado son la parte de PKIX que casi nadie usa, y vale decir para que
// estan: una CA puede declarar bajo que reglas emitio un certificado —cuanta verificacion de
// identidad hizo, por ejemplo— y quien valida puede exigir que la cadena entera sostenga una
// politica determinada de punta a punta. El arbol es el resultado de esa cuenta: cada nivel
// corresponde a un certificado del camino, y las ramas que sobreviven son las politicas que valen
// para toda la cadena.
//
// Todos los metodos son de solo lectura y el arbol lo construye el validador: no hay forma de
// armarlo a mano desde el API publico, y eso es intencional.
public interface PolicyNode {

    // El nodo padre, o null si es la raiz.
    PolicyNode getParent();

    // Los hijos. El iterador es inmutable.
    Iterator<? extends PolicyNode> getChildren();

    // La profundidad: 0 en la raiz, y coincide con la posicion en el camino.
    int getDepth();

    // El OID de la politica que representa este nodo.
    String getValidPolicy();

    // Los calificadores asociados: texto legal, URLs de la declaracion de practicas de la CA.
    Set<? extends PolicyQualifierInfo> getPolicyQualifiers();

    // Los OIDs que un hijo podria tener para seguir esta rama.
    Set<String> getExpectedPolicies();

    // Si la extension de politicas del certificado de este nivel venia marcada critica.
    boolean isCritical();
}
