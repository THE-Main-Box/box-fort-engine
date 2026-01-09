package official.sketchBook.game.gameObject_related;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import official.sketchBook.engine.animation_related.ObjectAnimationPlayer;
import official.sketchBook.engine.animation_related.Sprite;
import official.sketchBook.engine.animation_related.SpriteSheetDataHandler;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.MovableObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.util_related.StaticResourceDisposable;
import official.sketchBook.engine.components_related.objects.MovableObjectPhysicsComponent;
import official.sketchBook.engine.components_related.objects.MovementComponent;
import official.sketchBook.engine.components_related.objects.PhysicsComponent;
import official.sketchBook.engine.dataManager_related.BaseWorldDataManager;
import official.sketchBook.engine.gameObject_related.AnimatedRenderableGameObject;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.game.components_related.PlayerControllerComponent;
import official.sketchBook.game.util_related.path.GameAssetsPaths;

import java.util.ArrayList;
import java.util.Arrays;

import static official.sketchBook.engine.util_related.enumerators.CollisionLayers.*;

public class Player
    extends
    AnimatedRenderableGameObject
    implements
    StaticResourceDisposable,
    MovableObjectII,
    PhysicalObjectII {

    public static Texture playerSheet;

    private PlayerControllerComponent controllerC;
    private MovementComponent moveC;
    private MovableObjectPhysicsComponent physicsC;

    private Body body;
    private short maskBit, categoryBit;
    private float density, frict, rest;

    public Player(
        float x,
        float y,
        float z,
        float rotation,
        float width,
        float height,
        float scaleX,
        float scaleY,
        boolean xAxisInverted,
        boolean yAxisInverted,
        BaseWorldDataManager worldDataManager
    ) {
        super(
            x,
            y,
            z,
            rotation,
            width,
            height,
            scaleX,
            scaleY,
            xAxisInverted,
            yAxisInverted,
            worldDataManager
        );

        this.isRenderDimensionEqualsToObject = false;
    }

    @Override
    protected void initObject() {
        this.animationPlayerList = new ArrayList<>();
        this.spriteHandlerList = new ArrayList<>();

        initBodyData();
        createBody();
        initPhysicsComponent();

        initSpriteSheet();
        initAnimations();
        initController();
        initMovementComponent();
    }

    protected void initBodyData() {
        this.categoryBit = ALLY_ENTITY.bit();
        this.maskBit = (short) (SENSOR.bit() | ENVIRONMENT.bit());

        this.density = 0.5f;
        this.frict = 1f;
        this.rest = 0;
    }

    public void createBody() {
        this.body = BodyCreatorHelper.createBox(
            this.worldDataManager.getPhysicsWorld(),
            new Vector2(
                this.transformC.x,
                this.transformC.y
            ),
            this.transformC.rotation,
            this.transformC.width,
            this.transformC.height,
            BodyDef.BodyType.DynamicBody,
            density,
            frict,
            rest,
            categoryBit,
            maskBit
        );

        this.body.setFixedRotation(true);
        this.body.resetMassData();
        this.body.setLinearDamping(0);

        this.body.setUserData(
            new GameObjectTag(
                ObjectType.ENTITY,
                this
            )
        );

    }

    @Override
    public void onObjectAndBodyPosSync() {

    }

    private void initController() {
        this.controllerC = new PlayerControllerComponent(this);
        this.toUpdateComponentList.add(controllerC);
    }

    private void initMovementComponent() {
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
            true,
            false
        );
        this.toUpdateComponentList.add(moveC);
    }

    private void initPhysicsComponent() {
        this.physicsC = new MovableObjectPhysicsComponent(this);

        this.toUpdateComponentList.add(physicsC);
        this.toPostUpdateComponentList.add(physicsC);
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
                transformC.x,
                transformC.y,
                8,
                0,
                5,
                4,
                transformC.getScaleX(),
                transformC.getScaleY(),
                transformC.mirrorX,
                transformC.mirrorY,
                true,
                true,
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

    @Override
    public short getMaskBit() {
        return maskBit;
    }

    @Override
    public short getCategoryBit() {
        return categoryBit;
    }

    @Override
    public float getDensity() {
        return density;
    }

    @Override
    public float getFrict() {
        return frict;
    }

    @Override
    public float getRest() {
        return rest;
    }

    @Override
    public PhysicsComponent getPhysicsC() {
        return physicsC;
    }
}
