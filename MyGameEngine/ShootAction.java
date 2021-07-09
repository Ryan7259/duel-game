package MyGameEngine;

import DuelGame.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class ShootAction extends AbstractInputAction 
{
	private MyGame game;
	
	public ShootAction(MyGame g)
	{
		game = g;
	}
	
	@Override
	public void performAction(float time, Event event) 
	{
		if ( game.getAttackCooldown() <= 0 )
		{
			Player p = game.getPlayerNodes().get(0);
			if ( p.getBulletCount() > 0 )
			{
				p.setBulletCount( p.getBulletCount() - 1 );
				game.shootFrom(p, p.getNode().getWorldPosition(), p.getNode().getWorldRotation());
				
				if ( game.isClientConnected() && game.getPlayersJoined() > 0 )
				{
					game.getProtocolClient().sendShootMessage(p.getNode().getWorldPosition(), p.getNode().getWorldRotation());
				}
			}
			
			game.setAttackCooldown();
		}
	}

}
