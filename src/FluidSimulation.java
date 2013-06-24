import java.util.ArrayList;
import java.util.HashMap;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.MassData;
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
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

public class FluidSimulation {
	public static Integer LIQUID_INT = new Integer(1234598372);
	public static final int WIDTH = 800, HEIGHT = 600;
	public static final float BOX2D_WIDTH = 10;
	public static final float BOX2D_SCALE = ((float)BOX2D_WIDTH)/WIDTH; //To scale to Box2D
	public static final float OPENGL_SCALE = ((float)WIDTH)/BOX2D_WIDTH; //To scale to OpenGL
	public static final float BOX2D_HEIGHT = HEIGHT * BOX2D_SCALE;
	public final int MAX_PARTICLES = 2000;
	public final int TOTAL_MASS = 1;
	public final float MASS_PER_PARTICLE = ((float)TOTAL_MASS)/MAX_PARTICLES;
	public final float RADIUS = 0.6f;
	public final float VISCOSITY = 0.004f;
	public final float DT = 1.0f / 60.0f;
	public final float IDEAL_RADIUS = 50.0f;
	public final float IDEAL_RADIUS_SQ = IDEAL_RADIUS*IDEAL_RADIUS;
	public final float MULTIPLIER = IDEAL_RADIUS / RADIUS;
	public static final int MAX_NEIGHBORS = 75;
	
	private int numActiveParticles = 0;
	private Particle[] liquid;
	private ArrayList<Integer> activeParticles;
	private Vec2[] delta;
	private Vec2[] scaledPositions;
	private Vec2[] scaledVelocities;
	
	private HashMap<Integer, HashMap<Integer, ArrayList<Integer>>> grid;
	public final float CELL_SIZE = 0.5f;
	
	private World world;
	private MetaballSystem metaballSystem;
	
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
	      shape.setAsBox(5.0f, 0.5f);
	      ground.createFixture(shape, 0);

	      shape.setAsBox(10.0f, 0.1f, new Vec2(0.0f, 0.1f), 0.0f);
	      ground.createFixture(shape, 0);
	      
	      shape.setAsBox(0.1f, 7.5f, new Vec2(0.0f, 7.5f), 0.0f);
	      ground.createFixture(shape, 0);
	      
	      shape.setAsBox(0.1f, 7.5f, new Vec2(10.0f, 7.5f), 0.0f);
	      ground.createFixture(shape, 0);

	      CircleShape cd = new CircleShape();
	      cd.m_radius = 0.5f;
	      cd.m_p.set(5.0f, 3.75f);
	      ground.createFixture(cd, 0);
	    }

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
		GL11.glPointSize(2);	
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

		while (!Display.isCloseRequested()) {
			update();

			Display.update();
			sync(60);
		}

		Display.destroy();
	}
	
	public void update()
	{
		//Poll input
		if(Mouse.isButtonDown(0))
		{
			createParticle(4, Mouse.getX()*BOX2D_SCALE, Mouse.getY()*BOX2D_SCALE);
		}
		
		applyLiquidConstraints();
		world.step(DT, 10, 10);
		
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glColor3f(0.0f, 0.0f, 0.0f);
		
		
		// draw 
		
		GL11.glPushMatrix();
		for (int i = 0; i < numActiveParticles; i++)
	    {
	        Particle particle = liquid[activeParticles.get(i)];

	        GL11.glBegin(GL11.GL_POINTS);
			GL11.glColor3f(0.0f, 0.0f, 1.0f);
			GL11.glVertex3f(particle.body.m_xf.p.x, particle.body.m_xf.p.y, 0);
			GL11.glEnd();
	    }
		GL11.glPopMatrix();
		
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
					if(type.equals(ShapeType.CIRCLE))
					{
						CircleShape cs = (CircleShape)s;
						drawCircle(cs.m_p.x, cs.m_p.y, s.getRadius(), 20);
					}
					else if(type.equals(ShapeType.POLYGON))
					{
						
					}
					j++;
				}
			}
			i++;
		}
		
		
		if(metaballSystem != null && numActiveParticles > 0)
		{
			//metaballSystem.drawBalls(20.0f);
		}
	}
	
	private void drawCircle(float cx, float cy, float r, int num_segments) 
	{
		float theta = 2.0f * 3.1415926f / (float)num_segments; 
		float tangetial_factor = (float)Math.tan(theta);//calculate the tangential factor 

		float radial_factor = (float)Math.cos(theta);//calculate the radial factor 
		
		float x = r;//we start at angle = 0 

		float y = 0; 
	    
		GL11.glBegin(GL11.GL_LINE_LOOP); 
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
					CircleShape pd = new CircleShape();
					FixtureDef fd = new FixtureDef();
					fd.shape = pd;
					fd.density = 1f;
					fd.filter.groupIndex = -10;
					pd.m_radius = .05f;
					fd.restitution = 0.4f;
					fd.friction = 0.0f;
					BodyDef bd = new BodyDef();
					bd.fixedRotation = true;
					bd.type = BodyType.DYNAMIC;
					Vec2 jitter = new Vec2((float)(Math.random() * 2 - 1), (float)(Math.random()) - 0.5f);
					bd.position = new Vec2(x, y).add(jitter);
					Body b = world.createBody(bd);

					b.createFixture(fd).setUserData(LIQUID_INT);

					MassData md = new MassData();
					md.mass = MASS_PER_PARTICLE;
					md.I = 1.0f;
					b.setMassData(md);
					b.setSleepingAllowed(false);
					
					particle.body = b;
					
					particle.alive = true;
					particle.ci = getGridX(particle.body.m_xf.p.x);
					particle.cj = getGridY(particle.body.m_xf.p.y);

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
		    Metaball[] balls = new Metaball[numActiveParticles];
		    for(int i = 0; i < numActiveParticles; i++)
		    {
		    	int index = activeParticles.get(i);
		    	Particle particle = liquid[index];
		    	balls[i] = new Metaball(particle.body.m_xf.p, 10);
		    }
		    metaballSystem = new MetaballSystem(balls, 2.0f, 0.004f);
	    }
	}
	
	
	public void applyLiquidConstraints()
	{
		//Prepare simulation
		for(int i = 0; i < numActiveParticles; i++)
		{
			int index = activeParticles.get(i);
			Particle particle = liquid[index];
			
			scaledPositions[index] = particle.body.m_xf.p.mul(MULTIPLIER);
			scaledVelocities[index] = particle.body.getLinearVelocity().mul(MULTIPLIER);
			
			delta[index] = new Vec2();
			
			particle.pressure = 0.0f;
			particle.pressureNear = 0.0f;
		}
		
		for(int i = 0; i < numActiveParticles; i++)
		{
			int index = activeParticles.get(i);
			Particle particle = liquid[index];
			
			//Find neighbors
			findNeighbors(particle);
			
			// Calculate pressure
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
			
			// Apply forces
			float pressure = (particle.pressure - 5f) / 2.0f; //normal pressure term
			float presnear = particle.pressureNear / 2.0f; //near particles term
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
			        delta[particle.neighbors[a]] =  delta[particle.neighbors[a]].add(d);
			        change = change.sub(d);
			    }
			}
			delta[index] = delta[index].add(change);
		}
		
		// Move particles
		for (int i = 0; i < numActiveParticles; i++)
		{
		    int index = activeParticles.get(i);
		    Particle particle = liquid[index];
		    
		    //Replace this part with Verlet integration
		    particle.body.m_xf.p.set(particle.body.m_xf.p.add(delta[index].mul(1.0f/MULTIPLIER)));
		    particle.body.getLinearVelocity().set(particle.body.getLinearVelocity().add(delta[index].mul(1.0f/(MULTIPLIER * DT))));
		    
		    // Update particle cell
		    int x = getGridX(particle.body.m_xf.p.x);
		    int y = getGridY(particle.body.m_xf.p.y);

		    if (particle.ci == x && particle.cj == y)
		        continue;
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
	}
	
	private int getGridX(float x)
	{
		return (int)Math.floor(x/CELL_SIZE);
	}
	private int getGridY(float y)
	{
		return (int)Math.floor(y/CELL_SIZE);
	}
	
	private void findNeighbors(Particle particle)
	{
	    particle.neighborCount = 0;
	    HashMap<Integer, ArrayList<Integer>> gridX;
	    ArrayList<Integer> gridY;

	    for (int nx = -1; nx < 2; nx++)
	    {
	        for (int ny = -1; ny < 2; ny++)
	        {
	            int x = particle.ci + nx;
	            int y = particle.cj + ny;
	            if (grid.containsKey(x) && grid.get(x).containsKey(y))
	            {
	            	gridX = grid.get(x);
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
