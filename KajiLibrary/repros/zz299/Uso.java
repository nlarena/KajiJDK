package zz299;

// Repro de #299 - un tipo del MISMO paquete perdia el nombre simple contra un homonimo de otro
// paquete que se habia cargado antes.
//
//   bin\javac.exe --emit -cp "KajiLibrary;KajiLibrary/repros" KajiLibrary\repros\zz299\Type.java
//   bin\javac.exe --emit -cp "KajiLibrary;KajiLibrary/repros" KajiLibrary\repros\zz299\Uso.java
//   bin\jvm.exe --javap -p KajiLibrary\repros\zz299\Uso.class
//
// Las DOS rutas en el -cp hacen falta y son parte del repro: una trae `java.lang.Class` (que es
// quien arrastra el homonimo) y la otra trae el `zz299.Type` de este paquete. Con una sola no
// dispara.
//
// ANTES:
//
//   public abstract java.lang.reflect.Type<K> dameTipo();
//
// AHORA, y es lo que emite el javac del JDK 25:
//
//   public abstract zz299.Type<K> dameTipo();
//
// El tipo declarado en la firma emitida era **otra clase**. Compilaba, y el error viajaba adentro
// del `.class`.
//
// EL MECANISMO. El espacio de tipos externos es plano: se clavea por **nombre simple**, y ganaba el
// que se cargo primero (esta escrito en `SymbolTable::register_external`). Hasta aca el caso
// conocido era el de dos homonimos que la unidad nombra; este es peor, porque **al segundo no lo
// nombra nadie**:
//
//   1. la unidad nombra `Class` y `Type`;
//   2. `java.lang.Class` **implementa** `java.lang.reflect.Type`, asi que cargarlo lo arrastra;
//   3. ese `Type` se queda con la clave simple;
//   4. cuando le tocaba el turno al `Type` del propio paquete, la guardia de `try_load` veia la
//      clave ocupada y no hacia nada. El del paquete no se cargaba nunca.
//
// Por eso `getJavaType()` esta aca: sin el, la unidad no nombra `Class` y el repro no dispara.
//
// COMO APARECIO, que es la parte que vale: **dos recompilaciones seguidas de la biblioteca daban
// `.class` distintos**. No un `.class` mal, no un error: dos corridas identicas con resultados
// distintos. Los nombres que la unidad escribe y los tipos core de `java.lang` iban al mismo
// `HashSet` y se recorrian en su orden -- o sea, en ninguno --, asi que ganaba `Class` o `Type`
// segun el hash.
//
// AHORA, en dos partes:
//
//   - `load_externals` recorre la fase 1 en dos pasos y **ordenada**: primero los nombres que la
//     unidad escribe, despues los core de `java.lang`. Eso mata la no-determinacion.
//   - `try_load` deja de salir temprano cuando la clave esta ocupada: si el candidato que
//     corresponde es de los que el JLS hace **prevalecer** --el paquete propio o un `import` de un
//     solo tipo, §6.5.5.1-- le **quita** la clave al ocupante (`SymbolTable::override_external`).
//
// Solo esos dos candidatos reclaman la clave. `java.lang`, los `import` on-demand y el nombre
// pelado no: ahi el desempate por "el primero que llego" es tan bueno como cualquier otro, y pisar
// seria mas riesgo que beneficio.
//
// Lo que arreglo en la biblioteca: las cuatro clases de `jakarta.persistence.metamodel` que
// declaraban `java.lang.reflect.Type` -- `BasicType`, `MapAttribute`, `PluralAttribute` y
// `SingularAttribute` -- ya declaran el `Type` de su paquete.
public interface Uso<K> {

    // Este es el que salia mal.
    Type<K> dameTipo();

    // Y este es el que lo causaba: nombrar `Class` arrastra `java.lang.reflect.Type`.
    Class<K> getJavaType();
}
