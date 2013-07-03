import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;


public class Box {
	public Body body;
	public float numberDisplaced;
	public boolean collide;
	
	public Box(Vec2 position, World world)
	{
		BodyDef bd = new BodyDef();
		bd.fixedRotation = true;
		bd.type = BodyType.DYNAMIC;
     	bd.position.set(0.0f, 0.0f);
      
     	body = world.createBody(bd);
     	PolygonShape shape = new PolygonShape();

     	shape.setAsBox(1.0f, 1.0f, position, 0.0f);
     	FixtureDef fixture = new FixtureDef();
     	fixture.shape = shape;
     	fixture.density = 1.0f;
     	fixture.friction = 0.2f;
     	body.createFixture(fixture);
	}
}
