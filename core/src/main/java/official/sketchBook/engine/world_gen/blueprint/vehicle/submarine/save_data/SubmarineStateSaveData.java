package official.sketchBook.engine.world_gen.blueprint.vehicle.submarine.save_data;


import official.sketchBook.engine.util_related.exceptions.SaveDataException;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveDataInstance;

public class SubmarineStateSaveData extends SaveDataInstance<SubmarineStateSaveData.SubmarineSpawnState> {
    public static final String TYPE_KEY = "submarine_state";

    private String blueprintTag; // qual arquivo em BP_SUB carregar
    private float spawnX, spawnY;

    @Override
    public void loadFields(SaveData data) {
        blueprintTag = data.getStringRequired("blueprint_tag");
        spawnX = data.getFloatRequired("spawn_x");
        spawnY = data.getFloatRequired("spawn_y");
    }

    @Override
    protected SubmarineSpawnState executeInstantiation() throws SaveDataException {
        return new SubmarineSpawnState(blueprintTag, spawnX, spawnY);
    }

    @Override
    public SaveData save(SubmarineSpawnState instance) {
        return new SaveData()
            .put("blueprint_tag", instance.blueprintTag)
            .put("spawn_x", instance.spawnX)
            .put("spawn_y", instance.spawnY);
    }

    public static class SubmarineSpawnState {
        public final String blueprintTag;
        public final float spawnX, spawnY;

        public SubmarineSpawnState(String blueprintTag, float spawnX, float spawnY) {
            this.blueprintTag = blueprintTag;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
        }
    }
}
