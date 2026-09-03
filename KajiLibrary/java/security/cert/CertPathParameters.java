package java.security.cert;

// Los parametros de un algoritmo de validacion o de construccion de caminos.
//
// Es una interfaz marcadora con `clone()`: cada algoritmo define sus propios parametros
// —`PKIXParameters` es el unico que trae el JDK— y lo unico comun es que se puedan copiar. La copia
// no es prolijidad: estos objetos son mutables y el validador se los queda, asi que sin ella
// cambiar los parametros despues de arrancar cambiaria las reglas a mitad de la validacion.
public interface CertPathParameters extends Cloneable {

    Object clone();
}
