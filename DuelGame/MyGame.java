package DuelGame;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import MyGameEngine.GhostAvatar;
import MyGameEngine.GunFire;
import MyGameEngine.MeleeAction;
import MyGameEngine.MoveXAction;
import MyGameEngine.MoveYAction;
import MyGameEngine.NPCController;
import MyGameEngine.Player;
import MyGameEngine.ProtocolClient;
import MyGameEngine.QuitGameAction;
import MyGameEngine.SendCloseConnectionPacketAction;
import MyGameEngine.ShootAction;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import ray.audio.AudioManagerFactory;
import ray.audio.AudioResource;
import ray.audio.AudioResourceType;
import ray.audio.IAudioManager;
import ray.audio.Sound;
import ray.audio.SoundType;
import ray.input.GenericInputManager;
import ray.input.InputManager;
import ray.input.action.Action;
import ray.networking.IGameConnection.ProtocolType;
import ray.physics.PhysicsEngine;
import ray.physics.PhysicsEngineFactory;
import ray.physics.PhysicsObject;
import ray.rage.Engine;
import ray.rage.asset.texture.Texture;
import ray.rage.asset.texture.TextureManager;
import ray.rage.game.VariableFrameRateGame;
import ray.rage.rendersystem.RenderSystem;
import ray.rage.rendersystem.RenderWindow;
import ray.rage.rendersystem.Renderable.Primitive;
import ray.rage.rendersystem.Viewport;
import ray.rage.rendersystem.states.*;
import ray.rage.scene.SceneManager;
import ray.rage.scene.SceneNode;
import ray.rage.scene.SceneObject;
import ray.rage.scene.SkeletalEntity;
import ray.rage.scene.SkeletalEntity.EndType;
import ray.rage.scene.SkyBox;
import ray.rage.scene.Tessellation;
import ray.rage.util.Configuration;
import ray.rage.scene.Camera;
import ray.rage.scene.Entity;
import ray.rage.scene.Light;
import ray.rage.scene.Camera.Frustum.Projection;
import ray.rml.Degreef;
import ray.rml.Matrix3;
import ray.rml.Matrix3f;
import ray.rml.Matrix4;
import ray.rml.Matrix4f;
import ray.rml.Vector3;
import ray.rml.Vector3f;

public class MyGame extends VariableFrameRateGame implements MouseListener, MouseMotionListener
{
	private RenderSystem rs;
	private InputManager im;
	private RenderWindow rw;
	private SceneManager sm;
	protected ScriptEngine jsEngine;
	protected File scriptFile;
	
	private Camera camera;
	private SceneNode cameraN, avatarN, ghostAvN;
	private Vector<Player> playerNodes; // vector of animatable player nodes to update
	
	private float sensitivity = 1.0f;
	
	private static final String SKYBOX_NAME = "SkyBox";
	public Vector3 worldUp = Vector3f.createFrom(0.0f, 1.0f, 0.0f);
	
	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient = null;
	private boolean isClientConnected = false;
	private Vector<UUID> gameObjectsToRemove;

	public MyGame(String serverAddr, int sPort, String protocol)
	{
		super();
		
		this.serverAddress = serverAddr;
		this.serverPort = sPort;
		
		if (protocol.toUpperCase().compareTo("UDP") == 0)
		{
			this.serverProtocol = ProtocolType.UDP;
		}
		
		System.out.println("press ESC to exit\n");
	}
	
	public static void main(String[] args)
	{
        MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
        try 
        {
            game.startup();
            game.run();
        } 
        catch (Exception e) 
        {
            e.printStackTrace(System.err);
        } 
        finally 
        {
        	game.getProtocolClient().sendByeMessage();
            game.shutdown();
            game.exit();
        }
	}
	
	public PhysicsEngine getPhysEng()
	{
		return this.physicsEngine;
	}
	
	public Vector<Player> getPlayerNodes()
	{
		return this.playerNodes;
	}
	
	public void addGhostAvatarToGameWorld(GhostAvatar avatar) throws IOException
	{
		if ( avatar != null )
		{
	        // load skeletal entity – in this case it is an avatar
	        SkeletalEntity ghostSE = sm.createSkeletalEntity("ghostSkEnt"+avatar.getId().toString(), "cowboyHat.rkm", "cowboyHat.rks");
	        Texture tex = sm.getTextureManager().getAssetByPath("cowboyHat.png");	
	        TextureState tstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
	        tstate.setTexture(tex);
	        ghostSE.setRenderState(tstate);
	        ghostSE.loadAnimation("idleAnim", "idle.rka");
	        ghostSE.loadAnimation("runAnim", "run.rka");
	        ghostSE.loadAnimation("shootAnim", "shoot.rka");
	        ghostSE.loadAnimation("meleeAnim", "melee.rka");
	        ghostSE.loadAnimation("deathAnim", "death.rka");
	        
	        SkeletalEntity ghostNoHatSE = sm.createSkeletalEntity("ghostNoHatSE", "cowboy.rkm" , "cowboy.rks");
		    Texture ngtex = sm.getTextureManager().getAssetByPath("cowboy.png");
		    TextureState ngtstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
	        ngtstate.setTexture(ngtex);
	        ghostNoHatSE.setRenderState(ngtstate);
	        ghostNoHatSE.loadAnimation("idleAnim", "idle.rka");
	        ghostNoHatSE.loadAnimation("runAnim", "run.rka");
	        ghostNoHatSE.loadAnimation("shootAnim", "shoot.rka");
	        ghostNoHatSE.loadAnimation("meleeAnim", "melee.rka");
	        ghostNoHatSE.loadAnimation("deathAnim", "death.rka");
	        
	        if ( avatar.getHatChoice().equals("cowboyHat") )
	        {
	        	ghostNoHatSE.setVisible(false);
	        }
	        else
	        {
	        	ghostSE.setVisible(false);
	        }
	        
			SceneNode ghostN = sm.getRootSceneNode().createChildSceneNode(avatar.getId().toString());
			ghostN.attachObject(ghostSE);
			ghostN.attachObject(ghostNoHatSE);
			ghostN.setLocalPosition(avatar.getInitPos());
			ghostN.setLocalRotation(avatar.getInitRot());
			
			
			// init avatar player class info
			avatar.setLastPos(avatar.getInitPos());
			avatar.setNode(ghostN);
			avatar.setEntity(ghostSE);
			
			getPlayerNodes().add(avatar);
			
			playIdle(avatar);
		}
	}
	
	public void removeGhostAvatarFromGameWorld(GhostAvatar avatar)
	{ 
		if (avatar != null) 
		{
			gameObjectsToRemove.add(avatar.getId());
			getPlayerNodes().remove(avatar);
		}
	}
	
	public ProtocolClient getProtocolClient()
	{
		return this.protClient;
	}
	
	public Vector3 getAvatarPosition()
	{
		return ghostAvN.getWorldPosition();
	}
	
	public Matrix3 getAvatarRotation()
	{
		return ghostAvN.getWorldRotation();
	}
	
	public boolean isClientConnected() 
	{
		return isClientConnected;
	}

	public void setIsClientConnected(boolean isClientConnected) 
	{
		this.isClientConnected = isClientConnected;
	}
	
	public Camera getCamera()
	{
		return this.camera;
	}
	
	public SceneNode getCameraN()
	{
		return this.cameraN;
	}
	public SceneNode getAvatarN()
	{
		return this.avatarN;
	}
	public SceneNode getGhostAvN()
	{
		return this.ghostAvN;
	}
	
	public void takeDmg(Player p, String weapon)
	{
		int newHP = p.getHealth();
		
		if ( weapon.equals("bullet") )
		{
			newHP -= 25;
		}
		else if ( weapon.equals("melee") )
		{	
			newHP -= 15;
		}
		
		if ( newHP < 0 )
		{
			p.setHealth(0);
		}
		else
		{
			p.setHealth(newHP);
		}
		
		hurtSound.play();
		impactSound.play();
	}
	
	public void killPlayer(UUID id)
	{
		for (Player p : getPlayerNodes())
		{
			if ( p.getId().equals(id) )
			{
				//System.out.println("killing " + p.getNode().getName());
				playDeath(p);
				deathSound.play();
				p.setHealth(0);
			}
		}
	}

	public void meleeFrom(Player p)
	{
		//System.out.println("playing melee from " + p.getEntity().getName());
		playMelee(p);
		swingSound.play();
		
		float dist;
		
		// player is meleeing
		if (p.equals(getPlayerNodes().get(0)))
		{
			// tell other player that you are meleeing for animation purposes and also to test distance of attempted attack
			if ( isClientConnected() && getPlayersJoined() > 0 )
			{
				//System.out.println("sending melee msg from " + p.getEntity().getName());
				getProtocolClient().sendMeleeMessage();
			}
			
			// calculate distance from player's attempt to enemy
			dist = p.getNode().getWorldPosition().sub(getPlayerNodes().get(1).getNode().getWorldPosition()).length();
			// System.out.println("melee dist: " + dist);
			if (dist <= 25.0f)
			{
				// apply dmg to either ghost or npc 
				takeDmg(getPlayerNodes().get(1), "melee");
			}
		}
		
		// npc or other player is meleeing
		else
		{
			dist = p.getNode().getWorldPosition().sub(getPlayerNodes().get(0).getNode().getWorldPosition()).length();
			//System.out.println("melee dist: " + dist);
			// test dist of player or npc attack
			if (dist <= 25.0f)
			{
				takeDmg(getPlayerNodes().get(0), "melee");
			}
		}
	}
	
	Vector<GunFire> gunFires;
	public void shootFrom(Player p, Vector3 pos, Matrix3 rot)
	{
		try 
		{
			playShoot(p);
			
			shootSound.setLocation(getCameraN().getWorldPosition());
			shootSound.play();

			int bId = getPhysEng().nextUID();
			Entity bulletE = sm.createEntity("bulletEntity"+bId, "sphere.obj");
			bulletE.setPrimitive(Primitive.TRIANGLES);			
			SceneNode bulletN = sm.getRootSceneNode().createChildSceneNode("bulletNode"+bId);
			bulletN.attachObject(bulletE);
			
			bulletN.setLocalPosition(pos);
			bulletN.setLocalRotation(rot);
			bulletN.moveForward(5.0f);
			bulletN.scale(0.5f, 0.5f, 0.5f);
			
			Light muzzleFlash = sm.createLight("muzzleFlash"+bId, Light.Type.POINT);
			muzzleFlash.setAmbient(new Color(0.1f, 0.1f, 0.1f));
			muzzleFlash.setRange(0.15f);
			SceneNode mFN = sm.getRootSceneNode().createChildSceneNode("muzzleFlashNode"+bId);
			mFN.attachObject(muzzleFlash);
			mFN.setLocalPosition(bulletN.getWorldPosition());
			
			PhysicsObject bulletP = getPhysEng().addSphereObject(
					bId, 
					10.0f, 
					toDoubleArray(bulletN.getLocalTransform().toFloatArray()), 
					1);
			bulletN.setPhysicsObject(bulletP);
			createGroundPhysObj(bulletN);
			
			gunFires.add(new GunFire(sm, bulletN, mFN, p));
			
			Vector3 fd = bulletN.getWorldForwardAxis();
			float mult = 500.0f;
			float[] v = {fd.x()*mult, fd.y()*mult, fd.z()*mult};
			bulletP.setLinearVelocity(v);
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}
	}
	
	private float attackCooldown = 0.0f;
	public float getAttackCooldown()
	{
		return this.attackCooldown;
	}
	
	public void setAttackCooldown()
	{
		this.attackCooldown = 250.0f;
	}
	
	public void playDeath(Player p)
	{
		for ( SceneObject s : p.getNode().getAttachedObjects() )
		{
			SkeletalEntity e = (SkeletalEntity) s;
			e.stopAnimation();
			e.playAnimation("deathAnim", 0.25f, EndType.PAUSE, 2);
		}
		
		p.setCurrentAnim("death");
	}
	
	public void playMelee(Player p)
	{
		for ( SceneObject s : p.getNode().getAttachedObjects() )
		{
			SkeletalEntity e = (SkeletalEntity) s;
			e.stopAnimation();
			e.playAnimation("meleeAnim", 0.25f, EndType.PAUSE, 2);
		}

		p.setCurrentAnim("melee");
	}
	public void playShoot(Player p)
	{
		for ( SceneObject s : p.getNode().getAttachedObjects() )
		{
			SkeletalEntity e = (SkeletalEntity) s;
			e.stopAnimation();
			e.playAnimation("shootAnim", 0.25f, EndType.PAUSE, 2);
		}
		
		p.setCurrentAnim("shoot");
	}
	public void playRun(Player p)
	{
		for ( SceneObject s : p.getNode().getAttachedObjects() )
		{
			SkeletalEntity e = (SkeletalEntity) s;
			e.stopAnimation();
			e.playAnimation("runAnim", 0.25f, EndType.LOOP, 0);
		}
		
		p.setCurrentAnim("run");
	}
	public void playIdle(Player p)
	{
		for ( SceneObject s : p.getNode().getAttachedObjects() )
		{
			SkeletalEntity e = (SkeletalEntity) s;
			e.stopAnimation();
			e.playAnimation("idleAnim", 0.15f, EndType.LOOP, 0);
		}
		
		p.setCurrentAnim("idle");
	}
	
	public int getPlayersJoined()
	{
		return this.playersJoined;
	}
	public void setPlayersJoined(int count)
	{
		this.playersJoined = count;
	}
	
	private void setupNetworking()
	{
		gameObjectsToRemove = new Vector<UUID>();
		setIsClientConnected(false);
		
		try
		{
			this.protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		}
		catch(UnknownHostException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		if ( this.protClient == null )
		{
			System.out.println("Missing protocol host");
		}
		else
		{
			// ask client protocol to send initial join msg to server, with a unique identifier for this client
			this.protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime)
	{
		// Process packets received by the client from the server
		if ( getProtocolClient() != null )
		{
			getProtocolClient().processPackets();
			
			// only update for own avatar
			for ( GhostAvatar g : getProtocolClient().getGhosts())
			{
				for ( SceneObject s : g.getNode().getAttachedObjects() )
				{
					SkeletalEntity e = (SkeletalEntity) s;
					e.update();
				}
				
				// change anim and sound states if necessary
				updateMovement(g);
			}
		}
		
		// remove ghost avatars for players who have left the game
		Iterator<UUID> it = gameObjectsToRemove.iterator();
		
		while ( it.hasNext() )
		{
			sm.destroySceneNode(it.next().toString());
		}
		
		gameObjectsToRemove.clear();
	}
	
	protected PhysicsEngine physicsEngine;
	private boolean running = false;
	private void initPhysicsSystem()
	{ 
		String engine = "ray.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = {0, -100f, 0};
		physicsEngine = PhysicsEngineFactory.createPhysicsEngine(engine);
		physicsEngine.initSystem();
		physicsEngine.setGravity(gravity);
	}
	
	public void createGroundPhysObj(SceneNode n)
	{
		Vector3 worldPos = Vector3f.createFrom(
				n.getWorldPosition().x(), 
				tessE.getWorldHeight(n.getWorldPosition().x(), n.getWorldPosition().z()), 
				n.getWorldPosition().z());
		Matrix4 mat = Matrix4f.createFrom(Matrix3f.createIdentityMatrix(), worldPos);
		double[] tf = toDoubleArray(mat.toFloatArray());
		float[] size = {1,1,1};
		PhysicsObject nPG = physicsEngine.addBoxObject(
				physicsEngine.nextUID(),
				0,
				tf, 
				size);
		
		groundColliders.put(n, nPG);
	}
	
	private void createHatObj(Vector3 pos, float x, float y, float z)
	{
		try
		{
			int uid = physicsEngine.nextUID();
			Entity hatE = sm.createEntity("hatEntity"+uid, "hatObj.obj");
	        hatE.setPrimitive(Primitive.TRIANGLES);
	        SceneNode hatN = sm.getRootSceneNode().createChildSceneNode("hatNode"+uid);
	        hatN.attachObject(hatE);
	        
	        Vector3 offsetPos = Vector3f.createFrom(pos.x(), pos.y()+7.0f, pos.z());
	        hatN.setLocalPosition(offsetPos);
	        
			float mass = 50.0f;
			double[] temptf;
			float[] size;
			Matrix4 localTF;
	        
			
			localTF = hatN.getLocalTransform();
			temptf = toDoubleArray(localTF.toFloatArray());
			size = new float[]{1,1,1};
			PhysicsObject hatP = physicsEngine.addBoxObject(
					uid,
					mass, 
					temptf, 
					size);
			hatP.setDamping(0.99f, 0.99f);
			hatP.setFriction(100f);
			hatN.setPhysicsObject(hatP);
			
			createGroundPhysObj(hatN);
			
			hatP.setLinearVelocity(new float[] {x, y, z});
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private HashMap<SceneNode, PhysicsObject> groundColliders;
	private void createRagePhysicsWorld()
	{ 
		groundColliders = new HashMap<SceneNode, PhysicsObject>();
	}
	
	IAudioManager audioMgr;
	Sound impactSound, stepSound, shootSound, swingSound, hurtSound, deathSound, wind1Sound;
	
	public void initAudio(SceneManager sm)
	{ 
		AudioResource impactRes, stepRes, shootRes, swingRes, hurtRes, deathRes, wind1Res;
		audioMgr = AudioManagerFactory.createAudioManager("ray.audio.joal.JOALAudioManager");
		
		if (!audioMgr.initialize())
		{ 
			System.out.println("Audio Manager failed to initialize!");
			return;
		}
		
		impactRes = audioMgr.createAudioResource("impact.wav", AudioResourceType.AUDIO_SAMPLE);
		stepRes = audioMgr.createAudioResource("sand_step.wav", AudioResourceType.AUDIO_SAMPLE);
		shootRes = audioMgr.createAudioResource("shoot.wav", AudioResourceType.AUDIO_SAMPLE);
		swingRes = audioMgr.createAudioResource("swing.wav", AudioResourceType.AUDIO_SAMPLE);
		hurtRes = audioMgr.createAudioResource("hurt.wav", AudioResourceType.AUDIO_SAMPLE);
		deathRes = audioMgr.createAudioResource("death.wav", AudioResourceType.AUDIO_SAMPLE);
		wind1Res = audioMgr.createAudioResource("wasteland_wind.wav", AudioResourceType.AUDIO_SAMPLE);
		
		
		impactSound = new Sound(impactRes, SoundType.SOUND_EFFECT, 25, false);
		stepSound = new Sound(stepRes, SoundType.SOUND_EFFECT, 10, true);
		shootSound = new Sound(shootRes, SoundType.SOUND_EFFECT, 10, false);
		swingSound = new Sound(swingRes, SoundType.SOUND_EFFECT, 25, false);
		hurtSound = new Sound(hurtRes, SoundType.SOUND_EFFECT, 25, false);
		deathSound = new Sound(deathRes, SoundType.SOUND_EFFECT, 25, false);
		wind1Sound = new Sound(wind1Res, SoundType.SOUND_EFFECT, 10, true);
		
		impactSound.initialize(audioMgr);
		stepSound.initialize(audioMgr);
		shootSound.initialize(audioMgr);
		swingSound.initialize(audioMgr);
		hurtSound.initialize(audioMgr);
		deathSound.initialize(audioMgr);
		wind1Sound.initialize(audioMgr);
		
		
		setSoundOpts(impactSound);
		setSoundOpts(stepSound);
		setSoundOpts(shootSound);
		setSoundOpts(swingSound);
		setSoundOpts(hurtSound);
		setSoundOpts(deathSound);
		setSoundOpts(wind1Sound);
		
		wind1Sound.setLocation(getCameraN().getWorldPosition());
		setEarParameters(sm);
		wind1Sound.play();
	}
	
	public void setSoundOpts(Sound s)
	{
		s.setMaxDistance(1000.0f);
		s.setMinDistance(0.0f);
		s.setRollOff(1.0f);
	}
	
	public void setEarParameters(SceneManager sm)
	{ 
		audioMgr.getEar().setLocation(getCameraN().getWorldPosition());
		audioMgr.getEar().setOrientation(getCameraN().getWorldForwardAxis(), Vector3f.createFrom(0,1,0));
	}
	
	@Override
	protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) 
	{
		rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
	}
	
	@Override
	protected void setupCameras(SceneManager s, RenderWindow r) 
	{
		sm = s;
		rw = r;
		rs = sm.getRenderSystem();
		
		camera = sm.createCamera("MainCamera", Projection.PERSPECTIVE);
		rw.getViewport(0).setCamera(camera);
		
		camera.setRt((Vector3f)Vector3f.createFrom(1.0f, 0.0f, 0.0f));
		camera.setUp((Vector3f)Vector3f.createFrom(0.0f, 1.0f, 0.0f));
		camera.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, -1.0f));
		camera.setPo((Vector3f)Vector3f.createFrom(0.0f, 1.0f, 0.0f));
		
		cameraN = sm.getRootSceneNode().createChildSceneNode("MainCameraNode");
		cameraN.attachObject(camera);
		
        camera.setMode('n');
	}
	
	Tessellation tessE;
	SceneNode tessN;
	@Override
	protected void setupScene(Engine eng, SceneManager sm) throws IOException 
	{
    	// setup ambient and sun light
        sm.getAmbientLight().setIntensity(new Color(0.9f, 0.8f, 0.8f));
        
        Light sunLight = sm.createLight("sunLight", Light.Type.DIRECTIONAL);
        sunLight.setDiffuse(new Color(0.9f, 0.8f, 0.8f));
        sunLight.setSpecular(new Color(0f, 0f, 0f));
        SceneNode sunNode = sm.getRootSceneNode().createChildSceneNode("sunNode");
        sunNode.attachObject(sunLight);
        sunNode.moveBackward(20.0f);
        sunNode.moveUp(20.0f);
        sunNode.moveLeft(10.0f);
        
        // setup player and attach it to a node
        // load skeletal entity – in this case it is an avatar
        SkeletalEntity cowboySE = sm.createSkeletalEntity("cowboySkEnt", "cowboyFirstPerson.rkm", "cowboyFirstPerson.rks");
	    Texture tex = sm.getTextureManager().getAssetByPath("cowboy.png");
	    TextureState tstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
        tstate.setTexture(tex);
        cowboySE.setRenderState(tstate);
        cowboySE.loadAnimation("idleAnim", "idle.rka");
        cowboySE.loadAnimation("runAnim", "run.rka");
        cowboySE.loadAnimation("shootAnim", "shoot.rka");
        cowboySE.loadAnimation("meleeAnim", "melee.rka");
        cowboySE.loadAnimation("deathAnim", "death.rka");
        
        avatarN = sm.getRootSceneNode().createChildSceneNode("avatarNode");
        avatarN.attachObject(cowboySE);
        
        // stores all animatable player to update later
        playerNodes = new Vector<Player>();
        Player p = new Player(avatarN);
        p.setId(UUID.randomUUID());
        playIdle(p);
        getPlayerNodes().add(p);
        
        // first person visual of arms + gun of player
        Matrix3 matRot = Matrix3f.createRotationFrom(Degreef.createFrom(-90.0f), Vector3f.createFrom(0.0f, 1.0f, 0.0f));
        avatarN.setLocalRotation(matRot.mult(avatarN.getWorldRotation()));
        avatarN.moveUp(7.0f);
        avatarN.moveBackward(150.0f);
        p.setLastPos(avatarN.getWorldPosition());
        
        // keep camera separate for dead zone camera movement
        //cameraN.setLocalRotation(matRot.mult(cameraN.getWorldRotation()));
		cameraN.moveUp(10.0f);
		cameraN.moveBackward(20.0f);
        
        // create a invisible ghost avatar used to keep rotations locked around yaw axis visually for other clients
        SkeletalEntity ghostAvSE = sm.createSkeletalEntity("ghostSkEnt", "cowboy.rkm", "cowboy.rks");
        ghostAvSE.setVisible(false);
        ghostAvN = sm.getRootSceneNode().createChildSceneNode("ghostAvNode");
        ghostAvN.attachObject(ghostAvSE);
        //ghostAvN.setLocalRotation(matRot.mult(ghostAvN.getWorldRotation()));
        //ghostAvN.moveUp(7.0f);
        //ghostAvN.moveBackward(20.0f);
        
        // setup initial positions for resetting positions on round restarts
        initPosP1 = avatarN.getWorldPosition();
        initRotP1 = avatarN.getWorldRotation();
        matRot = Matrix3f.createRotationFrom(Degreef.createFrom(180.0f), Vector3f.createFrom(0.0f, 1.0f, 0.0f));
        initPosP2 = Vector3f.createFrom(initPosP1.x()-150.0f, initPosP1.y(), initPosP1.z());
        initRotP2 = matRot.mult(avatarN.getWorldRotation());
        
		initMouseMode();
        
        // set up sky box
        Configuration conf = eng.getConfiguration();
        TextureManager tm = getEngine().getTextureManager();
        tm.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path"));
        Texture front = tm.getAssetByPath("front.png");
        Texture back = tm.getAssetByPath("back.png");
        Texture left = tm.getAssetByPath("left.png");
        Texture right = tm.getAssetByPath("right.png");
        Texture top = tm.getAssetByPath("top.png");
        Texture bottom = tm.getAssetByPath("bottom.png");
        tm.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));
        // cubemap textures are flipped upside-down.
        // All textures must have the same dimensions, so any image’s
        // heights will work since they are all the same height
        AffineTransform xform = new AffineTransform();
        xform.translate(0, front.getImage().getHeight());
        xform.scale(1d, -1d);
        front.transform(xform);
        back.transform(xform);
        left.transform(xform);
        right.transform(xform);
        top.transform(xform);
        bottom.transform(xform);
        
        SkyBox sb = sm.createSkyBox(SKYBOX_NAME);
        sb.setTexture(front, SkyBox.Face.FRONT);
        sb.setTexture(back, SkyBox.Face.BACK);
        sb.setTexture(left, SkyBox.Face.LEFT);
        sb.setTexture(right, SkyBox.Face.RIGHT);
        sb.setTexture(top, SkyBox.Face.TOP);
        sb.setTexture(bottom, SkyBox.Face.BOTTOM);
        sm.setActiveSkyBox(sb);
		
		// 2^patches: min=5, def=7, warnings start at 10
		tessE = sm.createTessellation("tessE", 7);
		
		// subdivisions per patch: min=0, try up to 32
		tessE.setSubdivisions(16f);
		
		tessN = sm.getRootSceneNode().createChildSceneNode("tessN");
		tessN.attachObject(tessE);
		
		tessN.scale(1000, 750, 1000);
		tessE.setHeightMap(this.getEngine(), "heightmap.png");
		tessE.setTexture(this.getEngine(), "dirt_big.jpg");
		
		/*
		tessE.getTextureState().setWrapMode(WrapMode.REPEAT_MIRRORED);
		tessE.setHeightMapTiling(4);
		tessE.setTextureTiling(4);
		tessE.setNormalMapTiling(4);;
		*/
		
		// prepare script engine
		ScriptEngineManager factory = new ScriptEngineManager();
		jsEngine = factory.getEngineByName("js");
		scriptFile = new File("setup.js");
		this.runScript();
		getPlayerNodes().get(0).setHatChoice((String)jsEngine.get("cowboyModel"));
		System.out.println("hat: " + getPlayerNodes().get(0).getHatChoice());
		this.sensitivity = (float)(double)jsEngine.get("sensitivity");
		
		// prepare physics engine
		initPhysicsSystem();
		createRagePhysicsWorld();
		running = true;
		gunFires = new Vector<GunFire>();
		
        if ( !isClientConnected() )
        {
        	setupNetworking();
        }
        
		im = new GenericInputManager();
		setupInputs(sm);
		
		initAudio(sm);
	}
	
	private Vector3 initPosP1, initPosP2;
	private Matrix3 initRotP1, initRotP2;
	public void resetPlayerPositions()
	{
		for ( Player p : getPlayerNodes() )
		{
			Vector3 pos = null;
			Matrix3 rot = null;
			
			// set position of client depending on if they are 1st or 2nd player
			if ( p.equals(getPlayerNodes().get(0)) )
			{
				// if client is 2nd player, set to 2nd init pos & rot
				if ( getProtocolClient().isJoinedAsP2() )
				{
					pos = initPosP2;
					rot = initRotP2;
				}
				// else set client to 1st init pos & rot
				else
				{
					pos = initPosP1;
					rot = initRotP1;
				}
				
				getAvatarN().setLocalPosition(pos);
				getAvatarN().setLocalRotation(rot);
				getCameraN().setLocalPosition(pos);
				getCameraN().setLocalRotation(rot);
				getGhostAvN().setLocalPosition(pos);
				getGhostAvN().setLocalRotation(rot);
				updateVerticalPosition(getCameraN(), 7.0f);
				updateVerticalPosition(getAvatarN(), 7.0f);
				updateVerticalPosition(getGhostAvN(), 0.0f);
			}
			
			// set position & rotation of other players
			else
			{
				// if client is 2nd player, set other player to 1st init pos & rot
				if ( getProtocolClient().isJoinedAsP2() )
				{
					pos = initPosP1;
					rot = initRotP1;
				}
				// else set other player to 2nd init pos & rot
				else
				{
					pos = initPosP2;
					rot = initRotP2;
				}
				
				p.getNode().setLocalPosition(pos);
				p.getNode().setLocalRotation(rot);
			}
		}
	}
	
	private int playersJoined = 0;
	private int playerScore = 0;
	private int enemyScore = 0;
	private int maxScore = 3;
	public void initRound()
	{
		// clear gun flash lights if round ended before it could be deleted
		for ( GunFire g : gunFires )
		{
			g.destroyLights();
		}
		
		//clear any physics objects from map
		for ( SceneNode p : groundColliders.keySet() )
		{
			getPhysEng().removeObject(p.getPhysicsObject().getUID());
			sm.destroySceneNode(p);
		}
		
		groundColliders.clear();
		gunFires.clear();
		
		// reset round timer values
		roundFreezeTime = maxFreezeTime;
		totalElapsedTimeMillis = 0.0f;
		totalElapsedSec = 0;
		newElapsedSec = 0;
	}
	
	public void initGame()
	{
		playerScore = 0;
		enemyScore = 0;
		gameStartFreezeTime = maxFreezeTime;
		initRound();
	}
	
	/*
	public void keyPressed(KeyEvent e)
	{ 
		switch (e.getKeyCode())
		{ 
			case KeyEvent.VK_SPACE:
				getPlayerNodes().get(1).setHealth(0);
			break;
		}
		super.keyPressed(e);
	}
	*/
	
	// actions for inputs
	private Action quitGameAction, moveYAction, moveXAction, sendCloseConnPackAction, shootAction, meleeAction;
	protected void setupInputs(SceneManager sm)
	{
		sendCloseConnPackAction = new SendCloseConnectionPacketAction(this);
		quitGameAction = new QuitGameAction(this);
		moveYAction = new MoveYAction(this);
		moveXAction = new MoveXAction(this);
		shootAction = new ShootAction(this);
		meleeAction = new MeleeAction(this);
		
    	ArrayList<Controller> controllers = im.getControllers();
    	  for (Controller c : controllers)
    	  { 
    		  
    		if ( c.getType() == Controller.Type.KEYBOARD )
    	    { 
    	    	im.associateAction(c, 
    	    			Component.Identifier.Key.BACK,
    	    			sendCloseConnPackAction,
    	    			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    	    	im.associateAction(c, 
    	    			Component.Identifier.Key.ESCAPE,
    	    			quitGameAction,
    	    			InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    	    	
    	    	im.associateAction(c, 
    	    			Component.Identifier.Key.W,
    	    			moveYAction,
    	    			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	    	im.associateAction(c, 
    	    			Component.Identifier.Key.A,
    	    			moveXAction,
    	    			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	    	im.associateAction(c, 
    	    			Component.Identifier.Key.S,
    	    			moveYAction,
    	    			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	    	im.associateAction(c, 
    	    			Component.Identifier.Key.D,
    	    			moveXAction,
    	    			InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    	    }
    		
    		else if ( c.getType() == Controller.Type.MOUSE )
    		{
    			
    			im.associateAction(c, 
    					Component.Identifier.Button.LEFT, 
    					shootAction, 
    					InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    			im.associateAction(c, 
    					Component.Identifier.Button.RIGHT, 
    					meleeAction, 
    					InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    		}
    		
    		/*
    	    else if ( c.getType() == Controller.Type.GAMEPAD )
    	    { 
    	    	
    	    }
    		*/
    	  }
	}
	
	private int maxFreezeTime = 3;
	private int roundFreezeTime = 0, gameStartFreezeTime = 0;
	private float totalElapsedTimeMillis = 0.0f;
	private int newElapsedSec = 0, totalElapsedSec = 0;
	private String hudStr, scoreStr, winStr, roundWinStr;
	private NPCController npcCtrl = null;
	private SkeletalEntity npcSE = null, npcNoHatSE = null;
	
	@Override
	protected void update(Engine eng) 
	{
		rs = eng.getRenderSystem();
		
		// check if player left, setup NPC if so
		if ( getPlayersJoined() == 0 )
		{
			if ( npcSE == null && npcNoHatSE == null )
			{
				try
				{
					npcSE = sm.createSkeletalEntity("npcSkEnt", "cowboyHat.rkm", "cowboyHat.rks");
				    Texture tex = sm.getTextureManager().getAssetByPath("cowboyHat.png");
				    TextureState tstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
			        tstate.setTexture(tex);
			        npcSE.setRenderState(tstate);
					npcSE.loadAnimation("idleAnim", "idle.rka");
					npcSE.loadAnimation("runAnim", "run.rka");
					npcSE.loadAnimation("shootAnim", "shoot.rka");
					npcSE.loadAnimation("meleeAnim", "melee.rka");
					npcSE.loadAnimation("deathAnim", "death.rka");
					
					npcNoHatSE = sm.createSkeletalEntity("npcNoHatSE", "cowboy.rkm" , "cowboy.rks");
				    Texture ntex = sm.getTextureManager().getAssetByPath("cowboy.png");
				    TextureState ntstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
			        ntstate.setTexture(ntex);
			        npcNoHatSE.setRenderState(ntstate);
			        npcNoHatSE.loadAnimation("idleAnim", "idle.rka");
			        npcNoHatSE.loadAnimation("runAnim", "run.rka");
			        npcNoHatSE.loadAnimation("shootAnim", "shoot.rka");
			        npcNoHatSE.loadAnimation("meleeAnim", "melee.rka");
			        npcNoHatSE.loadAnimation("deathAnim", "death.rka");
				}
				catch ( IOException e )
				{
					e.printStackTrace();
				}
			}
			// create npc controller if null, initialize npc, and restart game if player leaves
			if ( npcCtrl == null )
			{
		        npcCtrl = new NPCController(this, getPlayerNodes().get(0));
		        npcCtrl.start(npcSE, npcNoHatSE);
		        
		        initGame();
			}
		}
		// if player has joined, remove npc and reset game
		else if ( getPlayersJoined() == 1 && npcCtrl != null )
		{
			getPlayerNodes().remove(npcCtrl.getNPC());
			sm.destroySceneNode(npcCtrl.getNPC().getNode());
			npcCtrl = null;
			
			initGame();
		}
		
		scoreStr = Integer.toString(playerScore) + " : " + Integer.toString(enemyScore);
		rs.setHUD2(scoreStr, rw.getViewport(0).getActualScissorWidth()/2, rw.getViewport(0).getActualScissorHeight() - 30);
		
		// check game score and reset if necessary
		if ( (playerScore+enemyScore) == maxScore || (playerScore == 2) || (enemyScore == 2) )
		{
			// end of game, allow movement of player if alive
			if ( getPlayerNodes().get(0).getHealth() > 0 )
			{
				im.update(eng.getElapsedTimeMillis());
			}
			
			// player wins
			if ( playerScore > enemyScore )
			{
				if ( getProtocolClient().isJoinedAsP2() )
				{
					winStr = "Player 2 (You)";
				}
				else
				{
					winStr = "Player 1 (You)";
				}
			}
			// other player or npc wins
			else
			{
				if ( getPlayersJoined() == 0 )
				{
					winStr = "NPC";
				}
				else if ( getProtocolClient().isJoinedAsP2() )
				{
					winStr = "Player 1";
				}
				else
				{
					winStr = "Player 2";
				}
			}
			
			// count down from freeze time between start of game
			totalElapsedTimeMillis += eng.getElapsedTimeMillis();
			newElapsedSec = Math.round(totalElapsedTimeMillis/1000.0f);
			
			if ( newElapsedSec > totalElapsedSec )
			{
				totalElapsedSec = newElapsedSec;
				--gameStartFreezeTime;
			}
			
			winStr += " Wins! Restarting game in  " + Integer.toString(gameStartFreezeTime);
			rs.setHUD(winStr, rw.getViewport(0).getActualScissorWidth()/2-200, rw.getViewport(0).getActualScissorHeight()/2);
			
			if ( gameStartFreezeTime <= 0 )
			{
				initGame();
			}
		}
		// count down from freeze time at start of a round
		else if ( roundFreezeTime > 0 )
		{
			// start of game, don't allow movement
			// otherwise, a player died, allow movement of player if alive
			if ( ((playerScore+enemyScore) > 0) && (getPlayerNodes().get(0).getHealth() > 0) )
			{
				im.update(eng.getElapsedTimeMillis());
			}
			
			totalElapsedTimeMillis += eng.getElapsedTimeMillis();
			newElapsedSec = Math.round(totalElapsedTimeMillis/1000.0f);
			
			if ( newElapsedSec > totalElapsedSec )
			{
				totalElapsedSec = newElapsedSec;
				
				// at last second of round freeze time, reset player's states
				if ((--roundFreezeTime) == 0)
				{
					resetPlayerPositions();
					resetDZ();
					
					for ( Player p : getPlayerNodes() )
					{
						// reset health, bullets
						p.initPlayer();
						playIdle(p);
						
						// reset original skin for other players since we can't see ourselves
						if ( !getPlayerNodes().get(0).equals(p) )
						{
					        if ( p.getHatChoice().equals("cowboyHat") )
					        {
					        	p.getEntity().setVisible(true);
					        	p.getNode().getAttachedObject(1).setVisible(false);
					        }
					        else
					        {
					        	p.getEntity().setVisible(false);
					        	p.getNode().getAttachedObject(1).setVisible(true);
					        }
						}
					}
				}
			}
			
			// set display around center of screen depending on length of text, needs offset to do so
			int offset = 0;
			if ( (playerScore+enemyScore) > 0 )
			{
				hudStr = roundWinStr + "! Next round in " + Integer.toString(roundFreezeTime);
				offset = 200;
			}
			else
			{
				hudStr = "Round starts in " + Integer.toString(roundFreezeTime);
				offset = 60;
			}
			
			rs.setHUD(hudStr, (rw.getViewport(0).getActualScissorWidth()/2)-offset, rw.getViewport(0).getActualScissorHeight()/2);
			
			for ( GunFire gf: gunFires )
			{
				// kill muzzle flashes in freezetime
				gf.destroyLights();
			}
		}
		else
		{
			im.update(eng.getElapsedTimeMillis());
			
			// if there are any gun shots, calculate dist from bullet to a player and if its at a lethal velocity
			for ( GunFire gf: gunFires )
			{
				// iterate time of gun fire to kill muzzle flash and when to disregard bullet's damage after it has traveled too far
				if ( gf.getTimeLeft() > 0f )
				{
					gf.iterate(eng.getElapsedTimeMillis());
				}
				
				// bullet already did damage
				if ( !gf.isAlreadyDamaged())
				{
					for ( Player p : getPlayerNodes() )
					{
						// only calculate damage to p if gunfire wasn't from p and bullet has not traveled too far
						if ( !gf.getOwner().equals(p) && gf.getTimeLeft() > 0f )
						{
							float dist = gf.getBulletN().getWorldPosition().sub(p.getNode().getWorldPosition()).length();
							// System.out.println("hit dist: " + dist);
							if ( dist <= 15.0f )
							{
								float[] xyzVel = gf.getBulletN().getPhysicsObject().getLinearVelocity();
								if ( Math.abs(xyzVel[0]) >= 100.0f || Math.abs(xyzVel[1]) >= 100.0f || Math.abs(xyzVel[2]) >= 100.0f )
								{
									gf.setAlreadyDamaged(true);
									//System.out.println("hit dist: " + dist);
									takeDmg(p, "bullet");
									
									if ( !p.equals(getPlayerNodes().get(0)) && p.getHatChoice().equals("cowboyHat") && p.getNode().getAttachedObject(0).isVisible() )
									{
										float[] lV = gf.getBulletN().getPhysicsObject().getLinearVelocity();
										
										createHatObj(p.getNode().getWorldPosition(), (float)(lV[0]*0.5), (float)(lV[1])+150.0f, (float)(lV[2]*0.5));
										
										p.getNode().getAttachedObject(0).setVisible(false);
										p.getNode().getAttachedObject(1).setVisible(true);
										
										//p.setStillMoving(false);
									}
								}
							}
						}
					}
				}
			}
			
			// check if a death happened and update scores
			for ( Player p : getPlayerNodes() )
			{
				if ( p.getHealth() <= 0 )
				{
					if ( p.equals(getPlayerNodes().get(0)) )
					{
						++enemyScore;
						roundWinStr = "Your opponent won round " + (playerScore+enemyScore);
					}
					else
					{
						++playerScore;
						roundWinStr = "You won round " + (playerScore+enemyScore);
					}
					
					killPlayer(p.getId());
					
					// if any player dies on screen, force other client to die to stay consistent
					if ( isClientConnected() && getPlayersJoined() > 0 )
					{
						getProtocolClient().sendDeathMessage(p.getId());
					}
					
					initRound();
					return;
				}
			}
			
			// iterate npc loop if client is only player on server
			if ( npcCtrl != null && getPlayersJoined() == 0 )
			{
				float currentTime = eng.getElapsedTimeMillis();
				npcCtrl.setLastThinkUpdateTime(npcCtrl.getLastThinkUpdateTime()+currentTime);
				
				if (npcCtrl.getNPC().getHealth() > 0)
				{
					npcCtrl.getNPC().updateLocation(npcCtrl, currentTime); // "TICK"
				}
				
				if (npcCtrl.getLastThinkUpdateTime() >= 500.0f) // “THINK”
				{ 
					npcCtrl.getBT().update(npcCtrl.getLastThinkUpdateTime());
					npcCtrl.setLastThinkUpdateTime(currentTime);
				}
				
				Thread.yield();
			}
			
			hudStr = "Health: " + getPlayerNodes().get(0).getHealth() + " Bullets: " + getPlayerNodes().get(0).getBulletCount() + "/6";
			rs.setHUD(hudStr, rw.getViewport(0).getActualScissorLeft() + 30, rw.getViewport(0).getActualScissorBottom() + 30);
		}
		
		// if player attacked, decrement cooldown period to prevent player from being able to attack again until half a second has passed
		if ( getAttackCooldown() > 0 )
		{
			attackCooldown -= eng.getElapsedTimeMillis();
		}
		
		wind1Sound.setLocation(getCameraN().getWorldPosition());
		stepSound.setLocation(getCameraN().getWorldPosition());
		shootSound.setLocation(getCameraN().getWorldPosition());
		hurtSound.setLocation(getCameraN().getWorldPosition());
		deathSound.setLocation(getCameraN().getWorldPosition());
		impactSound.setLocation(getCameraN().getWorldPosition());
		setEarParameters(sm);
		
		processNetworking(eng.getElapsedTimeMillis());
		
		// send movement update to other client per frame
		if ( isClientConnected() && getPlayersJoined() > 0)
		{
			// update anims for player nodes but not for networked ghost avatar since it is out of sync with each frame
			for ( Player p : getPlayerNodes() )
			{
				// only update for own avatar
				for ( GhostAvatar g : getProtocolClient().getGhosts())
				{
					if ( !g.equals(p) )
					{
						for ( SceneObject s : p.getNode().getAttachedObjects() )
						{
							SkeletalEntity e = (SkeletalEntity) s;
							e.update();
						}
						
						// change anim and sound states if necessary
						updateMovement(p);
					}
				}
			}
			
			getProtocolClient().sendMoveMessage(getAvatarPosition(), getAvatarRotation());
		}
		// unnetworked anim updates
		else
		{
			// update anims for player nodes
			for ( Player p : getPlayerNodes() )
			{
				for ( SceneObject s : p.getNode().getAttachedObjects() )
				{
					SkeletalEntity e = (SkeletalEntity) s;
					e.update();
				}
				
				// change anim and sound states if necessary
				updateMovement(p);
			}
		}
		
		// update physics states
		if ( running )
		{
			physicsEngine.update(eng.getElapsedTimeMillis());
			for ( SceneNode s : groundColliders.keySet() )
			{
				if ( s.getPhysicsObject() != null )
				{
					// get transform of physics s
					Matrix4 physMat = Matrix4f.createFrom(toFloatArray(s.getPhysicsObject().getTransform()));
					
					// translate ground collider of physics s
					float worldHeight = tessE.getWorldHeight(physMat.value(0, 3), physMat.value(2, 3))+0.5f;
					Vector3 newWorldPos = Vector3f.createFrom(physMat.value(0, 3), worldHeight, physMat.value(2, 3));
					Matrix4 groundPhysMat = Matrix4f.createFrom(Matrix3f.createIdentityMatrix(), newWorldPos);
					groundColliders.get(s).setTransform(toDoubleArray(groundPhysMat.toFloatArray()));
					
					// after translating ground collider, make sure physics s doesn't go under it
					if ( physMat.value(1, 3) < worldHeight )
					{
						Vector3 newPos = Vector3f.createFrom(physMat.value(0, 3), worldHeight, physMat.value(2, 3));
						physMat = Matrix4f.createFrom(physMat.toMatrix3(), newPos);
						s.getPhysicsObject().setTransform(toDoubleArray(physMat.toFloatArray()));
					}
					
					// translate/rotate gameworld s
					s.setLocalPosition(physMat.value(0, 3), physMat.value(1, 3), physMat.value(2, 3));
					s.setLocalRotation(physMat.toMatrix3());
				}
			}
		}
	}
	
	public boolean checkIfMoving(Player p)
	{
		Vector3 newPos = p.getNode().getWorldPosition();
		
		if ( newPos.equals( p.getLastPos()) )
		{
			//System.out.println("Not moving");
			return false;
		}
		else
		{
			//System.out.println("Is moving");
			p.setLastPos(newPos);
			return true;
		}
	}
		
	private void updateMovement(Player p)
	{
		if ( !checkIfMoving(p) )
		{
			// if its not moving, but still checked as moving
			// client has stopped moving and is not dead so change to idle anim
			if ( p.isStillMoving() )
			{
				stepSound.stop();
				
				// set to idle animation if player isn't moving and isn't dead
				if ( p.getHealth() > 0 )
				{
					playIdle(p);
					//System.out.println("set play idle");
				}
				
				// record client as still
				p.setStillMoving(false);
			}
		}
		else
		{
			// if it is moving or rotating, but still checked as unmoving
			// client has started moving so change to run anim
			// also check cases for if client is playing shoot or melee animation
			if ( !p.isStillMoving() || p.getCurrentAnim().equals("shoot") || p.getCurrentAnim().equals("melee") )
			{
				stepSound.play();
				
				// set to run animation while player is moving
				playRun(p);
				
				//System.out.println("set play run");
				
				// record client as moving
				p.setStillMoving(true);
			}
		}
	}
	
	private void runScript()
	{ 
		try
		{ 
			FileReader fileReader = new FileReader(scriptFile);
			jsEngine.eval(fileReader);
			fileReader.close();
		}
		catch (FileNotFoundException e1)
		{ 
			System.out.println(scriptFile + " not found " + e1); 
		}
		catch (IOException e2)
		{ 
			System.out.println("IO problem with " + scriptFile + e2); 
		}
		catch (ScriptException e3)
		{ 
			System.out.println("Script Exception in " + scriptFile + e3); 
		}
		catch (NullPointerException e4)
		{ 
			System.out.println ("Null ptr exception reading " + scriptFile + e4); 
		}
	}

	public void updateVerticalPosition(SceneNode n, float offset)
	{ 
		// Figure out Avatar's position relative to plane
		Vector3 worldNPosition = n.getWorldPosition();
		Vector3 localNPosition = n.getLocalPosition();
		
		// use avatar World coordinates to get coordinates for height
		Vector3 newNPosition = Vector3f.createFrom(
			// Keep the X coordinate
			localNPosition.x(),
			// The Y coordinate is the varying height
			tessE.getWorldHeight(worldNPosition.x(), worldNPosition.z()) + offset,
			//Keep the Z coordinate
			localNPosition.z()
		);
		
		// use avatar Local coordinates to set position, including height
		n.setLocalPosition(newNPosition);
	}
	
	private Robot robot;
	private float prevMouseX, prevMouseY, curMouseX, curMouseY, centerX, centerY;
	private boolean isRecentering;
	public void initMouseMode()
	{
		Toolkit tk = Toolkit.getDefaultToolkit();
		Cursor invisibleCursor = tk.createCustomCursor(tk.getImage(""), new Point(), "InvisibleCursor");
		rs.getCanvas().setCursor(invisibleCursor);
		
		Viewport v = rw.getViewport(0);
		int left = rw.getLocationLeft();
		int top = rw.getLocationTop();
		int width = v.getActualScissorWidth();
		int height = v.getActualScissorHeight();
		
		centerX = left + width/2;
		centerY = top + height/2;
		isRecentering = false;
		
		try
		{
			robot = new Robot();
		}
		catch (AWTException ex)
		{
			throw new RuntimeException("Couldn't create Robot!");
		}
		
		recenterMouse();
		prevMouseX = centerX;
		prevMouseY = centerY;
	}
	
	@Override
	public void mouseMoved(MouseEvent e)
	{
		// don't allow movement if dead or at start of a game
		if ( ((playerScore+enemyScore) <= 0) && roundFreezeTime > 0 || (getPlayerNodes().get(0).getHealth() <= 0) )
		{
			return;
		}
			
		if ( isRecentering && centerX == e.getXOnScreen() && centerY == e.getYOnScreen() )
		{
			isRecentering = false;
		}
		else
		{
			curMouseX = e.getXOnScreen();
			curMouseY = e.getYOnScreen();
			float mouseDeltaX = prevMouseX - curMouseX;
			float mouseDeltaY = prevMouseY - curMouseY;
			yaw(mouseDeltaX);
			pitch(mouseDeltaY);
			prevMouseX = curMouseX;
			prevMouseY = curMouseY;
			// tell robot to put the cursor to the center (since user just moved it)
			recenterMouse();
			prevMouseX = centerX; //reset prev to center
			prevMouseY = centerY;
		}
	}
	
	private void recenterMouse()
	{
		Viewport v = rw.getViewport(0);
		int left = rw.getLocationLeft();
		int top = rw.getLocationTop();
		int width = v.getActualScissorWidth();
		int height = v.getActualScissorHeight();
		centerX = left + width/2;
		centerY = top + height/2;
		isRecentering = true;
		robot.mouseMove((int) centerX, (int) centerY);
	}
	
	// setup deadzone boundary values and local deadzone value
	private float dZ = 10.0f;
	private float worldP = 0.0f;
	private float localP = 0.0f;
	private float topPDZ = -1.0f*dZ;
	private float botPDZ = dZ;
	private void pitch(float mouseDeltaY)
	{
		if ( getPlayerNodes().get(0).getHealth() <= 0 )
		{
			return;
		}
		
		float tilt = mouseDeltaY*sensitivity*-1.0f;
		SceneNode rotateNode = getCameraN();
		
		worldP += tilt;
		
		if ( worldP <= 50.0f && worldP >= -50.0f)
		{
			/*
			System.out.println("worldPitch: " +  worldP);
			System.out.println("localPitch: " +  localP);
			*/
			
			Matrix3 matRot = Matrix3f.createRotationFrom(Degreef.createFrom(tilt), getAvatarN().getWorldRightAxis());
			getAvatarN().setLocalRotation(matRot.mult(getAvatarN().getWorldRotation()));
			
			localP += tilt;
			// once hit top or bottom deadzone, move both avatar and cam
			if ( localP <= topPDZ || localP >= botPDZ )
			{
				matRot = Matrix3f.createRotationFrom(Degreef.createFrom(tilt), rotateNode.getWorldRightAxis());
				rotateNode.setLocalRotation(matRot.mult(rotateNode.getWorldRotation()));
				
				// hit top deadzone
				if ( localP <= topPDZ )
				{
					localP = topPDZ;
				}
				// hit bottom deadzone
				else if ( localP >= botPDZ )
				{
					localP = botPDZ;
				}
			}
		}
		else 
		{
			if ( worldP >= 50.0f )
			{
				worldP = 50.0f;
			}
			else if ( worldP <= -50.0f )
			{
				worldP = -50.0f;
			}
		}
	}
	
	private float localY = 0.0f;
	private float leftYDZ = -1.0f*dZ;
	private float rightYDZ = dZ;
	private void yaw(float mouseDeltaX)
	{
		if ( getPlayerNodes().get(0).getHealth() <= 0 )
		{
			return;
		}
		
		float tilt = mouseDeltaX*sensitivity;
		SceneNode rotateNode = getCameraN();
		
		Matrix3 matRot = Matrix3f.createRotationFrom(Degreef.createFrom(tilt), worldUp);
		getAvatarN().setLocalRotation(matRot.mult(getAvatarN().getWorldRotation()));
		getGhostAvN().setLocalRotation(matRot.mult(getGhostAvN().getWorldRotation()));
		
		localY += tilt;
		// moved out of yaw deadzone, shift camera and avatar
		if ( localY <= leftYDZ || localY >= rightYDZ )
		{
			/*
			System.out.println("localYaw: " + localY);
			*/
			
			rotateNode.setLocalRotation(matRot.mult(rotateNode.getWorldRotation()));
			
			// hit left deadzone
			if ( localY <= leftYDZ )
			{
				localY = leftYDZ;
			}
			// hit right deadzone
			if ( localY >= rightYDZ )
			{
				localY = rightYDZ; 
			}
		}
	}
	
	public void resetDZ()
	{
		localY = 0.0f;
		worldP = 0.0f;
		localP = 0.0f;
	}
	
	public float[] toFloatArray(double[] arr)
	{ 
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++)
		{ 
			ret[i] = (float)arr[i];
		}
		return ret;
	}
	public double[] toDoubleArray(float[] arr)
	{ 
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++)
		{ 
			ret[i] = (double)arr[i];
		}
		return ret;
	}
}