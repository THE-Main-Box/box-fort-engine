package official.sketchBook.engine.components_related.interact;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.HoldInteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractionTriggerer;
import official.sketchBook.engine.components_related.objects.TimerComponent;

import java.util.ArrayList;
import java.util.List;

public class InteractTriggerComponent implements Component {

    /// Objeto dono do componente
    private InteractionTriggerer triggererObject;

    /// Lista de objetos capazes de interagirmos
    private List<InteractableObjectII> interactableList;

    /// Cache do objeto interativo mais próximo
    private InteractableObjectII cachedNearest;

    /// Dirty flag para auxiliar na otimização de search
    private boolean nearestDirty = true;

    /// Estado de hold do timer
    private final TimerComponent holdTimer;

    /// Flag para input
    private boolean holding = false;

    ///Buffer de dado de posição antiga
    private final Vector2 lastTriggererPos = new Vector2();

    public InteractTriggerComponent(InteractionTriggerer triggererObject) {
        this.triggererObject = triggererObject;

        this.interactableList = new ArrayList<>();
        this.holdTimer = new TimerComponent();

        initObject();
    }

    // --- Lista ---

    /// Adiciona um objeto e notifica uma atualização
    public void addInteractable(InteractableObjectII interactable) {
        if (interactableList.contains(interactable)) return;
        interactableList.add(interactable);
        nearestDirty = true;
    }

    /// Remove um objeto e notifica uma atualização
    public void removeInteractable(InteractableObjectII interactable) {
        if (!interactableList.remove(interactable)) return;
        nearestDirty = true;

        //Se o que foi removido foi o que estava mais próximo e estávamos segurando para interagir,
        // cancelamos a interação
        if (holding && cachedNearest == interactable) cancelHold();
    }

    // --- Input ---

    /// Quando tentamos marcar para interagir com um objeto
    public void onInteractPress() {
        if (holding) return; // já está segurando, ignora

        //Atualizamos o objeto mais próximo
        InteractableObjectII nearest = getNearestInteractable();
        if (nearest == null) return;

        //Se o objeto for um que usa hold para interagir
        if (nearest instanceof HoldInteractableObjectII) {
            HoldInteractableObjectII hold = (HoldInteractableObjectII) nearest;
            if (hold.isTriggerInteract()) nearest.interact();
            holdTimer.setTargetTime(hold.getHoldTimer());
            holdTimer.reset();
            holdTimer.start();
            holding = true;
        } else {
            nearest.interact();
        }
    }

    public void onInteractRelease() {
        cancelHold();
    }

    // --- Update ---

    @Override
    public void update(float delta) {
        Vector2 currentPos = triggererObject.getCoordinatesInMeters();
        if (!currentPos.epsilonEquals(lastTriggererPos, 0.01f)) {
            nearestDirty = true;
            lastTriggererPos.set(currentPos);
        }

        if (!holding) return;

        InteractableObjectII nearest = getNearestInteractable();

        if (!(nearest instanceof HoldInteractableObjectII)) {
            cancelHold();
            return;
        }

        holdTimer.update(delta);
        if (holdTimer.isFinished()) {
            nearest.interact();
            cancelHold();
        }
    }

    // --- Nearest ---

    public InteractableObjectII getNearestInteractable() {
        if (!nearestDirty) return cachedNearest;
        cachedNearest = computeNearest();
        nearestDirty = false;
        return cachedNearest;
    }

    private static InteractableObjectII computeNearest(
        List<InteractableObjectII> list,
        Vector2 origin
    ) {
        InteractableObjectII nearest = null;
        float nearestDist = Float.MAX_VALUE;

        for (int i = 0; i < list.size(); i++) {
            InteractableObjectII obj = list.get(i);
            if (!obj.canInteract()) continue;
            float dist = origin.dst2(obj.getCoordinatesInMeters());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = obj;
            }
        }
        return nearest;
    }

    private InteractableObjectII computeNearest() {
        return computeNearest(interactableList, triggererObject.getCoordinatesInMeters());
    }

    // --- Helpers ---

    private void cancelHold() {
        holding = false;
        holdTimer.stop();
        holdTimer.reset();
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
        cachedNearest = null;
    }
}
