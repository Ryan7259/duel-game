package DuelGameServer;

import java.io.Serializable;

import ray.rml.Matrix3;
import ray.rml.Matrix3f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class PacketObject implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1598406219907264712L;
	private String message = "";
	private float[] pos = {};
	private float[] rot = {};
	private String hatChoice = "";
	
	public PacketObject(String msg)
	{
		this.message = msg;
	}
	public PacketObject(String msg, Vector3 p, Matrix3 r)
	{
		this.message = msg;
		
		if (p != null)
		{
			this.pos = new float[] {p.x(), p.y(), p.z()};
		}

		if (r != null)
		{
			Vector3 col1 = r.column(0);
			Vector3 col2 = r.column(1);
			Vector3 col3 = r.column(2);
			
			this.rot = new float[] {
					col1.x(),col1.y(),col1.z(), 
					col2.x(),col2.y(),col2.z(),
					col3.x(),col3.y(),col3.z()};
		}
	}
	public PacketObject(String msg, Vector3 p, Matrix3 r, String hatChoice)
	{
		this.message = msg;
		
		if (p != null)
		{
			this.pos = new float[] {p.x(), p.y(), p.z()};
		}

		if (r != null)
		{
			Vector3 col1 = r.column(0);
			Vector3 col2 = r.column(1);
			Vector3 col3 = r.column(2);
			
			this.rot = new float[] {
					col1.x(),col1.y(),col1.z(), 
					col2.x(),col2.y(),col2.z(),
					col3.x(),col3.y(),col3.z()};
		}
		
		this.hatChoice = hatChoice;
	}
	public String getMessage() 
	{
		return message;
	}
	public Vector3 getPosition() 
	{
		Vector3 vecPos = null;
				
		if (pos.length > 0)
		{
			vecPos = Vector3f.createFrom(pos);
		}
		
		return vecPos;
	}
	public Matrix3 getRotation() 
	{
		Matrix3 matRot = null;
		
		if (rot.length > 0)
		{
			matRot = Matrix3f.createFrom(rot);
		}

		return matRot;
	}
	public String getHatChoice() 
	{
		return hatChoice;
	}
}