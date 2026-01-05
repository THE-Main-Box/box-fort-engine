package official.sketchBook.game.gameObject_related;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.animation_related.ObjectAnimationPlayer;
import official.sketchBook.engine.animation_related.Sprite;
import official.sketchBook.engine.animation_related.SpriteSheetDataHandler;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.StaticResourceDisposable;
import official.sketchBook.engine.components_related.objects.MovementComponent;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;
import official.sketchBook.engine.gameObject_related.AnimatedRenderableGameObject;
import official.sketchBook.game.components_related.PlayerControllerComponent;
import official.sketchBook.game.util_related.path.GameAssetsPaths;

import java.util.ArrayList;
import java.util.Arrays;

public class Player extends AnimatedRenderableGameObject implements StaticResourceDisposable, MovableObjectII, PhysicalObjectII {

    public static Texture playerSheet;

    private PlayerControllerComponent controllerC;
    private MovementComponent moveC;

    private Body body;

    private short mask, category;

    public Player(
        float x,
        float y,
        float z,
        float width,
        float height,
        boolean xAxisInverted,
        boolean yAxisInverted,
        BaseWorldDataManager worldDataManager
    ) {
        super(
            x,
            y,
            z,
            width,
            height,
            xAxisInverted,
            yAxisInverted,
            worldDataManager
        );
    }

    @Override
    protected void initObject() {
        this.animationPlayerList = new ArrayList<>();
        this.spriteHandlerList = new ArrayList<>();

        initSpriteSheet();
        initAnimations();
        initController();
        initMovementComponent();

        initBody();
    }

    protected void initBody(){
//        this.body;
    }

    private void initController(){
        this.controllerC = new PlayerControllerComponent(this);
        this.toUpdateComponentList.add(controllerC);
    }

    private void initMovementComponent(){
        this.moveC = new MovementComponent(
            this,
            350,
            900,
            800f,
            1,
            true,
            true,
            true,
            true,
            true,
            4
        );
        this.toUpdateComponentList.add(moveC);
//        this.toPostUpdateComponentList.add(moveC);
    }

    private void initAnimations() {
        this.animationPlayerList.add(
            new ObjectAnimationPlayer()
        );

        this.animationPlayerList.get(0).addAnimation("player_idle", Arrays.asList(
            new Sprite(0, 0, 0.15f),
            new Sprite(1, 0, 0.15f),
            new Sprite(2, 0, 0.15f),
            new Sprite(3, 0, 0.15f)
        ));

        this.animationPlayerList.get(0).playAnimation("player_idle");
    }

    private void initSpriteSheet() {
        if (playerSheet == null) {
            playerSheet = new Texture(GameAssetsPaths.EntitiesAssetsPaths.PLAYER_SHEET_PATH);
        }

        this.spriteHandlerList.add(
            new SpriteSheetDataHandler(
                transformC.getX(),
                transformC.getY(),
                8,
                0,
                5,
                4,
                transformC.isxAxisInverted(),
                transformC.isyAxisInverted(),
                playerSheet
            )
        );
    }

    @Override
    public void update(float delta) {
        super.update(delta);
    }

    @Override
    public void postUpdate() {
        super.postUpdate();
    }

    @Override
    protected void onObjectDestruction() {
        System.out.println("Player iniciando destruição");
    }

    @Override
    protected void disposeData() {
        System.out.println("Player limpando dados de instancia");
    }

    public static void disposeStaticResources() {
        System.out.println("Player limpando dados estaticos");
        playerSheet.dispose();
    }

    public PlayerControllerComponent getControllerC() {
        return controllerC;
    }

    public MovementComponent getMoveC() {
        return moveC;
    }

    @Override
    public Body getBody() {
        return body;
    }
}
