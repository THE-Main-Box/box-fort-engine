package official.sketchBook.engine.camera_related;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import static com.badlogic.gdx.math.MathUtils.lerp;

public class OrthographicCameraManager {
    private final OrthographicCamera camera;
    private final Viewport viewport;

    /// Limites absolutos do mundo em pixels — onde a c�mera pode ir
    private float
        worldMinX = 0,
        worldMaxX = 0,
        worldMinY = 0,
        worldMaxY = 0;

    /// Dead zone relativa ao centro da c�mera em pixels
    public float
        rightBorder = 0,
        leftBorder = 0,
        topBorder = 0,
        bottomBorder = 0;

    /// Suavizadores de movimento, 1 = instant�neo, 0 = demorado
    public float
        xEase = 0.5f,
        yEase = 0.5f;

    public OrthographicCameraManager(float viewportWidth, float viewportHeight) {
        this.camera = new OrthographicCamera();
        this.viewport = new ExtendViewport(viewportWidth, viewportHeight, camera);
        this.camera.position.set(viewportWidth / 2f, viewportHeight / 2f, 0);
        this.camera.update();
    }

    /// Posiciona a c�mera diretamente sobre o alvo sem suaviza��o
    public void trackObjectDirectly(float targetX, float targetY) {
        camera.position.set(
            clampCameraX(targetX),
            clampCameraY(targetY),
            0
        );
        camera.update();
    }

    /// Posiciona a c�mera seguindo o alvo com dead zone e suaviza��o
    public void trackObjectByOffset(float targetX, float targetY) {
        float halfW = (camera.viewportWidth * camera.zoom) / 2f;
        float halfH = (camera.viewportHeight * camera.zoom) / 2f;

        float currentX = camera.position.x;
        float currentY = camera.position.y;

        /// Diferen�a entre o alvo e o centro atual da c�mera
        float diffX = targetX - currentX;
        float diffY = targetY - currentY;

        float desiredX = currentX;
        float desiredY = currentY;

        /// Atualiza X se o alvo saiu da dead zone
        if (diffX > rightBorder) {
            desiredX = lerp(currentX, currentX + (diffX - rightBorder), xEase);
        } else if (diffX < -leftBorder) {
            desiredX = lerp(currentX, currentX + (diffX + leftBorder), xEase);
        }

        /// Atualiza Y se o alvo saiu da dead zone
        if (diffY > topBorder) {
            desiredY = lerp(currentY, currentY + (diffY - topBorder), yEase);
        } else if (diffY < -bottomBorder) {
            desiredY = lerp(currentY, currentY + (diffY + bottomBorder), yEase);
        }

        /// Aplica com clamp nos limites do mundo
        camera.position.x = clampCameraX(desiredX);
        camera.position.y = clampCameraY(desiredY);

        camera.update();
    }

    /// Limita a posi��o X da c�mera para n�o sair dos limites do mundo
    private float clampCameraX(float x) {
        float halfW = (camera.viewportWidth * camera.zoom) / 2f;
        return clamp(x, worldMinX + halfW, worldMaxX - halfW);
    }

    /// Limita a posi��o Y da c�mera para n�o sair dos limites do mundo
    private float clampCameraY(float y) {
        float halfH = (camera.viewportHeight * camera.zoom) / 2f;
        return clamp(y, worldMinY + halfH, worldMaxY - halfH);
    }

    /// Define os limites absolutos do mundo — recalcula automaticamente com o zoom atual
    public void updateRoomLimits(float minPosX, float minPosY,float roomWidthPx, float roomHeightPx) {
        worldMinX = minPosX;
        worldMinY = minPosY;
        worldMaxX = roomWidthPx;
        worldMaxY = roomHeightPx;
    }

    public void updateRoomLimits(float roomWidthPx, float roomHeightPx) {
        updateRoomLimits(
            0,
            0,
            roomWidthPx,
            roomHeightPx
        );
    }

    /// Define a dead zone da c�mera para cada borda em pixels
    public void defineDeadZone(
        float marginLeft,
        float marginRight,
        float marginTop,
        float marginBottom
    ) {
        this.leftBorder = marginLeft;
        this.rightBorder = marginRight;
        this.topBorder = marginTop;
        this.bottomBorder = marginBottom;
    }

    ///Realiza uma suavização da movimentação de posição
    private float lerp(float from, float to, float t) {
        float lerped = com.badlogic.gdx.math.MathUtils.lerp(from, to, t);
        return Math.round(lerped * 10) / 10f;
    }

    ///Mantém a limitação dos limites minimos e máximos
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    ///Atualiza os dados da viewport
    public void updateViewport(int width, int height) {
        viewport.update(width, height, true);
    }

    ///Atualiza o zoom e chama os métodos para atualizar as variaveis corretas
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
