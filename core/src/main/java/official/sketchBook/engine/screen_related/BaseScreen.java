package official.sketchBook.engine.screen_related;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.AppMain;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.RenderSystem;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.UpdateSystem;

import static official.sketchBook.game.util_related.constants.RenderingConstants.FPS_TARGET;

public abstract class BaseScreen implements Screen {

    /// Dimensões atuais da tela em pixels
    protected float screenWidthInPx, screenHeightInPx;

    ///Métrica de desempenho
    private float metricsTimer = 0;

    private int
        fps,
        ups;

    /// Referência ao Inicializador do app
    protected final AppMain app;

    /// Sistema de gestão de atualização
    protected UpdateSystem updateSystem;
    /// Sistema de gestão de renderização
    protected RenderSystem renderSystem;

    public BaseScreen(AppMain app) {
        this.app = app;

        this.initSystems();
    }

    /// Inicializa os sistemas locais
    protected void initSystems() {
        //Determina o fps
        Gdx.graphics.setForegroundFPS((int) FPS_TARGET);
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

    /// Organização do gameLoop para usarmos os sistemas corretos e isolados corretamente
    @Override
    public void render(float delta) {
        updateSystem.update(delta);         //Seguimos a pipeline de atualização

        updateMetrics(delta);               //Atualiza as métricas para debugging

        renderSystem.draw(delta);           //Seguimos a pipeline de renderização

    }

    /// Atualiza a contagem de fps e ups a cada segundo
    private void updateMetrics(float delta) {
        //Atualiza o temporizador global para verificar se está no tempo de lidar com uma nova medição
        metricsTimer += delta;

        if (metricsTimer >= 1.0f) {
            //Medida de fps vinda do gdx
            fps = Gdx.graphics.getFramesPerSecond();
            //Média de ups vinda do sistema de updates
            ups = updateSystem.getUpdatesMetric();
            // zeramos o timer
            metricsTimer = 0;
            //resetamos as metricas do sistema de update, para marcar o final do ciclo
            updateSystem.resetUpdateMetric();
        }
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
        //Atualiza a viewPort da camera
        uiCamera.updateViewport(width, height);
        //Atualiza as dimensões da camera
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

    public int getFps() {
        return fps;
    }

    public int getUps() {
        return ups;
    }
}
