package MyGameEngine;

import java.util.UUID;
import ray.rml.Matrix3;
import ray.rml.Vector3;

public class GhostAvatar extends Player
{
	private Vector3 initPos;
	private Matrix3 initRot;
	
	public GhostAvatar(UUID id, Vector3 initPos, Matrix3 initRot, String hatChoice)
	{
		super(id);
		this.initPos = initPos;
		this.initRot = initRot;
		this.setHatChoice(hatChoice);
	}

	public void update(Vector3 pos, Matrix3 rot)
	{
		if ( this.getHealth() <= 0 )
		{
			return;
		}
		
		if (rot != null)
		{
			this.getNode().setLocalRotation(rot);
		}
		if (pos != null)
		{
			this.getNode().setLocalPosition(pos);
		}
	}
	public Matrix3 getInitRot() 
	{
		return this.initRot;
	}
	public Vector3 getInitPos() 
	{
		return this.initPos;
	}
}
