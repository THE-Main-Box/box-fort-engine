package official.sketchBook.engine.liquid_related.model;

public interface ILiquid {
    /**
     * Garante que teremos um valor de densidade,
     * para determinar o quanto um objeto precisa ter de flutuabilidade para flutuar
     */
    float getDensity();

    /**
     * Garante que teremos um valor de resistência de liquido,
     * para determinar o quão difícil é se mover no liquido
     */
    float getResistance();

    /**
     * Garante que teremos um valor para limitar a velocidade de descida,
     * para impedir velocidade absurdas de descida
     */
    float getMaxSinkSpeed();

    /**
     * Garante que teremos um valor para limitar a velocidade de ascensão,
     * para impedir velocidade absurdas de subida dentro do liquido
     */
    float getMaxRiseSpeed();
}
