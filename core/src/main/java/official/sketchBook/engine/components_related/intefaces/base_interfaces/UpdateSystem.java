package official.sketchBook.engine.components_related.intefaces.base_interfaces;

public interface UpdateSystem {
    void update(float delta);
    void postUpdate();

    int getUpdatesMetric();
    void resetUpdateMetric();

    void dispose();
}
