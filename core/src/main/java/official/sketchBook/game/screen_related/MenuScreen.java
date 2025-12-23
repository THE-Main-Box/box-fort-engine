package official.sketchBook.game.screen_related;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.AppMain;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.camera_related.utils.CameraUtils;
import official.sketchBook.engine.screen_related.BaseScreen;

import static official.sketchBook.game.util_related.constants.DebugC.show_fps_ups_metrics;


public class MenuScreen extends BaseScreen {

    private final OrthographicCameraManager uiCameraManager;
    private final BitmapFont font;

    public MenuScreen(AppMain app) {
        super(app);

        uiCameraManager = CameraUtils.createScreenCamera();

        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
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
    protected void prepareUIBatchAndRender() {
        super.prepareUIBatchAndRender();
    }

    @Override
    public void drawUI(SpriteBatch batch) {
        batch.setProjectionMatrix(uiCameraManager.getCamera().combined);

        if (show_fps_ups_metrics) {

            font.draw(batch, "FPS: " + getFps(), 10, this.screenHeightInPx - 10);
            font.draw(batch, "UPS: " + getUps(), 10, this.screenHeightInPx - 30);
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.resizeUiCamera(uiCameraManager, width, height);
    }

    @Override
    public void dispose() {
        super.dispose();

        this.font.dispose();
    }
}
