package official.sketchBook.game.screen_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.AppMain;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.camera_related.utils.CameraUtils;
import official.sketchBook.engine.components_related.system_utils.SingleThreadRenderSystem;
import official.sketchBook.engine.components_related.system_utils.SingleThreadUpdateSystem;
import official.sketchBook.engine.screen_related.BaseScreen;


public class MenuScreen extends BaseScreen {

    private OrthographicCameraManager uiCameraManager;

    public MenuScreen(AppMain app) {
        super(app);
    }

    @Override
    protected void initSystems() {
        super.initSystems();

        uiCameraManager = CameraUtils.createScreenCamera();

        this.updateSystem = new SingleThreadUpdateSystem(
            null,
            this
        );
        this.renderSystem = new SingleThreadRenderSystem(
            this,
            null,
            null,
            null,
            null,
            null
        );

    }

    @Override
    public void updateScreen(float delta) {

    }

    public void updateVisuals(float delta) {
        uiCameraManager.getCamera().update();

    }

    @Override
    public void drawGame(SpriteBatch batch) {
        //Não renderizamos jogo
    }

    @Override
    public void drawUI(SpriteBatch batch) {

    }

    @Override
    public void show() {
        this.app.setScreen(app.getPlayScreen());
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.resizeUiCamera(uiCameraManager, width, height);
    }

    @Override
    public void dispose() {
        super.dispose();

    }
}
