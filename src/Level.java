import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.iforce2d.Jb2dJson;
import org.iforce2d.Jb2dJsonImage;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;


public class Level implements ContactListener {
	protected World world;
	private FluidSimulation sim;
    protected ArrayList<Button> buttons;
    protected Player player;
    protected Camera camera;
    private Body ground, boundary;
    private Jb2dJsonImage[] groundImages;
    private HashMap<String, Texture> groundTextures;
    private ArrayList<Body> otherBodies;
    private AABB screenAABB;
    
    public Level(int level)
    {
    	Jb2dJson json = new Jb2dJson();
		String levelFile = "levels/level"+level+".rube";
		StringBuilder errorMessage = new StringBuilder();
		world = json.readFromFile(levelFile, errorMessage);
		System.err.println(errorMessage);
		
	    player = new Player(json);
	    ground = json.getBodyByName("ground");
	    boundary = json.getBodyByName("boundary");
	    otherBodies = new ArrayList<Body>();
	    for(Body b:json.getBodiesByName("object"))
	    	otherBodies.add(b);
	    groundImages = json.getImagesByName("groundImage");
	    PriorityQueue<Jb2dJsonImage> queue = new PriorityQueue<Jb2dJsonImage>(groundImages.length, new Comparator<Jb2dJsonImage>(){
			public int compare(Jb2dJsonImage one, Jb2dJsonImage two) {
				return (int)(one.renderOrder-two.renderOrder);
			}
	    	
	    });
	    for(int i = 0; i < groundImages.length; i++)
	    {
	    	queue.add(groundImages[i]);
	    }
	    queue.toArray(groundImages);
	    groundTextures = new HashMap<String, Texture>();
	    try
	    {
	    	for(int i = 0; i < groundImages.length; i++)
	    	{
	    		String imageLocation = groundImages[i].file.substring(3);
	    		if(groundTextures.get(imageLocation)==null)
	    			groundTextures.put(imageLocation, TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream(imageLocation)));
	    	}
	    }
	    catch(IOException e)
	    {
	    	e.printStackTrace();
	    }
	    
	    screenAABB = new AABB();
	    screenAABB.lowerBound.set(new Vec2(player.body.getPosition().x-100.0f, player.body.getPosition().y-100.0f));
	    screenAABB.upperBound.set(new Vec2(player.body.getPosition().x+100.0f, player.body.getPosition().y+100.0f));
	    camera = new Camera(player, json.getBodyByName("boundary"), "res/background.png");
	    sim = new FluidSimulation(world, screenAABB, camera);
	    buttons = new ArrayList<Button>();
	    world.setContactListener(this);
	    
	    if(level == 1)
	    {
	    	//Button 1
	    	final Body wall = json.getBodyByName("wall");
	    	otherBodies.add(wall);
	    	Fixture sensor = json.getBodyByName("button1").getFixtureList();
	    	Jb2dJsonImage image = json.getImageByName("buttonImage");
	    	buttons.add(new Button(sensor, image, new ActionCommand(){
	    		public float startY = wall.getPosition().y;
	    		public float endY = -2.2553f;
				public void doCommand() {
					wall.setLinearVelocity(new Vec2(0.0f, -10.0f));
					if(wall.getPosition().y<endY)
					{
						wall.getPosition().set(new Vec2(wall.getPosition().x, endY));
						wall.setLinearVelocity(new Vec2(0.0f, 0.0f));
					}
				}

				public void undoCommand() {
					wall.setLinearVelocity(new Vec2(0.0f, 10.0f));
					if(wall.getPosition().y>startY)
					{
						wall.getPosition().set(new Vec2(wall.getPosition().x, startY));
						wall.setLinearVelocity(new Vec2(0.0f, 0.0f));
					}
				}
	    		
	    	}));
	    	
	    	//Button 2
	    	final Body wall2 = json.getBodyByName("wall2");
	    	otherBodies.add(wall2);
	    	sensor = json.getBodyByName("button2").getFixtureList();
	    	image = json.getImageByName("buttonImage");
	    	buttons.add(new Button(sensor, image, new ActionCommand(){
	    		public float startY = wall2.getPosition().y;
	    		public float endY = 10.0792f;
				public void doCommand() {
					wall2.setLinearVelocity(new Vec2(0.0f, 10.0f));
					if(wall2.getPosition().y>endY)
					{
						wall2.getPosition().set(new Vec2(wall2.getPosition().x, endY));
						wall2.setLinearVelocity(new Vec2(0.0f, 0.0f));
					}
				}

				public void undoCommand() {
					wall2.setLinearVelocity(new Vec2(0.0f, -10.0f));
					if(wall2.getPosition().y<startY)
					{
						wall2.getPosition().set(new Vec2(wall2.getPosition().x, startY));
						wall2.setLinearVelocity(new Vec2(0.0f, 0.0f));
					}
				}
	    		
	    	}));
	    	
	    	sensor = json.getBodyByName("door").getFixtureList();
	    	image = json.getImageByName("buttonImage");
	    	buttons.add(new Button(sensor, image, new ActionCommand(){
				public void doCommand() {
					Main.nextLevel();
				}

				public void undoCommand() {
				}
	    		
	    	}));
	    	/*
			buttons.add(new Button(levelFile, new ActionCommand(){
				public void doCommand() {
					boxes.add(new Box(new Vec2(10.0f, 10.0f), world));
				}

				public void undoCommand() {
					
				}
		    }));
		    */
	    }
    }
    public Player getPlayer()
    {
    	return player;
    }
    
    public void pollInput()
    {
    	//Poll input
		if(Mouse.isButtonDown(0))
		{
			float mouseX = Mouse.getX()*Main.BOX2D_SCALE+(camera.getScreenX()*Main.BOX2D_SCALE);
			float mouseY = Mouse.getY()*Main.BOX2D_SCALE+(camera.getScreenY()*Main.BOX2D_SCALE);
			sim.createParticle(4, mouseX, mouseY, player);
			player.mouseDown(mouseX);
		}
		else
		{
			player.mouseUp();
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_DOWN) || Keyboard.isKeyDown(Keyboard.KEY_S))
		{
			player.moveDown();
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_UP) || Keyboard.isKeyDown(Keyboard.KEY_W))
		{
			player.jump();
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
		{
			Display.destroy();
			System.exit(0);
		}
		boolean keyTest = true;
		if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_D))
		{
			player.moveRight();
			player.resetStillTime();
			keyTest = false;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_A))
		{
			player.moveLeft();
			player.resetStillTime();
			keyTest = false;
		}
		if(keyTest)
		{
			player.addStillTime(Main.getTime() - Main.lastTime);
			player.body.setLinearVelocity(new Vec2(player.body.getLinearVelocity().x *0.9f, player.body.getLinearVelocity().y));
			player.setKeyTest(keyTest);
		}
    }
    
    public void logic()
    {
    	player.reset();
    	//Logic
		sim.applyLiquidConstraints();
		for(int i = 0; i < buttons.size(); i++)
		{
			buttons.get(i).update();
		}
		player.update();
		world.step(Main.DT, 4, 4);
		
		camera.update();
    }
    public void drawFluid(int shaderProgram)
    {
    	sim.drawFluid(shaderProgram);
    }
    public void renderObjects()
    {
    	for(int i = 0; i < buttons.size(); i++)
    	{
    		buttons.get(i).draw();
    	}
    	//Render other bodies
    	GL11.glPushMatrix();
		for(int i = 0; i < otherBodies.size(); i++)
		{
			Body body = otherBodies.get(i);
			Transform transform = body.getTransform();
			Fixture fix = body.getFixtureList();
			for(int j = 0; j < body.m_fixtureCount; fix = fix.getNext())
			{
				Shape s = fix.getShape();
				ShapeType type = s.getType();
				GL11.glColor3f(0.0f, 0.0f, 1.0f);
				if(type.equals(ShapeType.CIRCLE))
				{
					CircleShape cs = (CircleShape)s;
					Vec2 center = Transform.mul(transform, cs.m_p);
					GL11.glPushMatrix();
					Main.drawCircle(center.x*Main.OPENGL_SCALE, center.y*Main.OPENGL_SCALE, s.getRadius()*Main.OPENGL_SCALE, 20);
					GL11.glPopMatrix();
				}
				else if(type.equals(ShapeType.POLYGON))
				{
					GL11.glPushMatrix();
					PolygonShape ps = (PolygonShape)s;
					Vec2[] vertices = ps.m_vertices;
					GL11.glTranslatef(body.getPosition().x * Main.OPENGL_SCALE, body.getPosition().y * Main.OPENGL_SCALE, 0.0f);
					GL11.glRotatef((float) (body.getAngle() * (180 / Math.PI)), 0.0f, 0.0f, 1.0f);
					GL11.glBegin(GL11.GL_TRIANGLE_FAN); 
					for(int k = 0; k < ps.getVertexCount(); k++)
					{
						GL11.glVertex2f(vertices[k].x * Main.OPENGL_SCALE, vertices[k].y * Main.OPENGL_SCALE);
					} 
					GL11.glEnd(); 
					GL11.glPopMatrix();
				}
				j++;
			}
		}
		GL11.glPopMatrix();
		
		/*
		GL11.glPushMatrix();
		Body render = world.getBodyList();
		for(int i = 0; i < world.getBodyCount(); render = render.getNext())
		{
			Fixture fix = render.getFixtureList();
			if(render.getType() == BodyType.STATIC)
			{
				for(int j = 0; j < render.m_fixtureCount; fix = fix.getNext())
				{
					Shape s = fix.getShape();
					ShapeType type = s.getType();
					GL11.glColor3f(0.0f, 0.0f, 1.0f);
					if(type.equals(ShapeType.CIRCLE))
					{
						CircleShape cs = (CircleShape)s;
						Main.drawCircle(cs.m_p.x, cs.m_p.y, s.getRadius(), 20);
					}
					else if(type.equals(ShapeType.POLYGON))
					{
						GL11.glPushMatrix();
						GL11.glTranslatef(render.getPosition().x*Main.OPENGL_SCALE, render.getPosition().y*Main.OPENGL_SCALE, 0.0f);
						PolygonShape ps = (PolygonShape)s;
						Vec2[] vertices = ps.m_vertices;
						GL11.glBegin(GL11.GL_TRIANGLE_FAN); 
						for(int k = 0; k < ps.getVertexCount(); k++) 
						{ 
							GL11.glVertex2f(vertices[k].x*Main.OPENGL_SCALE, vertices[k].y*Main.OPENGL_SCALE);
						} 
						GL11.glEnd(); 
						GL11.glPopMatrix();
					}
					j++;
				}
			}
			i++;
		}
		GL11.glPopMatrix();
		*/

		
		//Render ground
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		for(int i = 0; i < groundImages.length; i++)
		{
			String imageLocation = groundImages[i].file.substring(3);
			Texture groundTexture = groundTextures.get(imageLocation);
			Jb2dJsonImage groundImageInfo = groundImages[i];
			groundTexture.bind();
			float height = groundImageInfo.scale * Main.OPENGL_SCALE;
	        float ratio = (float)groundTexture.getImageWidth()/groundTexture.getImageHeight();
	        float width = height*ratio;
	        GL11.glPushMatrix();
	    	GL11.glTranslatef((ground.getPosition().x + groundImageInfo.center.x)*Main.OPENGL_SCALE, (ground.getPosition().y + groundImageInfo.center.y)*Main.OPENGL_SCALE, 0.0f);
	    	if(!groundImageInfo.flip)
	    		GL11.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
	    	else
	    		GL11.glRotatef(-180.0f, 0.0f, 1.0f, 0.0f);
	        GL11.glRotatef(-(float)(groundImageInfo.angle*180/Math.PI), 0.0f, 0.0f, 1.0f);
	    	GL11.glColor3f(1.0f, 1.0f, 1.0f);
	        float textureWidth = (float)groundTexture.getImageWidth()/Main.get2Fold(groundTexture.getImageWidth());
	        float textureHeight = (float)groundTexture.getImageHeight()/Main.get2Fold(groundTexture.getImageHeight());
	        GL11.glBegin(GL11.GL_QUADS);
	        	GL11.glTexCoord2f(textureWidth, textureHeight); GL11.glVertex2f(-width/2, -height/2);
	        	GL11.glTexCoord2f(0.0f, textureHeight); GL11.glVertex2f(width/2, -height/2);
	        	GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex2f(width/2, height/2);
	        	GL11.glTexCoord2f(textureWidth, 0.0f); GL11.glVertex2f(-width/2, height/2);
	       	GL11.glEnd();
	       	GL11.glPopMatrix();
		}
       	GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
    public void renderPlayer()
    {
    	player.draw();
    }
	@Override
	public void beginContact(Contact contact) {
		player.beginContact(contact);
		for(int i = 0; i < buttons.size(); i++)
		{
			buttons.get(i).beginContact(contact);
		}
	}
	@Override
	public void endContact(Contact contact) {
		player.endContact(contact);
		for(int i = 0; i < buttons.size(); i++)
		{
			buttons.get(i).endContact(contact);
		}
	}
	@Override
	public void postSolve(Contact arg0, ContactImpulse arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void preSolve(Contact arg0, Manifold arg1) {
		// TODO Auto-generated method stub
		
	}
}
