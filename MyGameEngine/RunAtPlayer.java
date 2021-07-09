package MyGameEngine;

import ray.ai.behaviortrees.BTAction;
import ray.ai.behaviortrees.BTStatus;

public class RunAtPlayer extends BTAction 
{
	private NPCController npcCtrl;
	private NPC npc;
	
	public RunAtPlayer(NPCController nC, NPC n)
	{
		this.npcCtrl = nC;
		this.npc = n;
	}
	
	@Override
	protected BTStatus update(float time) 
	{
		npc.runAtPlayer(npcCtrl);
		return BTStatus.BH_SUCCESS;
	}

}
