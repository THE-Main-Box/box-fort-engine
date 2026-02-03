package official.sketchBook.engine.components_related.intefaces.base_interfaces;

public interface RenderSystem {
    void draw(float delta);

    void updateVisuals(float delta);

    void dispose();
}
