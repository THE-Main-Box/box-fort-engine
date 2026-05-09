package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.util_related.contact_listener.ContactActions;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

public interface AutoListenedPhysicalObjectII {
    void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB);

    void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB);

    void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB);

    void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB);
}
