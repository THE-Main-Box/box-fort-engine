package official.sketchBook.engine.world_gen.blueprint.room;

import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.util_related.serialization.persistance.SaveDataArrayUtil;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveDataInstance;

public class RoomBlueprintSaveData extends SaveDataInstance<RoomBlueprint> {

    public static final String TYPE_KEY = "room_blueprint";

    private int blueprintId;
    private String debugName;
    private int gridWidth;
    private int gridHeight;
    private int[] layerGenerationStyles;
    private int[][][] grid; // pode ser null

    @Override
    public void loadFields(SaveData data) {
        this.blueprintId = data.getIntRequired("blueprint_id");
        this.debugName = data.getString("debug_name", String.valueOf(blueprintId));
        this.gridWidth = data.getIntRequired("grid_width");
        this.gridHeight = data.getIntRequired("grid_height");

        this.layerGenerationStyles = SaveDataArrayUtil.fromSaveData1D(
            data.getSaveDataRequired("layer_generation_styles")
        );

        SaveData gridData = data.getSaveDataOrNull("grid");
        this.grid = (gridData == null) ? null : SaveDataArrayUtil.fromSaveData3D(gridData);
    }

    @Override
    protected RoomBlueprint executeInstantiation() {
        return new RoomBlueprint(
            blueprintId,
            debugName,
            gridWidth,
            gridHeight,
            layerGenerationStyles,
            grid
        );
    }

    @Override
    public SaveData save(RoomBlueprint instance) {
        SaveData data = new SaveData();

        data.put("blueprint_id", instance.id);
        data.put("debug_name", instance.name);
        data.put("grid_width", instance.gridWidth);
        data.put("grid_height", instance.gridHeight);
        data.put("layer_generation_styles", SaveDataArrayUtil.toSaveData(instance.layerGenerationStyles));

        if (instance.hasGrid()) {
            data.put("grid", SaveDataArrayUtil.toSaveData(instance.grid));
        }

        return data;
    }
}
