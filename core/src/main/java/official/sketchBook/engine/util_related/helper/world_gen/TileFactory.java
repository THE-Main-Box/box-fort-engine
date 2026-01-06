package official.sketchBook.engine.util_related.helper.world_gen;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.game.util_related.enumerators.TileType;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.helper.world_gen.RoomBodyDataGeneratorHelper.createBoxBodyForTiles;

public class TileFactory {
    public static List<Body> createBlockBody(
        World world,
        TileType type,
        int mapX,
        int mapY,
        int width,
        int height,
        int totalRows
    ) {
        Body body = createBoxBodyForTiles(
            world,
            type,
            BodyDef.BodyType.StaticBody,
            mapX,
            mapY,
            0,
            width,
            height,
            totalRows,
            type.getCategoryBit(),
            type.getMaskBit()
        );

        body.setUserData(new GameObjectTag(ObjectType.ENVIRONMENT, type));

        List<Body> bodies = new ArrayList<>();
        bodies.add(body);
        return bodies;
    }
}
