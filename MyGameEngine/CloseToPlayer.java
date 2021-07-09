package MyGameEngine;

import ray.ai.behaviortrees.BTCondition;

public class CloseToPlayer extends BTCondition {
	
	private NPCController npcCtrl;
	
	public CloseToPlayer(NPCController nC, boolean toNegate) 
	{
		super(toNegate);
		this.npcCtrl = nC;
	}

	@Override
	protected boolean check() 
	{
		float dist = npcCtrl.getPlayer().getNode().getWorldPosition()
				.sub(npcCtrl.getNPC().getNode().getWorldPosition())
				.length();
		
		if ( dist <= 25.0f )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

}
