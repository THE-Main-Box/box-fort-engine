package official.sketchBook.engine.camera_related;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Gerencia a câmera principal do jogo, controlando o que o jogador vê.
 * Utiliza o conceito de Unidades do Mundo (Metros), onde 1 unidade = 100 pixels (1cm por pixel).
 */
public class OrthographicCameraManager {
    private final OrthographicCamera camera;
    private final Viewport viewport;

    // --- CONFIGURAÇÕES DE MOVIMENTO ---

    // Distância máxima (em metros) que o alvo pode se afastar do centro antes da câmera segui-lo.
    // Ex: 2f significa que o player pode andar 2 metros para os lados antes da câmera se mover.
    private float deadzoneWidth = 2f;
    private float deadzoneHeight = 2f;

    // Velocidade de ajuste da câmera (0.1f é suave, 1.0f é instantâneo).
    private float lerpFactor = 0.1f;

    // --- LIMITES DO MUNDO (Mundo em Metros) ---
    // Evita que a câmera mostre o "vazio" fora das bordas do mapa.
    private float worldLimitLeft = 0;
    private float worldLimitRight = 100;
    private float worldLimitBottom = 0;
    private float worldLimitTop = 100;

    /**
     * @param viewportWidth Largura da janela visível em metros (Ex: 12.8f para ~1280px)
     * @param viewportHeight Altura da janela visível em metros (Ex: 7.2f para ~720px)
     */
    public OrthographicCameraManager(float viewportWidth, float viewportHeight) {
        this.camera = new OrthographicCamera();
        // O ExtendViewport mantém a proporção e expande a visão em telas maiores sem esticar a imagem.
        this.viewport = new ExtendViewport(viewportWidth, viewportHeight, camera);
    }

    /**
     * Atualiza a posição da câmera para seguir um alvo (como o jogador).
     * @param targetX Posição X atual do alvo no mundo (em metros).
     * @param targetY Posição Y atual do alvo no mundo (em metros).
     */
    public void updateCamera(float targetX, float targetY) {
        // 1. Determinar o destino teórico (onde a câmera quer chegar)
        float destinationX = camera.position.x;
        float destinationY = camera.position.y;

        // Cálculo da Deadzone Horizontal:
        // Se a distância entre o alvo e o centro da câmera for maior que a zona morta...
        float deltaX = targetX - camera.position.x;
        if (Math.abs(deltaX) > deadzoneWidth) {
            // Empurra o destino para acompanhar o alvo, mantendo a distância da borda da zona morta.
            destinationX = (deltaX > 0) ? targetX - deadzoneWidth : targetX + deadzoneWidth;
        }

        // Cálculo da Deadzone Vertical:
        float deltaY = targetY - camera.position.y;
        if (Math.abs(deltaY) > deadzoneHeight) {
            destinationY = (deltaY > 0) ? targetY - deadzoneHeight : targetY + deadzoneHeight;
        }

        // 2. Movimentação Suave (Linear Interpolation - LERP)
        // Move a posição atual da câmera em direção ao destino com base no lerpFactor.
        camera.position.x = MathUtils.lerp(camera.position.x, destinationX, lerpFactor);
        camera.position.y = MathUtils.lerp(camera.position.y, destinationY, lerpFactor);

        // 3. Restrição de Bordas (Clamp)
        // Garante que a câmera não saia dos limites do mapa.
        applyWorldConstraints();

        // 4. Atualização Interna
        // Recalcula a matriz de projeção da câmera (essencial para o renderizador).
        camera.update();
    }

    /**
     * Garante que as bordas da câmera nunca ultrapassem os limites definidos para o nível.
     */
    private void applyWorldConstraints() {
        // Precisamos considerar metade da largura/altura da visão atual para travar a câmera.
        float halfViewWidth = (camera.viewportWidth * camera.zoom) / 2f;
        float halfViewHeight = (camera.viewportHeight * camera.zoom) / 2f;

        // MathUtils.clamp(valor, min, max) mantém o valor dentro da faixa desejada.
        camera.position.x = MathUtils.clamp(camera.position.x, worldLimitLeft + halfViewWidth, worldLimitRight - halfViewWidth);
        camera.position.y = MathUtils.clamp(camera.position.y, worldLimitBottom + halfViewHeight, worldLimitTop - halfViewHeight);
    }

    // --- MÉTODOS DE CONFIGURAÇÃO (SETTERS) ---

    public void setDeadzone(float widthMeters, float heightMeters) {
        this.deadzoneWidth = widthMeters;
        this.deadzoneHeight = heightMeters;
    }

    /// Atualiza os limites de mundo
    public void setWorldLimits(float left, float bottom, float right, float top) {
        this.worldLimitLeft = left;
        this.worldLimitBottom = bottom;
        this.worldLimitRight = right;
        this.worldLimitTop = top;
    }

    /// Atualiza o fato de suavização
    public void setLerpFactor(float factor) {
        this.lerpFactor = MathUtils.clamp(factor, 0, 1);
    }

    /**
     *  Chamado na função resize() da Screen para ajustar a proporção da janela.
     *
     * @param width largura nova
     * @param height altura nova
     */
    public void updateViewport(int width, int height) {
        viewport.update(width, height, true);
    }

    public void setZoom(float zoom){
        camera.zoom = zoom;
        camera.update();
    }

    public OrthographicCamera getCamera() { return camera; }
    public Viewport getViewport() { return viewport; }
}
