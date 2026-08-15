package official.sketchBook.engine.world_gen.blueprint.embedded;

import official.sketchBook.engine.util_related.serialization.instantiation.EmbeddedSaveData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;

/**
 * Referência a uma part dentro de um node — não a part em si. O node
 * guarda "qual tag" + "onde", e a part real é resolvida (lida + cacheada)
 * na hora de instanciar. Mantém o JSON do node leve e a part
 * genuinamente reutilizável entre nodes/posições diferentes.
 */
public class PartPlacement implements EmbeddedSaveData {
    public String tag;
    public float offsetX, offsetY;

    public PartPlacement() {
    }

    @Override
    public SaveData toSaveData() {
        return new SaveData()
            .put("tag", tag)
            .put("offset_x", offsetX)
            .put("offset_y", offsetY);
    }

    @Override
    public void load(SaveData data) {
        this.tag = data.getStringRequired("tag");
        this.offsetX = data.getFloat("offset_x", 0);
        this.offsetY = data.getFloat("offset_y", 0);
    }


}
