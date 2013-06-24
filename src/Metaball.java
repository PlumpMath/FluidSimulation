import org.jbox2d.common.Vec2;


public class Metaball {
	public Vec2 position;
	public Vec2 initialPosition;
	public Vec2 edgePosition;
	public boolean tracking;
	public float size;
	
	public Metaball(Vec2 pos, float s)
	{
		position = pos;
		size = s;
	}
}
