package MyGameEngine;

import ray.ai.behaviortrees.BTAction;
import ray.ai.behaviortrees.BTStatus;

public class ShootPlayer extends BTAction 
{
	private NPCController npcCtrl;
	private NPC npc;
	
	public ShootPlayer(NPCController nC, NPC n)
	{
		this.npcCtrl = nC;
		this.npc = n;
	}
	
	@Override
	protected BTStatus update(float time) 
	{
		npc.shootPlayer(npcCtrl);
		return BTStatus.BH_SUCCESS;
	}

}
