// Repro de #04 - un tipo del classpath no se auto-cargaba por su nombre SIMPLE estando en el
// MISMO paquete.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_04.java
//
// ANTES: `List` esta en el classpath (`java/util/List.class`) y la clase declara
// `package java.util`, o sea que es visible sin import. Igual fallaba:
//
//   error: no se encuentra el simbolo: List
//
// El rodeo era agregar `import java.util.List;` — un import que en Java real sobra.
//
// AHORA: **resuelve**. Comprobado aparte con una clase minima en `package java.util` que nombra
// `List` sin import: compila.
//
// OJO, que este archivo tuvo que cambiar: `P` se declara `abstract`. No es un rodeo del defecto
// original, es al reves — el chequeo de completitud de metodos abstractos (#08) hoy SI funciona,
// y `List` crecio con la tanda de colecciones, asi que un `class P implements List<Object> {}`
// concreto es correctamente rechazado por no implementar nada. Dejarlo concreto haria que el
// archivo fallara por un motivo que no es el que viene a probar.
//
// Queda como REGRESION del nombre simple en el mismo paquete.
package java.util;

public abstract class P implements List<Object> {}
