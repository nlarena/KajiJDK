// #508 -- una clase interna de una clase de nivel superior no puede crear a una hermana.
//
// JLS 15.9.2: en `new Hermana()` escrito dentro de otra clase interna de la misma externa, la
// instancia envolvente es implicita (`Externa.this`). Nuestro javac la pide como primer argumento
// y no la pone solo... pero solo cuando la clase externa es de nivel superior. Si la externa es a
// su vez una clase anidada, la instancia implicita si se pasa.
//
// El sintoma es un error de compilacion, no un `.class` malo:
//   "no se encontró un constructor `Hermana(int)` aplicable
//    método Hermana.Hermana(Externa, int) no es aplicable"
public class Finding508 {

    // --- falla: la externa es de nivel superior ------------------------------------------------
    // (esta es la forma del JDK en `StringContent` y `StyleContext`, y por eso salio)

    class Hermana {
        int v;

        Hermana(int v) {
            this.v = v;
        }
    }

    class DesdeMetodo {
        Hermana crea() {
            return new Hermana(1);       // falla
        }
    }

    class DesdeConstructor {
        Hermana h;

        DesdeConstructor() {
            h = new Hermana(2);          // falla
        }
    }

    // --- anda: la misma forma, con la externa anidada -----------------------------------------

    static class Externa {
        int dato = 7;

        class Uno {
            Dos hermana() {
                return new Dos();        // compila
            }

            Uno otraDeMiTipo() {
                return new Uno();        // compila
            }
        }

        class Dos {
            int leer() {
                return dato + 1;
            }
        }

        Uno desdeLaExterna() {
            return new Uno();            // compila en los dos casos
        }
    }

    // --- anda: la hermana es estatica, y entonces no hace falta instancia externa --------------

    static class Estatica {
        int leer() {
            return 0;
        }
    }

    class UsaEstatica {
        Estatica crea() {
            return new Estatica();
        }
    }

    public static int run() {
        Finding508 f = new Finding508();
        Externa e = new Externa();
        System.out.println("//nivelSuperior " + f.new DesdeConstructor().h.v);
        System.out.println("//anidada " + e.desdeLaExterna().hermana().leer());
        return 0;
    }

    public static void main(String[] a) {
        run();
    }
}
