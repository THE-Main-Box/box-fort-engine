package official.sketchBook.engine.components_related.interact;

import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractionTriggerer;

import java.util.ArrayList;
import java.util.List;


public class InteractTriggerComponent implements Component {

    private InteractionTriggerer triggererObject;

    private List<InteractableObjectII> interactableList;

    public InteractTriggerComponent(InteractionTriggerer triggererObject) {
        this.triggererObject = triggererObject;
        interactableList = new ArrayList<>();

        initObject();
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {

    }

    @Override
    public void initObject() {

    }

    @Override
    public void dispose() {

        interactableList.clear();

        interactableList = null;
        triggererObject = null;
    }
}
