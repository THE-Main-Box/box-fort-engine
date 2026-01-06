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
    private float x, y;

    /// Offset de ajuste para o ponto de renderização (em pixels, nunca escala)
    private float drawOffSetX, drawOffSetY;

    private float originOffSetX, originOffSetY;

    /// Origem da rotação (ponto em torno do qual o sprite será rotacionado)
    /// Atualizado automaticamente para o centro quando a escala muda
    private float originX = 0f, originY = 0f;

    /// Rotação atual do sprite em graus
    private float rotation = 0f;

    /// Dimensões de cada quadro da sprite sheet (em pixels)
    private final int canvasHeight, canvasWidth;

    /// Dimensões reais de renderização após aplicação da escala
    private float renderWidth, renderHeight;

    /// Escala da imagem (multiplicativa: 1.0 = normal, 2.0 = 2x maior, 0.5 = metade)
    private float scaleX = 1f, scaleY = 1f;

    /// Define se o sprite está espelhado horizontalmente
    private boolean xAxisInvert;

    /// Define se o sprite está espelhado verticalmente
    private boolean yAxisInvert;

    /// Textura contendo a sprite sheet (não owned, gerenciada pelo AssetManager)
    private final Texture spriteSheet;

    /**
     * Construtor da classe responsável por inicializar os dados da sprite sheet.
     *
     * @param x               Posição X inicial da imagem.
     * @param y               Posição Y inicial da imagem.
     * @param drawOffSetX     Offset de renderização no eixo X (em pixels, não escalado).
     * @param drawOffSetY     Offset de renderização no eixo Y (em pixels, não escalado).
     * @param spriteQuantityX Quantidade de sprites na horizontal.
     * @param spriteQuantityY Quantidade de sprites na vertical.
     * @param xAxisInvert     Define se o sprite começa espelhado horizontalmente.
     * @param yAxisInvert     Define se o sprite começa espelhado verticalmente.
     * @param spriteSheet     Textura contendo a sprite sheet (não será disposed aqui).
     * @throws IllegalArgumentException Se spriteSheet for null ou quantidades forem <= 0.
     */
    public SpriteSheetDataHandler(
        float x,
        float y,
        float drawOffSetX,
        float drawOffSetY,
        int spriteQuantityX,
        int spriteQuantityY,
        boolean xAxisInvert,
        boolean yAxisInvert,
        Texture spriteSheet
    ) {
        if (spriteSheet == null) {
            throw new IllegalArgumentException("Texture não pode ser null");
        }
        if (spriteQuantityX <= 0 || spriteQuantityY <= 0) {
            throw new IllegalArgumentException("Quantidades de sprites devem ser maiores que 0");
        }

        this.x = x;
        this.y = y;
        this.drawOffSetX = drawOffSetX;
        this.drawOffSetY = drawOffSetY;

        this.xAxisInvert = xAxisInvert;
        this.yAxisInvert = yAxisInvert;

        this.spriteSheet = spriteSheet;
        this.canvasWidth = spriteSheet.getWidth() / spriteQuantityX;
        this.canvasHeight = spriteSheet.getHeight() / spriteQuantityY;

        updateRenderDimensions();
        updateRotationOriginToCenter();
    }

    /// Calcula as dimensões reais de renderização com base na escala atual
    /// Usa apenas multiplicação: renderWidth = canvasWidth * scaleX
    private void updateRenderDimensions() {
        renderWidth = canvasWidth * scaleX;
        renderHeight = canvasHeight * scaleY;
    }

    /// Atualiza a origem de rotação para o centro do sprite renderizado
    /// Chamado automaticamente quando a escala muda (ALWAYS_CENTER mode)
    private void updateRotationOriginToCenter() {
        originX = renderWidth / 2;
        originY = renderHeight / 2;
    }

    /// Atualiza a posição da imagem
    /// Offset é em pixels e NUNCA escala, permanece constante
    public void updatePosition(float x, float y) {
        this.x = x - drawOffSetX;
        this.y = y - drawOffSetY;
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
    }

    /**
     * Define manualmente o ponto de origem para rotação do sprite.
     * <p>
     * Use esta função se quiser controle explícito sobre a origem após uma mudança de escala,
     * ou para casos especiais onde a rotação não deve ser no centro.
     *
     * @param originX Coordenada X do ponto de rotação.
     * @param originY Coordenada Y do ponto de rotação.
     */
    public void setRotationOrigin(float originX, float originY) {
        this.originX = originX;
        this.originY = originY;
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
                xAxisInvert,
                yAxisInvert
            ),
            x,
            y,
            originX - originOffSetX,
            originY - originOffSetY,
            renderWidth,
            renderHeight,
            1f,
            1f,
            rotation
        );
    }

    /// Define o offset X da renderização (em pixels, não escala com a imagem)
    public void setDrawOffSetX(float drawOffSetX) {
        this.drawOffSetX = drawOffSetX;
    }

    /// Define o offset Y da renderização (em pixels, não escala com a imagem)
    public void setDrawOffSetY(float drawOffSetY) {
        this.drawOffSetY = drawOffSetY;
    }

    /// Retorna a origem da rotação no eixo X
    public float getOriginX() {
        return originX;
    }

    /// Retorna a origem da rotação no eixo Y
    public float getOriginY() {
        return originY;
    }

    /// Define se o sprite está espelhado horizontalmente
    public void setxAxisInvert(boolean xAxisInvert) {
        this.xAxisInvert = xAxisInvert;
    }

    /// Define se o sprite está espelhado verticalmente
    public void setyAxisInvert(boolean yAxisInvert) {
        this.yAxisInvert = yAxisInvert;
    }

    /// Retorna a textura da sprite sheet (não owned, gerenciada externamente)
    public Texture getSpriteSheet() {
        return spriteSheet;
    }

    /// Retorna a altura de cada quadro da sprite sheet em pixels
    public int getCanvasHeight() {
        return canvasHeight;
    }

    /// Retorna a largura de cada quadro da sprite sheet em pixels
    public int getCanvasWidth() {
        return canvasWidth;
    }

    /// Retorna a escala X atual
    public float getScaleX() {
        return scaleX;
    }

    /// Retorna a escala Y atual
    public float getScaleY() {
        return scaleY;
    }

    /// Retorna a largura renderizada (canvasWidth * scaleX)
    public float getRenderWidth() {
        return renderWidth;
    }

    /// Retorna a altura renderizada (canvasHeight * scaleY)
    public float getRenderHeight() {
        return renderHeight;
    }

    /// Retorna a posição X da imagem
    public float getX() {
        return x;
    }

    /// Retorna a posição Y da imagem
    public float getY() {
        return y;
    }

    /// Retorna a rotação atual em graus
    public float getRotation() {
        return rotation;
    }

    public void setRenderHeight(float renderHeight) {
        this.renderHeight = renderHeight;
    }

    public void setRenderWidth(float renderWidth) {
        this.renderWidth = renderWidth;
    }

    public float getDrawOffSetY() {
        return drawOffSetY;
    }

    public float getDrawOffSetX() {
        return drawOffSetX;
    }

    public float getOriginOffSetX() {
        return originOffSetX;
    }

    public void setOriginOffSetX(float originOffSetX) {
        this.originOffSetX = originOffSetX;
    }

    public float getOriginOffSetY() {
        return originOffSetY;
    }

    public void setOriginOffSetY(float originOffSetY) {
        this.originOffSetY = originOffSetY;
    }
}
