package official.sketchBook.engine.world_gen.blueprint.room;

/**
 * Dados m?nimos pra reconstruir uma PhysicalPlayableRoom via RoomGenerator:
 * dimens?es + estilo de gera??o por camada, e opcionalmente uma grid j?
 * pronta (quando a blueprint n?o ? procedural).
 * <p>
 * N?O guarda nada de resultado f?sico (nativeBodies, RoomLiquid, etc) ?
 * isso ? sempre recriado pelos resolvers a partir do que est? aqui.
 */
public class RoomBlueprint {

    /// Identidade real, usada como chave de persist?ncia/lookup
    public final int id;

    /// Label opcional, s? pra debug/tooling ? nunca usado como chave
    public final String name;

    public final int gridWidth;
    public final int gridHeight;

    /// ?ndice = camada, valor = estilo de gera??o daquela camada
    public final int[] layerGenerationStyles;

    /**
     * Grid j? pronta, opcional. Null significa "esta blueprint n?o traz
     * grid pronta" ? quem consome decide o que fazer (normalmente,
     * significa que algum resolver da pipeline ? respons?vel por gerar
     * a grid proceduralmente, n?o a blueprint).
     */
    public final int[][][] grid;

    public RoomBlueprint(
        int id,
        String name,
        int gridWidth,
        int gridHeight,
        int[] layerGenerationStyles,
        int[][][] grid
    ) {
        this.id = id;
        this.name = (name == null || name.isEmpty())
            ? String.valueOf(id)
            : name;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.layerGenerationStyles = layerGenerationStyles;
        this.grid = grid; // pode ser null
    }

    public RoomBlueprint(
        String name,
        int gridWidth,
        int gridHeight,
        int[] layerGenerationStyles,
        int[][][] grid
    ) {
        this(
            generateId(),
            name,
            gridWidth,
            gridHeight,
            layerGenerationStyles,
            grid
        );
    }

    public boolean hasGrid() {
        return grid != null;
    }


    /**
     * Gera um id a partir dos ?ltimos d?gitos do timestamp atual (ms) ?
     * curto, sem depend?ncia de estado est?tico compartilhado (nada de
     * contador global), suficiente pra diferenciar blueprints geradas
     * em momentos distintos.
     */
    public static int generateId() {
        return (int) (System.currentTimeMillis() % 1_000_000);
    }
}
