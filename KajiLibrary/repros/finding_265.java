// #265 -- una asignacion GRANDE hace que la VM lea un u32 pasando el fin del heap y panique.
//
//   panicked at src\jvm\interpreter\heap.rs:752:51:
//   range end index 55815 out of range for slice of length 55811
//
// Cuatro bytes exactos pasado el final, o sea la lectura de UNA palabra de cabecera sobre un
// objeto cuyo offset cae justo en el limite. No depende del contenido: alcanza con concatenar
// hasta unos cuantos cientos de chars y decodificar.
//
// PREEXISTENTE y ajeno a lo que se estaba tocando cuando aparecio: se verifico guardando los
// cambios de la VM de esa sesion (`git stash`) y reconstruyendo -- panica igual, con los mismos
// dos numeros. Salio a la luz recien cuando `StringBuilder` empezo a copiar de verdad (el fix de
// #261 en `System.arraycopy`), porque hasta entonces el buffer no crecia y se asignaba menos.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_265.java
//   target\debug\run-headless.exe KajiLibrary\repros\finding_265.class run
public class finding_265 {

    /** Devuelve 0 si sobrevive. Sobre el JDK 25 da 0. */
    public static int run() {
        char[] u = new char[5];
        u[0] = 'a'; u[1] = 'b'; u[2] = 'c'; u[3] = (char) 0x20ac; u[4] = 'd';
        String s = String.valueOf(u, 0, 5);
        int k = 0;
        while (k < 7) {
            s = s + s;
            k = k + 1;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            sb.append(s.charAt(i));
            i = i + 1;
        }
        return sb.toString().length() == s.length() ? 0 : 1;
    }

    public static void main(String[] a) {
        System.out.println("run " + finding_265.run());
    }
}
