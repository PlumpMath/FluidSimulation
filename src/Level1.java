import java.util.ArrayList;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;


public class Level1 implements ContactListener {
	private World world;
	private FluidSimulation sim;
    public static ArrayList<Box> boxes;
    private ArrayList<Button> buttons;
    private Player player;
    private Camera camera;
    private AABB screenAABB;
    
    public Level1()
    {
    	Body ground = null;
	    {
	    	world = new World(new Vec2(0.0f, -80.8f));
	     	BodyDef bd = new BodyDef();
	     	bd.position.set(0.0f, 0.0f);
	     	
	     	ground = world.createBody(bd);
	     	PolygonShape shape = new PolygonShape();
	     	
	     	FixtureDef fd = new FixtureDef();
	     	fd.userData = "ground";
	     	fd.shape = shape;
	     	shape.setAsBox(20.0f, 0.9f, new Vec2(20.0f, 0.9f), 0.0f);
	     	ground.createFixture(fd);
	      
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(1.0f, 7.5f), 0.0f);
	     	ground.createFixture(fd);
	     	
	     	shape.setAsBox(0.5f, 4.5f, new Vec2(4.75f, 7.5f), 45.0f);
	     	ground.createFixture(fd);
	      
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(21.0f, 11.5f), 0.0f);
	     	ground.createFixture(fd);
	     	
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(31.0f, 7.5f), 0.0f);
	     	ground.createFixture(fd);
	     	
	     	shape.setAsBox(0.5f, 4.5f, new Vec2(17.2f, 7.5f), -45.0f);
	     	ground.createFixture(fd);
	     	
	     	/*
	     	CircleShape cd = new CircleShape();
	     	cd.m_radius = 3.0f;
	     	cd.m_p.set(10.7f, 4.0f);
	     	ground.createFixture(cd, 0);
	     	*/
	    }
	    
	    screenAABB = new AABB();
	    screenAABB.lowerBound.set(new Vec2(-10.0f, -10.0f));
	    screenAABB.upperBound.set(new Vec2(Main.BOX2D_WIDTH+10.0f, Main.BOX2D_HEIGHT+10.0f));
	    
	    sim = new FluidSimulation(world, screenAABB);
	    boxes = new ArrayList<Box>();
	    buttons = new ArrayList<Button>();
	    buttons.add(new Button(new Vec2(4.75f, 3.75f), world, new ActionCommand(){
			public void doCommand() {
				boxes.add(new Box(new Vec2(10.0f, 10.0f), world));
			}

			public void undoCommand() {
				
			}
	    }));
	    player = new Player(new Vec2(10.5f, 4.0f), world);
	    boxes.add(player);
	    camera = new Camera(player);
	    world.setContactListener(this);
    }
    public ArrayList<Box> getBoxes()
    {
    	return boxes;
    }
    
    public void pollInput()
    {
    	//Poll input
		if(Mouse.isButtonDown(0))
		{
			float mouseX = Mouse.getX()*Main.BOX2D_SCALE+camera.getScreenX();
			float mouseY = Mouse.getY()*Main.BOX2D_SCALE+camera.getScreenY();
			sim.createParticle(4, mouseX, mouseY, player);
		}
		if(Mouse.isButtonDown(1))
		{
			float mouseX = Mouse.getX()*Main.BOX2D_SCALE+camera.getScreenX();
			float mouseY = Mouse.getY()*Main.BOX2D_SCALE+camera.getScreenY();
			boxes.add(new Box(new Vec2(mouseX, mouseY), world));
		}
		if(Mouse.isButtonDown(2))
		{
			float mouseX = Mouse.getX()*Main.BOX2D_SCALE+camera.getScreenX();
			float mouseY = Mouse.getY()*Main.BOX2D_SCALE+camera.getScreenY();
			BodyDef bd = new BodyDef();
			bd.position.set(0.0f, 0.0f);
			
			Body temp = world.createBody(bd);
			PolygonShape shape = new PolygonShape();
			
			shape.setAsBox(1.0f, 1.0f, new Vec2(mouseX, mouseY), 0.0f);
			temp.createFixture(shape, 0);

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
    	//Logic
		for(int i = 0; i < boxes.size(); i++)
		{
			boxes.get(i).numberDisplaced = 0.0f;
			boxes.get(i).waterCollide = false;
		}
		sim.applyLiquidConstraints();
		for(int i = 0; i < boxes.size(); i++)
		{
			Box box = boxes.get(i);
			box.body.applyForce(world.getGravity().mul(-box.numberDisplaced), box.body.getPosition());
			if(box.waterCollide)
			{
				box.body.setLinearDamping(1.0f);
			}
		}
		for(int i = 0; i < buttons.size(); i++)
		{
			buttons.get(i).update();
		}
		player.update();
		world.step(Main.DT, 4, 4);
		
		camera.update();
    }
    public void drawFluid(int shaderProgram, float width, float height)
    {
    	sim.drawFluid(shaderProgram, width, height);
    }
    public void renderGround()
    {
    	GL11.glPushMatrix();
		Body render = world.getBodyList();
		for(int i = 0; i < world.getBodyCount(); render = render.getNext())
		{
			Fixture fix = render.getFixtureList();
			for(int j = 0; j < render.m_fixtureCount; fix = fix.getNext())
			{
				Shape s = fix.getShape();
				ShapeType type = s.getType();
				if(render.getType() == BodyType.DYNAMIC)
					GL11.glColor3f(1.0f, 0.0f, 0.0f);
				else
					GL11.glColor3f(0.0f, 0.0f, 1.0f);
				if(type.equals(ShapeType.CIRCLE))
				{
					CircleShape cs = (CircleShape)s;
					Main.drawCircle(cs.m_p.x, cs.m_p.y, s.getRadius(), 20);
				}
				else if(type.equals(ShapeType.POLYGON))
				{
					GL11.glPushMatrix();
					GL11.glTranslatef(render.getPosition().x, render.getPosition().y, 0.0f);
					PolygonShape ps = (PolygonShape)s;
					Vec2[] vertices = ps.m_vertices;
					GL11.glBegin(GL11.GL_QUADS); 
					for(int k = 0; k < vertices.length; k++) 
					{ 
						GL11.glVertex2f(vertices[k].x, vertices[k].y);
					} 
					GL11.glEnd(); 
					GL11.glPopMatrix();
				}
				j++;
			}
			i++;
		}
		GL11.glPopMatrix();
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
