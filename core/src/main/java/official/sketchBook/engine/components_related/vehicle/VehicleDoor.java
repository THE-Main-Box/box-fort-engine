package official.sketchBook.engine.components_related.vehicle;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractionTriggerer;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ProximityInteractableObjectII;
import official.sketchBook.engine.components_related.objects.TangibleSwitchComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.ArrayList;
import java.util.List;

public class VehicleDoor extends VehicleInteractableComponent implements ProximityInteractableObjectII {
    public static int quantity;

    /// Flags de estado
    public boolean
        open,           //Se está aberta
        broken,         //Se a porta está quebrada
        locked;         //Se a porta está trancada

    private TangibleSwitchComponent tangibleComponent;

    private boolean pendingStateUpdate = false;

    private List<InteractionTriggerer> nearList;

    public VehicleDoor(
        VehicleSection ownerSection,
        FixtureData fixData,
        FixtureData triggerFixData,
        boolean broken,
        boolean locked,
        boolean open
    ) {

        super(
            "Door_id: " + quantity,
            String.valueOf(quantity),
            ownerSection,
            VehicleComponentType.PHYSICAL_INTERNAL,
            fixData,
            triggerFixData
        );

        this.broken = broken;
        this.locked = locked;
        this.open = open;

        initObject();

        quantity++;
    }

    @Override
    public void initObject() {
        super.initObject();

        this.tangibleComponent = new TangibleSwitchComponent(
            fixList.get(0).getFilterData().maskBits,
            open,
            fixList
        );

        this.nearList = new ArrayList<>();
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        //Se não tiver nenhum objeto que possa abrir a porta por perto, fechamos automaticamente
        if (!isNear() && open) {
            //Atualizamos para fechar a porta
            this.updateDoorOpenState(false);
        }
    }

    @Override
    public void executeInteraction(InteractionTriggerer triggerer) {
        updateDoorOpenState(!this.open);
    }

    @Override
    public void postUpdate() {
        super.postUpdate();
        //Se estivermos pendendo uma atualização
        if (!pendingStateUpdate) return;

        //Atualizamos o estado de tangível em si
        tangibleComponent.updateTangibleState();
        //Desmarcamos pra não entrar aqui de novo sem necessidade
        pendingStateUpdate = false;
    }

    /***
     * Atualiza o estado de aberto da porta
     *
     * @param newOpenState novo estado a passar pro open
     */
    protected void updateDoorOpenState(boolean newOpenState) {
        //Evitamos passar por tudo isso se o estado for repetido
        if (newOpenState == open) return;

        //Atualizamos o estado da porta
        this.open = newOpenState;
        //Atualizamos o estado de tangível
        this.tangibleComponent.setTangible(!newOpenState);
        //Marcamos para atualizar o estado de tangível
        this.pendingStateUpdate = true;
    }

    public boolean canInteract() {
        return !broken && !locked;
    }

    @Override
    public List<InteractionTriggerer> getNearList() {
        return nearList;
    }

}
