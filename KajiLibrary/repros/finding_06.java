// Finding #6 — `java.lang.Object` se emite con `super_class` apuntándose a sí mismo.
// El .class de Object sale con super_class = `java/lang/Object` en vez de `#0`. `javap` lo
// tolera y el gate de forma-de-API no lo ve, pero el intérprete arma la vtable recursando por
// la superclase → Object → Object → … → stack overflow.
//
// Esperado (javac real): SOLO Object lleva super_class = 0.
// Verificación (no es error de compilación, es de EMISIÓN):
//   cargo run -- --emit KajiLibrary/repros/finding_06.java
//   bin/javap-clon.exe -v Object.class | grep super_class     # debe ser #0
//
// Fix propuesto: caso especial this_class == java/lang/Object ⇒ super_class = 0.
package java.lang;

public class Object {}
