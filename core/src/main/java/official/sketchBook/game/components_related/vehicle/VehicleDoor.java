package official.sketchBook.game.components_related.vehicle;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.InteractionTriggerer;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ProximityInteractableObjectII;
import official.sketchBook.engine.components_related.objects.TangibleSwitchComponent;
import official.sketchBook.engine.components_related.vehicle.VehicleInteractableComponent;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.body.FixtureData;
import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;
import official.sketchBook.engine.world_gen.blueprint.embedded.FixtureDataBlueprint;

import java.util.ArrayList;
import java.util.List;

public class VehicleDoor extends VehicleInteractableComponent implements ProximityInteractableObjectII {

    public static final String TYPE_KEY = "vehicle_door";

    public boolean
        open,
        broken,
        locked;

    private TangibleSwitchComponent tangibleComponent;
    private boolean pendingStateUpdate = false;
    private List<InteractionTriggerer> nearList;

    /// Construtor "vivo" ? usado quando o jogo cria a porta diretamente
    /// (editor, spawn manual, testSubmarinePersistence etc).
    public VehicleDoor(
        String id,
        FixtureData fixData,
        FixtureData triggerFixData,
        boolean broken,
        boolean locked,
        boolean open
    ) {
        super(id, VehicleComponentType.PHYSICAL_INTERNAL, fixData, triggerFixData);
        this.broken = broken;
        this.locked = locked;
        this.open = open;
    }

    /// Construtor vazio ? exigido pra reflection (getEmbeddedList/newInstance).
    /// Fica em estado incompleto de prop�sito at� load() popular os campos.
    public VehicleDoor() {
        super();
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
        if (!isNear() && open) {
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
        if (!pendingStateUpdate) return;
        tangibleComponent.updateTangibleState();
        pendingStateUpdate = false;
    }

    protected void updateDoorOpenState(boolean newOpenState) {
        if (newOpenState == open) return;
        this.open = newOpenState;
        this.tangibleComponent.setTangible(!newOpenState);
        this.pendingStateUpdate = true;
    }

    public boolean canInteract() {
        return !broken && !locked;
    }

    @Override
    public List<InteractionTriggerer> getNearList() {
        return nearList;
    }

    // ============================================================
    // SERIALIZA��O ? o pr�prio componente sabe se descrever
    // ============================================================

    @Override
    public SaveData toSaveData() {
        return new SaveData()
            .put("id", id)
            .putTypeKey(TYPE_KEY)
            .putEmbedded("fix_data", FixtureDataBlueprint.toBlueprint(fixData))
            .putEmbedded("trigger_fix_data", FixtureDataBlueprint.toBlueprint(triggerFixData))
            .put("broken", broken)
            .put("locked", locked)
            .put("open", open);
    }

    @Override
    public void load(SaveData data) {
        this.id = data.getStringRequired("id");

        FixtureDataBlueprint fixBp = data.getEmbedded("fix_data", FixtureDataBlueprint.class);
        FixtureDataBlueprint triggerBp = data.getEmbedded("trigger_fix_data", FixtureDataBlueprint.class);

        // globalOffset s� existe no momento de attach (depende de onde o
        // componente est� no node) ? aqui usamos 0,0, e attachToSection
        // pode reaplicar o offset real antes de initObject() se precisar.
        this.fixData = fixBp.toFixtureData(0, 0);
        this.triggerFixData = triggerBp.toFixtureData(0, 0);

        this.broken = data.getBoolean("broken", false);
        this.locked = data.getBoolean("locked", false);
        this.open = data.getBoolean("open", false);

        this.type = VehicleComponentType.PHYSICAL_INTERNAL;
    }

    @Override
    public boolean toUpdate() {
        return true;
    }

    @Override
    public boolean toPostUpdate() {
        return true;
    }
}
