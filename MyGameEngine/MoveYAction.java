package MyGameEngine;

import DuelGame.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;
import ray.rage.scene.SceneNode;

public class MoveYAction extends AbstractInputAction 
{
	private MyGame game;
	
	public MoveYAction(MyGame g)
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
		if (componentStr.equals("W") || componentStr.equals("Y Axis") && event.getValue() < -0.3)
		{
			// leave delta as is
		}
		else if (componentStr.equals("S") || componentStr.equals("Y Axis") && event.getValue() > 0.3)
		{
			// flip direction to scale
			delta *= -1.0f;
		}
		else
		{
			// disable movement if within gamepad's dead-zone
			delta = 0.0f;
		}
		
		// move from final results
		moveNode.moveForward(delta);
		game.updateVerticalPosition(moveNode, 7.0f);
		game.getAvatarN().setLocalPosition(moveNode.getWorldPosition());
		game.getGhostAvN().setLocalPosition(moveNode.getWorldPosition());
		game.updateVerticalPosition(game.getGhostAvN(), 0.0f);
	}
}
