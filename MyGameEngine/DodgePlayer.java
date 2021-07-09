package MyGameEngine;

import ray.ai.behaviortrees.BTAction;
import ray.ai.behaviortrees.BTStatus;

public class DodgePlayer extends BTAction 
{
	private NPCController npcCtrl;
	private NPC npc;
	
	public DodgePlayer(NPCController nC, NPC n)
	{
		this.npcCtrl = nC;
		this.npc = n;
	}
	
	@Override
	protected BTStatus update(float time) 
	{
		npc.dodgePlayer(npcCtrl);
		return BTStatus.BH_SUCCESS;
	}

}
