package official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.List;

public interface CompositeRenderableObjectII
    extends RenderableObjectII {

    List<? extends RenderableObjectII> getRenderableObjList();

    @Override
    default void updateVisuals(float delta){

        List<? extends RenderableObjectII> list =
            getRenderableObjList();

        if(list == null)
            return;

        for(int i = 0; i < list.size(); i++){
            list.get(i).updateVisuals(delta);
        }
    }

    @Override
    default void render(SpriteBatch batch){

        List<? extends RenderableObjectII> list =
            getRenderableObjList();

        if(list == null)
            return;

        for(int i = 0; i < list.size(); i++){

            RenderableObjectII obj =
                list.get(i);

            if(obj.canRender())
                obj.render(batch);
        }
    }

    @Override
    default void disposeGraphics(){

        List<? extends RenderableObjectII> list =
            getRenderableObjList();

        if(list == null)
            return;

        for(int i = 0; i < list.size(); i++){
            list.get(i).disposeGraphics();
        }

    }

    @Override
    default boolean canRender(){
        return true;
    }
}
