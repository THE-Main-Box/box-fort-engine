package official.sketchBook.game.gameObject_related;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.StaticResourceDisposable;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;
import official.sketchBook.engine.gameObject_related.BaseGameObject;

public class Player extends BaseGameObject implements StaticResourceDisposable {
    public Player(BaseWorldDataManager worldDataManager) {
        super(worldDataManager);
    }

    @Override
    protected void initObject() {

    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {

    }

    @Override
    protected void onObjectDestruction() {

    }

    @Override
    protected void disposeData() {

    }

    public static void disposeStaticResources(){

    }
}
