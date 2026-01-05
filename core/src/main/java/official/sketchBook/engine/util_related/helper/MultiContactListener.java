package official.sketchBook.engine.util_related.helper;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

import java.util.HashMap;
import java.util.Map;

public class MultiContactListener implements ContactListener {
    // Usando um HashMap para armazenar os listeners, onde a chave é uma String (ou qualquer tipo único)
    private final Map<String, ContactListener> listeners = new HashMap<>();

    // Adiciona um listener com uma chave única
    public void addListener(String key, ContactListener listener) {
        listeners.put(key, listener);
    }

    // Remove um listener pelo chave
    public void removeListener(String key) {
        listeners.remove(key);
    }

    @Override
    public void beginContact(Contact contact) {
        // Chama beginContact para cada listener registrado
        for (Map.Entry<String, ContactListener> entry : listeners.entrySet()) {
            entry.getValue().beginContact(contact);  // Delegando para cada listener
        }
    }

    @Override
    public void endContact(Contact contact) {
        // Chama endContact para cada listener registrado
        for (Map.Entry<String, ContactListener> entry : listeners.entrySet()) {
            entry.getValue().endContact(contact);  // Delegando para cada listener
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        // Chama preSolve para cada listener registrado
        for (Map.Entry<String, ContactListener> entry : listeners.entrySet()) {
            entry.getValue().preSolve(contact, oldManifold);  // Delegando para cada listener
        }

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        // Chama postSolve para cada listener registrado
        for (Map.Entry<String, ContactListener> entry : listeners.entrySet()) {
            entry.getValue().postSolve(contact, impulse);  // Delegando para cada listener
        }
    }

    public boolean existListener(String key) {
        return listeners.containsKey(key);
    }
}
