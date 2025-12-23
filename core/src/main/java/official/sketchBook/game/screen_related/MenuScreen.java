package official.sketchBook.game.screen_related;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.AppMain;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.camera_related.utils.CameraUtils;
import official.sketchBook.engine.screen_related.BaseScreen;


public class MenuScreen extends BaseScreen {

    private final OrthographicCameraManager uiCameraManager;

    public MenuScreen(AppMain app) {
        super(app);

        uiCameraManager = CameraUtils.createScreenCamera();
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {
        //Garante que a matrix esteja atualizada
        uiCameraManager.getCamera().update();
    }

    @Override
    public void updateVisuals(float delta) {

    }

    @Override
    protected void prepareGameBatchAndRender() {
        //mantemos vazia pois não iremos preparar para renderizar nada do jogo
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
