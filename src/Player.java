import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;


public class Player extends Box {
	public Player(Vec2 position, World world)
	{
		super(position, world);
	}
	public void moveRight()
	{
		body.applyForce(new Vec2(100.0f, 0.0f), body.getPosition());
	}
	public void moveLeft()
	{
		body.applyForce(new Vec2(-100.0f, 0.0f), body.getPosition());
	}
	public void moveDown()
	{
		body.applyForce(body.getWorld().getGravity().mul(-1.1f), body.getPosition());
	}
}
