package MyGameEngine;

import DuelGame.MyGame;
import net.java.games.input.Event;
import ray.input.action.AbstractInputAction;

public class SendCloseConnectionPacketAction extends AbstractInputAction {

	private MyGame game;
	
	public SendCloseConnectionPacketAction(MyGame g)
	{
		this.game = g;
	}
	
	@Override
	public void performAction(float arg0, Event arg1) 
	{
		if(game.getProtocolClient() != null && game.isClientConnected() == true)
		{ 
			game.getProtocolClient().sendByeMessage();
			game.setIsClientConnected(false);
		}
	}

}
