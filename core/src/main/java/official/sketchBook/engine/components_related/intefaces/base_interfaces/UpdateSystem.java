package official.sketchBook.engine.components_related.intefaces.base_interfaces;

public interface UpdateSystem {
    /// Atualização geral do manager e screen
    void update(float delta);

    int getUpdatesMetric();
    void resetUpdateMetric();

    void dispose();
}
