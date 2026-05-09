package official.sketchBook.engine.util_related.contact_listener.listeners;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.AutoListenedPhysicalObjectII;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

public class AutoListenedPhysicalObjectContactListener implements MultiContactListener.SubContactListener {
    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA.owner instanceof AutoListenedPhysicalObjectII)
            ((AutoListenedPhysicalObjectII) tagA.owner).beginContact(contact, tagA, tagB);

        if (tagB.owner instanceof AutoListenedPhysicalObjectII)
            ((AutoListenedPhysicalObjectII) tagB.owner).beginContact(contact, tagB, tagA);
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA.owner instanceof AutoListenedPhysicalObjectII)
            ((AutoListenedPhysicalObjectII) tagA.owner).endContact(contact, tagA, tagB);

        if (tagB.owner instanceof AutoListenedPhysicalObjectII)
            ((AutoListenedPhysicalObjectII) tagB.owner).endContact(contact, tagB, tagA);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA.owner instanceof AutoListenedPhysicalObjectII)
            ((AutoListenedPhysicalObjectII) tagA.owner).preSolve(contact, oldManifold, tagA, tagB);

        if (tagB.owner instanceof AutoListenedPhysicalObjectII)
            ((AutoListenedPhysicalObjectII) tagB.owner).preSolve(contact, oldManifold, tagB, tagA);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA.owner instanceof AutoListenedPhysicalObjectII)
            ((AutoListenedPhysicalObjectII) tagA.owner).postSolve(contact, impulse, tagA, tagB);

        if (tagB.owner instanceof AutoListenedPhysicalObjectII)
            ((AutoListenedPhysicalObjectII) tagB.owner).postSolve(contact, impulse, tagB, tagA);
    }
}
