package official.sketchBook.game.world_gen.generation;

import com.badlogic.gdx.utils.Array;
import official.sketchBook.engine.data_manager_related.PhysicalGameObjectDataManager;
import official.sketchBook.engine.liquid_related.model.LiquidData;
import official.sketchBook.engine.liquid_related.model.RoomLiquid;
import official.sketchBook.engine.liquid_related.util.LiquidRegion;

import official.sketchBook.engine.world_gen.model.PhysicalPlayableRoom;
import official.sketchBook.engine.world_gen.util.LayerGenerationResolver;
import official.sketchBook.engine.world_gen.util.TileClusterUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolver de gera??o de l?quido para UMA camada. Agrupa tiles adjacentes
 * do mesmo id (via TileClusterUtil) em clusters, converte cada cluster em
 * um LiquidRegion (em pixels, considerando a posi??o da sala e o tamanho
 * de tile) e instancia um ?nico RoomLiquid com todas as regi?es encontradas.
 * <p>
 * N?o acessa WorldConstants.TILE_SIZE_PX diretamente ? tileSizePx ? sempre
 * recebido de fora (construtor), pra n?o acoplar o engine a uma constante
 * do lado do jogo.
 * <p>
 * Uma inst?ncia desta classe ? configurada para UM id de l?quido espec?fico
 * (ex: "water" no id 2) ? se a sala tiver m?ltiplos tipos de l?quido em
 * camadas diferentes, cada camada recebe sua pr?pria inst?ncia registrada
 * sob um estilo de gera??o distinto.
 */
public class LiquidGenerationResolver implements LayerGenerationResolver {

    private final PhysicalGameObjectDataManager worldDataManager;
    private final LiquidData liquidData;
    private final int liquidTileId;
    private final int tileSizePx;

    public LiquidGenerationResolver(
        PhysicalGameObjectDataManager worldDataManager,
        LiquidData liquidData,
        int liquidTileId,
        int tileSizePx
    ) {
        this.worldDataManager = worldDataManager;
        this.liquidData = liquidData;
        this.liquidTileId = liquidTileId;
        this.tileSizePx = tileSizePx;
    }

    @Override
    public void resolve(PhysicalPlayableRoom room, int layer) {
        int[][] layerData = extractLayerData(room, layer);

        Array<TileClusterUtil.ClusterRect> clusters = TileClusterUtil.findClusters(
            layerData,
            (originId, candidateId) -> originId == candidateId,
            true,
            EMPTY_TILE_ID
        );

        if (clusters.isEmpty()) return; // nada desse liquido nesta camada, nada a gerar

        List<LiquidRegion> regionList = new ArrayList<>(clusters.size);

        for (int i = 0; i < clusters.size; i++) {
            TileClusterUtil.ClusterRect cluster = clusters.get(i);

            // s? converte clusters que s?o de fato o id de liquido esperado
            // (findClusters tamb?m agrupa outros ids n?o vazios presentes na
            // camada ? aqui filtramos pra s? este liquido especifico)
            if (layerData[cluster.y][cluster.x] != liquidTileId) continue;

            regionList.add(toLiquidRegion(room, cluster));
        }

        if (regionList.isEmpty()) return;

        new RoomLiquid(worldDataManager, room, liquidData, regionList);
    }

    private LiquidRegion toLiquidRegion(PhysicalPlayableRoom room, TileClusterUtil.ClusterRect cluster) {
        float x = room.getRoomXPos() + (cluster.x * tileSizePx);
        float y = room.getRoomYPos() + (cluster.y * tileSizePx);
        float width = cluster.width * tileSizePx;
        float height = cluster.height * tileSizePx;

        return new LiquidRegion(x, y, width, height);
    }

    private int[][] extractLayerData(PhysicalPlayableRoom room, int layer) {
        int height = room.gridHeight;
        int width = room.gridWidth;

        int[][] layerData = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                layerData[y][x] = room.getGridDataAt(layer, y, x);
            }
        }

        return layerData;
    }
}
