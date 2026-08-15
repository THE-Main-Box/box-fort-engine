package official.sketchBook.engine.world_gen.blueprint.embedded;

import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.util_related.serialization.instantiation.EmbeddedSaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;

/**
 * Espelha FixtureData 1:1 — puramente dados, sem lógica. Existe como
 * DTO separado (em vez de reusar FixtureData direto no JSON) porque
 * FixtureData é imutável via construtor só, e o desserializador de
 * JSON do libGDX precisa de um formato com campos settáveis/no-arg
 * constructor para popular via reflection.
 */
public class FixtureDataBlueprint
    implements EmbeddedSaveData {

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
    }

    @Override
    public SaveData toSaveData() {
        return new SaveData()
            .put("density", density)
            .put("restitution", restitution)
            .put("friction", friction)
            .put("offset_x", offsetX)
            .put("offset_y", offsetY)
            .put("radius", radius)
            .put("width", width)
            .put("height", height)
            .put("category_bit", categoryBit)
            .put("mask_bit", maskBit)
            .put("is_circle", isCircle)
            .put("is_sensor", isSensor);
    }

    @Override
    public void load(SaveData data) {
        density = data.getFloatRequired("density");
        restitution = data.getFloatRequired("restitution");
        friction = data.getFloatRequired("friction");

        offsetX = data.getFloatRequired("offset_x");
        offsetY = data.getFloatRequired("offset_y");
        radius = data.getFloatRequired("radius");
        width = data.getFloatRequired("width");
        height = data.getFloatRequired("height");

        categoryBit = (short) data.getIntRequired("category_bit");
        maskBit = (short) data.getIntRequired("mask_bit");

        isCircle = data.getBooleanRequired("is_circle");
        isSensor = data.getBooleanRequired("is_sensor");
    }

    public static FixtureDataBlueprint toBlueprint(FixtureData data) {
        FixtureDataBlueprint fb = new FixtureDataBlueprint();
        fb.density = data.density;
        fb.restitution = data.restitution;
        fb.friction = data.friction;
        fb.offsetX = data.offsetX;
        fb.offsetY = data.offsetY;
        fb.radius = data.radius;
        fb.width = data.width;
        fb.height = data.height;
        fb.categoryBit = data.categoryBit;
        fb.maskBit = data.maskBit;
        fb.isCircle = data.isCircle();
        fb.isSensor = data.isSensor();
        return fb;
    }

    public FixtureData toFixtureData(
        float offsetX,
        float offsetY
    ) {
        return new FixtureData(
            density,
            restitution,
            friction,
            offsetX,
            offsetY,
            this.offsetX,
            this.offsetY,
            radius,
            width,
            height,
            categoryBit,
            maskBit,
            isCircle,
            isSensor
        );
    }
}
