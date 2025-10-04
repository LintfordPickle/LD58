package net.lintfordlib.ld58.screens.game;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.controllers.ControllerManager;
import net.lintfordlib.core.LintfordCore;
import net.lintfordlib.core.graphics.sprites.spritesheet.SpriteSheetDefinition;
import net.lintfordlib.core.graphics.textures.FullScreenBuffer;
import net.lintfordlib.core.graphics.textures.FullScreenBuffer.StencilMode;
import net.lintfordlib.core.maths.InterpolationHelper;
import net.lintfordlib.core.maths.MathHelper;
import net.lintfordlib.core.maths.Vector3f;
import net.lintfordlib.data.DataManager;
import net.lintfordlib.data.scene.SceneHeader;
import net.lintfordlib.ld58.ConstantsGame;
import net.lintfordlib.ld58.LD58KeyActions;
import net.lintfordlib.ld58.controllers.GameStateController;
import net.lintfordlib.ld58.data.GameOptions;
import net.lintfordlib.ld58.data.GameTextureNames;
import net.lintfordlib.ld58.data.GameWorld;
import net.lintfordlib.ld58.data.IGameStateListener;
import net.lintfordlib.ld58.renderers.HudRenderer;
import net.lintfordlib.renderers.SimpleRendererManager;
import net.lintfordlib.screenmanager.ScreenManager;
import net.lintfordlib.screenmanager.screens.BaseGameScreen;

public class GameScreen extends BaseGameScreen implements IGameStateListener {

	// --------------------------------------
	// Inner-Classes
	// --------------------------------------

	// moving entities
	public class TrackEntity {

		public int spriteFrameUid;

		public float xOffset;
		public float zOffset;
		public float speed;
		public float percent;

		public TrackEntity(int spriteFrameUid, float xOffset, float zOffset, float speed) {
			this.spriteFrameUid = spriteFrameUid;
			this.xOffset = xOffset;
			this.zOffset = zOffset;
			this.speed = speed;
		}
	}

	// static entities
	public class TrackProp {

		public int spriteFrameUid;

		public float xOffset;
		public float zOffset;
		public float speed;
		public float percent;

		public TrackProp(int spriteFrameUid, float xOffset, float zOffset, float speed) {
			this.spriteFrameUid = spriteFrameUid;
			this.xOffset = xOffset;
			this.zOffset = zOffset;
			this.speed = speed;
		}

	}

	// tracks are defined in world space, and projected to screen space for rendering
	public class TrackPoint {

		public final Vector3f world = new Vector3f();
		public final Vector3f camera = new Vector3f();
		public final Vector3f screen = new Vector3f();
		public float screenScale = 1;
		public float curvature;

		public void projectWorldToCamera(float camX, float camY, float camZ, float pitch, float yaw) {
			camera.x = world.x - camX;
			camera.y = world.y - camY;
			camera.z = world.z - camZ;

			// Rotate around Y-axis (yaw)
			final var cosYaw = (float) Math.cos(yaw);
			float sinYaw = (float) Math.sin(yaw);

			// Rotate around X-axis (pitch)
			final var cosPitch = (float) Math.cos(pitch);
			final var sinPitch = (float) Math.sin(pitch);

			final var x = camera.x * cosYaw + camera.z * sinYaw;
			final var y = camera.y * cosPitch - camera.z * sinPitch;
			var z = -camera.x * sinYaw + camera.z * cosYaw;

			z = camera.y * sinPitch + camera.z * cosPitch;

			camera.x = x;
			camera.y = y;
			camera.z = z;
		}

		public void projectCameraToScreen(float camDepth, float width, float height, float roadWidth) {
			screenScale = camDepth / camera.z;

			screen.x = Math.round(width / 2 + (screenScale * camera.x * width / 2));
			screen.y = Math.round(height / 2 - (screenScale * camera.y * -height / 2));
			screen.z = Math.round((screenScale * roadWidth * width / 2));
		}
	}

	public class TrackSegment {

		private static int segmentIndexCounter;

		public static void resetIndexCounter() {
			segmentIndexCounter = 0;
		}

		public int index;
		public float curve;
		public boolean isLooped;
		public boolean isClipped;
		public float clipSpaceY;

		public final TrackPoint p0 = new TrackPoint(); // closest
		public final TrackPoint p1 = new TrackPoint(); // furthest

		public final List<TrackEntity> entities = new ArrayList<>();
		public final List<TrackProp> props = new ArrayList<>();

		public TrackSegment(float curve, float endHeight) {
			this.index = segmentIndexCounter++;
			this.curve = curve;

			p0.world.x = 0.f;
			p0.world.y = lastSegmentHeight();
			p0.world.z = index * mSegmentLength;

			p1.world.x = 0.f;
			p1.world.y = endHeight;
			p1.world.z = (index + 1) * mSegmentLength;

		}
	}

	// --------------------------------------
	// Variables
	// --------------------------------------

	private FullScreenBuffer mScreenBuffer;

	private SceneHeader mSceneHeader;
	private GameOptions mGameOptions;
	private GameWorld mGameWorld;
	private GameStateController mGameStateController;
	private HudRenderer mHudRenderer;

	private SpriteSheetDefinition mGameSpriteSheet;

	private List<TrackSegment> mTrackSegments = new ArrayList<>();
	private List<TrackEntity> mEntities = new ArrayList<>();

	// camera vars
	private float mFoV = 100;
	private float mCameraHeight = 150;
	private float mCameraDepth; // computed (cam dist from screen)
	private float mCameraPitch = 0;
	private float mCameraYaw = 0;

	// world vars
	public final int mSegmentLength = 10;
	public final int mRumbleLength = 2;
	private float mTrackLength; // computed
	private int mNumLanes = 4;
	private float mRoadWidth = 300;

	private int mDrawDistance = 50; // number of segments to draw
	private float mPlayerX; // player offset from center on X axis
	private float mPlayerZ; // (computed) player relative z distance from camera
	private float mPosition; // camera Z position (add mPlayerZ to get player's absolute Z position).
	private float mSpeed;
	private final float step = 1f / 60f;
	private float mMaxSpeed = mSegmentLength / step;

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public GameScreen(ScreenManager screenManager, SceneHeader sceneHeader, GameOptions options) {
		super(screenManager, new SimpleRendererManager(screenManager.core(), ConstantsGame.GAME_RESOURCE_GROUP_ID));

		mSceneHeader = sceneHeader;
		mGameOptions = options;

		mScreenBuffer = new FullScreenBuffer(ConstantsGame.GAME_CANVAS_WIDTH, ConstantsGame.GAME_CANVAS_HEIGHT);

		reset();
		setupWorld();
	}

	// --------------------------------------
	// Core-Methods
	// --------------------------------------

	@Override
	public void loadResources(ResourceManager resourceManager) {
		super.loadResources(resourceManager);

		mGameSpriteSheet = resourceManager.spriteSheetManager().getSpriteSheet("SPRITESHEET_GAME", ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mScreenBuffer.loadResources(resourceManager);
	}

	@Override
	public void unloadResources() {
		super.unloadResources();

		mGameSpriteSheet = null;

		mScreenBuffer.unloadResources();
	}

	@Override
	public void handleInput(LintfordCore core) {
		super.handleInput(core);

		if (core.input().keyboard().isKeyDownTimed(GLFW.GLFW_KEY_ESCAPE, this) || core.input().gamepads().isGamepadButtonDownTimed(GLFW.GLFW_GAMEPAD_BUTTON_START, this)) {

			if (ConstantsGame.START_GAME_IMMEDIATELY) {
				reset();
				setupWorld();
			} else {
				screenManager.addScreen(new PauseScreen(screenManager, mSceneHeader, mGameOptions));
			}

			return;
		}

		if (core.input().eventActionManager().getCurrentControlActionStateTimed(LD58KeyActions.KEY_BINDING_PRIMARY_FIRE)) {
			screenManager.toastManager().addMessage(getClass().getSimpleName(), "PRIMARY FIRE", 1500);
		}

		if (core.input().eventActionManager().getCurrentControlActionState(LD58KeyActions.KEY_BINDING_FORWARD)) {
			float speed = 60.f;
			if (core.input().keyboard().isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT))
				speed += 200;
			mPosition += speed * core.gameTime().elapsedTimeMilli() * 0.001f;
		}

		if (core.input().eventActionManager().getCurrentControlActionState(LD58KeyActions.KEY_BINDING_BACKWARD)) {
			float speed = 60.f;
			if (core.input().keyboard().isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT))
				speed += 200;
			mPosition -= speed * core.gameTime().elapsedTimeMilli() * 0.001f;
		}

	}

	@Override
	public void update(LintfordCore core, boolean otherScreenHasFocus, boolean coveredByOtherScreen) {
		super.update(core, otherScreenHasFocus, coveredByOtherScreen);

	}

	@Override
	public void draw(LintfordCore core) {

		final var pixels = mScreenBuffer.getPixels();

		clearBuffer(core, pixels);

		mDrawDistance = 200;
		drawTrack(core, pixels);
		drawPlayer(core, pixels);

		mScreenBuffer.draw(core);

		super.draw(core);
	}

	private void clearBuffer(LintfordCore core, int[] buffer) {
		mScreenBuffer.clear(0xff1f2f3f);
	}

	private void drawTrack(LintfordCore core, int[] buffer) {

		final var baseSegment = findSegment(mPosition);
		final var basePercent = (mPosition % mSegmentLength) / mSegmentLength;

		final var playerSegment = findSegment(mPosition + mPlayerZ);
		final var playerPercent = ((mPosition + mPlayerZ) % mSegmentLength) / mSegmentLength;

		final var playerY = InterpolationHelper.lerp(playerSegment.p0.world.y, playerSegment.p1.world.y, playerPercent);

		final var canvasWidth = ConstantsGame.GAME_CANVAS_WIDTH;
		final var canvasHeight = ConstantsGame.GAME_CANVAS_HEIGHT;

		mScreenBuffer.writeOnceLock = true;

		// segments drawn front to back
		float maxY = -ConstantsGame.GAME_CANVAS_HEIGHT / 2; // clip segments based on height
		final var numSegments = mTrackSegments.size();
		float x = 0;
		float dx = -(baseSegment.curve * basePercent);

		for (int i = 0; i < mDrawDistance; i++) {
			final var segment = mTrackSegments.get((baseSegment.index + i) % numSegments);

			segment.isLooped = segment.index < baseSegment.index;
			segment.clipSpaceY = maxY; // used to clip the sprites/cars in next pass

			segment.p0.curvature = x;
			segment.p1.curvature = x + dx;

			final var camX = mPlayerX;
			segment.p0.projectWorldToCamera((camX * mRoadWidth) - segment.p0.curvature, playerY + mCameraHeight, mPosition, mCameraPitch, mCameraYaw);
			segment.p1.projectWorldToCamera((camX * mRoadWidth) - segment.p1.curvature, playerY + mCameraHeight, mPosition, mCameraPitch, mCameraYaw);

			segment.p0.projectCameraToScreen(mCameraDepth, canvasWidth, canvasHeight, mRoadWidth / 2);
			segment.p1.projectCameraToScreen(mCameraDepth, canvasWidth, canvasHeight, mRoadWidth / 2);

			x = x + dx;
			dx = dx + segment.curve * 3.f;

			// check clipped (height based)
			final var isBehindUs = (segment.p0.camera.z <= mCameraDepth * mPlayerZ);
			final var isOccluded = false; // (segment.p1.screen.y < maxY); // rely on writeOnce lock

			if (isBehindUs || isOccluded)
				continue;
			var drawLanes = (segment.p1.screen.y > maxY);
			// render the track segment
			drawSegment(segment, (int) canvasWidth, mNumLanes, drawLanes);

			// Track the top of this segment
			maxY = segment.p1.screen.y;
		}

		mScreenBuffer.writeOnceLock = false;
	}

	// @formatter:off
	private void drawSegment(TrackSegment segment, int canvasWidth, int numLanes, boolean drawLanes) {

		final var p0 = segment.p0;
		final var p1 = segment.p1;

		final var r0 = p0.screen.z / 30.0f;
		final var r1 = p1.screen.z / 30.0f;

		// mScreenBuffer.drawRect(0, (int) p1.screen.y, canvasWidth, (int) (p1.screen.y - p0.screen.y) + 1, 0xff00ff00, true);

		final var wallHeight = 60;
		
		mScreenBuffer.mStencilEnabled = !drawLanes;
		mScreenBuffer.mStencilMode = StencilMode.LessEqual;
		mScreenBuffer.mStencilValue = 2;
		
		// road
		mScreenBuffer.drawPolygon(
				(int)(p0.screen.x - 1 - p0.screen.z), (int)p0.screen.y, 
				(int)(p0.screen.x + p0.screen.z), (int)p0.screen.y, 
				(int)(p1.screen.x + p1.screen.z), (int)p1.screen.y, 
				(int)(p1.screen.x - 1 - p1.screen.z), (int)p1.screen.y, 
				0xffffbfcf, true);
		
		mScreenBuffer.mStencilEnabled = true;
		mScreenBuffer.mStencilMode = StencilMode.LessEqual;
		mScreenBuffer.mStencilValue = 2;
		
		// rumble left
		mScreenBuffer.drawPolygon(
				(int)(p0.screen.x - p0.screen.z-r0), 	(int)p0.screen.y, 
				(int)(p0.screen.x - p0.screen.z), 		(int)(p0.screen.y+wallHeight * p0.screenScale * 240), 
				(int)(p1.screen.x - p1.screen.z), 		(int)(p1.screen.y+wallHeight * p1.screenScale * 240), 
				(int)(p1.screen.x - p1.screen.z-r1), 	(int)p1.screen.y, 
				0xffcc0419, true);

		// rumble right
		mScreenBuffer.drawPolygon(
				(int)(p0.screen.x + p0.screen.z+r0), (int)p0.screen.y, 
				(int)(p0.screen.x + p0.screen.z),    (int)(p0.screen.y+wallHeight * p0.screenScale * 240), 
				(int)(p1.screen.x + p1.screen.z),    (int)(p1.screen.y+wallHeight * p1.screenScale * 240), 
				(int)(p1.screen.x + p1.screen.z+r1), (int)p1.screen.y, 
				0x55cc0419, true);
		
		
		
		// Lanes
		if (drawLanes) {
			mScreenBuffer.mStencilEnabled = true;
			mScreenBuffer.writeOnceLock = false; // 
//			
			mScreenBuffer.mStencilMode = StencilMode.LessEqual; // only render walls where tracks have not already been rendered
			mScreenBuffer.mStencilValue = 1;

			final var lineZ0 = p0.screenScale * canvasWidth / 2;
			final var lineZ1 = p1.screenScale * canvasWidth / 2;

			final var laneWidth = 2;

			if (mNumLanes <= 1)
				return;

			var lanes1 = mNumLanes - 1;
			for (int i = 0; i < lanes1; i++) {

				final var lineStepX = (i + 1) * mRoadWidth / (lanes1 + 1);
				final var lx0 = p0.screen.x - p0.screen.z + lineStepX * p0.screenScale * canvasWidth / 2;
				final var lx1 = p1.screen.x - p1.screen.z + lineStepX * p1.screenScale * canvasWidth / 2;

				// if (segment.rumbleColor == 0) {
				mScreenBuffer.drawPolygon(
						(int) (lx0 - laneWidth * lineZ0), (int) p0.screen.y, 
						(int) (lx0 + laneWidth * lineZ0), (int) p0.screen.y, 
						(int) (lx1 + laneWidth * lineZ1), (int) p1.screen.y, 
						(int) (lx1 - laneWidth * lineZ1), (int) p1.screen.y, 
						0xff0f2f0f, true);
				// }
			}
			mScreenBuffer.mStencilEnabled = true;
			mScreenBuffer.writeOnceLock = true;
		}
		
		mScreenBuffer.mStencilEnabled = false;
		
		
		// @formatter:on

	}

	private void drawObstacles() {

	}

	private void drawPlayer(LintfordCore core, int[] buffer) {

		final var playerSegment = findSegment(mPosition + mPlayerZ + 20);
		final var playerPercent = ((mPosition + mPlayerZ) % mSegmentLength) / mSegmentLength;
		final var scale = InterpolationHelper.lerp(playerSegment.p0.screenScale, playerSegment.p1.screenScale, playerPercent);

		final var segmentCurvature = InterpolationHelper.lerp(playerSegment.p0.curvature, playerSegment.p1.curvature, playerPercent);
		final var segmentHeight = InterpolationHelper.lerp(playerSegment.p0.screen.y, playerSegment.p1.screen.y, playerPercent);

		final var playerFrame = mGameSpriteSheet.getSpriteFrame(GameTextureNames.PLAYER_MID);

		// ---

		final var playerWorldY = -segmentHeight * scale;
		System.out.println(playerWorldY);

		// ---

		mPlayerX = getLaneOffsetX(2);
		final var playerW = (int) (playerFrame.width() * scale * ConstantsGame.GAME_CANVAS_WIDTH / 2);
		final var playerH = (int) (playerFrame.height() * scale * ConstantsGame.GAME_CANVAS_HEIGHT / 2);
		final var playerX = (int) (mPlayerX + segmentCurvature) + (ConstantsGame.GAME_CANVAS_WIDTH / 2 - playerW / 2);
		final var playerY = (int) segmentHeight;

		// var destX = segment.p0.screen.x + (spriteObstacle.xOffset * scale * mRoadWidth * SCREEN_WIDTH / 2) - destW / 2;

		final var texture = mGameSpriteSheet.texture();

		final var srcX = (int) playerFrame.x();
		final var srcY = (int) (texture.getTextureHeight() - playerFrame.height());

		mScreenBuffer.copyPixelsAtlas(texture.ARGBColorData(), // Src pixels
				srcX, srcY, (int) playerFrame.width(), (int) playerFrame.height(), texture.getTextureWidth(), // src rect
				playerX, playerY, playerW, playerH, // dest rect
				0xffffffff, false);

	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	// DATA ----------------------------------------

	@Override
	protected void createData(DataManager dataManager) {
		mGameWorld = new GameWorld();
	}

	private void reset() {
		mPosition = 0; // back to start
		mPlayerX = 0.f; // center
		mSpeed = 0.f;
	}

	private void setupWorld() {

		// cam
		mFoV = 140;
		mCameraDepth = 1f / (float) Math.tan(mFoV / 2 * Math.PI / 180);
		mPlayerZ = mCameraHeight * mCameraDepth;

		// track
		TrackSegment.resetIndexCounter();
		mTrackSegments.clear();

		final var testHillHeight = 120;

		addRoad(0, 4, 0, 0, 0);
		addRoad(10, 50, 10, 0, testHillHeight * 1.4f);
		addRoad(0, 30, 0, .5f, -testHillHeight / 4);
		addRoad(10, 30, 10, -.6f, testHillHeight);
		addRoad(10, 30, 10, .8f, -testHillHeight);
		addRoad(10, 30, 10, -.3f, testHillHeight);
		addRoad(30, 100, 0, -0.1f, -testHillHeight);
		addRoad(30, 75, 0, .3f, -testHillHeight / 2);
		addRoad(60, 50, 20, .3f, 0);
		addRoad(0, 75, 0, .4f, testHillHeight * 2);
		addRoad(0, 75, 0, 0, testHillHeight);

		addProp(50, GameTextureNames.COIN_00, 1);

	}

	private void addRoad(int enter, int hold, int leave, float curve, float height) {

		var startY = lastSegmentHeight();
		var endY = startY + (int) (height * mSegmentLength);
		var total = enter + hold + leave;

		for (int i = 0; i < enter; i++) {

			final var curveAmt = InterpolationHelper.easeIn(0, curve, (float) i / (float) enter);
			final var hillAmt = InterpolationHelper.easeInOut(startY, endY, (float) i / (float) total);

			mTrackSegments.add(new TrackSegment(curveAmt, hillAmt));

		}

		for (int i = 0; i < hold; i++) {

			final var hillAmt = InterpolationHelper.easeInOut(startY, endY, (float) (enter + i) / (float) total);

			mTrackSegments.add(new TrackSegment(curve, hillAmt));

		}

		for (int i = 0; i < leave; i++) {

			final var curveAmt = InterpolationHelper.easeOut(0, curve, (float) i / (float) leave);
			final var hillAmt = InterpolationHelper.easeInOut(startY, endY, (float) (enter + hold + i) / (float) total);

			mTrackSegments.add(new TrackSegment(curveAmt, hillAmt));

		}
	}

	private void addProp(int segmentUid, int spriteFrameUid, int laneNum) {
		final var newProp = new TrackProp(spriteFrameUid, getLaneOffsetX(laneNum), spriteFrameUid, laneNum);

		final var segment = findSegment(segmentUid);
		segment.props.add(newProp);
	}

	private void addEntity(int segmentUid, int spriteFrameUid, int laneNum) {
		final var newEntity = new TrackEntity(spriteFrameUid, getLaneOffsetX(laneNum), spriteFrameUid, laneNum);

		final var segment = findSegment(segmentUid);
		segment.entities.add(newEntity);
	}

	private TrackSegment findSegment(float z) {
		if (z < 0)
			return mTrackSegments.get(0);

		final var index = (int) Math.floor((z / mSegmentLength)) % mTrackSegments.size();
		return mTrackSegments.get(index);
	}

	private float lastSegmentHeight() {
		if (mTrackSegments.size() == 0)
			return 0;

		return mTrackSegments.get(mTrackSegments.size() - 1).p1.world.y;
	}

	private float getLaneOffsetX(int lane) {
		lane = MathHelper.clampi(lane, 0, mNumLanes);

		final var laneI = 1.f / (mNumLanes * 2);
		final var laneS = 1.f / (mNumLanes);

		return -0.5f + laneI + laneS * lane;
	}

	// CONTROLLERS ---------------------------------

	@Override
	protected void createControllers(ControllerManager controllerManager) {
		mGameStateController = new GameStateController(controllerManager, ConstantsGame.GAME_RESOURCE_GROUP_ID);

		mGameStateController.setGameStateListener(this);
	}

	@Override
	protected void initializeControllers(LintfordCore core) {
		mGameStateController.initialize(core);
	}

	// RENDERERS -----------------------------------

	@Override
	protected void createRenderers(LintfordCore core) {
		mHudRenderer = new HudRenderer(mRendererManager, ConstantsGame.GAME_RESOURCE_GROUP_ID);
	}

	@Override
	protected void createRendererStructure(LintfordCore core) {
		mRendererManager.addRenderer(mHudRenderer);

	}

	// --------------------------------------
	// Callback-Methods
	// --------------------------------------

	@Override
	public void onGameWon() {
		screenManager.addScreen(new WonScreen(screenManager, mSceneHeader, mGameOptions));
	}

	@Override
	public void onGameLost() {
		screenManager.addScreen(new LostScreen(screenManager, mSceneHeader, mGameOptions));
	}
}
