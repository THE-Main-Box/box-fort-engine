package official.sketchBook.engine.components_related.interact;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractionTriggerer;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.SelfListenedPhysicalObjectII;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper.createVoidBody;
import static official.sketchBook.engine.util_related.helper.body.BodyTagHelper.getFromBodyTag;
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

    public Body getTriggerBody() {
        return triggerBody;
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

        InteractableObjectII interactable = extractInteractable(fixTagA, fixTagB);
        if (interactable == null) return;

        // triggerer vem do body, não da fixture
        GameObjectTag bodyTagA = getFromBodyTag(contact.getFixtureA());
        GameObjectTag bodyTagB = getFromBodyTag(contact.getFixtureB());

        InteractionTriggerer triggerer = extractTriggerer(bodyTagA, bodyTagB);
        if (triggerer == null) return;

        triggerer.getTriggerC().addInteractable(interactable);
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        GameObjectTag fixTagA = getFromFixtureTag(contact.getFixtureA());
        GameObjectTag fixTagB = getFromFixtureTag(contact.getFixtureB());

        InteractableObjectII interactable = extractInteractable(fixTagA, fixTagB);
        if (interactable == null) return;

        GameObjectTag bodyTagA = getFromBodyTag(contact.getFixtureA());
        GameObjectTag bodyTagB = getFromBodyTag(contact.getFixtureB());

        InteractionTriggerer triggerer = extractTriggerer(bodyTagA, bodyTagB);
        if (triggerer == null) return;

        triggerer.getTriggerC().removeInteractable(interactable);
    }

    private InteractableObjectII extractInteractable(GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA != null && tagA.owner instanceof InteractableObjectII)
            return (InteractableObjectII) tagA.owner;
        if (tagB != null && tagB.owner instanceof InteractableObjectII)
            return (InteractableObjectII) tagB.owner;
        return null;
    }

    private InteractionTriggerer extractTriggerer(GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA != null && tagA.owner instanceof InteractionTriggerer)
            return (InteractionTriggerer) tagA.owner;
        if (tagB != null && tagB.owner instanceof InteractionTriggerer)
            return (InteractionTriggerer) tagB.owner;
        return null;
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {

    }
}
