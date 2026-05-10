package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractableObjectII;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;

import java.util.ArrayList;
import java.util.List;

public class InteractableObjectManagerComponent implements Component {
    private final List<InteractableObjectII>
        interactableList;

    public final Body triggerBody;

    public InteractableObjectManagerComponent(
        Body triggerBody
    ) {
        this.interactableList = new ArrayList<>();
        this.triggerBody = triggerBody;

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
}
