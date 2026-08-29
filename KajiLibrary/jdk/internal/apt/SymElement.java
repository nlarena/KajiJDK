package jdk.internal.apt;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

// La **reificación** de un símbolo del compilador (`javax.lang.model` cara al procesador
// de anotaciones): un objeto del heap que envuelve el `SymbolId` de un `Symbol` de la
// tabla de símbolos de `javac`. El VM lo construye (ver `jvm::interpreter::apt`) y sus
// métodos leen la tabla —viva en el propio proceso— a través de ese `sym`.
//
// Capas 2-5: ya `implements TypeElement` de verdad. El nombre viaja como un `Name`
// (`SymName`, no `String`); el FQN sale del *binary name* con el `$` del anidamiento
// vuelto `.`; el elemento envolvente se reifica vía `Symbol.owner` (con la misma caché de
// identidad de `element_for`). `getKind()`/`getEnclosedElements()` **no** son native del
// bridge sino **intrínsecos** sobre `Exec`: el primero devuelve una constante de un enum
// (necesita correr el `<clinit>` de `ElementKind`), el segundo construye una `List` y hace
// re-entrada al intérprete por cada miembro (`ArrayList.add`) — cosas que el bridge (sin
// vista `Exec`) no puede hacer. Ambos están declarados `native` acá sólo para que la clase
// no traiga cuerpo: el intérprete los intercepta antes de llegar al bridge.
public final class SymElement implements TypeElement {
    // El `SymbolId` (índice estable en `SymbolTable.symbols`) que este elemento reifica.
    // Lo escribe el VM al construir el objeto; los native/intrínsecos de abajo lo leen para
    // indexar la tabla del compilador.
    int sym;

    // El nombre simple del símbolo (`Symbol.name`): "Foo" para `class Foo {}`, envuelto en
    // un `Name` (`SymName`).
    public native Name getSimpleName();

    // El nombre cualificado del tipo (`Symbol.binary` con `$` → `.`): "a.b.Outer.Inner".
    public native Name getQualifiedName();

    // El elemento que lo encierra léxicamente (`Symbol.owner`): la clase de un miembro, el
    // paquete de un tipo top-level. `null` si no tiene dueño. Misma identidad que
    // `element_for`, así que dos hijos del mismo dueño devuelven el **mismo** objeto.
    public native Element getEnclosingElement();

    // La clase de `ElementKind` de este símbolo (CLASS/INTERFACE/ENUM/METHOD/…). Intrínseco.
    public native ElementKind getKind();

    // Los elementos declarados directamente por este (sus miembros), como una `List`.
    // Intrínseco: construye un `ArrayList` y reifica cada miembro.
    public native List<? extends Element> getEnclosedElements();
}
