package MyGameEngine;

import ray.input.action.AbstractInputAction;
import ray.rage.game.*;
import DuelGame.MyGame;
import net.java.games.input.Event;

public class QuitGameAction extends AbstractInputAction 
{
	private MyGame game;
	
	public QuitGameAction(MyGame g)
	{
		game = g;
	}
	
	@Override
	public void performAction(float time, Event event) 
	{
		System.out.println("Shutdown requested");
		game.setState(Game.State.STOPPING);
	}

}
