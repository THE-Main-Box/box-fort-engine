package official.sketchBook.engine.world_gen.blueprint.vehicle;

import official.sketchBook.engine.util_related.exceptions.SaveDataException;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveDataInstance;
import official.sketchBook.engine.world_gen.blueprint.embedded.FixtureDataBlueprint;

import java.util.ArrayList;
import java.util.List;

public class SubmarinePartBlueprintSaveData extends SaveDataInstance<SubmarinePartBlueprint>{
    private int id;
    private String tag;

    private float baseMass;

    private float
        marginLeft,
        marginRight,
        marginUp,
        marginDown;

    private List<FixtureDataBlueprint> fixtures;

    @Override
    public SaveData save(SubmarinePartBlueprint instance) {
        SaveData data = new SaveData();

        data.put("id", instance.id);
        data.put("tag", instance.tag);

        data.put("base_mass", instance.baseMass);

        data.put("margin_left", instance.marginLeft);
        data.put("margin_right", instance.marginRight);
        data.put("margin_up", instance.marginUp);
        data.put("margin_down", instance.marginDown);

        data.putEmbeddedList("fixtures", instance.fixtures);
        return data;
    }

    @Override
    public void loadFields(SaveData data) {
        this.id = data.getIntRequired("id");
        this.tag = data.getStringRequired("tag");

        this.baseMass = data.getFloatRequired("base_mass");

        this.marginLeft = data.getFloat("margin_left", 0);
        this.marginRight= data.getFloat("margin_right", 0);
        this.marginUp = data.getFloat("margin_up", 0);
        this.marginDown = data.getFloat("margin_down", 0);

        this.fixtures = data.getEmbeddedList("fixtures", FixtureDataBlueprint.class);
    }

    @Override
    protected SubmarinePartBlueprint executeInstantiation() throws SaveDataException {
        SubmarinePartBlueprint bp = new SubmarinePartBlueprint(
            id,
            tag,
            baseMass,
            marginLeft,
            marginRight,
            marginUp,
            marginDown
        );

        bp.fixtures = this.fixtures;

        return bp;
    }
}
