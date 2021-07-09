package MyGameEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import DuelGame.MyGame;
import DuelGameServer.PacketObject;
import ray.networking.client.GameConnectionClient;
import ray.rml.Matrix3;
import ray.rml.Vector3;

public class ProtocolClient extends GameConnectionClient 
{
	private MyGame game = null;
	private UUID id = null;
	private Vector<GhostAvatar> ghostAvatars = null;
	private boolean joinedAsP2 = false;

	public ProtocolClient(InetAddress remAddr, int remPort,
			ProtocolType pType, MyGame game) throws IOException
	{
		super(remAddr, remPort, pType);
		this.game = game;
		this.id = game.getPlayerNodes().get(0).getId();
		this.ghostAvatars = new Vector<GhostAvatar>();
	}
	
	public UUID getId()
	{
		return this.id;
	}
	
	public boolean isJoinedAsP2() {
		return joinedAsP2;
	}

	public void setJoinedAsP2(boolean b) {
		this.joinedAsP2 = b;
	}
	
	public Vector<GhostAvatar> getGhosts()
	{
		return this.ghostAvatars;
	}
	
	@Override
	public void processPacket(Object o)
	{
		PacketObject obj = (PacketObject) o;
		
		if ( obj == null || obj.getMessage() == null )
		{
			return;
		}
		
		String message = obj.getMessage();
		//System.out.println("message: " + message);
		String[] messageTokens = message.split(",");
		Vector3 pos = obj.getPosition();
		Matrix3 rot = obj.getRotation();
		
		if ( messageTokens.length > 0 )
		{
			// receive player count from server
			// format: message: playerCount,# (of players)
			if ( messageTokens[0].compareTo("playerCount") == 0 )
			{
				int count = Integer.parseInt(messageTokens[1]);
				System.out.println("server player count: " + count);
			}
			
			// receive "join" result from server
			// format: message: "join,success,playerCount(before joining)" or "join,failure,playerCount"
			if ( messageTokens[0].compareTo("join") == 0 )
			{ 
				int prevPlayerCount = Integer.parseInt(messageTokens[2]);
				
				if ( messageTokens[1].compareTo("success") == 0 )
				{ 	
					// this client successfully connected
					game.setIsClientConnected(true);
					
					// tell server to tell others to create a ghost avatar for this client
					sendCreateMessage(game.getAvatarPosition(), game.getAvatarRotation(), game.getPlayerNodes().get(0).getHatChoice());
					
					// if there is already a player on server, set client as P2 for initialization purposes
					if ( prevPlayerCount > 0 )
					{
						setJoinedAsP2(true);
						
						System.out.println("Joined as P2: " + isJoinedAsP2());
					}
					
					System.out.println("Client connected to server!");
				}
				else if ( messageTokens[1].compareTo("failure") == 0 )
				{ 
					System.out.println("Client failed to connect to server...Players on server (limit of 2): " + prevPlayerCount);
				} 
			}
			
			// receive “bye” which means remove ghost avatar of another client, ghostID
			// format: message: "bye,ghostID"
			if ( messageTokens[0].compareTo("bye") == 0 )
			{ 
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// remove ghost avatar from game
				removeGhostAvatar(ghostID);
				
				// set client as P1 if other player left
				setJoinedAsP2(false);
				
				System.out.println("Set as P1: " + !isJoinedAsP2());
			}
			
			 // receive "create" or "dsfr"
			// format: message: "create,ghostID" or "dsfr,ghostID"
			//		   position: Vector3 pos
			//		   rotation: Matrix3 rot
			if ( (messageTokens[0].compareTo("create") == 0) || (messageTokens[0].compareTo("dsfr") == 0) )
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				// if no existing ghost avatar of ghostID, it is a create message
				// create an existing or new ghost avatar for a client, ghostID
				createGhostAvatar(ghostID, pos, rot, obj.getHatChoice());
			}
			
			// receive "wants details for" from an asking client
			// format: message: "wsds,askingClientID"
			if ( messageTokens[0].compareTo("wsds") == 0 )
			{
				UUID askingClientID = UUID.fromString(messageTokens[1]);
				// get details for this client and send to asking client
				sendDetailsForMessage(askingClientID);
			}
			
			// receive "move" from a ghost avatar's client
			// format: message: "move/rotate,ghostID"
			//		   position: Vector3 pos
			//		   rotation: Matrix3 rot
			if ( messageTokens[0].compareTo("move") == 0 ) 
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				// update ghost avatar's rot for existing client, ghostID
				updateGhostAvatar(ghostID, pos, rot);
			}
			
			// receive "shoot" from a ghost avatar's client
			// format: message: "shoot,ghostID,pos,rot"
			if ( messageTokens[0].compareTo("shoot") == 0 )
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				shootFromGhost(ghostID, pos, rot);
			}
			
			// receive "melee" from a ghost avatar's client meaning they did melee dmg to client
			if ( messageTokens[0].compareTo("melee") == 0 )
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				meleeFromGhost(ghostID);
			}
			
			// receive "death" from a ghost avatar's client
			// format: "died,ghostID"
			if ( messageTokens[0].compareTo("died") == 0 )
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				killGhost(ghostID);
			}
		}
	}
	
	// finds and returns a ghost avatar by its ID or null if not found
	private GhostAvatar findGhostAvatar(UUID ghostID)
	{
		Iterator<GhostAvatar> itr = ghostAvatars.iterator();
		
		while ( itr.hasNext() )
		{
			GhostAvatar avatar = itr.next();
			
			if ( avatar.getId().equals(ghostID) )
			{
				return avatar;
			}
		}
		
		return null;
	}
	
	// finds and removes ghost avatar with ghostID
	private void removeGhostAvatar(UUID ghostID) 
	{
		GhostAvatar avatar = findGhostAvatar(ghostID);
		
		if ( avatar != null )
		{
			ghostAvatars.remove(avatar);
			game.removeGhostAvatarFromGameWorld(avatar);
			game.setPlayersJoined(game.getPlayersJoined()-1);
		}
	}

	// creates a ghost avatar with ghostID and set initial position/rotation
	private void createGhostAvatar(UUID ghostID, Vector3 pos, Matrix3 rot, String hatChoice) 
	{
		try 
		{
			GhostAvatar avatar = new GhostAvatar(ghostID, pos, rot, hatChoice);
			ghostAvatars.add(avatar);
			game.addGhostAvatarToGameWorld(avatar);
			game.setPlayersJoined(game.getPlayersJoined()+1);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// update pos/rot of a ghost avatar with ghostID
	private void updateGhostAvatar(UUID ghostID, Vector3 pos, Matrix3 rot)
	{
		GhostAvatar avatar = findGhostAvatar(ghostID);
		
		if ( avatar != null )
		{
			avatar.update(pos, rot);
		}
	}
	
	private void meleeFromGhost(UUID ghostID)
	{
		GhostAvatar avatar = findGhostAvatar(ghostID);
		
		if ( avatar != null )
		{
			game.meleeFrom(avatar);
		}
	}
	
	private void shootFromGhost(UUID ghostID, Vector3 pos, Matrix3 rot)
	{
		GhostAvatar avatar = findGhostAvatar(ghostID);
		
		if ( avatar != null )
		{
			game.shootFrom(avatar, pos, rot);
		}
	}
	
	private void killGhost(UUID ghostID)
	{
		game.killPlayer(ghostID);
	}
	
	// ask server for player count
	// format: "playerCount, askingClientID"
	public void sendAskPlayerCountMessage()
	{
		try
		{
			String message = new String("playerCount," + id.toString());
			PacketObject obj = new PacketObject(message);
			System.out.println("calling sendAskPlayerCountMessage()...");
			sendPacket(obj);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	// attempt server connection by sending a join,clientID message
	// format: message: "join,clientID"
	public void sendJoinMessage() 
	{
		try
		{
			String message = new String("join," + id.toString());
			PacketObject obj = new PacketObject(message);
			System.out.println("calling sendJoinMessage()...");
			sendPacket(obj);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	// tell server to create a ghost avatar for this client at position x,y,z
	// format:  message: "create,clientID"
	//			position: Vector3f pos
	//			rotation: Matrix3 rot
	private void sendCreateMessage(Vector3 pos, Matrix3 rot, String hatChoice)
	{
		try
		{
			String message = new String("create," + id.toString());
			PacketObject obj = new PacketObject(message, pos, rot, hatChoice);
			System.out.println("calling sendCreateMessage()...");
			sendPacket(obj);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	// tell server this client will disconnect
	// format: message: "bye,clientID"
	public void sendByeMessage()
	{
		try
		{
			String message = new String("bye," + id.toString());
			PacketObject obj = new PacketObject(message);
			System.out.println("calling sendByeMessage()...");
			sendPacket(obj);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	// send details back to server for asking client, askingClientID
	// format:  message: "dsfr,clientID,askingClientID"
	//			position: Vector3f pos
	//			rotation: Matrix3 rot
	private void sendDetailsForMessage(UUID askingClientID)
	{
		try 
		{
			String message = new String("dsfr," + id.toString() + "," + askingClientID.toString());
			PacketObject obj = new PacketObject(message, game.getAvatarPosition(), game.getAvatarRotation(), game.getPlayerNodes().get(0).getHatChoice());
			System.out.println("calling sendDetailsForMessage()...");
			sendPacket(obj);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// tell server that this client has moved/rotated
	// format:  message:  "move,clientID"
	//			position: Vector3f pos
	//			rotation: Matrix3 rot
	public void sendMoveMessage(Vector3 pos, Matrix3 rot) 
	{
		try 
		{
			String message = new String("move," + id.toString());
			PacketObject obj = new PacketObject(message, pos, rot);
			//System.out.println("calling sendMoveMessage()...");
			sendPacket(obj);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// tell sever that client is shooting
	// format: message: "shoot,clientID, pos, rot"
	public void sendShootMessage(Vector3 pos, Matrix3 rot)
	{
		try
		{
			String message = new String("shoot," + id.toString());
			PacketObject obj = new PacketObject(message, pos, rot);
			sendPacket(obj);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// tell server that client has melee'd
	// format: "melee,clientID"
	public void sendMeleeMessage()
	{
		try
		{
			String message = new String("melee," + id.toString());
			PacketObject obj = new PacketObject(message);
			sendPacket(obj);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// tell server that a player with UUID playerID has lost all health
	// format: "died,playerID"
	public void sendDeathMessage(UUID playerID)
	{
		try
		{
			String message = new String("died," + playerID.toString());
			PacketObject obj = new PacketObject(message);
			sendPacket(obj);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}