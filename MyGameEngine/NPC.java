package MyGameEngine;

import java.util.Random;
import java.util.UUID;

import ray.rage.scene.SceneNode;
import ray.rml.Degreef;
import ray.rml.Matrix3;
import ray.rml.Matrix3f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class NPC extends Player 
{
	private float moveRate = 0.03f;
	private float moveDirX = 0.0f;
	private float moveDirY = 0.0f;
	private Random r = new Random();
	
	public NPC(SceneNode n)
	{
		super(n);
		this.setId(UUID.randomUUID());
	}
	
	public void dodgePlayer(NPCController npcCtrl)
	{
		if ( r.nextFloat() < 0.5f )
		{
			moveDirX = 1.0f;
		}
		else
		{
			moveDirX = -1.0f;
		}
	}
	
	public void meleePlayer(NPCController npcCtrl)
	{
		moveDirY = 0.0f;
		moveDirX = 0.0f;
		npcCtrl.getGame().meleeFrom(this);
	}
	
	public void shootPlayer(NPCController npcCtrl)
	{
		if ( this.getBulletCount() > 0 && r.nextFloat() > 0.6f)
		{
			// randomize a little for fairness
			float negation = 1.0f;
			if (r.nextFloat()*2.0f >= 1.0f)
			{
				negation = -1.0f;
			}
			
			Matrix3 rotMat = Matrix3f.createRotationFrom(Degreef.createFrom(negation*r.nextFloat()*10.0f), Vector3f.createFrom(0.0f, 1.0f, 0.0f));
			this.getNode().setLocalRotation(rotMat.mult(this.getNode().getWorldRotation()));
			
			Vector3 oldPos = this.getNode().getWorldPosition();
			Vector3 offset = Vector3f.createFrom(oldPos.x(), oldPos.y()+7.0f, oldPos.z());
			
			npcCtrl.getGame().shootFrom(this, offset, this.getNode().getWorldRotation());
			this.setBulletCount(this.getBulletCount()-1);
		}
	}
	
	public void runAtPlayer(NPCController npcCtrl)
	{
		moveDirY = 1.0f;
		moveDirX = 0.0f;
	}
	
	public void playDead(NPCController npcCtrl)
	{
		moveDirY = 0.0f;
		moveDirY = 0.0f;
		npcCtrl.getGame().killPlayer(this.getId());
	}
	public void updateLocation(NPCController npcCtrl, float time)
	{
		Vector3 z = npcCtrl.getGame().getGhostAvN().getWorldPosition().sub(this.getNode().getWorldPosition()).normalize();
		Vector3 x = z.cross(Vector3f.createFrom(0.0f, 1.0f, 0.0f)).normalize().negate();
		Vector3 y = z.cross(x).normalize();
		
		Matrix3 rotMat = Matrix3f.createFrom(x, y, z);
		this.getNode().setLocalRotation(rotMat);
		
		this.getNode().moveForward(time*moveRate*moveDirY);
		this.getNode().moveLeft(time*moveRate*moveDirX);
		
		npcCtrl.getGame().updateVerticalPosition(this.getNode(), 0.0f);
	}
}