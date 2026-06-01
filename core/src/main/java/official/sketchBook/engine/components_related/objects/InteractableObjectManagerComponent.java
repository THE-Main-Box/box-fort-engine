package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.SelfListenedPhysicalObjectII;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper.createVoidBody;
import static official.sketchBook.engine.util_related.helper.body.BodyTagHelper.getFromFixtureTag;

public class InteractableObjectManagerComponent implements Component, SelfListenedPhysicalObjectII {
    private List<InteractableObjectII>
        interactableList;

    private Body
        referenceBody,
        triggerBody;

    public InteractableObjectManagerComponent(
        Body referenceBody
    ) {
        this.interactableList = new ArrayList<>();

        this.referenceBody = referenceBody;

        Vector2 pos = referenceBody.getPosition();

        this.triggerBody = createVoidBody(
            referenceBody.getWorld(),
            pos.x,
            pos.y,
            BodyDef.BodyType.KinematicBody
        );

        triggerBody.setUserData(
            new GameObjectTag(
                ObjectType.INTERACTABLE,
                this
            )
        );
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public void postUpdate() {
        triggerBody.setTransform(
            referenceBody.getPosition(),
            referenceBody.getAngle()
        );
    }

    public void addToList(InteractableObjectII object) {
        interactableList.add(object);
    }

    @Override
    public void initObject() {

    }

    @Override
    public void dispose() {
        interactableList.clear();

        triggerBody.getWorld().destroyBody(triggerBody);

        referenceBody = null;
        triggerBody = null;
        interactableList = null;
    }

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        GameObjectTag fixTagA = getFromFixtureTag(contact.getFixtureA());
        GameObjectTag fixTagB = getFromFixtureTag(contact.getFixtureB());

        GameObjectTag interactTag = null;

        if (fixTagA != null && fixTagA.owner instanceof InteractableObjectII) {
            interactTag = fixTagA;
        } else if (fixTagB != null && fixTagB.owner instanceof InteractableObjectII) {
            interactTag = fixTagB;
        }

        if (interactTag == null) return;

        ((InteractableObjectII) interactTag.owner).interact();
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
