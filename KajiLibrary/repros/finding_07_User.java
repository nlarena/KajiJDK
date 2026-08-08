// Finding #7 (parte b) — referencia a una clase KajiLibrary-only (`Sib`) desde otro archivo.
// Sin `-cp` que apunte a donde vive `Sib`, esto no compila.
//
// Esperado (javac real, con -cp): resuelve `Sib`.
// Síntoma del bug:                "no se encuentra el símbolo: Sib".
//
// Repro: cargo run -- --emit KajiLibrary/repros/finding_07_User.java
public class User {
    int r() { return Sib.v(); }
}
