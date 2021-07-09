package DuelGameServer;

import java.io.IOException;

public class NetworkingServer 
{
	private GameServerUDP thisUDPServer;
	
	public NetworkingServer(int serverPort, String protocol)
	{
		try
		{
			if ( protocol.toUpperCase().compareTo("UDP") == 0 )
			{
				thisUDPServer = new GameServerUDP(serverPort);
			}
			else
			{
				System.out.println("No server implementation of " + protocol + " protocol! Please use UDP!");
			}
			
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		if (args.length > 1)
		{
			NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
			System.out.println("Server succesfully started on port " + args[0] + " on " + args[1] + " protocol...");
		}
	}
}
