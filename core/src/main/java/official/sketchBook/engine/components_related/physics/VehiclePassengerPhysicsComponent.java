package official.sketchBook.engine.components_related.physics;

import com.badlogic.gdx.math.Vector2;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.VehiclePassenger;
import official.sketchBook.engine.game_object_related.vehicle.VehicleSection;

public class VehiclePassengerPhysicsComponent extends MovableObjectPhysicsComponent implements Component {

    /// Seção atual em que estamos
    private VehicleSection currentSection;

    /// Buffer de posição
    private final Vector2 relativePos = new Vector2();

    private boolean disposed = false;

    public VehiclePassengerPhysicsComponent(
        VehiclePassenger object,
        int categoryBit,
        int maskBit,
        float density,
        float frict,
        float rest
    ) {
        super(object, categoryBit, maskBit, density, frict, rest);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

    }

    @Override
    public void postUpdate() {
        super.postUpdate();

        synObjectPositionToVehicle();
    }

    @Override
    public void syncObjectToBodyPos() {
        super.syncObjectToBodyPos();

    }

    private void synObjectPositionToVehicle(){
        if (currentSection == null) return;

        // Pegamos a posição atual do passageiro no mundo
        Vector2 passengerPos = object.getBody().getPosition();

        // Pegamos a posição da section no mundo
        Vector2 sectionPos = currentSection.getBody().getPosition();

        // Calculamos a posição relativa do passageiro em relação à section
        relativePos.set(
            passengerPos.x - sectionPos.x,
            passengerPos.y - sectionPos.y
        );
    }

    @Override
    public void dispose() {
        if (disposed) return;

        nullifyReferences();

        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        super.nullifyReferences();

        this.currentSection = null;
    }

    /// Atualiza qual section o passageiro está
    public void setCurrentSection(VehicleSection section) {
        this.currentSection = section;
    }

    public VehicleSection getCurrentSection() {
        return currentSection;
    }

    public boolean isInsideVehicle() {
        return currentSection != null;
    }
}
