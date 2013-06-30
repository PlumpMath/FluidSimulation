import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Transform;
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

public class FluidSimulation {
	public static Integer LIQUID_INT = new Integer(1234598372);
	public static final int WIDTH = 1280, HEIGHT = 720;
	public static final float BOX2D_WIDTH = 30;
	public static final float BOX2D_SCALE = ((float)BOX2D_WIDTH)/WIDTH; //To scale to Box2D
	public static final float OPENGL_SCALE = ((float)WIDTH)/BOX2D_WIDTH; //To scale to OpenGL
	public static final float BOX2D_HEIGHT = HEIGHT * BOX2D_SCALE;
	public final int MAX_PARTICLES = 20000;
	public final int TOTAL_MASS = 1;
	public final float MASS_PER_PARTICLE = ((float)TOTAL_MASS)/MAX_PARTICLES;
	public final float RADIUS = 0.9f;
	public final float VISCOSITY = 0.004f;
	public final float DT = 1.0f / 120.0f;
	public final float IDEAL_RADIUS = 50.0f;
	public final float IDEAL_RADIUS_SQ = IDEAL_RADIUS*IDEAL_RADIUS;
	public final float MULTIPLIER = IDEAL_RADIUS / RADIUS;
	public static final int MAX_NEIGHBORS = 75;
	public static final int NUMBER_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();
	public static final Vec2 GRAVITY = new Vec2(0.0f, -9.8f).mul(1.0f / 3000.0f);
	public static final float MAX_PRESSURE = 10.0f;
	
	private int numActiveParticles = 0;
	private Particle[] liquid;
	private ArrayList<Integer> activeParticles;
	private Vec2[] delta;
	private Vec2[] scaledPositions;
	private Vec2[] scaledVelocities;
	private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> grid;
	public static final float CELL_SIZE = 0.6f;
	
	private World world;
	private AABB screenAABB;
	private int screenX, screenY;
	
	private Texture waterTexture;
	
	private int shaderProgram;
    private int vertexShader;
    private int fragmentShader;
    private boolean useShader = true;

	
	public FluidSimulation()
	{
		activeParticles = new ArrayList<Integer>(MAX_PARTICLES);
		liquid = new Particle[MAX_PARTICLES];
		delta = new Vec2[MAX_PARTICLES];
		scaledPositions = new Vec2[MAX_PARTICLES];
		scaledVelocities = new Vec2[MAX_PARTICLES];
		grid = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>();
		for (int i = 0; i < MAX_PARTICLES; i++)
		{
		    liquid[i] = new Particle(false);
		    liquid[i].index = i;
		}
		
		Body ground = null;
	    {
	    	world = new World(new Vec2(0.0f, -9.8f));
	     	BodyDef bd = new BodyDef();
	     	bd.position.set(0.0f, 0.0f);
	      
	     	ground = world.createBody(bd);
	     	PolygonShape shape = new PolygonShape();

	     	shape.setAsBox(20.0f, 0.9f, new Vec2(20.0f, 0.9f), 0.0f);
	     	ground.createFixture(shape, 0);
	      
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(1.0f, 7.5f), 0.0f);
	     	ground.createFixture(shape, 0);
	     	
	     	//shape.setAsBox(0.5f, 4.5f, new Vec2(4.75f, 7.5f), 45.0f);
	     	//ground.createFixture(shape, 0);
	      
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(21.0f, 7.5f), 0.0f);
	     	ground.createFixture(shape, 0);
	     	
	     	shape.setAsBox(0.9f, 7.5f, new Vec2(31.0f, 7.5f), 0.0f);
	     	ground.createFixture(shape, 0);
	     	
	     	//shape.setAsBox(0.5f, 4.5f, new Vec2(17.2f, 7.5f), -45.0f);
	     	//ground.createFixture(shape, 0);
	     	
	     	/*
	     	CircleShape cd = new CircleShape();
	     	cd.m_radius = 3.0f;
	     	cd.m_p.set(10.7f, 4.0f);
	     	ground.createFixture(cd, 0);
	     	*/
	    }
	    screenAABB = new AABB();
	    screenAABB.lowerBound.set(new Vec2(-100.0f, -100.0f));
	    screenAABB.upperBound.set(new Vec2(BOX2D_WIDTH+100.0f, BOX2D_HEIGHT+100.0f));
	}
	
	public static void main(String[] args)
	{
		FluidSimulation sim = new FluidSimulation();
		sim.start();
	}
	
	public void initGL() {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, BOX2D_WIDTH, 0, BOX2D_HEIGHT, 1, -1);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
		GL11.glEnable(GL11.GL_POINT_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPointSize(2);
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
			sync(60);
		}
		destroyShaderProgram();
		Display.destroy();
	}
	public void update()
	{
		//Poll input
		if(Mouse.isButtonDown(0))
		{
			createParticle(4, (Mouse.getX()-screenX)*BOX2D_SCALE, (Mouse.getY()-screenY)*BOX2D_SCALE);
		}
		if(Mouse.isButtonDown(1))
		{
			BodyDef bd = new BodyDef();
	     	bd.position.set(0.0f, 0.0f);
	      
	     	Body temp = world.createBody(bd);
	     	PolygonShape shape = new PolygonShape();

	     	shape.setAsBox(1.0f, 1.0f, new Vec2((Mouse.getX()-screenX)*BOX2D_SCALE, (Mouse.getY()-screenY)*BOX2D_SCALE), 0.0f);
	     	temp.createFixture(shape, 0);
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_DOWN))
		{
			screenY++;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_UP))
		{
			screenY--;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT))
		{
			screenX--;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_LEFT))
		{
			screenX++;
		}
		useShader = !Keyboard.isKeyDown(Keyboard.KEY_SPACE);
		//screenX--;
		
		applyLiquidConstraints();
		world.step(DT, 10, 10);
		
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glColor3f(0.0f, 0.0f, 0.0f);
		
		GL11.glViewport(screenX, screenY, WIDTH, HEIGHT);
		
		// draw 	
		GL20.glUseProgram(shaderProgram);
		//GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		waterTexture.bind();
		for (int i = 0; i < numActiveParticles; i++)
	    {
	        Particle particle = liquid[activeParticles.get(i)];
	        int loc = GL20.glGetUniformLocation(shaderProgram, "speed");
		    GL20.glUniform1f(loc, (float)Math.sqrt(particle.velocity.x*particle.velocity.x + particle.velocity.y*particle.velocity.y));
	        GL11.glPushMatrix();
	        float width = waterTexture.getWidth();
	        float height = waterTexture.getHeight();
	        GL11.glTranslatef(particle.position.x-(width/2), particle.position.y-(height/2), 0.0f);
	        GL11.glBegin(GL11.GL_QUADS);
		        GL11.glTexCoord2d(0.0,0.0); GL11.glVertex2d(0.0,0.0);
		        GL11.glTexCoord2d(1.0,0.0); GL11.glVertex2d(width,0.0);
		        GL11.glTexCoord2d(1.0,1.0); GL11.glVertex2d(width, height);
		        GL11.glTexCoord2d(0.0,1.0); GL11.glVertex2d(0.0, height);
	       	GL11.glEnd();
	       	GL11.glPopMatrix();
	    }
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL20.glUseProgram(0);
		
		Body render = world.getBodyList();
		for(int i = 0; i < world.getBodyCount(); render = render.getNext())
		{
			if(render.m_type != BodyType.DYNAMIC)
			{
				Fixture fix = render.getFixtureList();
				for(int j = 0; j < render.m_fixtureCount; fix = fix.getNext())
				{
					Shape s = fix.getShape();
					ShapeType type = s.getType();
					GL11.glColor3f(0.0f, 0.0f, 1.0f);
					if(type.equals(ShapeType.CIRCLE))
					{
						CircleShape cs = (CircleShape)s;
						drawCircle(cs.m_p.x, cs.m_p.y, s.getRadius(), 20);
					}
					else if(type.equals(ShapeType.POLYGON))
					{
						PolygonShape ps = (PolygonShape)s;
						Vec2[] vertices = ps.m_vertices;
						GL11.glBegin(GL11.GL_QUADS); 
						for(int k = 0; k < vertices.length; k++) 
						{ 
							GL11.glVertex2f(vertices[k].x, vertices[k].y);
						} 
						GL11.glEnd(); 
					}
					j++;
				}
			}
			i++;
		}
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

	public void createParticle(int numParticlesToSpawn, float x, float y)
	{
		ArrayList<Particle> inactiveParticles = new ArrayList<Particle>();
	    for(int i = 0; i < liquid.length; i++)
	    {
	    	Particle particle = liquid[i];
	    	if(particle != null && !particle.alive)
	    	{
	    		inactiveParticles.add(liquid[i]);
	    	}
	    }
	    
	    if(inactiveParticles.size() >= numParticlesToSpawn)
	    {
		    for(int i = 0; i < numParticlesToSpawn; i++)
		    {
		    	Particle particle = inactiveParticles.get(i);
		        if (numActiveParticles < MAX_PARTICLES)
		        {
					Vec2 jitter = new Vec2((float)(Math.random() * 2 - 1), (float)(Math.random()) - 0.5f);
					particle.position = new Vec2(jitter.x + x, jitter.y + y);
					particle.oldPosition = particle.position;
					particle.velocity = new Vec2();
					
					particle.alive = true;
					particle.ci = getGridX(particle.position.x);
					particle.cj = getGridY(particle.position.y);

					// Create grid cell if necessary
					if (!grid.containsKey(particle.ci))
						grid.put(particle.ci,
								new HashMap<Integer, ArrayList<Integer>>());
					if (!grid.get(particle.ci).containsKey(particle.cj))
						grid.get(particle.ci).put(particle.cj,
								new ArrayList<Integer>(20));
					grid.get(particle.ci).get(particle.cj).add(particle.index);

					activeParticles.add(particle.index);
					numActiveParticles++;
		        }
		    }
	    }
	}
	public void moveParticle(int index)
	{
		Particle particle = liquid[index];
	    
	    //Replace this part with Verlet integration
		particle.oldPosition = particle.position;
		
		particle.velocity = particle.velocity.add(delta[index]);
		
	    particle.position = particle.position.add(delta[index]);
	    particle.position = particle.position.add(particle.velocity);
	    
	    // Update particle cell
	    int x = getGridX(particle.position.x);
	    int y = getGridY(particle.position.y);

	    if (particle.ci == x && particle.cj == y)
	        return;
	    else
	    {
	    	int removeIndex = grid.get(particle.ci).get(particle.cj).indexOf(index);
	        grid.get(particle.ci).get(particle.cj).remove(removeIndex);

	        if (grid.get(particle.ci).get(particle.cj).size() == 0)
	        {
	            grid.get(particle.ci).remove(particle.cj);

	            if (grid.get(particle.ci).size() == 0)
	            {
	                grid.remove(particle.ci);
	            }
	        }

	        if (!grid.containsKey(x))
	            grid.put(x, new HashMap<Integer, ArrayList<Integer>>());
	        if (!grid.get(x).containsKey(y))
	        	grid.get(x).put(y, new ArrayList<Integer>(20));

	        grid.get(x).get(y).add(index);
	        particle.ci = x;
	        particle.cj = y;
	    }
	}
	public void prepareCollisions()
	{	
		world.queryAABB(new QueryCallback(){
			public boolean reportFixture(Fixture fixture) {
				ArrayList<Integer> collisionGridY;
				
				Transform transform = fixture.getBody().getTransform();
				AABB aabb = new AABB();
				fixture.getShape().computeAABB(aabb, transform, 0);
				
				int Ax = getGridX(aabb.lowerBound.x);
				int Ay = getGridY(aabb.lowerBound.y);
				
				int Bx = getGridX(aabb.upperBound.x);
				int By = getGridY(aabb.upperBound.y);
				
				for(int i = Ax; i < Bx; i++)
				{
					for(int j = Ay; j < By; j++)
					{
						if (grid.containsKey(i) && grid.get(i).containsKey(j))
			            {
							collisionGridY = grid.get(i).get(j);
							for(int k = 0; k < collisionGridY.size(); k++)
							{
								Particle particle = liquid[collisionGridY.get(k)];
								if(particle.numFixturesToTest < Particle.MAX_FIXTURES_TO_TEST)
								{
									particle.fixtures[particle.numFixturesToTest] = fixture;
									particle.numFixturesToTest++;
								}
							}
			            }
					}
				}
				return true;
			}
		}
		, screenAABB);
	}
	public void resolveCollisions(int index)
	{
		Particle particle = liquid[index];
		for(int i = 0; i < particle.numFixturesToTest; i++)
		{
			Fixture fixture = particle.fixtures[i];
			
			Vec2 newPosition = particle.position.add(particle.velocity).add(delta[index]);
			if(fixture.testPoint(newPosition))
			{
				Body body = fixture.getBody();
				Vec2 closestPoint = new Vec2();
				Vec2 normal = new Vec2();
				
				if(fixture.getShape().getType() == ShapeType.POLYGON)
				{
					PolygonShape shape = (PolygonShape)fixture.getShape();
					Transform collisionXF = body.getTransform();
					
					for(int v = 0; v < shape.getVertexCount(); v++)
					{
						particle.collisionVertices[v] = Transform.mul(collisionXF, shape.m_vertices[v]);
			            particle.collisionNormals[v] = Transform.mul(collisionXF, shape.m_normals[v]);
					}
					
					float shortestDistance = 9999999f;
					for(int v = 0; v < shape.getVertexCount(); v++)
					{
						float distance = Vec2.dot(particle.collisionNormals[v], particle.collisionVertices[v].sub(particle.position));
						if(distance < shortestDistance)
						{
							shortestDistance = distance;
							
							closestPoint = particle.collisionNormals[v].mul(distance).add(particle.oldPosition);
							normal = particle.collisionNormals[v];
						}
					}
					//particle.position = closestPoint.add(normal.mul(0.01f));
					//particle.position = particle.oldPosition;
					final Vec2 lambda = new Vec2(1.0f, 0.0f);
					final Vec2 normalRay = new Vec2();
					world.raycast(new RayCastCallback(){
						public float reportFixture(Fixture fixture, Vec2 point,
								Vec2 normal, float fraction) {
							lambda.set(new Vec2(fraction, 0.0f));
							normalRay.set(normal);
							return 1;
						}
						
					}, particle.position, particle.oldPosition);
					if(lambda.x != 1.0f)
					{
						Vec2 delta = particle.position.sub(particle.oldPosition);
						delta = delta.mul(lambda.x);
						normalRay.set(normalRay.mul(RADIUS));
						particle.position = particle.oldPosition.add(delta).add(normalRay);
					}
				}
				else if(fixture.getShape().getType() == ShapeType.CIRCLE)
				{
					CircleShape shape = (CircleShape)fixture.getShape();
					Vec2 center = shape.m_p.add(body.getPosition());
					Vec2 difference = particle.position.sub(center);
					normal = difference;
					normal.normalize();
					closestPoint = center.add(difference.mul(shape.getRadius() * difference.length()));
					particle.position = closestPoint.add(normal.mul(0.05f));
				}
				particle.velocity = (particle.velocity.sub(normal.mul(Vec2.dot(particle.velocity, normal)).mul(1.2f))).mul(0.85f);
				//particle.applyGravity = false;
				//particle.velocity = new Vec2();
				//particle.position = particle.position.add(particle.velocity.mul(DT));
				delta[index] = new Vec2();
			}
			else
			{
				//particle.applyGravity = true;
			}
		}
	}
	public void applyLiquidConstraints()
	{
		int sections = numActiveParticles/NUMBER_OF_PROCESSORS;
		Thread[] threads = new Thread[NUMBER_OF_PROCESSORS];
		if(sections > 0)
		{
			//Prepare simulation
			for(int i = 0; i < NUMBER_OF_PROCESSORS; i++)
			{
				int start = i*sections;
				int end = i*sections+sections;
				
				threads[i] = new Thread(new PrepareSimulation(start, end), "Prepare Simulation: " + i);
				threads[i].setPriority(Thread.MAX_PRIORITY);
				threads[i].start();
			}
			try
			{
				for(int i = 0; i < threads.length; i++)
					threads[i].join();
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			
			prepareCollisions();
		
			//Calculate pressure
			threads = new Thread[NUMBER_OF_PROCESSORS];
			for(int i = 0; i < NUMBER_OF_PROCESSORS; i++)
			{
				int start = i*sections;
				int end = i*sections+sections;
				
				threads[i] = new Thread(new CalculatePressure(start, end), "Calculate Pressure: " + i);
				threads[i].setPriority(Thread.MAX_PRIORITY);
				threads[i].start();
			}
			try
			{
				for(int i = 0; i < threads.length; i++)
					threads[i].join();
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			
			//Apply forces
			Vec2[] accumulatedDelta = new Vec2[numActiveParticles];
			for(int i = 0; i < numActiveParticles; i++)
			{
				accumulatedDelta[i] = new Vec2();
			}
			threads = new Thread[NUMBER_OF_PROCESSORS];
			for(int i = 0; i < NUMBER_OF_PROCESSORS; i++)
			{
				int start = i*sections;
				int end = i*sections+sections;
				
				threads[i] = new Thread(new ApplyForces(start, end, accumulatedDelta), "Apply Forces: " + i);
				threads[i].setPriority(Thread.MAX_PRIORITY);
				threads[i].start();
			}
			try
			{
				for(int i = 0; i < threads.length; i++)
					threads[i].join();
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
			for(int i = 0; i < numActiveParticles; i++)
			{
				int index = activeParticles.get(i);
				delta[index] = delta[index].add(accumulatedDelta[index].mul(1.0f / MULTIPLIER));
			}
		}
		
		//Resolve collisions
		/*
		threads = new Thread[NUMBER_OF_PROCESSORS];
		for(int i = 0; i < NUMBER_OF_PROCESSORS; i++)
		{
			int start = i*sections;
			int end = i*sections+sections;
			
			threads[i] = new Thread(new ResolveCollisions(start, end), "Resolve Collisiion: " + i);
			threads[i].setPriority(Thread.MAX_PRIORITY);
			threads[i].start();
		}
		try
		{
			for(int i = 0; i < threads.length; i++)
				threads[i].join();
		}
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}
		*/
		for(int i = 0; i < numActiveParticles; i++)
		{
			resolveCollisions(activeParticles.get(i));
		}
		
		// Move particles
		for (int i = 0; i < numActiveParticles; i++)
		{
		    moveParticle(activeParticles.get(i));
		}
	}
	class PrepareSimulation implements Runnable
	{
		private int startIndex, endIndex;
		public PrepareSimulation(int start, int end)
		{
			startIndex = start;
			endIndex = end;
		}
		public void run() 
		{
			for(int i = startIndex; i < endIndex; i++)
			{
				prepareSimulationThread(activeParticles.get(i));
			}
		}
		public void prepareSimulationThread(int index)
		{
			Particle particle = liquid[index];
			
			//Find neighbors
			findNeighbors(particle);
			
			scaledPositions[index] = particle.position.mul(MULTIPLIER);
			scaledVelocities[index] = particle.velocity.mul(MULTIPLIER);
			
			delta[index] = new Vec2();
			
			particle.pressure = 0.0f;
			particle.pressureNear = 0.0f;
			particle.numFixturesToTest = 0;
		}
	}
	class CalculatePressure implements Runnable
	{
		private int startIndex, endIndex;
		public CalculatePressure(int start, int end)
		{
			startIndex = start;
			endIndex = end;
		}
		public void run() 
		{
			for(int i = startIndex; i < endIndex; i++)
			{
				calculatePressureThread(activeParticles.get(i));
			}
		}
		public void calculatePressureThread(int index)
		{
			Particle particle = liquid[index];
			for (int a = 0; a < particle.neighborCount; a++)
			{
			    Vec2 relativePosition = scaledPositions[particle.neighbors[a]].sub(scaledPositions[index]);
			    float distanceSq = relativePosition.lengthSquared();

			    //within idealRad check
			    if (distanceSq < IDEAL_RADIUS_SQ)
			    {
			        particle.distances[a] = (float)Math.sqrt(distanceSq);
			        //if (particle.distances[a] < Settings.EPSILON) particle.distances[a] = IDEAL_RADIUS - .01f;
			        float oneminusq = 1.0f - (particle.distances[a] / IDEAL_RADIUS);
			        particle.pressure = (particle.pressure + oneminusq * oneminusq);
			        particle.pressureNear = (particle.pressureNear + oneminusq * oneminusq * oneminusq);
			    }
			    else
			    {
			        particle.distances[a] = Float.MAX_VALUE;
			    }
			}
		}
	}
	class ApplyForces implements Runnable
	{
		private int startIndex, endIndex;
		private Vec2[] accumulatedDelta;
		public ApplyForces(int start, int end, Vec2[] d)
		{
			startIndex = start;
			endIndex = end;
			accumulatedDelta = d;
		}
		public void run() 
		{
			for(int i = startIndex; i < endIndex; i++)
			{
				applyForcesThread(activeParticles.get(i), accumulatedDelta);
			}
		}
		public Vec2[] applyForcesThread(int index, Vec2[] accumulatedDelta)
		{
			Particle particle = liquid[index];
			float pressure = (particle.pressure - 5f) / 2.0f; //normal pressure term
			float presnear = particle.pressureNear / 2.0f; //near particles term
			if(Math.abs(pressure)>MAX_PRESSURE)
			{
				pressure = MAX_PRESSURE * (pressure/Math.abs(pressure));
			}
			Vec2 change = new Vec2();
			for (int a = 0; a < particle.neighborCount; a++)
			{
			    Vec2 relativePosition = scaledPositions[particle.neighbors[a]].sub(scaledPositions[index]);

			    if (particle.distances[a] < IDEAL_RADIUS && particle.distances[a] > 0)
			    {
			        float q = particle.distances[a] / IDEAL_RADIUS;
			        float oneminusq = 1.0f - q;
			        float factor = oneminusq * (pressure + presnear * oneminusq) / (2.0F * particle.distances[a]);
			        Vec2 d = relativePosition.mul(factor);
			        Vec2 relativeVelocity = scaledVelocities[particle.neighbors[a]].sub(scaledVelocities[index]);

			        factor = VISCOSITY * oneminusq * DT;
			        d = d.sub(relativeVelocity.mul(factor));
			        accumulatedDelta[particle.neighbors[a]] =  accumulatedDelta[particle.neighbors[a]].add(d);
			        change = change.sub(d);
			    }
			}
			accumulatedDelta[index] = accumulatedDelta[index].add(change);
			if(particle.applyGravity)
				particle.velocity = particle.velocity.add(GRAVITY);
			return accumulatedDelta;
		}
	}
	class ResolveCollisions implements Runnable
	{
		private int startIndex, endIndex;
		public ResolveCollisions(int start, int end)
		{
			startIndex = start;
			endIndex = end;
		}
		public void run() 
		{
			for(int i = startIndex; i < endIndex; i++)
			{
				resolveCollisions(activeParticles.get(i));
			}
		}
		public void resolveCollisions(int index)
		{
			Particle particle = liquid[index];
			for(int i = 0; i < particle.numFixturesToTest; i++)
			{
				Fixture fixture = particle.fixtures[i];
				
				Vec2 newPosition = particle.position.add(particle.velocity).add(delta[index]);
				if(fixture.testPoint(newPosition))
				{
					Body body = fixture.getBody();
					Vec2 closestPoint = new Vec2();
					Vec2 normal = new Vec2();
					
					if(fixture.getShape().getType() == ShapeType.POLYGON)
					{
						PolygonShape shape = (PolygonShape)fixture.getShape();
						Transform collisionXF = body.getTransform();
						
						for(int v = 0; v < shape.getVertexCount(); v++)
						{
							Vec2 vertex = shape.m_vertices[v];
							float x = (collisionXF.q.c * vertex.x - collisionXF.q.s * vertex.y) + collisionXF.p.x;
				            float y = (collisionXF.q.s * vertex.x + collisionXF.q.c * vertex.y) + collisionXF.p.x;
							particle.collisionVertices[v] = new Vec2(x, y);
							
							vertex = shape.m_normals[v];
							x = (collisionXF.q.c * vertex.x - collisionXF.q.s * vertex.y) + collisionXF.p.x;
				            y = (collisionXF.q.s * vertex.x + collisionXF.q.c * vertex.y) + collisionXF.p.x;
				            particle.collisionNormals[v] = new Vec2(x, y);
						}
						
						float shortestDistance = Float.MAX_VALUE;
						for(int v = 0; v < shape.getVertexCount(); v++)
						{
							float distance = Vec2.dot(particle.collisionNormals[v], particle.collisionVertices[v].sub(particle.position));
							if(distance < shortestDistance)
							{
								shortestDistance = distance;
								
								closestPoint = particle.collisionNormals[v].mul(distance).add(particle.position);
								normal = particle.collisionNormals[v];
							}
						}
						particle.position = closestPoint.add(normal.mul(0.05f));
					}
					else if(fixture.getShape().getType() == ShapeType.CIRCLE)
					{
						CircleShape shape = (CircleShape)fixture.getShape();
						Vec2 center = shape.m_p.add(body.getPosition());
						Vec2 difference = particle.position.sub(center);
						normal = difference;
						normal.normalize();
						closestPoint = center.add(difference.mul(shape.getRadius() * difference.length()));
						particle.position = closestPoint.add(normal.mul(0.05f));
					}
					particle.velocity = (particle.velocity.sub(normal.mul(Vec2.dot(particle.velocity, normal)).mul(1.2f))).mul(0.85f);
					delta[index] = new Vec2();
				}
			}
		}
	}
	
	public static int getGridX(float x)
	{
		return (int)Math.floor(x/CELL_SIZE);
	}
	public static int getGridY(float y)
	{
		return (int)Math.floor(y/CELL_SIZE);
	}
	
	private void findNeighbors(Particle particle)
	{
	    particle.neighborCount = 0;
	    ArrayList<Integer> gridY;

	    for (int nx = -1; nx < 2; nx++)
	    {
	        for (int ny = -1; ny < 2; ny++)
	        {
	            int x = particle.ci + nx;
	            int y = particle.cj + ny;
	            if (grid.containsKey(x) && grid.get(x).containsKey(y))
	            {
	            	gridY = grid.get(x).get(y);

	                for (int a = 0; a < gridY.size(); a++)
	                {
	                    if (gridY.get(a) != particle.index)
	                    {
	                        particle.neighbors[particle.neighborCount] = gridY.get(a);
	                        particle.neighborCount++;

	                        if (particle.neighborCount >= MAX_NEIGHBORS)
	                            return;
	                    }
	                }
	            }
	        }
	    }
	}
	
	static long variableYieldTime;
	private static long lastTime;
	
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
