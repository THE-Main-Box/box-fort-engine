package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.physics.box2d.*;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.SelfListenedPhysicalObjectII;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;

import java.util.ArrayList;
import java.util.List;

import static official.sketchBook.engine.util_related.helper.body.BodyTagHelper.getFromFixtureTag;

public class InteractableObjectManagerComponent implements Component, SelfListenedPhysicalObjectII {
    private final List<InteractableObjectII>
        interactableList;

    public final Body triggerBody;

    public InteractableObjectManagerComponent(
        Body triggerBody
    ) {
        this.interactableList = new ArrayList<>();
        this.triggerBody = triggerBody;

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
    }

    public void addToList(InteractableObjectII object) {

        if(object.getTriggerFixData() == null) return;

        //Criamos as fixtures triggers de evento do objeto interativo atual e armazenamos ela
        List<Fixture> fixList = BodyCreatorHelper.createFixturesFromData(
            object.getTriggerFixData(),
            triggerBody
        );

        //Percorremos a lista de fixtures trigger criadas e settamos a sua userData
        // para direcionar para o objeto correto
        for (int j = 0; j < fixList.size(); j++) {

            fixList.get(j).setUserData(
                new GameObjectTag(
                    ObjectType.INTERACTABLE,
                    object
                )
            );
        }

        interactableList.add(object);
    }

    @Override
    public void initObject() {

    }

    @Override
    public void dispose() {
        interactableList.clear();
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
