package official.sketchBook.game.screen_related;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.AppMain;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.camera_related.utils.CameraUtils;
import official.sketchBook.engine.screen_related.BaseScreen;

import static official.sketchBook.game.util_related.constants.DebugC.show_fps_ups_metrics;

public class PlayScreen extends BaseScreen {
    private final OrthographicCameraManager uiCameraManager;
    private final OrthographicCameraManager gameCameraManager;
    private final BitmapFont font;

    public PlayScreen(AppMain app) {
        super(app);

        uiCameraManager = CameraUtils.createScreenCamera();
        gameCameraManager = CameraUtils.createScreenCamera();

        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void postUpdate() {
        uiCameraManager.getCamera().update();
        gameCameraManager.getCamera().update();
    }

    @Override
    public void updateVisuals(float delta) {
    }

    @Override
    public void drawGame(SpriteBatch batch) {
        // Aplica a câmera do JOGO
        batch.setProjectionMatrix(gameCameraManager.getCamera().combined);

    }

    @Override
    public void drawUI(SpriteBatch batch) {
        // Aplica a câmera da UI (Coordenadas de tela fixas)
        batch.setProjectionMatrix(uiCameraManager.getCamera().combined);

        if (show_fps_ups_metrics) {
            font.draw(batch, "FPS: " + getFps(), 10, this.screenHeightInPx - 10);
            font.draw(batch, "UPS: " + getUps(), 10, this.screenHeightInPx - 30);
        }
    }

    @Override
    public void show() {
        // Agora a PlayScreen é mostrada após o MenuScreen dar o comando
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        resizeUiCamera(uiCameraManager, width, height);
        gameCameraManager.updateViewport(width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }
}
