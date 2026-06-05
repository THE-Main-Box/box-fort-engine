package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.math.Vector2;

public class DimensionsCoordinatesV2BuffInMeters {

    public final Vector2
        bufferedPos,
        bufferedCoord;

    public DimensionsCoordinatesV2BuffInMeters(Vector2 bufferedPos, Vector2 bufferedCoord) {
        this.bufferedPos = bufferedPos;
        this.bufferedCoord = bufferedCoord;
    }
}
