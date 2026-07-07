package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction;

public interface WirableConfigurable extends WirableObjectII{
    WiringConfig getCurrentConfiguration();
    void setCurrentConfiguration(WiringConfig newConfiguration);
}
