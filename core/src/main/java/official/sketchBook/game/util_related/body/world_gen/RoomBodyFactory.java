package official.sketchBook.game.util_related.body.world_gen;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.helper.world_gen.RoomBodyFactoryHelper;
import official.sketchBook.engine.util_related.helper.world_gen.TileBodyFactory;
import official.sketchBook.game.util_related.enumerators.TileBodyType;
import official.sketchBook.game.world_gen.TileFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomBodyFactory {
    /// Registro de factories por tipo de tile
    private static final Map<TileBodyType, TileBodyFactory> tileFactories = new HashMap<>();

    /// Inicializa as factories padrão
    static {
        register(TileBodyType.BLOCK, TileFactory::createBlockTile);
    }

    public static List<Body> createRoomBodies(int[][] bodyIdMap, World world){
        return RoomBodyFactoryHelper.buildWorldTileBodies(
            RoomBodyFactoryHelper.convertToBodyTypeMap(bodyIdMap),
            world
        );
    }

    /// Registra uma factory customizada para um tipo de tile
    public static void register(TileBodyType tileBodyType, TileBodyFactory factory) {
        tileFactories.put(tileBodyType, factory);
    }

    /// Obtém a factory para um tipo de tile
    public static TileBodyFactory getFactory(TileBodyType type) {
        return tileFactories.getOrDefault(type, TileFactory::createBlockTile);
    }
}
