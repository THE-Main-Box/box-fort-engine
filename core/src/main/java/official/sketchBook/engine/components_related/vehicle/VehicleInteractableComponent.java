package official.sketchBook.engine.components_related.vehicle;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractableObjectII;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.ArrayList;
import java.util.List;

public abstract class VehicleInteractableComponent extends VehicleBaseComponent implements InteractableObjectII {
    /// Dados da fixture
    public FixtureData
        triggerFixData,
        fixData;

    /// Lista de fixtures do objeto
    public List<Fixture> fixList;

    ///Buffer de posição
    private Vector2 buffedPos;

    public VehicleInteractableComponent(
        String name,
        String id,
        VehicleSection ownerSection,
        VehicleComponentType type,
        FixtureData fixData,
        FixtureData triggerFixData
    ) {
        super(
            name,
            id,
            ownerSection,
            type
        );

        this.triggerFixData = triggerFixData;
        this.fixData = fixData;

        this.fixList = new ArrayList<>();

        this.buffedPos = new Vector2();

    }

    @Override
    public void initObject() {
        super.initObject();
        initPhysicalFixtures();
        initTriggerFixtures();
    }

    private void initPhysicalFixtures() {
        fixList.addAll(
            BodyCreatorHelper.createFixturesFromData(
                fixData,
                ownerSection.getInternalBody()
            )
        );
        for (Fixture fix : fixList) {
            fix.setUserData(new GameObjectTag(ObjectType.VEHICLE, this));
        }
    }

    private void initTriggerFixtures() {
        List<Fixture> triggerFixList = BodyCreatorHelper.createFixturesFromData(
            triggerFixData,
            ownerSection.getTriggerBody()
        );
        for (Fixture fix : triggerFixList) {
            fix.setUserData(new GameObjectTag(ObjectType.INTERACTABLE, this));
        }
    }

    public Vector2 getObjectPosMeters() {
        buffedPos.set(
            ownerSection.getBody().getPosition()
        );

        buffedPos.x += fixData.globalOffsetX + fixData.offsetX;
        buffedPos.y += fixData.globalOffsetY + fixData.offsetY;

        return buffedPos;
    }

    @Override
    public FixtureData getTriggerFixData() {
        return triggerFixData;
    }

    @Override
    protected void executeDispose() {
        super.executeDispose();

        fixList.clear();
    }

    @Override
    protected void nullifyReferences() {
        super.nullifyReferences();
        triggerFixData = null;
        fixData = null;
        fixList = null;
        buffedPos = null;
    }
}
