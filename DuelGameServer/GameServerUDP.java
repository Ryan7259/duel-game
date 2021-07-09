
package DuelGameServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;
import ray.rml.Matrix3;
import ray.rml.Vector3;

public class GameServerUDP extends GameConnectionServer<UUID> 
{
	// max player limit is 2
	private final int playerLimit = 2;
	private int playerCount = 0;
	
	public GameServerUDP(int localPort) throws IOException
	{
		super(localPort, ProtocolType.UDP);
	}
	
	@Override
	public void processPacket(Object o, InetAddress senderIP, int senderPort)
	{
		PacketObject obj = (PacketObject) o;
		String message = obj.getMessage();
		//System.out.println("message: " + message);
		String[] messageTokens = message.split(",");
		Vector3 pos = obj.getPosition();
		Matrix3 rot = obj.getRotation();
		
		if ( messageTokens.length > 0 )
		{
			// case where client asks for number of players already joined
			// format: message: playerCount,clientID
			if ( messageTokens[0].compareTo("playerCount") == 0 )
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				sendPlayerCountMessage(clientID);
			}
			
			// case where server receives a JOIN message
			// format: message: join,clientID
			if ( messageTokens[0].compareTo("join") == 0 )
			{ 
				UUID clientID = UUID.fromString(messageTokens[1]);
				
				// only send join message if server isn't full
				if ( playerCount >= playerLimit )
				{
					// tell clientID that it has failed to connect to server
					sendJoinedMessage(clientID, false);
					
					System.out.println("Too many players already joined...");
				}
				else
				{
					try
					{ 
						IClientInfo ci;
						ci = getServerSocket().createClientInfo(senderIP, senderPort);
						addClient(ci, clientID);
						
						// tell clientID that it has successfully connected to the server
						sendJoinedMessage(clientID, true);
						
						System.out.println("Client: " + clientID.toString() + " has connected!");
					}
					catch ( IOException e )
					{ 
						// tell clientID that it has failed to connect to server
						sendJoinedMessage(clientID, false);
						e.printStackTrace();
					} 
				}
			}
			
			// case where server receives a CREATE message
			// format:  message: "create,clientID"
			//			position: vector3f pos
			//			rotation: matrix4 rot
			if ( messageTokens[0].compareTo("create") == 0 )
			{ 
				UUID clientID = UUID.fromString(messageTokens[1]);
				
				// tell other clients to create ghost avatar for new client, clientID
				sendCreateMessages(clientID, pos, rot, obj.getHatChoice());
				
				// ask other clients to send details to new client, clientID
				sendWantsDetailsMessages(clientID);
			}
			
			// case where server receives a BYE message
			// format: message: "bye,clientID"
			if ( messageTokens[0].compareTo("bye") == 0 )
			{ 
				UUID clientID = UUID.fromString(messageTokens[1]);
				
				// tell other clients to remove ghost avatar for disconnected clientID
				sendByeMessages(clientID);
				removeClient(clientID);
				
				System.out.println("Disconnected client: " + clientID.toString());
			}
			
			// case where server receives a DETAILS-FOR message
			// format: message: "dsfr,clientID,askingClientID"
			//		   position: Vector3 pos
			// 		   rotation: Matrix3 rot
			if ( messageTokens[0].compareTo("dsfr") == 0 )
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				UUID askingClientID = UUID.fromString(messageTokens[2]); 
				
				// send clientID's pos/rot to askingClientID
				sendDetailsMsg(clientID, askingClientID, pos, rot, obj.getHatChoice()); 
			}
			
			// case where server receives a MOVE message
			// format: message:  "move/rotate,clientID"
			//		   position: Vector3 pos
			// 		   rotation: Matrix3 rot
			if ( messageTokens[0].compareTo("move") == 0 )
			{
				UUID clientID = UUID.fromString(messageTokens[1]);
				
				// send clientID's pos/rot to other clients for updating ghost avatar
				sendMoveMessages(clientID, pos, rot);
			}
			
			// receive "shoot" from a ghost avatar's client
			// format: "shoot,clientID,pos,rot"
			if ( messageTokens[0].compareTo("shoot") == 0 )
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendShootMessages(ghostID, pos, rot);
			}
			
			// pass on successful "melee" damage to ghostID
			// format: "melee,clientID"
			if ( messageTokens[0].compareTo("melee") == 0 )
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendMeleeMessages(ghostID);
			}
			
			// receive "death" from a ghost avatar's client
			// format: "died,clientID"
			if ( messageTokens[0].compareTo("died") == 0 )
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDeathMessages(ghostID);
			}
		}
			
	}

	// tell clientID the player count already joined on server
	private void sendPlayerCountMessage(UUID clientID)
	{
		try
		{
			String message = new String("playerCount," + playerCount);
			PacketObject obj = new PacketObject(message);
			System.out.println("Sending packet in sendPlayerCountMessage()...");
			sendPacket(obj, clientID);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	// tell clientID that it has either joined or failed to join the server
	// format: message: "join,success,playerCount(before joining)" or "join,failure,playerCount"
	private void sendJoinedMessage(UUID clientID, boolean success)
	{ 
		try
		{ 
			String message = new String("join,");
			
			if ( success ) 
			{
				message += "success";
				message += "," + playerCount;
				++playerCount;
			}
			else 
			{
				message += "failure";
				message += "," + playerCount;
			}
			
			PacketObject obj = new PacketObject(message);
			System.out.println("Sending packet in sendJoinedMessage()...");
			sendPacket(obj, clientID);
		}
		catch (IOException e) 
		{ 
			e.printStackTrace(); 
		}
	}
	
	// tell other clients to create a new ghost avatar for newly connected client, ghostID
	// format: message: "create,ghostID"
	//		   position: Vector3 pos
	//		   rotation: Matrix3 rot
	private void sendCreateMessages(UUID ghostID, Vector3 pos, Matrix3 rot, String hatChoice)
	{ 
		try
		{ 
			String message = new String("create," + ghostID.toString());
			PacketObject obj = new PacketObject(message, pos, rot, hatChoice);
			System.out.println("Forwarding packet to all in sendCreateMessages()...");
			forwardPacketToAll(obj, ghostID);
		}
		catch (IOException e) 
		{ 
			e.printStackTrace();
		} 
	}
	
	// send details of ghostID's pos/rot to askingClientID's ghost avatar equivalent
	// format:  message:  "dsfr,ghostID"
	//			position: Vector3f pos
	//			rotation: Matrix3 rot
	private void sendDetailsMsg(UUID ghostID, UUID askingClientID, Vector3 pos, Matrix3 rot, String hatChoice)
	{
		try 
		{
			String message = new String("dsfr," + ghostID.toString());
			PacketObject obj = new PacketObject(message, pos, rot, hatChoice);
			System.out.println("Sending packet in sendDetailsMsg()...");
			sendPacket(obj, askingClientID);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// send a "wants details for" an askingClientID to all other clients
	// format: message: "wsds,askingClientID"
	private void sendWantsDetailsMessages(UUID askingClientID)
	{
		try 
		{
			String message = new String("wsds," + askingClientID.toString());
			PacketObject obj = new PacketObject(message);
			System.out.println("Forwarding packet to all in sendWantsDetailsMessages()...");
			forwardPacketToAll(obj, askingClientID);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// send clientID's movement update to other clients
	// format:  message:  "move,clientID"
	//			position: Vector3f pos
	//			rotation: Matrix3 rot
	private void sendMoveMessages(UUID ghostID, Vector3 pos, Matrix3 rot)
	{
		try 
		{
			String message = new String("move," + ghostID.toString());
			PacketObject obj = new PacketObject(message, pos, rot);
			forwardPacketToAll(obj, ghostID);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// tell other clients that ghostID has melee'd from their position
	// format: "melee, ghostID"
	private void sendMeleeMessages(UUID ghostID)
	{
		try 
		{
			String message = new String("melee," + ghostID.toString());
			PacketObject obj = new PacketObject(message);
			forwardPacketToAll(obj, ghostID);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// tell other clients that ghostID has fired bullet from their position
	// format: "shoot,ghostID"
	private void sendShootMessages(UUID ghostID, Vector3 pos, Matrix3 rot)
	{
		try 
		{
			String message = new String("shoot," + ghostID.toString());
			PacketObject obj = new PacketObject(message, pos, rot);
			forwardPacketToAll(obj, ghostID);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// tell other clients that ghostID has disconnected
	// format: message: "bye,ghostID"
	private void sendByeMessages(UUID ghostID)
	{
		try 
		{
			--playerCount;
			
			String message = new String("bye," + ghostID.toString());
			PacketObject obj = new PacketObject(message);
			System.out.println("Forwarding packet to all in sendByeMessages()...");
			forwardPacketToAll(obj, ghostID);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	// tell other clients that ghostID has lost all health
	// format: "died, ghostID"
	public void sendDeathMessages(UUID ghostID)
	{
		try
		{
			String message = new String("died," + ghostID.toString());
			PacketObject obj = new PacketObject(message);
			forwardPacketToAll(obj, ghostID);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}
