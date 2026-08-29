// El auxiliar lleva el prefijo del probe: `java/` es un paquete por defecto **plano**, asi
// que dos fuentes que declaren la misma clase escriben el mismo `.class` y gana la ultima
// compilada -- el resultado de la suite pasa a depender del orden de compilacion (#273).
// `Shape.java` declara los mismos tres tipos y se queda con los nombres pelados; esta unidad
// es casi un duplicado suyo -- la unica diferencia es el `public` de la interfaz.
public sealed interface SealedShape permits SealedCircle, SealedSquare {}
final class SealedCircle implements SealedShape {}
final class SealedSquare implements SealedShape {}
