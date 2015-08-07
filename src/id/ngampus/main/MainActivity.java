package id.ngampus.main;

import java.text.DecimalFormat;

import org.andengine.engine.camera.Camera;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl;
import org.andengine.engine.camera.hud.controls.AnalogOnScreenControl.IAnalogOnScreenControlListener;
import org.andengine.engine.camera.hud.controls.BaseOnScreenControl;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.engine.handler.timer.ITimerCallback;
import org.andengine.engine.handler.timer.TimerHandler;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.PathModifier;
import org.andengine.entity.modifier.PathModifier.IPathModifierListener;
import org.andengine.entity.modifier.PathModifier.Path;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.scene.background.RepeatingSpriteBackground;
import org.andengine.entity.shape.IShape;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.LayoutGameActivity;
import org.andengine.util.debug.Debug;

import android.graphics.Color;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

public class MainActivity extends LayoutGameActivity implements
		IOnSceneTouchListener, IOnAreaTouchListener {

	// Scene properties
	public enum SceneType {
		SPLASH, MENU, OPTIONS, GAME,
	}

	public SceneType currentScene = SceneType.SPLASH;

	// Camera
	private static final int CAMERA_WIDTH = 800;
	private static final int CAMERA_HEIGHT = 480;
	private Camera mCamera;

	// Scenes
	// 1. Splash Scene
	private Scene mSplashScene;
	private BitmapTextureAtlas mSplashTextureAtlas;
	private ITextureRegion mSplashTextureRegion;
	private Sprite mSplash;

	private Sprite mOverlay;
	private BitmapTextureAtlas mOverlayTextureAtlas;
	private ITextureRegion mOverlayTextureRegion;

	// 2. Menu Scene
	private Scene mMenuScene;
	private TextureRegion mMainMenuBgTex, mPlayTex, mTitleTex, mCreditsTex,
			mQuitTex, mBackTex;
	private Sprite mMainMenuBg, mTitle, mPlay, mCredits, mQuit, mBack;

	// 4. Game Scene
	private Scene mGameScene;
	private AnimatedSprite mPlayer;
	private AnimatedSprite mQuestion, mDanger1, mDanger, mWoman;
	private Sprite mDeadTree, mCocoTree, mDanger2;

	// 5. Game Analog control
	private BitmapTextureAtlas mOnScreenControlTexture;
	private ITextureRegion mOnScreenControlBaseTextureRegion;
	private ITextureRegion mOnScreenControlKnobTextureRegion;

	private BuildableBitmapTextureAtlas mBitmapTextureAtlas;
	private BitmapTextureAtlas mOstacleTextureAtlas;

	private TiledTextureRegion mPlayerTextureRegion;
	private TiledTextureRegion mObstacleTextureRegion;
	private RepeatingSpriteBackground mGrassBackground;
	private TextureRegion mDeadTreeTextureRegion, mNiceTreeTextureRegion,
			mCocoTreeTextureRegion, mNarkobaTextureRegion;

	// 6. Game Elements
	private GameUpdateHandler mGameUpdateHandler;

	private boolean movingFwd = true;

	public float pLeftVolume = 1;

	public float pRightVolume = 1;

	// 7. Texts
	private Font mFont, mCreditsFont, mWinLoseFont;
	private Text mEatenText,mEngineUpdateText, mEatsText;
	private VertexBufferObjectManager mVertexBufferObjectManager;

	@Override
	public EngineOptions onCreateEngineOptions() {
		mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		EngineOptions engineOptions = new EngineOptions(true,
				ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(
						CAMERA_WIDTH, CAMERA_HEIGHT), mCamera);
		engineOptions.getAudioOptions().setNeedsMusic(true);
		engineOptions.getAudioOptions().setNeedsSound(true);
		return engineOptions;
	}

	@Override
	public void onCreateResources(
			OnCreateResourcesCallback pOnCreateResourcesCallback) {
		mVertexBufferObjectManager = this.getVertexBufferObjectManager();
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");
		loadSplashSceneResources();
		loadMainMenuResources();
		loadGameSceneResources();
		loadFont();
		pOnCreateResourcesCallback.onCreateResourcesFinished();
	}

	@Override
	public void onCreateScene(OnCreateSceneCallback pOnCreateSceneCallback)
			throws Exception {

		initSplashScreen();
		pOnCreateSceneCallback.onCreateSceneFinished(this.mSplashScene);
	}

	@Override
	public void onPopulateScene(Scene pScene,
			OnPopulateSceneCallback pOnPopulateSceneCallback) throws Exception {
		mEngine.registerUpdateHandler(new TimerHandler(4f,
				new ITimerCallback() {

					@Override
					public void onTimePassed(TimerHandler pTimerHandler) {
						mEngine.unregisterUpdateHandler(pTimerHandler);
						mSplash.detachSelf();
						setMenuScene();
					}
				}));
		pOnPopulateSceneCallback.onPopulateSceneFinished();

	}

	@Override
	protected int getLayoutID() {
		// TODO Auto-generated method stub
		return R.layout.activity_main;
	}

	@Override
	protected int getRenderSurfaceViewID() {
		// TODO Auto-generated method stub
		return R.id.xmllayoutRenderSurfaceView;
	}

	@Override
	public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
			ITouchArea pTouchArea, float pTouchAreaLocalX,
			float pTouchAreaLocalY) {
		if (pSceneTouchEvent.isActionDown() && pTouchArea.equals(mPlay)
				&& mMenuScene.getChildByTag(503) == mPlay) {
			mMenuScene.detachSelf();
			setGameScene();
		} else if (pSceneTouchEvent.isActionDown()
				&& pTouchArea.equals(mCredits)
				&& mMenuScene.getChildByTag(504) == mCredits) {
			mMenuScene.detachChild(mTitle);
			mMenuScene.detachChild(mPlay);
			mMenuScene.detachChild(mCredits);
			mMenuScene.detachChild(mQuit);

			mEatsText = new Text(50, 120, mCreditsFont,
					"\t\t\t\t\t\t\t\t Faisal Lutviyanto\n\n"
							+ "\t\t\t\t\t\t\t\t YosuaAgustinus\n\n"
							+ "\t\t\t\t\t\t\t\t Nicolas Gramlich\n\n",
					mVertexBufferObjectManager);
			mMenuScene.attachChild(mEatsText);
			mMenuScene.attachChild(mBack);

		} else if (pSceneTouchEvent.isActionDown() && pTouchArea.equals(mQuit)
				&& mMenuScene.getChildByTag(501) == mQuit) {
			finish();
			return true;
		} else if (pSceneTouchEvent.isActionDown()
				&& pTouchArea.equals(mBack)
				&& (mMenuScene.getChildByTag(502) == mBack || mGameScene
						.getChildByTag(502) == mBack)) {
			if (mEngine.getScene() != mMenuScene) {
				mSeconds = 0.0f;
				mGameScene.detachChildren();
				mGameScene.detachSelf();
				mEngine.setScene(mMenuScene);
			} else {
				mMenuScene.detachChild(mEatsText);
				mMenuScene.detachChild(mBack);
			}
			if (!mTitle.hasParent()) {
				mMenuScene.attachChild(mTitle);
			}
			if (!mPlay.hasParent()) {
				mMenuScene.attachChild(mPlay);
			}
			if (!mCredits.hasParent()) {
				mMenuScene.attachChild(mCredits);
			}
			if (!mQuit.hasParent()) {
				mMenuScene.attachChild(mQuit);
			}
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (currentScene) {
			case OPTIONS:
			case GAME:
				// mEngine.setScene(mMenuScene);
				// currentScene = SceneType.MENU;
				// break;
			case MENU:
			case SPLASH:
				onBackPressed();
			default:
				break;
			}
		}

		if (keyCode == KeyEvent.ACTION_UP) {
			if (mPlayer.isAnimationRunning()) {
				mPlayer.stopAnimation();
			}
		}
		return false;
	}

	static int i = 0;

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		Log.d("Ngampus ", "onSceneTouchEvent,pScene = " + pScene);
		if (pSceneTouchEvent.isActionDown()) {
			Sprite s = (Sprite) pScene.getChildByTag(mPlayer.getTag());
			s.setPosition(s.getX() + (i++), s.getY());
		}

		if (pScene == mMenuScene) {

		}

		return false;
	}

	

	public void setMenuScene() {
		this.mMenuScene = new Scene();
		mMainMenuBg = new Sprite(0, 0, this.mMainMenuBgTex,
				mVertexBufferObjectManager);
		mTitle = new Sprite(40, 60, this.mTitleTex, mVertexBufferObjectManager);
		mPlay = new Sprite(220, 180, this.mPlayTex, mVertexBufferObjectManager);
		mPlay.setTag(503);
		mCredits = new Sprite(220, 250, this.mCreditsTex,
				mVertexBufferObjectManager);
		mCredits.setTag(504);
		mQuit = new Sprite(CAMERA_WIDTH - 125, CAMERA_HEIGHT - 128,
				this.mQuitTex, mVertexBufferObjectManager);
		mQuit.setTag(501);
		mBack = new Sprite(CAMERA_WIDTH - 125, CAMERA_HEIGHT - 128,
				this.mBackTex, mVertexBufferObjectManager);
		mBack.setTag(502);
		mMenuScene.attachChild(mMainMenuBg);
		mMenuScene.attachChild(mTitle);
		mMenuScene.attachChild(mPlay);
		mMenuScene.attachChild(mCredits);
		mMenuScene.attachChild(mQuit);
		mMenuScene.registerTouchArea(mPlay);
		mMenuScene.registerTouchArea(mCredits);
		mMenuScene.registerTouchArea(mQuit);
		mMenuScene.registerTouchArea(mBack);
		mMenuScene.setTouchAreaBindingOnActionDownEnabled(true);
		mMenuScene.setOnAreaTouchListener(this);
		this.mEngine.setScene(mMenuScene);
	}

	private void loadMainMenuResources() {
		BitmapTextureAtlas btmpMenu = new BitmapTextureAtlas(
				getTextureManager(), 2800, 2480);
		mMainMenuBgTex = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(btmpMenu, this, "menu/menubg.png", 0, 0);

		mTitleTex = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				btmpMenu, this, "menu/title.png", 0,
				(int) mMainMenuBgTex.getHeight() + 1);

		mPlayTex = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				btmpMenu, this, "menu/play.png", 0,
				(int) mMainMenuBgTex.getHeight() + (int) mTitleTex.getHeight());

		mCreditsTex = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(
						btmpMenu,
						this,
						"menu/credits.png",
						0,
						((int) mMainMenuBgTex.getHeight()
								+ (int) mTitleTex.getHeight() + (int) mMainMenuBgTex
								.getHeight()));

		mQuitTex = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(
						btmpMenu,
						this,
						"menu/quit.png",
						0,
						((int) mMainMenuBgTex.getHeight()
								+ (int) mTitleTex.getHeight() + (int) mMainMenuBgTex
									.getHeight())
								+ (int) mMainMenuBgTex.getHeight());

		mBackTex = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				btmpMenu,
				this,
				"menu/back.png",
				0,
				((int) mMainMenuBgTex.getHeight() + (int) mTitleTex.getHeight()
						+ (int) mMainMenuBgTex.getHeight() + (int) mQuitTex
							.getHeight()) + (int) mMainMenuBgTex.getHeight());

		btmpMenu.load();
	}


	private void loadObstacleResources() {
		// resources
		this.mOstacleTextureAtlas = new BitmapTextureAtlas(
				this.getTextureManager(), 700, 700);

		this.mObstacleTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(this.mOstacleTextureAtlas, this,
						"obstacle.png", 0, 0, 12, 8);
		// NotMove
		BitmapTextureAtlas treeTextureAtlas = new BitmapTextureAtlas(
				this.getTextureManager(), 400, 400);
		this.mDeadTreeTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(treeTextureAtlas, this, "deadtree.png", 0, 0);
		this.mNiceTreeTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(treeTextureAtlas, this, "tree.png",
						(int) mDeadTreeTextureRegion.getWidth() + 1, 0);
		this.mCocoTreeTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(treeTextureAtlas, this, "cocotree.png",
						(int) mNiceTreeTextureRegion.getWidth() + 1, 0);
		this.mNarkobaTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(treeTextureAtlas, this, "narkoba.png", 0,
						(int) mDeadTreeTextureRegion.getHeight() + 50);
		treeTextureAtlas.load();
		this.mOstacleTextureAtlas.load();

	}
	private void loadGameSceneResources() {
		this.mBitmapTextureAtlas = new BuildableBitmapTextureAtlas(
				this.getTextureManager(), 512, 512, TextureOptions.NEAREST);
		this.mPlayerTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createTiledFromAsset(this.mBitmapTextureAtlas, this,
						"player1.png", 3, 4);

		try {
			this.mBitmapTextureAtlas
					.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(
							0, 0, 1));
			this.mBitmapTextureAtlas.load();
		} catch (TextureAtlasBuilderException e) {
			Debug.e(e);
		}

		this.mGrassBackground = new RepeatingSpriteBackground(CAMERA_WIDTH,
				CAMERA_HEIGHT, this.getTextureManager(),
				AssetBitmapTextureAtlasSource.create(this.getAssets(),
						"gfx/background_grass.png"), mVertexBufferObjectManager);
		this.mBitmapTextureAtlas.load();

		loadObstacleResources();

		// Analog control resources
		this.mOnScreenControlTexture = new BitmapTextureAtlas(
				this.getTextureManager(), 256, 128, TextureOptions.BILINEAR);
		this.mOnScreenControlBaseTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mOnScreenControlTexture, this,
						"onscreen_control_base.png", 0, 0);
		this.mOnScreenControlKnobTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(this.mOnScreenControlTexture, this,
						"onscreen_control_knob.png", 158, 0);
		this.mOnScreenControlTexture.load();

		this.mOverlayTextureAtlas = new BitmapTextureAtlas(
				this.getTextureManager(), CAMERA_WIDTH * 2, CAMERA_HEIGHT * 2);
		this.mOverlayTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(mOverlayTextureAtlas, this, "overlaybg4.png",
						0, 0);
		this.mOverlayTextureAtlas.load();

	}
	protected void setGameScene() {
		mGameScene = new Scene();
		mGameScene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));
		mPlayer = new AnimatedSprite(250, 220, mPlayerTextureRegion,
				mVertexBufferObjectManager);
		mPlayer.setTag(1);
		final long[] playerFrmTime = { 200, 200, 200, 200 };
		final int[] playerFwdFrms = { 8, 9, 10, 11 };
		final int[] playerBwdFrms = { 4, 5, 6, 7 };
		mPlayer.animate(playerFrmTime, playerFwdFrms);

		mGameScene.attachChild(mPlayer);
		final PhysicsHandler physicsHandler = new PhysicsHandler(mPlayer);
		mPlayer.registerUpdateHandler(physicsHandler);

		//Background
		mGameScene.setBackground(this.mGrassBackground);

		/*
		 * Calculate the coordinates for the face, so its centered on the
		 * camera.
		 */
		final float centerX = (CAMERA_WIDTH - this.mObstacleTextureRegion
				.getWidth()) / 2;
		final float centerY = (CAMERA_HEIGHT - this.mObstacleTextureRegion
				.getHeight()) / 2;

		/* Create the sprite and add it to the scene. */
		// trees
		mDeadTree = new Sprite(70, 70, this.mDeadTreeTextureRegion,
				mVertexBufferObjectManager);
		//mNiceTree = new Sprite(300, 40, this.mNiceTreeTextureRegion,
		//		mVertexBufferObjectManager);
		mCocoTree = new Sprite(420, 120, this.mCocoTreeTextureRegion,
				mVertexBufferObjectManager);
		mDanger2 = new Sprite(350, 330, this.mNarkobaTextureRegion,
				mVertexBufferObjectManager);
		mGameScene.attachChild(mDeadTree);
		mGameScene.attachChild(mDanger2);

		mQuestion = new AnimatedSprite(centerX, centerY, 24, 32,
				this.mObstacleTextureRegion, mVertexBufferObjectManager);
		mQuestion.setTag(100);

		mDanger1 = new AnimatedSprite(70, 160, 24, 70, this.mObstacleTextureRegion,
				mVertexBufferObjectManager);
		mDanger1.setTag(101);
		long[] danger1FrmTime = { 200, 200, 200 };
		int[] danger1Frm = { 66, 67, 68 };
		mDanger1.animate(danger1FrmTime, danger1Frm);

		mDanger = new AnimatedSprite(centerX + 160, centerY - 50, 24, 32,
				this.mObstacleTextureRegion, mVertexBufferObjectManager);
		mDanger.setTag(102);
		long[] dangerFrmTime = { 200, 200, 200 };
		int[] dangerFrm = { 72, 73, 74 };
		mDanger.animate(dangerFrmTime, dangerFrm);

		mWoman = new AnimatedSprite(CAMERA_WIDTH - 50, centerY, 30, 50,
				this.mObstacleTextureRegion, mVertexBufferObjectManager);
		mWoman.setTag(103);
		long[] womanFrmTime = { 200, 200, 200 };
		int[] womanFrm = { 57, 58, 59 };
		mWoman.animate(womanFrmTime, womanFrm);

		final Path path = new Path(10).to(10, 10).to(10, CAMERA_HEIGHT - 74)
				.to(CAMERA_WIDTH - 58, CAMERA_HEIGHT - 74)
				.to(CAMERA_WIDTH - 58, 10).to(CAMERA_WIDTH / 2, 10)
				.to(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2).to(10, 10)
				.to(CAMERA_WIDTH - 258, 10)
				.to(CAMERA_WIDTH - 258, CAMERA_HEIGHT / 2 + 50).to(10, 10);

		mQuestion.registerEntityModifier(new LoopEntityModifier(new PathModifier(
				100, path, null, new IPathModifierListener() {
					@Override
					public void onPathStarted(final PathModifier pPathModifier,
							final IEntity pEntity) {

					}

					@Override
					public void onPathWaypointStarted(
							final PathModifier pPathModifier,
							final IEntity pEntity, final int pWaypointIndex) {
						switch (pWaypointIndex) {
						case 0:
							mQuestion.animate(new long[] { 200, 200, 200 }, 0, 2,
									true);
							break;
						case 1:
							mQuestion.animate(new long[] { 200, 200, 200 }, 24, 26,
									true);
							break;
						case 2:
							mQuestion.animate(new long[] { 200, 200, 200 }, 36, 38,
									true);
							break;
						case 3:
							mQuestion.animate(new long[] { 200, 200, 200 }, 12, 14,
									true);
							break;
						case 4:
							mQuestion.animate(new long[] { 200, 200, 200 }, 0, 2,
									true);
							break;
						case 5:
							mQuestion.animate(new long[] { 200, 200, 200 }, 12, 14,
									true);
							break;
						case 6:
							mQuestion.animate(new long[] { 200, 200, 200 }, 24, 26,
									true);
							break;
						case 7:
							mQuestion.animate(new long[] { 200, 200, 200 }, 0, 2,
									true);
							break;
						case 8:
							mQuestion.animate(new long[] { 200, 200, 200 }, 12, 14,
									true);
							break;
						case 9:
							mQuestion.animate(new long[] { 200, 200, 200 }, 12, 14,
									true);
							break;
						}
					}

					@Override
					public void onPathWaypointFinished(
							final PathModifier pPathModifier,
							final IEntity pEntity, final int pWaypointIndex) {

					}

					@Override
					public void onPathFinished(
							final PathModifier pPathModifier,
							final IEntity pEntity) {

					}
				})));
		mGameScene.attachChild(mQuestion);
		////////////////////////////////////
		final Path path2 = new Path(5).to(70, 160).to(70, CAMERA_HEIGHT - 74)
				.to(CAMERA_WIDTH / 2 - 58, CAMERA_HEIGHT / 2 - 74)
				.to(CAMERA_WIDTH / 2 - 58, 160).to(70, 160);
		mDanger1.registerEntityModifier(new LoopEntityModifier(new PathModifier(
				60, path2, null, new IPathModifierListener() {
					@Override
					public void onPathStarted(final PathModifier pPathModifier,
							final IEntity pEntity) {

					}

					@Override
					public void onPathWaypointStarted(
							final PathModifier pPathModifier,
							final IEntity pEntity, final int pWaypointIndex) {
						switch (pWaypointIndex) {
						case 0:
							mDanger1.animate(new long[] { 200, 200, 200 }, 54, 56,
									true);
							break;
						case 1:
							mDanger1.animate(new long[] { 200, 200, 200 }, 78, 80,
									true);
							break;
						case 2:
							mDanger1.animate(new long[] { 200, 200, 200 }, 90, 92,
									true);
							break;
						case 3:
							mDanger1.animate(new long[] { 200, 200, 200 }, 66, 68,
									true);
							break;
						}
					}

					@Override
					public void onPathWaypointFinished(
							final PathModifier pPathModifier,
							final IEntity pEntity, final int pWaypointIndex) {

					}

					@Override
					public void onPathFinished(
							final PathModifier pPathModifier,
							final IEntity pEntity) {

					}
				})));
		mGameScene.attachChild(mDanger1);
		final Path path3 = new Path(5).to(centerX + 160, centerY - 50)
				.to(centerX + 360, 0)
				.to(CAMERA_WIDTH / 2 - 58, CAMERA_HEIGHT / 2 - 74)
				.to(CAMERA_WIDTH - 70 - 58, centerY - 50)
				.to(centerX + 160, centerY - 50);
		mDanger.registerEntityModifier(new LoopEntityModifier(new PathModifier(
				50, path3, null, new IPathModifierListener() {
					@Override
					public void onPathStarted(final PathModifier pPathModifier,
							final IEntity pEntity) {

					}

					@Override
					public void onPathWaypointStarted(
							final PathModifier pPathModifier,
							final IEntity pEntity, final int pWaypointIndex) {
						switch (pWaypointIndex) {
						case 0:
							mDanger.animate(new long[] { 200, 200, 200 }, 84, 86,
									true);
							break;
						case 1:
							mDanger.animate(new long[] { 200, 200, 200 }, 60, 62,
									true);
							break;
						case 2:
							mDanger.animate(new long[] { 200, 200, 200 }, 72, 74,
									true);
							break;
						case 3:
							mDanger.animate(new long[] { 200, 200, 200 }, 60, 62,
									true);
							break;
						}
					}

					@Override
					public void onPathWaypointFinished(
							final PathModifier pPathModifier,
							final IEntity pEntity, final int pWaypointIndex) {

					}

					@Override
					public void onPathFinished(
							final PathModifier pPathModifier,
							final IEntity pEntity) {

					}
				})));
		mGameScene.attachChild(mDanger);
		mGameScene.attachChild(mCocoTree);
		mGameScene.attachChild(mWoman);
		//////////////////////////////////
			// attach overlay image
				float playerCenterX = this.mPlayer.getX() + this.mPlayer.getWidth() / 2;
				float playerCenterY = this.mPlayer.getY() + this.mPlayer.getHeight()
						/ 2;

				mOverlay = new Sprite(0, 0, mOverlayTextureRegion,
						this.getVertexBufferObjectManager());

				float overlayX = playerCenterX - mOverlay.getWidth() / 2;
				float overlayY = playerCenterY - mOverlay.getHeight() / 2 + 40;

				mOverlay.setX(overlayX);
				mOverlay.setY(overlayY);

				mGameScene.attachChild(mOverlay);
				////////////////////
				final PhysicsHandler physicsHandler2 = new PhysicsHandler(mOverlay);
				mPlayer.registerUpdateHandler(physicsHandler2);

				// Analog control
				final AnalogOnScreenControl analogOnScreenControl = new AnalogOnScreenControl(
						0, CAMERA_HEIGHT
								- this.mOnScreenControlBaseTextureRegion.getHeight(),
						this.mCamera, this.mOnScreenControlBaseTextureRegion,
						this.mOnScreenControlKnobTextureRegion, 0.1f, 200,
						mVertexBufferObjectManager,
						new IAnalogOnScreenControlListener() {
							@Override
							public void onControlChange(
									final BaseOnScreenControl pBaseOnScreenControl,
									final float pValueX, final float pValueY) {
								float incX = 0, incY = 0;

								if (pValueX > 0 && !movingFwd) {
									mPlayer.animate(playerFrmTime, playerFwdFrms);
									movingFwd = true;
								}

								if (pValueX < 0 && movingFwd) {
									mPlayer.animate(playerFrmTime, playerBwdFrms);
									movingFwd = false;
								}

								// left
								if (mPlayer.getX() >= 0 && pValueX > 0) {
									incX = pValueX * 100;
								}

								if (mPlayer.getX() < 0) {
									mPlayer.setX(0);
									mOverlay.setX(mPlayer.getWidth() / 2
											- mOverlay.getWidth() / 2);
									incX = 0;
								}

								// top
								if (mPlayer.getY() >= 0 && pValueY > 0) {
									incY = pValueY * 100;
								}

								if (mPlayer.getY() < 0) {
									mPlayer.setY(0);
									mOverlay.setY(mPlayer.getWidth() / 2
											- mOverlay.getHeight() / 2 + 40);
									incY = 0;
								}

								// right
								if (mPlayer.getX() + mPlayer.getWidth() <= CAMERA_WIDTH
										&& pValueX < 0) {
									incX = pValueX * 100;
								}

								if (mPlayer.getX() + mPlayer.getWidth() > CAMERA_WIDTH - 20) {
									mPlayer.setX(CAMERA_WIDTH - mPlayer.getWidth() - 20);
									mOverlay.setX(mPlayer.getX() + mPlayer.getWidth()
											/ 2 - mOverlay.getWidth() / 2);
									incX = 0;
								}

								// bottom
								if (mPlayer.getY() + mPlayer.getHeight() <= CAMERA_HEIGHT
										&& pValueY < 0) {
									incY = pValueY * 100;
								}

								if (mPlayer.getY() + mPlayer.getHeight() > CAMERA_HEIGHT - 20) {
									mPlayer.setY(CAMERA_HEIGHT - mPlayer.getHeight()
											- 20);
									mOverlay.setY(mPlayer.getY() + mPlayer.getHeight()
											/ 2 - mOverlay.getHeight() / 2 + 40);
									incY = 0;
								}

								physicsHandler.setVelocity(incX, incY);
								physicsHandler2.setVelocity(incX, incY);
							}

							@Override
							public void onControlClick(
									final AnalogOnScreenControl pAnalogOnScreenControl) {
								// mPlayer.registerEntityModifier(new
								// SequenceEntityModifier(
								// new ScaleModifier(0.25f, 1, 1.5f),
								// new ScaleModifier(0.25f, 1.5f, 1)));
							}
						});
				analogOnScreenControl.getControlBase().setBlendFunction(
						GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
				analogOnScreenControl.getControlBase().setAlpha(0.5f);
				analogOnScreenControl.getControlBase().setScaleCenter(0, 128);
				analogOnScreenControl.getControlBase().setScale(1.25f);
				analogOnScreenControl.getControlKnob().setScale(1.25f);
				analogOnScreenControl.refreshControlKnobPosition();

				mGameScene.setChildScene(analogOnScreenControl);
				mGameUpdateHandler = new GameUpdateHandler();
				mGameScene.registerUpdateHandler(mGameUpdateHandler);

				mEngine.setScene(mGameScene);
				this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Toast.makeText(MainActivity.this,
								"Temukan Soal Warna Putih.",
								Toast.LENGTH_LONG).show();
					}
				});

				// attach fonts
				attachFonts(mGameScene);
	}

	private void loadSplashSceneResources() {
		// Splash screen
		mSplashTextureAtlas = new BitmapTextureAtlas(getTextureManager(),
				CAMERA_WIDTH, CAMERA_HEIGHT, TextureOptions.DEFAULT);
		mSplashTextureRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(mSplashTextureAtlas, this, "splash.png", 0, 0);
		mSplashTextureAtlas.load();
	}

	private void initSplashScreen() {
		mSplashScene = new Scene();
		mSplash = new Sprite(0, 0, mSplashTextureRegion,
				getVertexBufferObjectManager()) {
			protected void preDraw(GLState pGLState, Camera pCamera) {
				super.preDraw(pGLState, pCamera);
				pGLState.enableDither();
			};
		};
		mSplashScene.attachChild(mSplash);
	}

	public float getCenter(float total, float size) {
		return (total - size) / 2f;
	}

	float mSeconds = 0.0f;
	private int atecount = 10;

	private class GameUpdateHandler implements IUpdateHandler {

		@Override
		public void onUpdate(float pSecondsElapsed) {

			mSeconds = mSeconds + pSecondsElapsed;
			int time = (int) mSeconds;
			DecimalFormat format = new DecimalFormat("##.#");
			String formatted = format.format(mSeconds);
			mEngineUpdateText.setText(" " + formatted);

			IShape pet = (IShape) mGameScene.getChildByTag(100);
			if (mPlayer.collidesWith(pet)) {
				mGameScene.detachChild(mQuestion);
			} else if (mPlayer.collidesWith(mDanger)
					|| mPlayer.collidesWith(mDanger1)) {
				final String animal = mPlayer.collidesWith(mDanger) ? " !"
						: " !";
				runOnUiThread(new Runnable() {

					@Override
					public void run() { // TODO Auto-generated method stub
						Toast.makeText(
								MainActivity.this,
								" " + animal
										+ " . !",
								Toast.LENGTH_LONG).show();
					}
				});
				mGameScene.detachChild(mPlayer);
			} else if (mPlayer.collidesWith(mDanger2)) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() { // TODO Auto-generated method stub
						Toast.makeText(
								MainActivity.this,
								"NARKOBA MEMBUNUHMU!",
								Toast.LENGTH_LONG).show();
					}
				});
				mGameScene.detachChild(mPlayer);
			}

			if (mGameScene.getChildByTag(100) == null) {
				showGameWon(time);
			} else if (mGameScene.getChildByTag(1) == null) {
				showGameOver(time);
			}

			if (time != 0 && time % 10 == 0) { //
				mEatenText.setText((--atecount) + "");
			}

		}

		@Override
		public void reset() {
			// TODO Auto-generated method stub

		}
	}

	private void showGameWon(final int time) {
		String congo = "";
		if (time < 10) {
			congo = "A!";
		} else if (time < 20) {
			congo = "B!";
		} else if (time < 30) {
			congo = "C!";
		} else if (time < 40) {
			congo = "D!";
		} else {
			congo = "E?";
		}

		mGameScene.clearChildScene();
		mGameScene.detachChildren();
		mGameScene.unregisterUpdateHandler(mGameUpdateHandler);

		mEatsText = new Text(120, 150, mWinLoseFont,
				"\t\t\t\t\t\t\t\tYaay!\n Anda Mennemukannya.\n" + time
						+ " detik untuk Menemukannya.\n Nilaimu = " + congo,
				mVertexBufferObjectManager);
		mGameScene.attachChild(mEatsText);
		mGameScene.setOnAreaTouchListener(this);
		mGameScene.registerTouchArea(mBack);
		mGameScene.attachChild(mBack);
	}

	private void showGameOver(final int time) {
		mGameScene.clearChildScene();
		mGameScene.detachChildren();
		mGameScene.unregisterUpdateHandler(mGameUpdateHandler);
		// Create gameover image and attach it
		mEatsText = new Text(120, 150, mWinLoseFont,
				"\t\t\t\t\t\t\t\tMAAF!\n Anda Gagal.\n Gagal dalam" + time
						+ " detik.\n Coba Lagi!", mVertexBufferObjectManager);
		mGameScene.attachChild(mEatsText);
		mGameScene.setOnAreaTouchListener(this);
		mGameScene.registerTouchArea(mBack);
		mGameScene.attachChild(mBack);
	}

	private void loadFont() {
		FontFactory.setAssetBasePath("font/");
		final ITexture wonLostFontTexture = new BitmapTextureAtlas(
				this.getTextureManager(), 400, 600, TextureOptions.BILINEAR);
		final ITexture creditFontTexture = new BitmapTextureAtlas(
				this.getTextureManager(), 400, 600, TextureOptions.BILINEAR);
		// Font
		this.mFont = FontFactory.create(getFontManager(), getTextureManager(),
				100, 100, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 20,
				Color.WHITE);
		this.mFont.load();

		this.mCreditsFont = FontFactory.create(getFontManager(),
				getTextureManager(), 100, 100,
				Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 20,
				Color.WHITE);
		this.mCreditsFont.load();

		this.mWinLoseFont = FontFactory.create(getFontManager(),
				getTextureManager(), 100, 100,
				Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 20,
				Color.WHITE);
		this.mWinLoseFont.load();
	}

	private void attachFonts(Scene mScene) {
		// Texts
		mEatsText = new Text(10, 40, mFont, " : ",
				mVertexBufferObjectManager);
		mEatenText = new Text(mEatsText.getWidth() + 10, 40, mFont, "10", 100,
				mVertexBufferObjectManager);
		Text update = new Text(10, 65, mFont, " Time : ", 100,
				mVertexBufferObjectManager);
		mEngineUpdateText = new Text(update.getWidth() + 10, 65, mFont, " 0 ",
				100, mVertexBufferObjectManager);
		mScene.attachChild(update);
		mScene.attachChild(mEngineUpdateText);

	}

}
