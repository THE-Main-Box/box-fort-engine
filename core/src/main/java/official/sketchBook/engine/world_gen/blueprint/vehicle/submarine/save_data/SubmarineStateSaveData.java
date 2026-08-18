package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data;


import official.sketchBook.engine.util_related.exceptions.SaveDataException;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveDataInstance;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarineBlueprint;
import official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.SubmarineState;

public class SubmarineStateSaveData extends SaveDataInstance<SubmarineState> {
    public static final String TYPE_KEY = "submarine_state";

    private SubmarineBlueprint submarine;
    private float spawnX, spawnY;

    @Override
    public void loadFields(SaveData data) {
        submarine = data.getEmbedded("submarine", SubmarineBlueprint.class);
        spawnX = data.getFloatRequired("spawn_x");
        spawnY = data.getFloatRequired("spawn_y");
    }

    @Override
    protected SubmarineState executeInstantiation() throws SaveDataException {
        return new SubmarineState(submarine, spawnX, spawnY);
    }

    @Override
    public SaveData save(SubmarineState instance) {
        return new SaveData()
            .putEmbedded("submarine", instance.submarine)
            .put("spawn_x", instance.spawnX)
            .put("spawn_y", instance.spawnY);
    }
}
