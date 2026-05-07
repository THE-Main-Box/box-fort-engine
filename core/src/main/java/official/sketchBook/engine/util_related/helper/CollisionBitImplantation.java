package official.sketchBook.engine.util_related.helper;

import com.badlogic.gdx.physics.box2d.FixtureDef;

public class CollisionBitImplantation {
    public static void apply(FixtureDef def, short category, short... masks) {
        def.filter.categoryBits = category;
        def.filter.maskBits = 0;
        for (short m : masks) {
            def.filter.maskBits |= m;
        }
    }
}
