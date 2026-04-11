package official.sketchBook.engine.util_related.contact_listener.listeners;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.VehiclePassenger;
import official.sketchBook.engine.game_object_related.vehicle.VehicleSection;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyTagHelper;

import static official.sketchBook.engine.util_related.helper.body.BodyTagHelper.getFixtureTagFromContact;

public class VehicleContactListener implements MultiContactListener.SubContactListener {

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        handle(contact, tagA, tagB, true);
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        handle(contact, tagA, tagB, false);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
    }

    private void handle(Contact contact, GameObjectTag tagA, GameObjectTag tagB, boolean entering) {
        if (!tryHandle(contact, tagA, tagB, entering)) {
            tryHandle(contact, tagB, tagA, entering);
        }
    }

    private boolean tryHandle(Contact contact, GameObjectTag sectionTag, GameObjectTag passengerTag, boolean entering) {
        if (sectionTag == null || passengerTag == null) return false;
        // A body precisa ser um veículo
        if (sectionTag.type != ObjectType.VEHICLE) return false;

        if (!(sectionTag.owner instanceof VehicleSection)) return false;

        // O passageiro precisa ser um VehiclePassenger
        if (!(passengerTag.owner instanceof VehiclePassenger)) return false;

        // Verificamos se a fixture que colidiu é o sensor de área seca
        // Pegamos a fixture da section com base em qual body é a section
        GameObjectTag fixtureTag = getFixtureTagFromContact(contact);
        if (fixtureTag == null || fixtureTag.type != ObjectType.DYNAMIC_DRY_AREA) return false;

        VehiclePassenger passenger = (VehiclePassenger) passengerTag.owner;
        VehicleSection section = (VehicleSection) sectionTag.owner;

        if (entering) {
            passenger.getVehiclePassengerPhysicsC().setCurrentSection(section);
            passenger.getLiquidInteractionC().setCanInteract(false);
        } else {
            passenger.getVehiclePassengerPhysicsC().setCurrentSection(null);
            passenger.getLiquidInteractionC().setCanInteract(true);
        }

        return true;
    }
}
