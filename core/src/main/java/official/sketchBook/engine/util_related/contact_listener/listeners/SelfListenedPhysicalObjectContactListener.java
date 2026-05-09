package official.sketchBook.engine.util_related.contact_listener.listeners;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.SelfListenedPhysicalObjectII;
import official.sketchBook.engine.util_related.contact_listener.MultiContactListener;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import static official.sketchBook.engine.util_related.helper.body.BodyTagHelper.getFromFixtureTag;

public class SelfListenedPhysicalObjectContactListener implements MultiContactListener.SubContactListener {
    @Override
    public void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {

        GameObjectTag tagFixA = getFromFixtureTag(contact.getFixtureA());
        GameObjectTag tagFixB = getFromFixtureTag(contact.getFixtureB());

        if (tagA.owner instanceof SelfListenedPhysicalObjectII)
            ((SelfListenedPhysicalObjectII) tagA.owner).beginContact(contact, tagA, tagB);

        if (tagB.owner instanceof SelfListenedPhysicalObjectII)
            ((SelfListenedPhysicalObjectII) tagB.owner).beginContact(contact, tagB, tagA);
    }

    @Override
    public void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA.owner instanceof SelfListenedPhysicalObjectII)
            ((SelfListenedPhysicalObjectII) tagA.owner).endContact(contact, tagA, tagB);

        if (tagB.owner instanceof SelfListenedPhysicalObjectII)
            ((SelfListenedPhysicalObjectII) tagB.owner).endContact(contact, tagB, tagA);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA.owner instanceof SelfListenedPhysicalObjectII)
            ((SelfListenedPhysicalObjectII) tagA.owner).preSolve(contact, oldManifold, tagA, tagB);

        if (tagB.owner instanceof SelfListenedPhysicalObjectII)
            ((SelfListenedPhysicalObjectII) tagB.owner).preSolve(contact, oldManifold, tagB, tagA);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB) {
        if (tagA.owner instanceof SelfListenedPhysicalObjectII)
            ((SelfListenedPhysicalObjectII) tagA.owner).postSolve(contact, impulse, tagA, tagB);

        if (tagB.owner instanceof SelfListenedPhysicalObjectII)
            ((SelfListenedPhysicalObjectII) tagB.owner).postSolve(contact, impulse, tagB, tagA);
    }
}
