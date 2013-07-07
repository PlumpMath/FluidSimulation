import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;


public class Main {
	public static final int WIDTH = 1280, HEIGHT = 720;
	public static final float BOX2D_WIDTH = 80;
	public static final float BOX2D_SCALE = ((float)BOX2D_WIDTH)/WIDTH; //To scale to Box2D
	public static final float OPENGL_SCALE = ((float)WIDTH)/BOX2D_WIDTH; //To scale to OpenGL
	public static final float BOX2D_HEIGHT = HEIGHT * BOX2D_SCALE;
	public static final float DT = 1.0f / 60.0f;
	//public static final Vec2 buoyancyForce = new Vec2(0.0f, 10.0f);
	
	static long variableYieldTime;
	private static long lastTime;
	
	//Box2D things
	private World world;
	private AABB screenAABB;
	
	//Rendering things
	private Texture waterTexture;
	private int shaderProgram;
    private int vertexShader;
    private int fragmentShader;
    
    private FluidSimulation sim;
    public static ArrayList<Box> boxes;
    private Player player;
    private Camera camera;
	
	public Main()
	{
		Body ground = null;
	    {
	    	world = new World(new Vec2(0.0f, -80.8f));
	     	BodyDef bd = new BodyDef();
	     	bd.position.set(0.0f, 0.0f);
	     	
	     	ground = world.createBody(bd);
	     	PolygonShape shape = new PolygonShape();

	     	shape.setAsBox(20.0f, 0.9f, new Vec2(20.0f, 0.9f), 0.0f);
	     	ground.createFixture(shape, 0);
	      
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(1.0f, 7.5f), 0.0f);
	     	ground.createFixture(shape, 0);
	     	
	     	shape.setAsBox(0.5f, 4.5f, new Vec2(4.75f, 7.5f), 45.0f);
	     	ground.createFixture(shape, 0);
	      
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(21.0f, 11.5f), 0.0f);
	     	ground.createFixture(shape, 0);
	     	
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(31.0f, 7.5f), 0.0f);
	     	ground.createFixture(shape, 0);
	     	
	     	shape.setAsBox(0.5f, 4.5f, new Vec2(17.2f, 7.5f), -45.0f);
	     	ground.createFixture(shape, 0);
	     	
	     	/*
	     	CircleShape cd = new CircleShape();
	     	cd.m_radius = 3.0f;
	     	cd.m_p.set(10.7f, 4.0f);
	     	ground.createFixture(cd, 0);
	     	*/
	    }
	    screenAABB = new AABB();
	    screenAABB.lowerBound.set(new Vec2(-10.0f, -10.0f));
	    screenAABB.upperBound.set(new Vec2(BOX2D_WIDTH+10.0f, BOX2D_HEIGHT+10.0f));
	    
	    sim = new FluidSimulation(world, screenAABB);
	    boxes = new ArrayList<Box>();
	    player = new Player(new Vec2(10.5f, 4.0f), world);
	    boxes.add(player);
	    camera = new Camera(player);
	    world.setContactListener(player);
	}
	
	public static void main(String[] args)
	{
		Main game = new Main();
		game.start();
	}
	
	public void initGL() {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, BOX2D_WIDTH, 0, BOX2D_HEIGHT, 1.0f, -1.0f);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
		//GL11.glEnable(GL11.GL_POINT_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glViewport(0, 0, WIDTH, HEIGHT);
		//GL11.glPointSize(2);
		try {
			waterTexture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("particle.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}
	public void initShaderProgram()
	{
		shaderProgram = GL20.glCreateProgram();
        vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        StringBuilder vertexShaderSource = new StringBuilder();
        StringBuilder fragmentShaderSource = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("src/shader.vs"));
            String line;
            while ((line = reader.readLine()) != null) {
                vertexShaderSource.append(line).append('\n');
            }
        } catch (IOException e) {
            System.err.println("Vertex shader wasn't loaded properly.");
            e.printStackTrace();
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        BufferedReader reader2 = null;
        try {
            reader2 = new BufferedReader(new FileReader("src/shader.fs"));
            String line;
            while ((line = reader2.readLine()) != null) {
                fragmentShaderSource.append(line).append('\n');
            }
        } catch (IOException e) {
            System.err.println("Fragment shader wasn't loaded properly.");
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        GL20.glShaderSource(vertexShader, vertexShaderSource);
        GL20.glCompileShader(vertexShader);
        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Vertex shader wasn't able to be compiled correctly.");
        }
        GL20.glShaderSource(fragmentShader, fragmentShaderSource);
        GL20.glCompileShader(fragmentShader);
        if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Fragment shader wasn't able to be compiled correctly.");
        }
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        GL20.glValidateProgram(shaderProgram);
	}
	public void destroyShaderProgram()
	{
		GL20.glDeleteProgram(shaderProgram);
		GL20.glDeleteShader(vertexShader);
		GL20.glDeleteShader(fragmentShader);
	}
	public void setTextureUnit0(int programId) {
	    int loc = GL20.glGetUniformLocation(programId, "texture1");
	    GL20.glUniform1i(loc, 0);
	}
	public void start()
	{
		try {
			Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		initGL(); // init OpenGL
		initShaderProgram();
		setTextureUnit0(shaderProgram);

		while (!Display.isCloseRequested()) {
			update();

			Display.update();
			sync((int)(1.0f/DT));
		}
		destroyShaderProgram();
		Display.destroy();
	}
	public void update()
	{
		//Poll input
		if(Mouse.isButtonDown(0))
		{
			float mouseX = Mouse.getX()*BOX2D_SCALE+camera.getScreenX();
			float mouseY = Mouse.getY()*BOX2D_SCALE+camera.getScreenY();
			sim.createParticle(4, mouseX, mouseY, player);
		}
		if(Mouse.isButtonDown(1))
		{
			float mouseX = Mouse.getX()*BOX2D_SCALE+camera.getScreenX();
			float mouseY = Mouse.getY()*BOX2D_SCALE+camera.getScreenY();
			boxes.add(new Box(new Vec2(mouseX, mouseY), world));
		}
		if(Mouse.isButtonDown(2))
		{
			float mouseX = Mouse.getX()*BOX2D_SCALE+camera.getScreenX();
			float mouseY = Mouse.getY()*BOX2D_SCALE+camera.getScreenY();
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
			player.addStillTime(getTime() - lastTime);
			player.body.setLinearVelocity(new Vec2(player.body.getLinearVelocity().x *0.9f, player.body.getLinearVelocity().y));
			player.setKeyTest(keyTest);
		}
		
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
		player.update();
		world.step(DT, 4, 4);
		
		camera.update();
		
		// draw 	
		GL11.glPushMatrix();
		GL20.glUseProgram(shaderProgram);
		//GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		waterTexture.bind();
		sim.drawFluid(shaderProgram, waterTexture.getWidth(), waterTexture.getHeight());
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL20.glUseProgram(0);
		GL11.glPopMatrix();
		
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
					drawCircle(cs.m_p.x, cs.m_p.y, s.getRadius(), 20);
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
	
	private void drawCircle(float cx, float cy, float r, int num_segments) 
	{
		float theta = 2.0f * 3.1415926f / (float)num_segments; 
		float tangetial_factor = (float)Math.tan(theta);//calculate the tangential factor 

		float radial_factor = (float)Math.cos(theta);//calculate the radial factor 
		
		float x = r;//we start at angle = 0 

		float y = 0; 
	    
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		for(int ii = 0; ii < num_segments; ii++) 
		{ 
			GL11.glVertex2f(x + cx, y + cy);//output vertex 
	        
			//calculate the tangential vector 
			//remember, the radial vector is (x, y) 
			//to get the tangential vector we flip those coordinates and negate one of them 

			float tx = -y; 
			float ty = x; 
	        
			//add the tangential vector 

			x += tx * tangetial_factor; 
			y += ty * tangetial_factor; 
	        
			//correct using the radial factor 

			x *= radial_factor; 
			y *= radial_factor; 
		} 
		GL11.glEnd(); 
	}
	
	
	//Time keeping stuff
	/**
	 * An accurate sync method that adapts automatically
	 * to the system it runs on to provide reliable results.
	 * 
	 * @param fps The desired frame rate, in frames per second
	 */
	public static void sync(int fps) {
		if (fps <= 0) return;
		
		long sleepTime = 1000000000 / fps; // nanoseconds to sleep this frame
		// yieldTime + remainder micro & nano seconds if smaller than sleepTime
		long yieldTime = Math.min(sleepTime, variableYieldTime + sleepTime % (1000*1000));
		long overSleep = 0; // time the sync goes over by
		
		try {
			while (true) {
				long t = getTime() - lastTime;
				
				if (t < sleepTime - yieldTime) {
					Thread.sleep(1);
				}
				else if (t < sleepTime) {
					// burn the last few CPU cycles to ensure accuracy
					Thread.yield();
				}
				else {
					overSleep = t - sleepTime;
					break; // exit while loop
				}
			}
		} catch (InterruptedException e) {}
		
		lastTime = getTime() - Math.min(overSleep, sleepTime);
		
		// auto tune the time sync should yield
		if (overSleep > variableYieldTime) {
			// increase by 200 microseconds (1/5 a ms)
			variableYieldTime = Math.min(variableYieldTime + 200*1000, sleepTime);
		}
		else if (overSleep < variableYieldTime - 200*1000) {
			// decrease by 2 microseconds
			variableYieldTime = Math.max(variableYieldTime - 2*1000, 0);
		}
	}

	/**
	 * Get System Nano Time
	 * @return will return the current time in nano's
	 */
	private static long getTime() {
	    return (Sys.getTime() * 1000000000) / Sys.getTimerResolution();
	}

}
