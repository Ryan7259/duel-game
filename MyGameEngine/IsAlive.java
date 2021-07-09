package MyGameEngine;

import ray.ai.behaviortrees.BTCondition;

public class IsAlive extends BTCondition {

	private NPC npc;
	
	public IsAlive(NPC n, boolean toNegate) 
	{
		super(toNegate);
		this.npc = n;
	}

	@Override
	protected boolean check() 
	{
		if ( npc.getHealth() > 0 )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

}
