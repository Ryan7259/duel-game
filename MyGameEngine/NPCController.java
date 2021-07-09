package MyGameEngine;

import java.util.Random;

import DuelGame.MyGame;
import ray.ai.behaviortrees.BTCompositeType;
import ray.ai.behaviortrees.BTSelector;
import ray.ai.behaviortrees.BTSequence;
import ray.ai.behaviortrees.BehaviorTree;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.SkeletalEntity;

public class NPCController 
{
	private BehaviorTree bt = new BehaviorTree(BTCompositeType.SELECTOR);
	
	private MyGame game;
	private Player player;
	private NPC npc;
	private float lastThinkUpdateTime;
	
	public NPCController(MyGame g, Player p)
	{
		this.game = g;
		this.player = p;
		this.npc = null;
	}
	
	public void start(SkeletalEntity npcSE, SkeletalEntity npcNoHatSE)
	{
		setLastThinkUpdateTime(getGame().getEngine().getElapsedTimeMillis());
		setupNPC(npcSE, npcNoHatSE);
		setupBehaviorTree();
	}
	
	public void setupNPC(SkeletalEntity npcSE, SkeletalEntity npcNoHatSE)
	{ 
		SceneManager sm = getGame().getEngine().getSceneManager();
		
        SceneNode npcN = sm.getRootSceneNode().createChildSceneNode("npcNode");
        npcN.attachObject(npcSE);
        npcN.attachObject(npcNoHatSE);
        npcNoHatSE.setVisible(false);
        
        this.npc = new NPC(npcN);
        this.npc.setHatChoice("cowboyHat");
        getGame().getPlayerNodes().add(npc);
        
		Random rand = new Random();
		npc.getNode().setLocalPosition(npc.getNode().getWorldPosition().add(50.0f*rand.nextFloat(), 0.0f, 10.0f*rand.nextFloat()));
		game.updateVerticalPosition(npc.getNode(), 0.0f);
	}
	
	public void setupBehaviorTree()
	{ 
		bt.insertAtRoot(new BTSequence(1));
		bt.insertAtRoot(new BTSequence(2));
		bt.insertAtRoot(new BTSequence(10));
		bt.insert(2, new BTSelector(3));
		bt.insert(3, new BTSequence(4));
		bt.insert(3, new BTSequence(5));
		
		bt.insert(1, new IsAlive(npc, false));
		bt.insert(1, new HasBullets(npc, false));
		bt.insert(1, new DodgePlayer(this, npc));
		bt.insert(1, new ShootPlayer(this, npc));
		
		bt.insert(2, new IsAlive(npc, false));
		bt.insert(2, new HasBullets(npc, true));
		
		bt.insert(4, new CloseToPlayer(this, true));
		bt.insert(4, new RunAtPlayer(this, npc));
		
		bt.insert(5, new CloseToPlayer(this, false));
		bt.insert(5, new MeleePlayer(this, npc));
		
		bt.insert(10, new IsAlive(npc, true));
		bt.insert(10, new PlayDead(this, npc));
	}

	public NPC getNPC()
	{
		return npc;
	}
	public Player getPlayer() 
	{
		return player;
	}
	public MyGame getGame() 
	{
		return game;
	}
	public BehaviorTree getBT()
	{
		return bt;
	}

	public float getLastThinkUpdateTime() 
	{
		return lastThinkUpdateTime;
	}
	public void setLastThinkUpdateTime(float lastThinkUpdateTime) 
	{
		this.lastThinkUpdateTime = lastThinkUpdateTime;
	}
}