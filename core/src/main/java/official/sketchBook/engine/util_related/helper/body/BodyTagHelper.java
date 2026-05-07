package official.sketchBook.engine.util_related.helper.body;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

public class BodyTagHelper {
    public static GameObjectTag getFixtureTagFromContact(Contact contact) {

        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();

        GameObjectTag tag;

        // Checa fixture A
        tag = BodyTagHelper.getFromFixtureTag(fa);

        //Se houve retorno, é a fixture A
        if (tag != null) return tag;

        //Caso não haja retorno tentamos iterar pela B, e retornamos quer que o resultado seja null ou não
        tag = BodyTagHelper.getFromFixtureTag(fb);

        return tag;
    }

    public static GameObjectTag getBodyTagFromContact(Contact contact) {

        Fixture fa = contact.getFixtureA();
        Fixture fb = contact.getFixtureB();

        GameObjectTag tag;

        // Checa fixture A
        tag = BodyTagHelper.getFromBodyTag(fa);

        //Se houve retorno, é a fixture A
        if (tag != null) return tag;

        //Caso não haja retorno tentamos iterar pela B, e retornamos quer que o resultado seja null ou não
        tag = BodyTagHelper.getFromBodyTag(fb);

        return tag;
    }

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
