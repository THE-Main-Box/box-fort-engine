package official.sketchBook.engine.animation_rendering_related;

import java.util.*;

/**
 * Gerencia a reprodução de animações com suporte a eventos por frame.
 * Permite adicionar múltiplas animações, controlar velocidade, looping e disparar callbacks em frames específicos.
 */
public class ObjectAnimationPlayer {
    /// Animação atualmente em reprodução
    private List<Sprite> currentAnimation;

    /// Sprite atual baseado no índice da animação (aniTick)
    private Sprite currentSprite;

    /// Chave/nome da animação atualmente em reprodução
    private String currentAnimationKey;

    /// Mapa contendo todas as animações registradas (chave -> lista de sprites)
    private final Map<String, List<Sprite>> animations;

    /// Índice do frame atual na animação
    private int aniTick;

    /// Duração do frame atual em segundos
    private float frameDuration;

    /// Tempo acumulado desde a última transição de frame
    private float elapsedTime;

    /// Multiplicador de velocidade da animação (1.0 = velocidade normal)
    private double animationSpeed;

    /// Define se a animação deve atualizar automaticamente a cada frame
    private boolean autoUpdateAni;

    /// Define se a animação deve repetir ao atingir o último frame
    private boolean animationLooping = true;

    /// Mapa contendo eventos registrados para cada animação (animationKey -> lista de eventos)
    private final Map<String, List<AnimationFrameEvent>> frameEvents;

    /// Rastreia quais eventos já foram disparados nesta sessão para evitar repetições indevidas
    private final BitSet triggeredFrames;

    @FunctionalInterface
    public interface AnimationEventCallback {
        void onEvent(String animationKey, int frameIndex);
    }

    public static class AnimationFrameEvent {
        public final int frameIndex;
        public final AnimationEventCallback callback;

        public AnimationFrameEvent(int frameIndex, AnimationEventCallback callback) {
            this.frameIndex = frameIndex;
            this.callback = callback;
        }
    }

    public ObjectAnimationPlayer() {
        this.animations = new HashMap<>();
        this.frameEvents = new HashMap<>();
        this.triggeredFrames = new BitSet();
        this.autoUpdateAni = true;
        this.animationSpeed = 1.0;
    }

    public void addAnimation(String animationTitle, List<Sprite> animation) {
        if (animationTitle == null || animation == null || animation.isEmpty()) {
            throw new IllegalArgumentException("Animation title e lista não podem ser null ou vazios");
        }
        animations.put(animationTitle, animation);
        frameEvents.putIfAbsent(animationTitle, new ArrayList<>());
    }

    /**
     * Adiciona um evento que dispara quando a animação atinge um frame específico
     *
     * @param animationKey Nome da animação
     * @param frameIndex   Índice do frame (0-based)
     * @param callback     Função a ser executada
     */
    public void addFrameEvent(String animationKey, int frameIndex, AnimationEventCallback callback) {
        if (animationKey == null || callback == null) {
            throw new IllegalArgumentException("Animation key e callback não podem ser null");
        }

        if (!animations.containsKey(animationKey)) {
            throw new IllegalArgumentException("Animação '" + animationKey + "' não existe");
        }

        List<Sprite> animation = animations.get(animationKey);
        if (frameIndex < 0 || frameIndex >= animation.size()) {
            throw new IllegalArgumentException("Frame index " + frameIndex + " fora do intervalo [0, " + (animation.size() - 1) + "]");
        }

        frameEvents.get(animationKey).add(new AnimationFrameEvent(frameIndex, callback));
    }

    /**
     * Remove um evento específico
     */
    public void removeFrameEvent(String animationKey, int frameIndex) {
        if (frameEvents.containsKey(animationKey)) {
            frameEvents.get(animationKey).removeIf(e -> e.frameIndex == frameIndex);
        }
    }

    /**
     * Remove todos os eventos de uma animação
     */
    public void clearAnimationEvents(String animationKey) {
        if (frameEvents.containsKey(animationKey)) {
            frameEvents.get(animationKey).clear();
        }
    }

    /**
     * Verifica e dispara eventos para o frame atual
     */
    private void checkAndTriggerFrameEvents() {
        if (currentAnimationKey == null || !frameEvents.containsKey(currentAnimationKey)) {
            return;
        }

        List<AnimationFrameEvent> events = frameEvents.get(currentAnimationKey);

        for (AnimationFrameEvent event : events) {
            if (event.frameIndex == aniTick && !triggeredFrames.get(aniTick)) {
                event.callback.onEvent(currentAnimationKey, aniTick);
                triggeredFrames.set(aniTick);
            }
        }
    }


    /**
     * Limpa eventos disparados quando a animação muda ou faz loop
     */
    private void clearTriggeredEvents() {
        triggeredFrames.clear();
    }

    /// Atualiza a animação atual baseado no tempo decorrido
    /// Gerencia transição de frames, looping e disparo de eventos
    public void update(float deltaTime) {
        if (currentAnimation == null || currentAnimation.isEmpty() || !autoUpdateAni) return;

        elapsedTime += deltaTime;
        currentSprite = getCurrentSprite();

        if (currentSprite.getDuration() > 0) {
            frameDuration = currentSprite.getDuration();
        }

        if (elapsedTime >= (float) (frameDuration / animationSpeed)) {

            aniTick++;
            elapsedTime -= (float) (frameDuration / animationSpeed);

            if (aniTick >= currentAnimation.size()) {
                if (animationLooping) {
                    aniTick = 0;
                    clearTriggeredEvents();
                } else {
                    aniTick = currentAnimation.size() - 1;
                    autoUpdateAni = false;
                }
            }

            // Verifica eventos apenas se não chegou ao fim
            checkAndTriggerFrameEvents();

        }
    }

    /// Calcula o tempo total de uma animação somando a duração de todos os seus frames
    public float getTotalAnimationTime(List<Sprite> animation) {
        float value = 0;
        for (Sprite sprite : animation) {
            value += sprite.getDuration() > 0 ? sprite.getDuration() : 0;
        }
        return value;
    }

    /// Ajusta a velocidade da animação para que ela termine em um tempo específico em segundos
    public void setAnimationSpeedToTargetDuration(float targetDuration) {
        if (currentAnimation == null || currentAnimation.isEmpty() || targetDuration == 0) return;

        float totalAnimationTime = getTotalAnimationTime(currentAnimation);

        if (totalAnimationTime == 0) return;

        animationSpeed = totalAnimationTime / targetDuration;
    }

    /// Reseta a velocidade da animação para o padrão (1.0)
    public void resetAnimationSpeed() {
        animationSpeed = 1;
    }

    /// Define a animação atual sem usar a chave (para animações dinâmicas)
    public void setCurrentAnimation(List<Sprite> animation) {
        if (animation != null && !animation.isEmpty()) {
            this.currentAnimation = animation;
            this.aniTick = 0;
            this.elapsedTime = 0;
            this.currentAnimationKey = null;
            clearTriggeredEvents();
        }
    }

    /// Verifica se uma animação específica está sendo reproduzida
    public boolean isPlaying(String animationTitle) {
        return currentAnimation != null && currentAnimationKey != null && currentAnimationKey.equals(animationTitle);
    }

    /// Começa a reproduzir uma animação registrada pelo seu nome/chave
    public void playAnimation(String title) {
        if (title == null) {
            throw new IllegalArgumentException("Animation title não pode ser null");
        }

        List<Sprite> newAnimation = animations.get(title);

        if (newAnimation == null) {
            throw new IllegalArgumentException("Animação '" + title + "' não encontrada");
        }

        if (!newAnimation.isEmpty() && currentAnimation != newAnimation) {
            currentAnimation = newAnimation;
            currentAnimationKey = title;
            animationLooping = true;
            aniTick = 0;
            elapsedTime = 0;
            clearTriggeredEvents();
            checkAndTriggerFrameEvents();
        }
    }

    /// Verifica se a animação atual terminou (chegou ao último frame e não está em loop)
    public boolean isAnimationFinished() {
        if (currentAnimation == null || currentAnimation.isEmpty()) {
            return false;
        }
        return !animationLooping
            && aniTick == currentAnimation.size() - 1
            && !autoUpdateAni;
    }

    /// Define se a animação atual deve repetir ao fim
    public void setAnimationLooping(boolean looping) {
        if (this.animationLooping != looping) {
            this.animationLooping = looping;
        }
    }

    /// Retorna se a animação atual está em modo looping
    public boolean isAnimationLooping() {
        return animationLooping;
    }

    /// Retorna o sprite do frame atual
    public Sprite getCurrentSprite() {
        if (currentAnimation == null || currentAnimation.isEmpty()) {
            throw new IllegalStateException("Nenhuma animacao esta sendo tocada");
        }
        return currentAnimation.get(aniTick);
    }

    /// Retorna a lista de sprites da animação atual
    public List<Sprite> getCurrentAnimation() {
        return currentAnimation;
    }

    /// Verifica se a animação está sendo atualizada automaticamente
    public boolean isAutoUpdateAni() {
        return autoUpdateAni;
    }

    /// Define se a animação deve atualizar automaticamente a cada frame
    public void setAutoUpdateAni(boolean autoUpdateAni) {
        if (this.autoUpdateAni != autoUpdateAni) {
            this.autoUpdateAni = autoUpdateAni;
        }
    }

    /// Retorna o nome/chave da animação atualmente em reprodução
    public String getCurrentAnimationKey() {
        return currentAnimationKey;
    }

    /// Define o frame atual (aniTick) e dispara eventos se existirem para esse frame
    public void setAniTick(int aniTick) {
        if (currentAnimation == null || currentAnimation.isEmpty()) {
            throw new IllegalStateException("Nenhuma animação está sendo tocada");
        }
        if (aniTick >= 0 && aniTick < currentAnimation.size()) {
            this.aniTick = aniTick;
            clearTriggeredEvents();
            checkAndTriggerFrameEvents();
        }
    }

    /// Define manualmente o sprite atual (não recomendado, use setAniTick)
    public void setCurrentSprite(Sprite currentSprite) {
        this.currentSprite = currentSprite;
    }

    /// Retorna a velocidade atual da animação
    public double getAnimationSpeed() {
        return animationSpeed;
    }

    /// Define a velocidade de reprodução da animação
    public void setAnimationSpeed(double animationSpeed) {
        if (animationSpeed <= 0) {
            throw new IllegalArgumentException("Animation speed deve ser maior que 0");
        }
        this.animationSpeed = animationSpeed;
    }

    /// Retorna a lista de sprites de uma animação pela sua chave
    public List<Sprite> getAnimationByKey(String key) {
        return animations.get(key);
    }

    public int getAniTick() {
        return aniTick;
    }
}
