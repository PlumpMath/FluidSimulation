import org.jbox2d.dynamics.Body;

public class Particle {
	public Body body;
	public float[] distances;
	public int[] neighbors;
	public int neighborCount;
	public int ci, cj;
	public boolean alive;
	public float pressure, pressureNear;
	public int index;
	
	public Particle(boolean alive)
	{
		this.alive = alive;
		
		distances = new float[FluidSimulation.MAX_NEIGHBORS];
		neighbors = new int[FluidSimulation.MAX_NEIGHBORS];
	}
}
