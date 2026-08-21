package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

import official.sketchBook.engine.util_related.serialization.instantiation.SaveData;

public interface SerializableWirableConfigurable extends WirableConfigurable{
    // VehicleBaseComponent (ou uma interface WirableConfigurable-adjacent)
    default SaveData saveWiringConfig(WiringConfig config) { return null; } // sem config por padrão
    default void loadWiringConfig(SaveData data) { } // no-op por padrão
}
