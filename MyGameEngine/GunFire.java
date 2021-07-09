package MyGameEngine;

import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;

// manages time of bullet node and muzzle flash before deletion
public class GunFire {
	private boolean alreadyDamaged = false;
	private Player owner = null;
	
	private SceneNode bulletN, flashN;
	private SceneManager sm;
	
	private float totalElapsedMillis = 0.0f;
	private float timeLeft = 1500.0f;
	
	public GunFire(SceneManager sm, SceneNode bN, SceneNode fN, Player p)
	{
		this.sm = sm;
		this.bulletN = bN;
		this.flashN = fN;
		this.owner = p;
	}
	
	public SceneNode getBulletN()
	{
		return this.bulletN;
	}
	
	public boolean isAlreadyDamaged() 
	{
		return alreadyDamaged;
	}
	public void setAlreadyDamaged(boolean alreadyDamaged) 
	{
		this.alreadyDamaged = alreadyDamaged;
	}

	public float getTimeLeft()
	{
		return this.timeLeft;
	}
	public void setTimeLeft(int t)
	{
		this.timeLeft = t;
	}
	
	public Player getOwner()
	{
		return this.owner;
	}
	
	public void iterate(float elapsedMillis)
	{
		totalElapsedMillis += elapsedMillis;
		timeLeft -= elapsedMillis;
		
		if ( flashN != null && totalElapsedMillis >= 250.0f )
		{
			sm.destroySceneNode(flashN);
			flashN = null;
		}
	}
	
	public void destroyLights()
	{
		if ( flashN != null)
		{
			sm.destroySceneNode(flashN);
			flashN = null;
		}
	}
}
