package official.sketchBook.engine.screen_related;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.AppMain;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.RenderSystem;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.UpdateSystem;

import static official.sketchBook.game.util_related.constants.RenderingC.FPS_TARGET;

public abstract class BaseScreen implements Screen {

    /// Dimensões atuais da tela em pixels
    protected float screenWidthInPx, screenHeightInPx;

    //Métrica de desempenho
    private float metricsTimer = 0;
    private int fps, ups;

    /// Referência ao Inicializador do app
    protected final AppMain app;

    protected UpdateSystem updateSystem;
    protected RenderSystem renderSystem;

    public BaseScreen(AppMain app) {
        this.app = app;

        Gdx.graphics.setForegroundFPS((int) FPS_TARGET);
        this.initSystems();
    }

    protected void initSystems() {

    }


    /// Função para atualização geral
    public abstract void updateScreen(float delta);

    /// Atualiza os visuais antes de renderizar
    public abstract void updateVisuals(float delta);

    /**
     * Renderiza tudo da UserInterface
     *
     * @param batch referencia ao batch para renderização
     */
    public abstract void drawUI(SpriteBatch batch);

    /**
     * Renderiza tudo do jogo
     *
     * @param batch referencia ao batch para renderização
     */
    public abstract void drawGame(SpriteBatch batch);

    /// Organiza o gameLoop de um modo funcional e granular
    @Override
    public void render(float delta) {
        updateSystem.update(delta);         //Atualização

        updateMetrics(delta);               //Atualiza as métricas para visualização

        renderSystem.render(delta);         //Renderiza tudo

    }

    /// Atualiza a contagem de fps e ups a cada segundo
    private void updateMetrics(float delta) {
        metricsTimer += delta;

        if (metricsTimer >= 1.0f) {
            fps = Gdx.graphics.getFramesPerSecond();
            ups = updateSystem.getUpdatesMetric();
            metricsTimer = 0;
            updateSystem.resetUpdateMetric();
        }
    }

    public void setUpdateSystem(UpdateSystem updateSystem) {
        this.updateSystem = updateSystem;
    }

    public void setRenderSystem(RenderSystem renderSystem) {
        this.renderSystem = renderSystem;
    }

    public int getFps() {
        return fps;
    }

    public int getUps() {
        return ups;
    }

    /**
     * Atualiza a dimensão da camera com base na viewport
     *
     * @param uiCamera Camera ortográfica que iremos atualizar
     * @param width    nova largura em pixels
     * @param height   nova altura em pixels
     */
    protected void resizeUiCamera(OrthographicCameraManager uiCamera, int width, int height) {
        if (uiCamera == null) return;
        uiCamera.updateViewport(width, height);
        this.screenWidthInPx = uiCamera.getCamera().viewportWidth;
        this.screenHeightInPx = uiCamera.getCamera().viewportHeight;
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        updateSystem.dispose();
    }
}
