// double en un CAMPO: putfield/getfield de 8 bytes (bits f64).
class DoubField {
    static double run() {
        Dbox x = new Dbox();
        x.d = 3.5;
        return x.d;   // 3.5
    }
}
