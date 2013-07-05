import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;


public class Player extends Box implements ContactListener {
	private Fixture footSensor;
	private int fixturesUnderFoot;
	public int jumpTimeout;
	
	public Player(Vec2 position, World world)
	{
		super();
		BodyDef bd = new BodyDef();
		bd.fixedRotation = true;
		bd.type = BodyType.DYNAMIC;
     	bd.position.set(position);
      
     	body = world.createBody(bd);
     	PolygonShape shape = new PolygonShape();

     	shape.setAsBox(1.0f, 1.0f);
     	FixtureDef fixture = new FixtureDef();
     	fixture.shape = shape;
     	fixture.density = 1.0f;
     	fixture.friction = 2.0f;
     	body.createFixture(fixture);
     	
     	shape.setAsBox(0.3f, 0.3f, new Vec2(0.0f,-1.25f), 0.0f);
        fixture.isSensor = true;
        fixture.userData = -1;
        footSensor = body.createFixture(fixture);
	}
	public void update()
	{
		jumpTimeout--;
	}
	//TODO tweak player movement to be more responsive
	public void moveRight()
	{
		body.applyForce(new Vec2(150.0f, 0.0f), body.getPosition());
	}
	public void moveLeft()
	{
		body.applyForce(new Vec2(-150.0f, 0.0f), body.getPosition());
	}
	public void moveDown()
	{
		body.applyForce(body.getWorld().getGravity().mul(-1.1f), body.getPosition());
	}
	public void jump()
	{
		if(fixturesUnderFoot > 1 && jumpTimeout <= 0)
		{
			body.applyLinearImpulse(new Vec2(0.0f, body.getMass() * 22.0f), body.getWorldCenter());
			jumpTimeout = 20;
		}
	}

	public void beginContact(Contact contact)
	{
		fixturesUnderFoot++;
	}

	public void endContact(Contact contact)
	{
		fixturesUnderFoot--;
	}

	public void postSolve(Contact arg0, ContactImpulse arg1)
	{
		
	}

	public void preSolve(Contact arg0, Manifold arg1)
	{
		
	}
}
