package official.sketchBook.engine.screen_related;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import official.sketchBook.engine.AppMain;
import official.sketchBook.engine.camera_related.OrthographicCameraManager;

import static official.sketchBook.game.util_related.constants.PhysicsC.FIXED_TIMESTAMP;
import static official.sketchBook.game.util_related.constants.PhysicsC.MAX_ACCUMULATOR;
import static official.sketchBook.game.util_related.constants.RenderingC.FPS_TARGET;

public abstract class BaseScreen implements Screen {

    /// Dimensões atuais da tela em pixels
    protected float screenWidthInPx, screenHeightInPx;

    /// Controle de tempo
    private float accumulator = 0;

    //Métrica de desempenho
    private float metricsTimer = 0;
    private int fps, ups;
    private int updatesCounter = 0;

    /// Referência ao Inicializador do app
    protected final AppMain app;

    public BaseScreen(AppMain app) {
        this.app = app;

        Gdx.graphics.setForegroundFPS((int) FPS_TARGET);
    }

    /// Função para atualização geral
    public abstract void update(float delta);

    /// Atualização crítica para preparação pro próximo update
    public abstract void postUpdate();

    /// Função para atualização de visuais
    public abstract void updateVisuals(float delta);

    /// Limpa a tela com uma cor em específico
    protected void cleanScreen() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
    }

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

    /// Prepara para renderizar o que precisa ser renderizado do jogo
    protected void prepareGameBatchAndRender() {
        app.gameBatch.begin();
        drawGame(app.gameBatch);
        app.gameBatch.end();
    }

    /// Prepara para renderizar o que precisa ser renderizado da ui
    protected void prepareUIBatchAndRender() {
        app.uiBatch.begin();
        drawUI(app.uiBatch);
        app.uiBatch.end();
    }

    /// Organiza o gameLoop de um modo funcional e granular
    @Override
    public void render(float delta) {
        //Atualiza um acumulador para lidar com um loop isolado de atualizações
        this.accumulator += Math.min(delta, MAX_ACCUMULATOR);

        //Verificamos se podemos atualizar
        while (accumulator >= FIXED_TIMESTAMP) {
            update(FIXED_TIMESTAMP);    //Realizamos uma atualização comum
            postUpdate();               //Atualizamos preparando para a próxima atualização

            /*
             *  OBS: A ATUALIZAÇÃO NORMAL E PÓS,
             *  OCORRE DENTRO DA MESMA FUNÇÃO,
             *  PORTANTO NÃO É PRECISO CONTROLAR DE MODO GRANULAR O ACUMULADOR,
             *  POIS OS DOIS MÉTODOS DE ATUALIZAÇÃO E A PÓS ATUALIZAÇÃO IRÃO SER EXECUTADOS UM ATRÁS DO OUTRO
             */
            accumulator -= FIXED_TIMESTAMP; //Atualiza o valor do temporizador
            updatesCounter++;               //Atualiza a quantidade de fps
        }

        //Atualizamos a métrica
        updateMetrics(delta);

        //Atualizamos o que precisa ser atualizado antes de renderizar
        updateVisuals(delta);

        //Limpamos a tela
        cleanScreen();

        //Preparamos para renderizar o jogo e renderizamos em seguida
        prepareGameBatchAndRender();
        //Preparamos para renderizar a ui e renderizamos em seguida
        prepareUIBatchAndRender();

    }

    /// Atualiza a contagem de fps e ups a cada segundo
    private void updateMetrics(float delta) {
        metricsTimer += delta;

        if (metricsTimer >= 1.0f) {
            fps = Gdx.graphics.getFramesPerSecond();
            ups = updatesCounter;
            updatesCounter = 0;
            metricsTimer = 0;
        }
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
     * @param width nova largura em pixels
     * @param height nova altura em pixels
     */
    protected void resizeUiCamera(OrthographicCameraManager uiCamera, int width, int height){
        if(uiCamera == null) return;
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

    }
}
