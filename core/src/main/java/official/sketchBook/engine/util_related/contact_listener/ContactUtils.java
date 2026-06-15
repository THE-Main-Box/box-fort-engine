package official.sketchBook.engine.util_related.contact_listener;

public class ContactUtils {


    public static class keys{
        public static final String SELF_LISTENER = "self_listened";
        public static final String MOB_LISTENER = "movable_object";
        public static final String VEHICLE_LISTENER = "vehicle";
        public static final String INTERACT_LISTENER = "interactable";
        public static final String LIQUID_LISTENER = "liquid";
        public static final String PROJECTILE_LISTENER = "projectile";
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
