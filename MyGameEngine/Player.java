package MyGameEngine;

import java.util.UUID;

import ray.rage.scene.SceneNode;
import ray.rage.scene.SkeletalEntity;
import ray.rml.Vector3;

public class Player 
{
	private SceneNode node = null;
	private boolean stillMoving = false;
	private Vector3 lastPos = null;
	private SkeletalEntity se = null;
	private int bulletCount = 6;
	private int health = 100;
	private String curAnim = "";
	private String hatChoice = "";
	private UUID id = null;
	
	private final int maxHealth = 100;
	private final int maxBullets = 6;
	
	public Player(UUID id)
	{
		this.id = id;
		this.bulletCount = maxBullets;
		this.health = maxHealth;
	}
	public Player(SceneNode n)
	{
		this.node = n;
		this.se = (SkeletalEntity) n.getAttachedObject(0);
		this.lastPos = n.getWorldPosition();
	}
	
	public void initPlayer()
	{
		setBulletCount(maxBullets);
		setHealth(maxHealth);
		setStillMoving(false);
	}
	
	public UUID getId() 
	{
		return this.id;
	}
	public void setId(UUID id) 
	{
		this.id = id;
	}
	
	public boolean isStillMoving()
	{
		return this.stillMoving;
	}
	public void setStillMoving(boolean b)
	{
		this.stillMoving = b;
	}
	
	public Vector3 getLastPos()
	{
		return this.lastPos;
	}
	public void setLastPos(Vector3 newPos)
	{
		this.lastPos = newPos;
	}
	
	public SceneNode getNode() 
	{
		return node;
	}
	public void setNode(SceneNode node) 
	{
		this.node = node;
	}

	public SkeletalEntity getEntity() 
	{
		return se;
	}
	public void setEntity(SkeletalEntity se) 
	{
		this.se = se;
	}

	public int getBulletCount()
	{
		return this.bulletCount;
	}
	public void setBulletCount(int b)
	{ 
		this.bulletCount = b; 
	}
	
	public int getHealth()
	{
		return this.health;
	}
	public void setHealth(int hp)
	{
		this.health = hp;
	}
	
	public String getHatChoice() 
	{
		return hatChoice;
	}
	public void setHatChoice(String hC)
	{
		this.hatChoice = hC;
	}
	
	public String getCurrentAnim() 
	{
		return curAnim;
	}
	public void setCurrentAnim(String curAnim) 
	{
		this.curAnim = curAnim;
	}
}
