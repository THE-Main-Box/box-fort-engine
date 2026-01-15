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

    /// Garante que tenhamos constantes de densidade da body
    float getDensity();

    /// Garante que tenhamos constantes de restituição da body
    float getRest();

    /// Garante que tenhamos constantes de fricção da body
    float getFrict();

    /// Garante que exista uma mask para definir com quem podemos interagir
    short getMaskBit();

    /// Garante que exista uma category para dizer quem nós somos
    short getCategoryBit();
}
