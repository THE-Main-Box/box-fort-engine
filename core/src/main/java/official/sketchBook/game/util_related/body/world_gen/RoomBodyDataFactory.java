package official.sketchBook.game.util_related.body.world_gen;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.helper.world_gen.RoomBodyDataGeneratorHelper;
import official.sketchBook.engine.util_related.helper.world_gen.TileBodyFactory;
import official.sketchBook.game.util_related.enumerators.TileBodyType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomBodyDataFactory {
    /// Registro de factories por tipo de tile
    private static final Map<TileBodyType, TileBodyFactory> tileFactories = new HashMap<>();

    /// Inicializa as factories padrão
    static {
        register(TileBodyType.BLOCK, TileFactory::createBlockBody);
    }

    public static List<Body> createRoomBodies(int[][] bodyIdMap, World world){
        return RoomBodyDataGeneratorHelper.buildWorldTileBodies(
            RoomBodyDataGeneratorHelper.convertToBodyTypeMap(bodyIdMap),
            world
        );
    }

    /// Registra uma factory customizada para um tipo de tile
    public static void register(TileBodyType tileBodyType, TileBodyFactory factory) {
        tileFactories.put(tileBodyType, factory);
    }

    /// Obtém a factory para um tipo de tile
    public static TileBodyFactory getFactory(TileBodyType type) {
        return tileFactories.getOrDefault(type, TileFactory::createBlockBody);
    }
}
