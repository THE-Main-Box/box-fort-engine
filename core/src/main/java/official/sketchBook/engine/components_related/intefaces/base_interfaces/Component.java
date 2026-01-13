package official.sketchBook.engine.components_related.intefaces.base_interfaces;

public interface Component {
    void update(float delta);
    void postUpdate();

    void dispose();

    void nullifyReferences();
    boolean isDisposed();
}
