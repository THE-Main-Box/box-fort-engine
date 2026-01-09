package official.sketchBook.engine.world_gen;

public class TileModel {

    /// Id de referência
    private final int id;
    /// Id de referência para geração de corpo
    private final Integer bodyId;

    public TileModel(
        int id,
        int bodyId
    ) {
        this.id = id;
        this.bodyId = bodyId;
    }

    public int getId() {
        return id;
    }

    public Integer getBodyId() {
        return bodyId;
    }

}
