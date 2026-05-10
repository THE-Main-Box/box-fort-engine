package official.sketchBook.engine.util_related.contact_listener;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import official.sketchBook.engine.util_related.helper.GameObjectTag;

import java.util.HashMap;
import java.util.Map;

import static official.sketchBook.engine.util_related.helper.body.BodyTagHelper.getFromBodyTag;

public class MultiContactListener implements ContactListener {
    // Usando um HashMap para armazenar os listeners, onde a chave é uma String (ou qualquer tipo único)
    private final Map<String, SubContactListener> listeners = new HashMap<>();

    // Adiciona um listener com uma chave única
    public void addListener(String key, SubContactListener listener) {
        listeners.put(key, listener);
    }

    // Remove um listener pelo chave
    public void removeListener(String key) {
        listeners.remove(key);
    }

    @Override
    public void beginContact(Contact contact) {
        GameObjectTag tagA = getFromBodyTag(contact.getFixtureA());
        GameObjectTag tagB = getFromBodyTag(contact.getFixtureB());
        if (tagA == null || tagB == null) return;
        for (Map.Entry<String, SubContactListener> entry : listeners.entrySet()) {
            entry.getValue().beginContact(contact, tagA, tagB);
        }
    }

    @Override
    public void endContact(Contact contact) {
        GameObjectTag tagA = getFromBodyTag(contact.getFixtureA());
        GameObjectTag tagB = getFromBodyTag(contact.getFixtureB());
        if (tagA == null || tagB == null) return;
        for (Map.Entry<String, SubContactListener> entry : listeners.entrySet()) {
            entry.getValue().endContact(contact, tagA, tagB);
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        GameObjectTag tagA = getFromBodyTag(contact.getFixtureA());
        GameObjectTag tagB = getFromBodyTag(contact.getFixtureB());
        if (tagA == null || tagB == null) return;
        for (Map.Entry<String, SubContactListener> entry : listeners.entrySet()) {
            entry.getValue().preSolve(contact, oldManifold, tagA, tagB);
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        GameObjectTag tagA = getFromBodyTag(contact.getFixtureA());
        GameObjectTag tagB = getFromBodyTag(contact.getFixtureB());
        if (tagA == null || tagB == null) return;
        for (Map.Entry<String, SubContactListener> entry : listeners.entrySet()) {
            entry.getValue().postSolve(contact, impulse, tagA, tagB);
        }
    }

    public boolean existListener(String key) {
        return listeners.containsKey(key);
    }

    public interface SubContactListener{
        void beginContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB);

        void endContact(Contact contact, GameObjectTag tagA, GameObjectTag tagB);

        void preSolve(Contact contact, Manifold oldManifold, GameObjectTag tagA, GameObjectTag tagB);

        void postSolve(Contact contact, ContactImpulse impulse, GameObjectTag tagA, GameObjectTag tagB);
    }
}
