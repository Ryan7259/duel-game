package MyGameEngine;

import ray.ai.behaviortrees.BTAction;
import ray.ai.behaviortrees.BTStatus;

public class PlayDead extends BTAction {

	private NPCController npcCtrl;
	private NPC npc;
	
	public PlayDead(NPCController nC, NPC n)
	{
		this.npcCtrl = nC;
		this.npc = n;
	}
	
	@Override
	protected BTStatus update(float time) 
	{
		npc.playDead(npcCtrl);
		return BTStatus.BH_SUCCESS;
	}

}
