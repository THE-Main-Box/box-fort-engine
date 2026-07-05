package official.sketchBook.engine.components_related.vehicle;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ControllableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

public class WirableVehicleDoor extends VehicleDoor implements ControllableObjectII, RenderableObjectII {
    public WirableVehicleDoor(
        VehicleSection ownerSection,
        FixtureData fixData,
        FixtureData triggerFixData,
        boolean broken,
        boolean locked,
        boolean open
    ) {
        super(ownerSection, fixData, triggerFixData, broken, locked, open);
    }

    @Override
    public void executeWiringInteraction() {
        updateDoorOpenState(!open);
    }

    @Override
    public boolean canWireInteract() {
        return true;
    }

    @Override
    public int getRenderIndex() {
        return 0;
    }

    @Override
    public void updateVisuals(float delta) {
    }

    @Override
    public void render(SpriteBatch batch) {

    }

    @Override
    public boolean canRender() {
        return false;
    }

    @Override
    public boolean isInScreen() {
        return false;
    }

    @Override
    public void setInScreen(boolean inScreen) {

    }

    @Override
    public TransformComponent getTransformC() {
        return null;
    }

    @Override
    public void disposeGraphics() {

    }
}
