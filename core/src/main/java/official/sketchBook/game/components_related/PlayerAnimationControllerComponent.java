package official.sketchBook.game.components_related;

import official.sketchBook.engine.animation_rendering_related.ObjectAnimationPlayer;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.Component;
import official.sketchBook.game.gameObject_related.Player;

import static official.sketchBook.game.util_related.values.AnimationKeys.Entities.*;

public class PlayerAnimationControllerComponent implements Component {

    private Player player;
    private ObjectAnimationPlayer currentAniPlayer;

    private boolean disposed = false;

    public PlayerAnimationControllerComponent(Player player) {
        this.player = player;
    }


    @Override
    public void update(float delta) {
        currentAniPlayer = player.getAnimationRenderC().getLayers().get(0).aniPlayer;

        if (currentAniPlayer == null) return;

        // Ordem de prioridade: Pulo > Queda > Corrida > Idle
        if (handleJumpAnimation(currentAniPlayer)) return;
        if (handleAirborneAnimation(currentAniPlayer)) return;
        if (handleRunAnimation(currentAniPlayer)) return;
        handleIdleAnimation(currentAniPlayer);

    }

    private boolean handleJumpAnimation(ObjectAnimationPlayer ani) {
        float vy = player.getBody().getLinearVelocity().y;

        // Só entramos aqui enquanto estivermos no ar
        if (!player.isOnGround()) {
            // 1) Subida plena: vy acima do threshold de stall
            if (vy > 0.15f) {
                ani.playAnimation(jump);
                ani.setAnimationLooping(false);
                ani.setAutoUpdateAni(false);
                ani.setAniTick(0);

                // 2) Stall (pico do salto): vy próximo de zero, dentro do intervalo [-t, +t]
            } else if (Math.abs(vy) <= player.getJumpC().fallSpeedAfterJCancel) {
                ani.playAnimation(jump);
                ani.setAnimationLooping(false);
                ani.setAutoUpdateAni(false);
                ani.setAniTick(1);

                // 3) Queda: vy negativo além do stall threshold
            } else {
                ani.playAnimation(fall);
                ani.setAnimationLooping(false);
                ani.setAutoUpdateAni(false);
                ani.setAniTick(0);
            }

            return true;
        }

        // Quando tocar o chão, entramos no afterFall
        if (player.getJumpC().hasLanded() && !player.getMoveC().isMovingX()) {
            ani.playAnimation(afterFall);
            ani.setAnimationLooping(false);
            ani.setAutoUpdateAni(true);
            return true;
        }

        return false;
    }


    private boolean handleAirborneAnimation(ObjectAnimationPlayer ani) {
        // só entra aqui se ainda não está no chão
        if (player.isOnGround()) return false;

        // se já tá tocando afterFall e não terminou, mantém
        if (ani.getCurrentAnimationKey().equals(afterFall)
            && !ani.isAnimationFinished()) {
            return true;
        }

        // queda livre normal (loop)
        if (player.getBody().getLinearVelocity().y < 0) {
            ani.playAnimation(fall);
            ani.setAnimationLooping(true);
            ani.setAutoUpdateAni(true);
        }

        return true;
    }

    private boolean handleRunAnimation(ObjectAnimationPlayer animationPlayer) {
        if (!player.isOnGround() || !player.getMoveC().isMovingX() || isPlayingAfterFall(animationPlayer)) return false;

        animationPlayer.setAutoUpdateAni(true);
        animationPlayer.playAnimation(run);

        return true;
    }

    private void handleIdleAnimation(ObjectAnimationPlayer animationPlayer) {
        if (isPlayingAfterFall(animationPlayer)) return;

        animationPlayer.setAutoUpdateAni(true);
        animationPlayer.playAnimation(idle);
    }

    public boolean isPlayingAfterFall(ObjectAnimationPlayer ani) {
        return ani.getCurrentAnimationKey().equals(afterFall)
            && !ani.isAnimationFinished();
    }

    @Override
    public void postUpdate() {

    }

    @Override
    public void dispose() {
        if (disposed) return;
        nullifyReferences();
        disposed = true;
    }

    @Override
    public void nullifyReferences() {
        player = null;
        currentAniPlayer = null;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
