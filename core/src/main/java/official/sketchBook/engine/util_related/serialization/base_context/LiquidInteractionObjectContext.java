package official.sketchBook.engine.util_related.serialization.base_context;

public interface LiquidInteractionObjectContext {

    LIOContextData getLiquidInteractionContextData();

    class LIOContextData {
        public final float
            mass,
            volume;

        public final boolean canInteract;

        public LIOContextData(float mass, float volume, boolean canInteract) {
            this.mass = mass;
            this.volume = volume;
            this.canInteract = canInteract;
        }
    }

}
