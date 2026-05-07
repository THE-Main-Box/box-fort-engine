package official.sketchBook.engine.components_related.system_utils;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.RenderableObjectII;

import java.util.ArrayList;
import java.util.List;

public class RenderableAndDefaultComponentManagerComponent extends ComponentManagerComponent {

    private final List<RenderableObjectII>
        toRender;

    private boolean
        graphicsDisposed = false;

    public RenderableAndDefaultComponentManagerComponent() {
        super();
        toRender = new ArrayList<>();
    }

    public void updateVisuals(float deltaTime) {
        for (int i = toRender.size() - 1; i >= 0; i--) {
            toRender.get(i).updateVisuals(deltaTime);
        }
    }

    public void render(SpriteBatch batch) {
        for (int i = toRender.size() - 1; i >= 0; i--) {
            toRender.get(i).render(batch);
        }
    }

    public <T extends RenderableObjectII> void remove(
        Class<T> type,
        boolean autoDispose
    ) {
        for (int i = toRender.size() - 1; i >= 0; i--) {
            RenderableObjectII c = toRender.get(i);
            if (type.isInstance(c)) {
                if (autoDispose) c.disposeGraphics();
                toRender.remove(i);
            }
        }
    }

    public void addToRender(Component component) {
        if (!(component instanceof RenderableObjectII)) return;

        this.toRender.add(
            (RenderableObjectII) component
        );

    }

    public void disposeGraphics() {
        if (graphicsDisposed) return;

        for (RenderableObjectII object : toRender) {
            object.disposeGraphics();
        }

        toRender.clear();

        graphicsDisposed = true;
    }

}
