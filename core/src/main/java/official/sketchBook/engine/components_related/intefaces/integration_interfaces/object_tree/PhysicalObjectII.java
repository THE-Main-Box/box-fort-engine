package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;

public interface PhysicalObjectII {

    /// Garante que criemos uma body
    void createBody();

    void onObjectAndBodyPosSync();

    /// Garante que tenhamos um meio de obter as dimensões e posição
    TransformComponent getTransformC();

    PhysicsComponent getPhysicsC();

    PhysicalGameObjectDataManager getPhysicalManager();

    /// Garante que tenhamos uma body
    Body getBody();
}
