package MyGameEngine;

import DuelGame.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class MeleeAction extends AbstractInputAction 
{
	private MyGame game;
	
	public MeleeAction(MyGame g)
	{
		game = g;
	}
	
	@Override
	public void performAction(float time, Event event) 
	{
		if ( game.getAttackCooldown() <= 0 )
		{
			game.meleeFrom(game.getPlayerNodes().get(0));
			
			game.setAttackCooldown();
		}
	}

}
