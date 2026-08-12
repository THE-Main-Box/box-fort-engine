package official.sketchBook.engine.world_gen.model;

import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

public final class TilePhysicsConfig {
    public final int tileId;
    public final boolean mergeable;         // agrupa vizinhos do mesmo id em retângulo maior?
    public final boolean ownBodyPerCluster; // cada cluster vira Body própria, ou fixture da body compartilhada da sala?
    public final TileFixtureFactory fixtureFactory;
    public final ObjectType objectType;

    public TilePhysicsConfig(
        int tileId,
        boolean mergeable,
        boolean ownBodyPerCluster,
        ObjectType objectType,
        TileFixtureFactory fixtureFactory
    ) {
        this.tileId = tileId;
        this.mergeable = mergeable;
        this.ownBodyPerCluster = ownBodyPerCluster;
        this.fixtureFactory = fixtureFactory;
        this.objectType = objectType;
    }

    public TilePhysicsConfig(
        int tileId,
        boolean mergeable,
        boolean ownBodyPerCluster,
        TileFixtureFactory fixtureFactory
    ) {
        this(
            tileId,
            mergeable,
            ownBodyPerCluster,
            null,
            fixtureFactory
        );
    }

    public interface TileFixtureFactory {
        /**
         * @param clusterWidthTiles  largura do cluster resolvido, em c?lulas de grid
         * @param clusterHeightTiles altura do cluster resolvido, em c?lulas de grid
         */
        FixtureData create(int clusterWidthTiles, int clusterHeightTiles);
    }
}
