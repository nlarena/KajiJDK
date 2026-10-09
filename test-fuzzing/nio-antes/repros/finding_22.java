package repro22;

// Repro de #22 - la resolucion de metodos sobre `this` ignoraba TODO lo heredado.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_22.java
//
// ANTES: la resolucion sobre `this` (receptor explicito o implicito) solo miraba los metodos
// DECLARADOS en la propia clase. Fallaban todos estos, que el JDK compila:
//
//   this.getClass()                       error: no se encuentra el metodo: getClass
//   this.hashCode()   (heredado)          error: no se encuentra el metodo: hashCode
//   this.toString()   (heredado)          error: no se encuentra el metodo: toString
//   getClass()        (receptor implicito) error: no se encuentra el metodo: getClass
//   this.id()         (de interfaz)       error: no se encuentra el metodo: id
//
// Y andaban solo los dos que no dependian de la herencia: `this.hashCode()` con override propio,
// y `other.id()` a traves de una variable tipada por la interfaz.
//
// AHORA: **compila entero**. `#22` figura arreglado en COMPILER_FINDINGS.md, y de paso se
// quitaron los rodeos que habia dejado en `AbstractChronology`.
//
// Queda como REGRESION: si la resolucion vuelve a mirar solo las declaraciones propias, este
// archivo deja de compilar y lo dice en la primera linea.
interface I {
    String id();
}

class C {
    // A concrete class: inherited Object methods on `this` also fail to resolve.
    int inheritedObjectMethod() {
        return this.getClass().hashCode();   // heredados de Object: fallaban con #22, hoy resuelven
    }
    int ownOverride() {
        return this.hashCode();              // heredado: fallaba salvo que se declarara aca
    }
    public int hashCode() { return 1; }      // with this present, this.hashCode() above would resolve
}

abstract class A implements I {
    String viaThis() {
        return this.id();                    // heredado de la interfaz I: fallaba con #22
    }
    String viaParam(I other) {
        return other.id();                   // OK: same method on a variable of the interface type
    }
    String workaround() {
        I self = this;
        return self.id();                    // OK: rebinding `this` to the interface type resolves it
    }
}
