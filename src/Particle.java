import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Fixture;

public class Particle {
	public static final int MAX_FIXTURES_TO_TEST = 30;
	public Vec2 position, oldPosition, velocity;
	public float[] distances;
	public int[] neighbors;
	public Fixture[] fixtures;
	public int numFixturesToTest;
	public int neighborCount;
	public int ci, cj;
	public boolean alive, sendColor;
	public float pressure, pressureNear;
	public int index;
	public Vec2[] collisionVertices, collisionNormals;
	
	public Particle(boolean alive)
	{
		this.alive = alive;
		distances = new float[FluidSimulation.MAX_NEIGHBORS];
		neighbors = new int[FluidSimulation.MAX_NEIGHBORS];
		fixtures = new Fixture[MAX_FIXTURES_TO_TEST];
		collisionVertices = new Vec2[Settings.maxPolygonVertices];
		collisionNormals = new Vec2[Settings.maxPolygonVertices];
	}
}
