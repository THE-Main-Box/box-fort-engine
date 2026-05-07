package official.sketchBook.engine.util_related.contact_listener;

import com.badlogic.gdx.physics.box2d.ContactListener;

public class ContactUtils {


    public static class keys{
        public static final String MOB_LISTENER = "movable_object_listener";
        public static final String PROJECTILE_LISTENER = "projectile_listener";
        public static final String LIQUID_LISTENER = "physical_liquid_listener";
        public static final String VEHICLE_LISTENER = "vehicle_listener";
    }

    public static synchronized void handleContactListener(
        MultiContactListener contactListener,
        boolean remove,
        String listenerKey,
        MultiContactListener.SubContactListener listener
    ) {
        if(contactListener == null) return;
        if (!remove) {

            if (contactListener.existListener(listenerKey))
                contactListener.removeListener(listenerKey);

            contactListener.addListener(listenerKey, listener);
        } else {
            if (contactListener.existListener(listenerKey))
                contactListener.removeListener(listenerKey);
        }
    }
}
