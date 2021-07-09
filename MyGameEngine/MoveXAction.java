package MyGameEngine;

import DuelGame.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;

public class MoveXAction extends AbstractInputAction 
{
	private MyGame game;
	
	public MoveXAction(MyGame g)
	{
		game = g;
	}
	
	@Override
	public void performAction(float time, Event event) 
	{
		if ( game.getPlayerNodes().get(0).getHealth() <= 0 )
		{
			return;
		}
		
		float delta = time*0.03f;
		String componentStr = event.getComponent().getName();
		SceneNode moveNode = game.getCameraN();
		
		// determine movement direction based on key or axis
		if (componentStr.equals("A") || componentStr.equals("X Axis") && event.getValue() < -0.3)
		{
			// flip direction to scale
			delta *= -1.0f;
		}
		else if (componentStr.equals("D") || componentStr.equals("X Axis") && event.getValue() > 0.3)
		{
			// leave delta as is
		}
		else
		{
			// in gamepad's dead-zone so set delta to 0 to not move dolphin
			delta = 0.0f;
		}
		
		// move from final results
		moveNode.moveLeft(delta);
		game.updateVerticalPosition(moveNode, 7.0f);
		game.getAvatarN().setLocalPosition(moveNode.getWorldPosition());
		game.getGhostAvN().setLocalPosition(moveNode.getWorldPosition());
		game.updateVerticalPosition(game.getGhostAvN(), 0.0f);
	}
}
