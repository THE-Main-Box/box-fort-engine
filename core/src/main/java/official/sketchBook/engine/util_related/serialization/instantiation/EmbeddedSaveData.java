package official.sketchBook.engine.util_related.serialization.instantiation;

public interface EmbeddedSaveData{

    SaveData toSaveData();

    void load(SaveData data);
}
