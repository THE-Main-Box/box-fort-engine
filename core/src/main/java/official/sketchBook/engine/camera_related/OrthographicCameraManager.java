package official.sketchBook.engine.camera_related;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import static com.badlogic.gdx.math.MathUtils.lerp;

public class OrthographicCameraManager {
    private final OrthographicCamera camera;
    private final Viewport viewport;

    /// Offsets máximos nos eixos x e y
    public int
        minOffSetX,
        maxOffSetX,
        minOffsetY,
        maxOffSetY;

    /// Bordas para o uso da dead zone, são relativas ao eixo central da camera em pixels
    public float
        rightBorder = 0,
        leftBorder = 0,
        topBorder = 0,
        bottomBorder = 0;

    /// dislocamento atual da camera
    public float
        xOffset,
        yOffset;

    /// Suavisadores de movimento, 1 é instantaneo e 0 é demorado
    public float
        xEase = 0.5f,
        yEase = 0.5f;

    /**
     * Inicializa o gerenciador da câmera com viewport e configurações padrão
     *
     * @param viewportWidth  Largura da janela visível em metros (Ex: 12.8f para ~1280px)
     * @param viewportHeight Altura da janela visível em metros (Ex: 7.2f para ~720px)
     */
    public OrthographicCameraManager(float viewportWidth, float viewportHeight) {
        this.camera = new OrthographicCamera();
        this.viewport = new ExtendViewport(
            viewportWidth,
            viewportHeight,
            camera
        );
        this.camera.position.set(
            viewportWidth / 2f,
            viewportHeight / 2f,
            0
        );
        this.camera.update();
    }

    /// Posiciona a câmera diretamente sobre o alvo sem suavização
    public void trackObjectDirectly(float targetX, float targetY) {
        camera.position.set(targetX, targetY, 0);
        camera.update();
    }

    /// Posiciona a câmera seguindo o alvo com dead zone e suavização de movimento
    public void trackObjectByOffset(float targetX, float targetY) {
        //Calcula a largura e altura efetiva do viewport considerando o zoom
        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

        //Atualiza o offset no eixo x de acordo com a dead zone
        updateXOffset(targetX, rightBorder, leftBorder, effectiveViewportWidth);
        //Atualiza o offset no eixo y de acordo com a dead zone
        updateYOffset(targetY, bottomBorder, topBorder, effectiveViewportHeight);

        //Define a posição da câmera baseada no viewport efetivo e seus offsets
        camera.position.x = effectiveViewportWidth / 2f + xOffset;
        camera.position.y = effectiveViewportHeight / 2f + yOffset;

        camera.update();
    }

    /// Define os limites de deslocamento da câmera nos eixos x e y
    public void setCameraOffsetLimit(
        int minOffSetX,
        int maxXOffSet,
        int minOffsetY,
        int maxYOffSet
    ){
        this.minOffSetX = minOffSetX;
        this.minOffsetY = minOffsetY;
        this.maxOffSetX = maxXOffSet;
        this.maxOffSetY = maxYOffSet;
    }

    /// Define a dead zone da câmera para cada borda
    public void defineDeadZone(
        float marginLeft,
        float marginRight,
        float marginTop,
        float marginBottom
    ){
        this.leftBorder = marginLeft;
        this.rightBorder= marginRight;
        this.topBorder = marginTop;
        this.bottomBorder = marginBottom;
    }

    /// Atualiza o offset no eixo x baseado na posição do alvo e dead zone
    private void updateXOffset(
        float targetX,
        float rightBorder,
        float leftBorder,
        float effectiveViewportWidth
    ) {
        //Calcula o centro da câmera considerando o offset atual
        float centerX = effectiveViewportWidth / 2f + xOffset;
        //Encontra a diferença entre a posição do alvo e o centro da câmera
        float diffX = targetX - centerX;

        //Se o alvo ultrapassar a borda direita da dead zone
        if (diffX > rightBorder) {
            //Move o offset em direção ao alvo com suavização
            xOffset = roundLerp(xOffset, xOffset + (diffX - rightBorder), xEase);
        }
        //Se o alvo ultrapassar a borda esquerda da dead zone
        else if (diffX < leftBorder) {
            //Move o offset em direção ao alvo com suavização
            xOffset = roundLerp(xOffset, xOffset + (diffX - leftBorder), xEase);
        }

        //Limita o offset aos seus valores mínimo e máximo permitidos
        xOffset = clamp(
            xOffset,
            minOffSetX,
            maxOffSetX
        );
    }

    /// Atualiza o offset no eixo y baseado na posição do alvo e dead zone
    private void updateYOffset(
        float targetY,
        float bottomBorder,
        float topBorder,
        float effectiveViewportHeight
    ) {
        //Calcula o centro da câmera considerando o offset atual
        float centerY = effectiveViewportHeight / 2f + yOffset;
        //Encontra a diferença entre a posição do alvo e o centro da câmera
        float diffY = targetY - centerY;

        //Se o alvo ultrapassar a borda inferior da dead zone
        if (diffY < bottomBorder) {
            //Move o offset em direção ao alvo com suavização
            yOffset = roundLerp(yOffset, yOffset + (diffY - bottomBorder), yEase);
        }
        //Se o alvo ultrapassar a borda superior da dead zone
        else if (diffY > topBorder) {
            //Move o offset em direção ao alvo com suavização
            yOffset = roundLerp(yOffset, yOffset + (diffY - topBorder), yEase);
        }

        //Limita o offset aos seus valores mínimo e máximo permitidos
        yOffset = clamp(
            yOffset,
            minOffsetY,
            maxOffSetY
        );
    }

    /// Interpola linearmente entre dois valores e arredonda o resultado
    private float roundLerp(float from, float to, float smoothFactor) {
        //Realiza a interpolação linear entre os valores
        float lerped = lerp(from, to, smoothFactor);
        //Arredonda o resultado para evitar tremulação de pixel
        return Math.round(lerped * 10) / 10f;
    }

    /// Limita um valor entre um mínimo e um máximo
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    /// Atualiza o viewport quando a janela é redimensionada
    public void updateViewport(int width, int height) {
        viewport.update(width, height, true);
    }

    /// Define o nível de zoom da câmera
    public void setZoom(float zoom) {
        camera.zoom = zoom;
        camera.update();
    }

    /// Retorna a câmera ortográfica gerenciada
    public OrthographicCamera getCamera() {
        return camera;
    }

    /// Retorna o viewport utilizado
    public Viewport getViewport() {
        return viewport;
    }
}
