package official.sketchBook.engine.world_gen.blueprint.embedded;

import official.sketchBook.engine.util_related.helper.body.FixtureData;

/**
 * Espelha FixtureData 1:1 — puramente dados, sem lógica. Existe como
 * DTO separado (em vez de reusar FixtureData direto no JSON) porque
 * FixtureData é imutável via construtor só, e o desserializador de
 * JSON do libGDX precisa de um formato com campos settáveis/no-arg
 * constructor para popular via reflection.
 */
public class FixtureDataBlueprint {
    public float
        density,
        restitution,
        friction;

    public float
        offsetX,
        offsetY,
        radius,
        width,
        height;

    public short
        categoryBit,
        maskBit;

    public boolean
        isCircle,
        isSensor;

    public FixtureDataBlueprint() {
        // no-arg — exigido pelo Json do libGDX
    }

    public FixtureData toFixtureData(float compositionOffsetX, float compositionOffsetY) {
        return new FixtureData(
            density, restitution, friction,
            compositionOffsetX, compositionOffsetY,   // globalOffsetX/Y — resolvido agora
            offsetX, offsetY,                          // offset local da fixture — intrínseco, do JSON
            radius, width, height,
            categoryBit, maskBit, isCircle, isSensor
        );
    }
}
