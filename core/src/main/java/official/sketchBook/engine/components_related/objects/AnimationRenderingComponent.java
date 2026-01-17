package official.sketchBook.engine.components_related.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import official.sketchBook.engine.animation_rendering_related.ObjectAnimationPlayer;

import official.sketchBook.engine.animation_rendering_related.Sprite;
import official.sketchBook.engine.animation_rendering_related.SpriteSheetDataHandler;

import java.util.ArrayList;
import java.util.List;

public class AnimationRenderingComponent {

    /// Lista de camadas de animação
    private final List<AnimationLayer> layers;

    /// Referência ao componente conteiner de dados móveis
    private TransformComponent transformC;

    public boolean isRenderDimensionEqualsToObject = true;

    private boolean disposed = false;

    private SpriteSheetDataHandler currentRenderingSheetHandler;
    private ObjectAnimationPlayer currentRenderingAniPlayer;


    public AnimationRenderingComponent(TransformComponent transformC) {
        this.layers = new ArrayList<>();

        this.transformC = transformC;
    }

    /// Atualiza os componentes responsáveis pela renderização em camadas de imagens e animações
    public void updateVisuals(float delta) {
        //Se a lista estiver vazia não tem porque executarmos o código, early exit.
        if (layers.isEmpty()) return;

        //Percorremos as camadas de animação
        for (AnimationLayer currentLayer : layers) {

            //Usamos buffers globais para micro-otimização
            currentRenderingSheetHandler = currentLayer.sheetHandler;
            currentRenderingAniPlayer = currentLayer.aniPlayer;

            //Atualizamos a posição da sprite usando o componente de transform
            currentRenderingSheetHandler.updatePosition(
                transformC.x,
                transformC.y
            );

            //Atualizamos a rotação da imagem
            currentRenderingSheetHandler.setRotation(
                transformC.rotation
            );

            //Determinamos se podemos inverter a imagem em algum eixo
            currentRenderingSheetHandler.mirrorX = transformC.mirrorX;
            currentRenderingSheetHandler.mirrorY = transformC.mirrorY;

            //Se o tamanho de renderização de um objeto corresponde a seu tamanho lógico
            if (isRenderDimensionEqualsToObject) {
                //Fazemos com que as dimensões de renderização sejam as mesmas da largura e altura passadas
                currentRenderingSheetHandler.renderWidth = transformC.width;
                currentRenderingSheetHandler.renderHeight = transformC.height;
            }

            //Se tivermos algum tocador de animações nessa camada atualizamos ele
            if (currentRenderingAniPlayer != null) {
                currentRenderingAniPlayer.update(delta);
            }

        }
    }

    /// Renderiza as camadas
    public void render(SpriteBatch batch) {
        //Early exit caso lista vazia
        if (layers.isEmpty()) return;

        //renderizamos primeiro tudo o que tivermos para renderizar do objeto do jogador
        for (AnimationLayer aniLayer : layers) {

            currentRenderingSheetHandler = aniLayer.sheetHandler;
            currentRenderingAniPlayer = aniLayer.aniPlayer;

            //Obtemos o nosso handler e chamamos para renderizar
            currentRenderingSheetHandler.renderSprite(
                batch,
                currentRenderingAniPlayer != null ?
                    currentRenderingAniPlayer.getCurrentSprite() :
                    aniLayer.defaultSprite
            );

        }
    }

    /**
     * Adiciona uma nova camada de animação na lista
     *
     * @param spriteDataHandler gerenciador de spritesheets
     * @param aniPlayer tocador de animações
     */
    public void addNewLayer(
        SpriteSheetDataHandler spriteDataHandler,
        ObjectAnimationPlayer aniPlayer
    ) {
        layers.add(
            new AnimationLayer(
                spriteDataHandler,
                aniPlayer
            )
        );
    }


    /**
     * Adiciona uma nova camada de animação na lista
     *
     * @param spriteDataHandler gerenciador de spritesheets
     * @param defaultSprite sprite que iremos usar por não termos um tocador de animações
     */
    public void addNewLayer(
        SpriteSheetDataHandler spriteDataHandler,
        Sprite defaultSprite
    ) {
        layers.add(
            new AnimationLayer(
                spriteDataHandler,
                defaultSprite
            )
        );
    }


    public void dispose() {
        if (disposed) return;

        layers.clear();
        transformC = null;

        currentRenderingAniPlayer = null;
        currentRenderingSheetHandler = null;

        disposed = true;
    }

    public List<AnimationLayer> getLayers() {
        return layers;
    }

    public static class AnimationLayer {
        /// Gerenciador de dados de sprite sheet
        public SpriteSheetDataHandler sheetHandler;
        /// Gerenciador de animações
        public ObjectAnimationPlayer aniPlayer;
        /// Sprite a ser renderizada caso não passemos um gerenciador de animações
        public Sprite defaultSprite;

        AnimationLayer(
            SpriteSheetDataHandler sheetHandler,
            ObjectAnimationPlayer aniPlayer
        ) {
            this.sheetHandler = sheetHandler;
            this.aniPlayer = aniPlayer;
            this.defaultSprite = null;
        }

        public AnimationLayer(
            SpriteSheetDataHandler sheetHandler,
            Sprite defaultSprite
        ) {
            this.sheetHandler = sheetHandler;
            this.defaultSprite = defaultSprite;
            this.aniPlayer = null;
        }
    }
}
