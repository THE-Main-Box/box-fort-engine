package official.sketchBook.game.util_related.constants;

public class PhysicsC {

    /// Pixels Per Meters, constante que determina o quantos pixels correspondem a 1 metro
    public static final float PPM = 100;

    /// Taxa de atualização que tentamos seguir
    public static float UPS_TARGET = 120;

    /// Faixa de tempo que o sistema de física irá tentar seguir
    public static float FIXED_TIMESTAMP = 1 / UPS_TARGET;

    /// Acumulador máximo para evitar travamento acidental
    public static final float MAX_ACCUMULATOR = 0.25f;// Evita travar o PC se o frame demorar muito

}
