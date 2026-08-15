package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data;

import official.sketchBook.engine.util_related.exceptions.SaveDataException;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveDataInstance;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarineBlueprint;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarineNodeBlueprint;

import java.util.List;

public class SubmarineBlueprintSaveData extends SaveDataInstance<SubmarineBlueprint> {
    public static final String TYPE_KEY = "submarine_blueprint";

    private String tag;
    private List<SubmarineNodeBlueprint> nodes;

    @Override
    public void loadFields(SaveData data) {
        tag = data.getStringRequired("tag");
        nodes = data.getEmbeddedList("nodes", SubmarineNodeBlueprint.class);
    }

    @Override
    protected SubmarineBlueprint executeInstantiation() throws SaveDataException {
        SubmarineBlueprint bp = new SubmarineBlueprint();
        bp.tag = tag;
        bp.nodes = this.nodes;
        return bp;
    }

    @Override
    public SaveData save(SubmarineBlueprint instance) {
        SaveData data = new SaveData();
        data.put("tag", instance.tag);
        data.putEmbeddedList("nodes", instance.nodes);
        return data;
    }
}
