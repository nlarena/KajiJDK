package java.nio.file.attribute;

// La identidad de un grupo. Que extienda `UserPrincipal` --y no que sean hermanos-- es de la spec, y
// es lo que permite pasarle un grupo a `setOwner` en los sistemas donde eso tiene sentido.
public interface GroupPrincipal extends UserPrincipal {
}
