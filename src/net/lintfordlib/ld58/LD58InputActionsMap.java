package net.lintfordlib.ld58;

import org.lwjgl.glfw.GLFW;

import net.lintfordlib.core.input.BindableInputActionMap;
import net.lintfordlib.core.input.gamepad.GamepadInputMap;

//@formatter:off

public class LD58InputActionsMap extends BindableInputActionMap {

	public static final int KEY_BINDING_FIRE 		= 1000;
	public static final int KEY_BINDING_JUMP 		= 1001;
	public static final int KEY_BINDING_FORWARD 	= 1002;
	public static final int KEY_BINDING_BACKWARD 	= 1003;
	public static final int KEY_BINDING_LEFT 		= 1004;
	public static final int KEY_BINDING_RIGHT 		= 1005;

	public LD58InputActionsMap() {

		// These will appear on the keybinds screen.

		addNewEventAction("Primary Fire", 		KEY_BINDING_FIRE, 		GLFW.GLFW_KEY_Z, 		GamepadInputMap.LINTFORD_GAMEPAD_BUTTON_A);
		addNewEventAction("Jump", 				KEY_BINDING_JUMP, 		GLFW.GLFW_KEY_X, 		GamepadInputMap.LINTFORD_GAMEPAD_BUTTON_B);
		
		// These are the bindings we want to default to (for most gamepads and keyboard) 
		addNewEventAction("Forward", 			KEY_BINDING_FORWARD, 	GLFW.GLFW_KEY_UP, 		GamepadInputMap.LINTFORD_GAMEPAD_AXIS_LEFT_Y_UP);
		addNewEventAction("Backwards", 			KEY_BINDING_BACKWARD, 	GLFW.GLFW_KEY_DOWN, 	GamepadInputMap.LINTFORD_GAMEPAD_AXIS_LEFT_Y_DOWN);
		addNewEventAction("Left", 				KEY_BINDING_LEFT, 		GLFW.GLFW_KEY_LEFT, 	GamepadInputMap.LINTFORD_GAMEPAD_AXIS_LEFT_X_LEFT);
		addNewEventAction("Right", 				KEY_BINDING_RIGHT, 		GLFW.GLFW_KEY_RIGHT, 	GamepadInputMap.LINTFORD_GAMEPAD_AXIS_LEFT_X_RIGHT);
		
		// DEBUG: For the cheap gamepad I have with wierd mappings, I yould need to set the following overrides in the game options to make it work.
//		addNewEventAction("Forward", 			KEY_BINDING_FORWARD, 	GLFW.GLFW_KEY_UP, 		GamepadInputMap.LINTFORD_GAMEPAD_BUTTON_DPAD_UP);
//		addNewEventAction("Backwards", 			KEY_BINDING_BACKWARD, 	GLFW.GLFW_KEY_DOWN, 	GamepadInputMap.LINTFORD_GAMEPAD_BUTTON_DPAD_DOWN);
//		addNewEventAction("Left", 				KEY_BINDING_LEFT, 		GLFW.GLFW_KEY_LEFT, 	GamepadInputMap.LINTFORD_GAMEPAD_BUTTON_DPAD_LEFT);
//		addNewEventAction("Right", 				KEY_BINDING_RIGHT, 		GLFW.GLFW_KEY_RIGHT, 	GamepadInputMap.LINTFORD_GAMEPAD_BUTTON_DPAD_RIGHT);

	}
}
