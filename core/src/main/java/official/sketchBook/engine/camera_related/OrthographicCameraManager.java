package official.sketchBook.engine.camera_related;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import static com.badlogic.gdx.math.MathUtils.lerp;

/**
 * Gerencia a câmera principal do jogo, controlando o que o jogador vê.
 * Utiliza o conceito de Unidades do Mundo (Metros), onde 1 unidade = 100 pixels (1cm por pixel).
 */
public class OrthographicCameraManager {
    private final OrthographicCamera camera;
    private final Viewport viewport;

    /// Offsets máximos nos eixos x e y
    public int
        maxOffSetX,
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

    public void trackObjectDirectly(float targetX, float targetY) {
        camera.position.set(targetX, targetY, 0);
        camera.update();
    }

    public void trackObjectByOffset(float targetX, float targetY) {
        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

        updateXOffset(targetX, rightBorder, leftBorder, effectiveViewportWidth);
        updateYOffset(targetY, bottomBorder, topBorder, effectiveViewportHeight);

        camera.position.x = effectiveViewportWidth / 2f + xOffset;
        camera.position.y = effectiveViewportHeight / 2f + yOffset;

        camera.update();
    }

    public void setCameraOffsetLimit(
        int maxXOffSet,
        int maxYOffSet
    ){
        this.maxOffSetX = maxXOffSet;
        this.maxOffSetY = maxYOffSet;
    }

    /// Define a deadZone a respeitar
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

    private void updateXOffset(
        float targetX,
        float rightBorder,
        float leftBorder,
        float effectiveViewportWidth
    ) {

        float centerX = effectiveViewportWidth / 2f + xOffset;
        float diffX = targetX - centerX;

        if (diffX > rightBorder) {
            xOffset = roundLerp(xOffset, xOffset + (diffX - rightBorder), xEase);
        } else if (diffX < leftBorder) {
            xOffset = roundLerp(xOffset, xOffset + (diffX - leftBorder), xEase);
        }

        xOffset = clamp(xOffset, 0, maxOffSetX);
    }

    private void updateYOffset(
        float targetY,
        float bottomBorder,
        float topBorder,
        float effectiveViewportHeight
    ) {
        float centerY = effectiveViewportHeight / 2f + yOffset;
        float diffY = targetY - centerY;

        if (diffY < bottomBorder) {
            yOffset = roundLerp(yOffset, yOffset + (diffY - bottomBorder), yEase);
        } else if (diffY > topBorder) {
            yOffset = roundLerp(yOffset, yOffset + (diffY - topBorder), yEase);
        }

        yOffset = clamp(yOffset, 0, maxOffSetY);
    }

    private float roundLerp(float from, float to, float smoothFactor) {
        float lerped = lerp(from, to, smoothFactor);
        return Math.round(lerped * 10) / 10f; // Arredonda para 1 casa decimal
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    /**
     * Chamado na função resize() da Screen para ajustar a proporção da janela.
     *
     * @param width  largura nova
     * @param height altura nova
     */
    public void updateViewport(int width, int height) {
        viewport.update(width, height, true);
    }

    public void setZoom(float zoom) {
        camera.zoom = zoom;
        camera.update();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Viewport getViewport() {
        return viewport;
    }
}
