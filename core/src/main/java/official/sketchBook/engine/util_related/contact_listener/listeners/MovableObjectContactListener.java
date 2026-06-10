package official.sketchBook.engine.util_related.contact_listener.listeners;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.MovableObjectII;
import official.sketchBook.engine.util_related.contact_listener.ContactActions;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;
import official.sketchBook.engine.util_related.enumerators.Direction;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import static official.sketchBook.engine.util_related.contact_listener.ContactActions.shouldCollide;

public class MovableObjectContactListener implements MultiContactListener.SubContactListener {

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
//        handle(contact, tagA, tagB);
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
        ContactActions.applyDefaultFrictionLogic(contact);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
    }

    private void handle(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        if (contact == null || !contact.isTouching()) return;

        // Ignora se qualquer fixture for sensor
        if (contact.getFixtureA().isSensor() || contact.getFixtureB().isSensor()) return;

        // Ignora se as fixtures não colidem entre si pelas mask/category bits
        if (!shouldCollide(contact)) return;
        Direction dirA = ContactActions.getCollisionDirection(contact);

        if (tagA != null && tagA.owner instanceof MovableObjectII) {
            ContactActions.handleBlockedMovement(dirA, (MovableObjectII) tagA.owner);
        }

        if (tagB != null && tagB.owner instanceof MovableObjectII) {
            Direction dirB = dirA.getOpposite();
            ContactActions.handleBlockedMovement(dirB, (MovableObjectII) tagB.owner);
        }
    }

}
