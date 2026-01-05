package official.sketchBook.engine.components_related.intefaces.integration_interfaces;

import com.badlogic.gdx.physics.box2d.Body;

public interface MovablePhysicalObjectII extends MovableObjectII{
    Body getBody();

    void onBodyObjectSync();
}
