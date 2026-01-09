package official.sketchBook.engine.animation_related;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import static official.sketchBook.engine.util_related.texture.TextureUtils.obtainCurrentSpriteImage;

/**
 * Gerencia dados e operações relacionados a uma sprite sheet,
 * incluindo posicionamento, rotação, escala e renderização de sprites.
 * <p>
 * IMPORTANTE: A textura NÃO é disposada aqui - é gerenciada estaticamente por classe.
 * Cada GameObject tem sua própria instância desta classe.
 * <p>
 * Ordem de transformação esperada:
 * 1. setScale(x, y)
 * 2. setRotation(degrees)
 * 3. updatePosition(x, y)
 */
public class SpriteSheetDataHandler {
    /// Posição de renderização da imagem na tela
    public float
        xPos,
        yPos;

    /// Offset de ajuste para o ponto de renderização (em pixels, nunca escala)
    private float
        drawOffSetX,
        drawOffSetY;

    /// Offset de posição de origem de rotação de imagem
    public float
        originOffSetX,
        originOffSetY;

    /// Buffer neutro de posição onde a sprite será rotacionada
    /// Atualizado automaticamente para o centro quando a escala muda
    private float
        originX = 0f,
        originY = 0f;

    /// Dimensões de cada quadro da sprite sheet (em pixels)
    private final int
        canvasHeight,
        canvasWidth;

    /// Dimensões reais de renderização após aplicação da escala
    public float
        renderWidth,
        renderHeight;

    /// Escala da imagem (multiplicativa: 1.0 = normal, 2.0 = 2x maior, 0.5 = metade)
    public float
        scaleX = 1f,
        scaleY = 1f;

    /// Define se a sprite estará espelhada, no eixo passado
    public boolean
        mirrorX,
        mirrorY;

    public boolean
        autoUpdateDrawOffsetScale,
        autoUpdateRotationOffsetScale;

    /// Textura contendo a sprite sheet (não owned, gerenciada pelo AssetManager)
    private final Texture spriteSheet;

    /// Rotação atual do sprite em graus
    public float rotation = 0f;

    public SpriteSheetDataHandler(
        float xPos,
        float yPos,
        float drawOffSetX,
        float drawOffSetY,
        int spriteQuantityX,
        int spriteQuantityY,
        float scaleX,
        float scaleY,
        boolean mirrorX,
        boolean mirrorY,
        boolean autoUpdateDrawOffsetScale,
        boolean autoUpdateRotationOffsetScale,
        Texture spriteSheet
    ) {
        if (spriteSheet == null) {
            throw new IllegalArgumentException("Texture não pode ser null");
        }
        if (spriteQuantityX <= 0 || spriteQuantityY <= 0) {
            throw new IllegalArgumentException("Quantidades de sprites devem ser maiores que 0");
        }

        this.xPos = xPos;
        this.yPos = yPos;
        this.drawOffSetX = drawOffSetX;
        this.drawOffSetY = drawOffSetY;

        this.mirrorX = mirrorX;
        this.mirrorY = mirrorY;

        this.spriteSheet = spriteSheet;
        this.canvasWidth = spriteSheet.getWidth() / spriteQuantityX;
        this.canvasHeight = spriteSheet.getHeight() / spriteQuantityY;

        this.autoUpdateDrawOffsetScale = autoUpdateDrawOffsetScale;
        this.autoUpdateRotationOffsetScale = autoUpdateRotationOffsetScale;

        //Atualizamos a escala, que atualiza os dados de renderização internos
        this.setScale(
            scaleX,
            scaleY
        );
    }

    /// Atualiza a offset da origem de rotação caso alteremos a escala,
    private void updateRotationOriginAccordingToScale() {
        this.originOffSetX *= scaleX;
        this.originOffSetY *= scaleY;
    }

    /// Atualiza o offset de desenho caso alteremos a escala
    private void updateDrawOffsetAccordingToScale() {
        this.drawOffSetX *= scaleX;
        this.drawOffSetY *= scaleY;
    }

    /// Calcula as dimensões reais de renderização com base na escala atual
    /// Usa apenas multiplicação: renderWidth = canvasWidth * scaleX
    private void updateRenderDimensions() {
        renderWidth = canvasWidth * scaleX;
        renderHeight = canvasHeight * scaleY;
    }

    /// Atualiza a origem de rotação para o centro do sprite renderizado
    private void updateRotationOriginToCenter() {
        originX = renderWidth / 2;
        originY = renderHeight / 2;
    }

    /// Atualiza a posição da imagem
    public void updatePosition(
        float x,
        float y
    ) {
        this.xPos = x - drawOffSetX;
        this.yPos = y - drawOffSetY;
    }

    /**
     * Define a escala de renderização do sprite.
     * <p>
     * Exemplos:
     * - setScale(1.0f, 1.0f) = tamanho normal
     * - setScale(2.0f, 2.0f) = 2x maior
     * - setScale(0.5f, 0.5f) = metade do tamanho
     * <p>
     * A origem de rotação é automaticamente atualizada para o novo centro.
     * Se você precisar de um ponto de rotação customizado após isso, use setRotationOrigin().
     *
     * @param scaleX Escala no eixo X (deve ser > 0).
     * @param scaleY Escala no eixo Y (deve ser > 0).
     * @throws IllegalArgumentException Se escala for <= 0.
     */
    public void setScale(float scaleX, float scaleY) {
        if (scaleX <= 0 || scaleY <= 0) {
            throw new IllegalArgumentException("Escala deve ser maior que 0");
        }
        this.scaleX = scaleX;
        this.scaleY = scaleY;

        updateRenderDimensions();
        updateRotationOriginToCenter();

        if (autoUpdateRotationOffsetScale)
            updateRotationOriginAccordingToScale();
        if (autoUpdateDrawOffsetScale)
            updateDrawOffsetAccordingToScale();
    }

    /// Define a rotação atual da imagem em graus
    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    /**
     * Renderiza o sprite atual com base no estado interno da classe.
     * A textura NÃO é disposada aqui, pois é gerenciada estaticamente por classe.
     *
     * @param batch         SpriteBatch usado para desenhar o sprite.
     * @param currentSprite Instância de Sprite contendo as informações do frame atual.
     */
    public void renderSprite(SpriteBatch batch, Sprite currentSprite) {
        batch.draw(
            obtainCurrentSpriteImage(
                currentSprite,
                canvasWidth,
                canvasHeight,
                spriteSheet,
                mirrorX,
                mirrorY
            ),
            xPos,
            yPos,
            originX - originOffSetX,
            originY - originOffSetY,
            renderWidth,
            renderHeight,
            1f,
            1f,
            rotation
        );
    }

}
