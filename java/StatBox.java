// Campo estático de tipo referencia → vive en el mirror Class<StatBox>.
// Renombrada de `Box` (#273): `MHCtor.java` declara otra clase `Box` y su `.class` es el que
// esta en el arbol, asi que este par -- `StatBox`/`Stat` -- llevaba tiempo sin compilar. Se
// renombra el lado que SI se puede regenerar: `MHCtor.java` no compila hoy, porque
// `MethodHandle.invoke` es signature-polymorphic y el compilador todavia no lo resuelve.
class StatBox {
    static Animal shared;
}
