import java.util.ArrayList;
import java.util.HashMap;

import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class FluidSimulation {
	public static Integer LIQUID_INT = new Integer(1234598372);
	public final int MAX_PARTICLES = 20000;
	public final int TOTAL_MASS = 1;
	public final float MASS_PER_PARTICLE = ((float)TOTAL_MASS)/MAX_PARTICLES;
	public final float RADIUS = 0.9f;
	public final float VISCOSITY = 0.004f;
	public final float IDEAL_RADIUS = 50.0f;
	public final float IDEAL_RADIUS_SQ = IDEAL_RADIUS*IDEAL_RADIUS;
	public final float MULTIPLIER = IDEAL_RADIUS / RADIUS;
	public static final int MAX_NEIGHBORS = 75;
	public static final int NUMBER_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();
	public static final Vec2 GRAVITY = new Vec2(0.0f, -9.8f).mul(1.0f / 3000.0f);
	public static final float MAX_PRESSURE = 0.8f;
	public static final float MAX_PRESSURE_NEAR = 1.6f;
	
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
	
	public FluidSimulation(World world, AABB aabb)
	{
		this.world = world;
		screenAABB = aabb;
		
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
	}
	
	public void createParticle(int numParticlesToSpawn, float x, float y, Player player)
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
					Vec2 jitter = new Vec2((float)(Math.random() * 0.25f - 0.125f), (float)(Math.random()) - 0.125f);
					particle.position = new Vec2(player.body.getPosition().x + jitter.x, player.body.getPosition().y + jitter.y);
					particle.oldPosition = particle.position;
					Vec2 direction = new Vec2(x, y).sub(particle.position);
					direction.normalize();
					particle.velocity = direction.mul(0.4f);
					
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
				if(body.getType() == BodyType.STATIC)
				{
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
							float distance = Vec2.dot(particle.collisionNormals[v], particle.collisionVertices[v].sub(particle.oldPosition));
							if(distance < shortestDistance)
							{
								shortestDistance = distance;
								
								closestPoint = particle.collisionNormals[v].mul(distance).add(particle.oldPosition);
								normal = particle.collisionNormals[v];
							}
						}
						particle.position = closestPoint.add(normal.mul(0.01f));
						//particle.position = particle.oldPosition;
						/*
						final Vec2 lambda = new Vec2(1.0f, 0.0f);
						final Vec2 normalRay = new Vec2();
						world.raycast(new RayCastCallback(){
							public float reportFixture(Fixture fixture, Vec2 point,
									Vec2 normal, float fraction) {
								lambda.set(new Vec2(fraction, 0.0f));
								normalRay.set(normal);
								return fraction;
							}
							
						}, particle.position, particle.oldPosition);
						if(lambda.x != 1.0f)
						{
							Vec2 delta = particle.position.sub(particle.oldPosition);
							delta = delta.mul(lambda.x);
							normalRay.set(normalRay.mul(RADIUS));
							particle.position = particle.oldPosition.add(delta).add(normalRay);
						}
						*/
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
			}
		}
		for(int i = 0; i < Main.boxes.size(); i++)
		{
			boolean collide = false;
			int fixtures = Main.boxes.get(i).body.m_fixtureCount;
			Fixture fixture = Main.boxes.get(i).body.getFixtureList();
			for(int k = 0; k < fixtures; k++)
			{
				if(fixture.testPoint(particle.position))
				{
					if(Keyboard.isKeyDown(Keyboard.KEY_T) || particle.velocity.length() < 0.35f)
					{
						Main.boxes.get(i).numberDisplaced += 0.2f;
						Main.boxes.get(i).body.applyForce(particle.velocity.mul(5.0f), Main.boxes.get(i).body.getPosition());
						collide = true;
					}
				}
				fixture = fixture.getNext();
			}
			Main.boxes.get(i).waterCollide = Main.boxes.get(i).waterCollide || collide;
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
	public void drawFluid(int shaderProgram, float texWidth, float texHeight)
	{
		for (int i = 0; i < numActiveParticles; i++)
	    {
	        Particle particle = liquid[activeParticles.get(i)];
	        float pressure = (particle.pressure - 5f) / 2.0f;
	        float pressureN = particle.pressureNear / 2.0f;
	        float speed = (float)Math.sqrt(particle.velocity.x*particle.velocity.x + particle.velocity.y*particle.velocity.y) * 0.8f;
	        int loc = GL20.glGetUniformLocation(shaderProgram, "speed");
	        if((pressure+0.01f >= MAX_PRESSURE  || pressureN+0.01f >= MAX_PRESSURE_NEAR) && speed >= 0.8f)
	        	GL20.glUniform1f(loc, 0.0f);
	        else
	        	GL20.glUniform1f(loc, speed);
	        GL11.glPushMatrix();
	        GL11.glTranslatef(particle.position.x-(texWidth/2), particle.position.y-(texHeight/2), 0.0f);
	        GL11.glBegin(GL11.GL_QUADS);
		        GL11.glTexCoord2d(0.0,0.0); GL11.glVertex2d(0.0,0.0);
		        GL11.glTexCoord2d(1.0,0.0); GL11.glVertex2d(texWidth,0.0);
		        GL11.glTexCoord2d(1.0,1.0); GL11.glVertex2d(texWidth, texHeight);
		        GL11.glTexCoord2d(0.0,1.0); GL11.glVertex2d(0.0, texHeight);
	       	GL11.glEnd();
	       	GL11.glPopMatrix();
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
			float pressure = (float)Math.min(MAX_PRESSURE, (particle.pressure - 5f) / 2.0f); //normal pressure term
			float presnear = (float)Math.min(MAX_PRESSURE_NEAR, particle.pressureNear / 2.0f); //near particles term
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

			        factor = VISCOSITY * oneminusq * Main.DT;
			        d = d.sub(relativeVelocity.mul(factor));
			        accumulatedDelta[particle.neighbors[a]] =  accumulatedDelta[particle.neighbors[a]].add(d);
			        change = change.sub(d);
			    }
			}
			accumulatedDelta[index] = accumulatedDelta[index].add(change);
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
							particle.collisionVertices[v] = Transform.mul(collisionXF, shape.m_vertices[v]);
				            particle.collisionNormals[v] = Transform.mul(collisionXF, shape.m_normals[v]);
						}
						
						float shortestDistance = 9999999f;
						for(int v = 0; v < shape.getVertexCount(); v++)
						{
							float distance = Vec2.dot(particle.collisionNormals[v], particle.collisionVertices[v].sub(particle.oldPosition));
							if(distance < shortestDistance)
							{
								shortestDistance = distance;
								
								closestPoint = particle.collisionNormals[v].mul(distance).add(particle.oldPosition);
								normal = particle.collisionNormals[v];
							}
						}
						particle.position = closestPoint.add(normal.mul(0.01f));
						//particle.position = particle.oldPosition;
						/*
						final Vec2 lambda = new Vec2(1.0f, 0.0f);
						final Vec2 normalRay = new Vec2();
						world.raycast(new RayCastCallback(){
							public float reportFixture(Fixture fixture, Vec2 point,
									Vec2 normal, float fraction) {
								lambda.set(new Vec2(fraction, 0.0f));
								normalRay.set(normal);
								return fraction;
							}
							
						}, particle.position, particle.oldPosition);
						if(lambda.x != 1.0f)
						{
							Vec2 delta = particle.position.sub(particle.oldPosition);
							delta = delta.mul(lambda.x);
							normalRay.set(normalRay.mul(RADIUS));
							particle.position = particle.oldPosition.add(delta).add(normalRay);
						}
						*/
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
}