package official.sketchBook.game.world_gen;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.game.util_related.body.world_gen.TileBodyFactory;
import official.sketchBook.game.util_related.enumerators.TileBodyType;

import java.util.ArrayList;
import java.util.List;

public class TileFactory {
    public static List<Body> createBlockTile(
        World world,
        TileBodyType type,
        int mapX,
        int mapY,
        int width,
        int height,
        int totalRows
    ) {
        Body body = TileBodyFactory.createBoxBodyForTiles(
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

        body.setUserData(
            new GameObjectTag(
                ObjectType.ENVIRONMENT,
                type
            )
        );

        List<Body> bodies = new ArrayList<>();
        bodies.add(body);
        return bodies;
    }
}
