package official.sketchBook.engine.game_object_related.base_game_object;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.SelfListenedPhysicalObjectII;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.List;

public class InteractComponent implements Component, SelfListenedPhysicalObjectII {

    public final InteractableObjectII owner;
    public Body
        referenceBody,
        sensorBody;
    public final FixtureData fixData;

    public InteractComponent(
        InteractableObjectII owner,
        Body referenceBody,
        FixtureData fixData
    ) {
        this.owner = owner;
        this.referenceBody = referenceBody;
        this.fixData = fixData;

        initObject();
    }

    @Override
    public void initObject() {
        assert referenceBody != null;
        Vector2 pos = referenceBody.getPosition();

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.KinematicBody;
        bodyDef.position.set(pos.x, pos.y);

        sensorBody = referenceBody.getWorld().createBody(bodyDef);

        sensorBody.setUserData(
            new GameObjectTag(
                ObjectType.INTERACTABLE,
                this
            )
        );

        BodyCreatorHelper.createFixturesFromData(fixData, sensorBody);
    }

    public void interact() {
        if (!owner.canInteract()) return;
        owner.interact();
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {
        sensorBody.setTransform(
            referenceBody.getPosition(),
            referenceBody.getAngle()
        );
    }

    @Override
    public void dispose() {

    }

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
//        interact();
        System.out.println("hehey");
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {

    }
}
