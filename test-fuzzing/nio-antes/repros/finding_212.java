// Repro de #212 - el bound de un parametro de tipo se escribe en Signature con el
// NOMBRE SIMPLE, salvo que el tipo aparezca ademas en una posicion ordinaria del
// mismo archivo.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_212.java
//   javap -v -p -cp KajiLibrary\repros finding_212Solo finding_212Acomp
//
//   finding_212Solo  -> Signature: <T:LMap;>()TT;                MAL
//   finding_212Acomp -> Signature: <T::Ljava/util/Map;>()TT;     bien
//
// Se pierden DOS cosas: el nombre calificado, y el marcador de tipo de cota
// (`:` de clase donde corresponde `::` de interfaz).
import java.util.Map;

interface finding_212Solo  { <T extends Map> T only(); }
interface finding_212Acomp { <T extends Map> T only(); Map otroUso(); }

public class finding_212 { }
