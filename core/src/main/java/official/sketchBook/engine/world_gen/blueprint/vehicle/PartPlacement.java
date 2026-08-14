package official.sketchBook.engine.world_gen.blueprint.vehicle;

/**
 * Referência a uma part dentro de um node — não a part em si. O node
 * guarda "qual tag" + "onde", e a part real é resolvida (lida + cacheada)
 * na hora de instanciar. Mantém o JSON do node leve e a part
 * genuinamente reutilizável entre nodes/posições diferentes.
 */
public class PartPlacement {
    public String partTag;
    public float placementOffsetX, placementOffsetY;

    public PartPlacement() {
    }
}
