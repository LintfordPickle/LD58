package net.lintfordlib.samples.renderers;

import net.lintfordlib.assets.ResourceManager;
import net.lintfordlib.controllers.core.FafAnimationController;
import net.lintfordlib.core.graphics.sprites.spritesheet.SpriteSheetDefinition;
import net.lintfordlib.renderers.RendererManagerBase;
import net.lintfordlib.renderers.sprites.FafAnimationRenderer;
import net.lintfordlib.samples.ConstantsGame;

public class AnimationRenderer extends FafAnimationRenderer {

	// --------------------------------------
	// Constants
	// --------------------------------------

	public static final String RENDERER_NAME = "Animation Renderer";

	// --------------------------------------
	// Constructor
	// --------------------------------------

	public AnimationRenderer(RendererManagerBase rendererManager, FafAnimationController animationController, int entityGroupUid) {
		super(rendererManager, RENDERER_NAME, animationController, entityGroupUid);
	}

	// --------------------------------------
	// Methods
	// --------------------------------------

	@Override
	protected SpriteSheetDefinition loadSpriteSheetDefinition(ResourceManager resourceManager) {
		return resourceManager.spriteSheetManager().getSpriteSheet("SPRITESHEET_GAME", ConstantsGame.GAME_RESOURCE_GROUP_ID);
	}

}
