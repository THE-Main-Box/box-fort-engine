package official.sketchBook.engine.components_related.intefaces.base_interfaces;

import com.badlogic.gdx.utils.Disposable;

public interface UpdatableObject extends Disposable {
    void update(float delta);
    void postUpdate();

    void initObject();

}
