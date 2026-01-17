package official.sketchBook.game.util_related.constants;

public class PhysicsConstants {

    /// Pixels Per Meters, constante que determina o quantos pixels correspondem a 1 metro
    public static final float PPM = 100;

    /// Iterações de velocidade para o box2d
    public static int VELOCITY_ITERATIONS;
    /// Iterações de posição para o box2d
    public static int POSITION_ITERATIONS;
    /// Taxa de atualização que tentamos seguir
    public static float UPS_TARGET;
    /// Faixa de tempo que o sistema de física irá tentar seguir
    public static float FIXED_TIMESTAMP;



    /// Acumulador máximo para evitar travamento acidental
    public static final float MAX_ACCUMULATOR = 0.25f;// Evita travar o PC se o frame demorar muito

    static {
        VELOCITY_ITERATIONS = 6;
        POSITION_ITERATIONS = 2;
        UPS_TARGET = 60;
        FIXED_TIMESTAMP = 1 / UPS_TARGET;
    }

}
