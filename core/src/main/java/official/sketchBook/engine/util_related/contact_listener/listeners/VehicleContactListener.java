package official.sketchBook.engine.util_related.contact_listener.listeners;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.game_object_related.vehicle.Vehicle;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

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
        if (contact == null) return;

        tryHandle(tagA, tagB, entering);
        tryHandle(tagB, tagA, entering);
    }

    private void tryHandle(GameObjectTag vehicleTag, GameObjectTag otherTag, boolean entering) {
        if (vehicleTag == null
            ||
            otherTag == null
            ||
            !(vehicleTag.owner instanceof Vehicle)
        ) return;

        // Pronto para iterar futuramente
    }
}
