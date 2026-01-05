package official.sketchBook.engine.util_related.helper;

import com.badlogic.gdx.physics.box2d.Fixture;

public class BodyTagHelper {
    // Obtém a tag presente em uma body dentro de uma fixture
    public static GameObjectTag getFromBodyTag(Fixture fixture) {
        if (fixture == null || !(fixture.getBody().getUserData() instanceof GameObjectTag)) return null;
        return (GameObjectTag) fixture.getBody().getUserData();
    }

    public static GameObjectTag getFromFixtureTag(Fixture fixture) {
        if (fixture == null || !(fixture.getUserData() instanceof GameObjectTag)) return null;
        return (GameObjectTag) fixture.getUserData();
    }
}
