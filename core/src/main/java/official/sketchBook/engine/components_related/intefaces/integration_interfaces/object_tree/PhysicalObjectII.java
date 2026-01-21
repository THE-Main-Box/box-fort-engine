package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.objects.TransformComponent;
import official.sketchBook.engine.components_related.physics.PhysicsComponent;

public interface PhysicalObjectII {

    /// Callback de sincronização de dados de coordenadas
    void onObjectAndBodyPosSync();

    /// Garante que tenhamos um meio de obter as dimensões e posição
    TransformComponent getTransformC();

    /// Garante que teremos um componente de física
    PhysicsComponent getPhysicsC();


    /// Garante que tenhamos uma body
    Body getBody();
}
