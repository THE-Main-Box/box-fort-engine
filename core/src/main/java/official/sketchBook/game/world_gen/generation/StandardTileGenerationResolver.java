package official.sketchBook.game.world_gen.generation;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.world_gen.model.PhysicalPlayableRoom;
import official.sketchBook.engine.world_gen.model.TilePhysicsConfig;
import official.sketchBook.engine.world_gen.util.LayerGenerationResolver;
import official.sketchBook.engine.world_gen.util.TileClusterUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static official.sketchBook.game.util_related.constants.PhysicsConstants.toMeters;

public class StandardTileGenerationResolver implements LayerGenerationResolver {

    private static final int EMPTY_TILE_ID = 0;

    private final int tileSizePx;
    private final Map<Integer, TilePhysicsConfig> configByTileId;

    public StandardTileGenerationResolver(int tileSizePx, List<TilePhysicsConfig> configs) {
        this.tileSizePx = tileSizePx;
        this.configByTileId = new HashMap<>();
        for (TilePhysicsConfig config : configs) {
            configByTileId.put(config.tileId, config);
        }
    }

    @Override
    public void resolve(PhysicalPlayableRoom room, int layer) {
        int[][] layerData = extractLayerData(room, layer);

        for (TilePhysicsConfig config : configByTileId.values()) {
            resolveForConfig(room, layerData, config);
        }
    }

    private void resolveForConfig(PhysicalPlayableRoom room, int[][] layerData, TilePhysicsConfig config) {
        Array<TileClusterUtil.ClusterRect> clusters = TileClusterUtil.findClusters(
            layerData,
            (originId, candidateId) -> originId == candidateId,
            config.mergeable,
            EMPTY_TILE_ID
        );

        for (int i = 0; i < clusters.size; i++) {
            TileClusterUtil.ClusterRect cluster = clusters.get(i);

            if (layerData[cluster.y][cluster.x] != config.tileId) continue;

            FixtureData fixtureData = config.fixtureFactory.create(cluster.width, cluster.height);

            Body targetBody = config.ownBodyPerCluster
                ? createIndependentBody(room, cluster)
                : room.getOrCreateSharedStaticBody();

            attachFixture(targetBody, fixtureData, cluster, config);
        }
    }

    private Body createIndependentBody(PhysicalPlayableRoom room, TileClusterUtil.ClusterRect cluster) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(
            toMeters(room.getRoomXPos() + (cluster.x * tileSizePx)),
            toMeters(room.getRoomYPos() + (cluster.y * tileSizePx))
        );

        Body body = room.getPhysicsWorld().createBody(bodyDef);

        if (room.nativeBodies == null) room.nativeBodies = new java.util.ArrayList<>();
        room.nativeBodies.add(body);

        return body;
    }

    private void attachFixture(
        Body targetBody,
        FixtureData fixtureData,
        TileClusterUtil.ClusterRect cluster,
        TilePhysicsConfig config
    ) {
        float clusterCenterOffsetX = (cluster.width * tileSizePx) / 2f;
        float clusterCenterOffsetY = (cluster.height * tileSizePx) / 2f;

        float offsetX, offsetY;

        if (config.ownBodyPerCluster) {
            offsetX = clusterCenterOffsetX;
            offsetY = clusterCenterOffsetY;
        } else {
            offsetX = (cluster.x * tileSizePx) + clusterCenterOffsetX;
            offsetY = (cluster.y * tileSizePx) + clusterCenterOffsetY;
        }

        FixtureData positioned = new FixtureData(
            fixtureData.density,
            fixtureData.restitution,
            fixtureData.friction,
            fixtureData.globalOffsetX,
            fixtureData.globalOffsetY,
            fixtureData.offsetX + offsetX,
            fixtureData.offsetY + offsetY,
            fixtureData.radius,
            fixtureData.width,
            fixtureData.height,
            fixtureData.categoryBit,
            fixtureData.maskBit,
            fixtureData.isCircle(),
            fixtureData.isSensor()
        );

        List<Fixture> created = BodyCreatorHelper.createFixturesFromData(positioned, targetBody);

        if (config.objectType == null) return; // sem tipo configurado, não marca

        GameObjectTag tag = new GameObjectTag(config.objectType, null);
        for (Fixture fixture : created) {
            fixture.setUserData(tag);
        }
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
