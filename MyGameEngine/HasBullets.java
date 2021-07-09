package MyGameEngine;

import ray.ai.behaviortrees.BTCondition;

public class HasBullets extends BTCondition 
{
	
	private NPC npc;
	
	public HasBullets(NPC n, boolean toNegate) 
	{
		super(toNegate);
		this.npc = n;
	}

	@Override
	protected boolean check() 
	{
		if ( npc.getBulletCount() > 0 )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

}
