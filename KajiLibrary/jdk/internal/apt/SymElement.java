package jdk.internal.apt;

// La **reificación** de un símbolo del compilador (`javax.lang.model` cara al procesador
// de anotaciones): un objeto del heap que envuelve el `SymbolId` de un `Symbol` de la
// tabla de símbolos de `javac`. El VM lo construye (ver `jvm::interpreter::apt`) y sus
// métodos `native` leen la tabla —viva en el propio proceso— a través de ese `sym`.
//
// Capa 1 (hito mínimo): sólo el nombre. Todavía **no** `implements TypeElement`: esa
// interfaz declara `getSimpleName(): Name` (no `String`) más `getKind`/`getEnclosedElements`
// (enum/lista), que son las capas siguientes del plan. Por ahora es una clase suelta con el
// campo puente y los dos accesores de nombre.
public final class SymElement {
    // El `SymbolId` (índice estable en `SymbolTable.symbols`) que este elemento reifica.
    // Lo escribe el VM al construir el objeto; los `native` de abajo lo leen para indexar
    // la tabla del compilador.
    int sym;

    // El nombre simple del símbolo (`Symbol.name`): "Foo" para `class Foo {}`.
    public native String getSimpleName();

    // El nombre cualificado del tipo (por ahora, también el simple para un tipo raíz).
    public native String getQualifiedName();
}
