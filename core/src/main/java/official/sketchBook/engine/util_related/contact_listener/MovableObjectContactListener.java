package official.sketchBook.engine.util_related.contact_listener;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.util_related.enumerators.Direction;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

public class MovableObjectContactListener implements MultiContactListener.SubContactListener {

    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        handle(contact, tagA, tagB);
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

        // Obtemos a direção da perspectiva da Fixture A
        Direction dirA = ContactActions.getCollisionDirection(contact);

        // Processamos a Fixture A
        if (tagA != null && tagA.owner instanceof MovableObjectII) {
            ContactActions.handleBlockedMovement(dirA, (MovableObjectII) tagA.owner);
        }

        // Processamos a Fixture B
        if (tagB != null && tagB.owner instanceof MovableObjectII) {
            // Para a Fixture B, a colisão vem do lado oposto da Normal
            Direction dirB = dirA.getOpposite();
            ContactActions.handleBlockedMovement(dirB, (MovableObjectII) tagB.owner);
        }
    }

}
