package MyGameEngine;

import ray.ai.behaviortrees.BTAction;
import ray.ai.behaviortrees.BTStatus;

public class MeleePlayer extends BTAction 
{
	private NPCController npcCtrl;
	private NPC npc;
	
	public MeleePlayer(NPCController nC, NPC n)
	{
		this.npcCtrl = nC;
		this.npc = n;
	}
	
	@Override
	protected BTStatus update(float time) 
	{
		npc.meleePlayer(npcCtrl);
		return BTStatus.BH_SUCCESS;
	}

}
