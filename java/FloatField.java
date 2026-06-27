// float en un CAMPO: putfield/getfield de 4 bytes (bits f32, 1 slot).
class FloatField {
    static float run() {
        Fbox x = new Fbox();
        x.f = 3.5f;
        return x.f;   // 3.5
    }
}
