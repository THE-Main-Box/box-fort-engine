package official.sketchBook.engine.components_related.interact;

import com.badlogic.gdx.math.Vector2;
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

    /// Chamado pelo sistema de colisão quando o sensor sobrepõe um interativo
    public void addInteractable(InteractableObjectII interactable) {
        if (!interactableList.contains(interactable)) {
            interactableList.add(interactable);
        }
    }

    /// Chamado quando o sensor para de sobrepor
    public void removeInteractable(InteractableObjectII interactable) {
        interactableList.remove(interactable);
    }

    /// Interage com o mais próximo que pode ser interagido
    public void interact() {
        InteractableObjectII nearest = getNearestInteractable();
        if (nearest == null) return;
        nearest.interact();
    }

    /// Retorna o interativo mais próximo que pode ser interagido, ou null se não houver
    public InteractableObjectII getNearestInteractable() {
        InteractableObjectII nearest = null;
        float nearestDist = Float.MAX_VALUE;

        Vector2 origin = triggererObject.getCoordinatesInMeters();

        for (int i = 0; i < interactableList.size(); i++) {
            InteractableObjectII obj = interactableList.get(i);
            if (!obj.canInteract()) continue;

            float dist = origin.dst2(obj.getCoordinatesInMeters());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = obj;
            }
        }
        return nearest;
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
