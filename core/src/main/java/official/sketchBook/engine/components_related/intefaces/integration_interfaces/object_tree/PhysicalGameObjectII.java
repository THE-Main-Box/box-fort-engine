package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.dataManager_related.PhysicalGameObjectDataManager;

public interface PhysicalGameObjectII extends PhysicalObjectII{
    PhysicalGameObjectDataManager getPhysicalManager();


}
