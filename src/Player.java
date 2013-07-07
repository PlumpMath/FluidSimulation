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
import org.lwjgl.input.Keyboard;


public class Player extends Box implements ContactListener {
	public static final float MAX_VELOCITY = 13.0f;
	private Fixture footSensor, playerFixture;
	private int fixturesUnderFoot;
	private int jumpTimeout;
	private float stillTime;
	private boolean keyTest;
	
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
     	fixture.friction = 0.2f;
     	fixture.userData = 1;
     	playerFixture = body.createFixture(fixture);
     	
     	shape.setAsBox(0.3f, 0.3f, new Vec2(0.0f,-1.25f), 0.0f);
        fixture.isSensor = true;
        fixture.userData = -1;
        footSensor = body.createFixture(fixture);
	}
	public void update()
	{
		if(Math.abs(body.getLinearVelocity().x) >= MAX_VELOCITY)
		{
			float vx = Math.signum(body.getLinearVelocity().x) * MAX_VELOCITY;
			body.setLinearVelocity(new Vec2(vx, body.getLinearVelocity().y));
		}
		System.out.println(playerFixture.getFriction());
		if(fixturesUnderFoot == 0)
		{
			footSensor.setFriction(0.0f);
			playerFixture.setFriction(0.0f);
		}
		else
		{
			if(keyTest && stillTime > 0.2f)
			{
				playerFixture.setFriction(100.0f);
				footSensor.setFriction(100.0f);
			}
			else
			{
				playerFixture.setFriction(0.4f);
				footSensor.setFriction(0.4f);
			}
		}
		jumpTimeout--;
	}
	//TODO tweak player movement to be more responsive
	public void moveRight()
	{
		if(body.getLinearVelocity().x <= MAX_VELOCITY)
			body.applyLinearImpulse(new Vec2(6.0f, 0.0f), body.getWorldCenter());
	}
	public void moveLeft()
	{
		if(body.getLinearVelocity().x >= -MAX_VELOCITY)
			body.applyLinearImpulse(new Vec2(-6.0f, 0.0f), body.getWorldCenter());
	}
	public void moveDown()
	{
		body.applyForce(body.getWorld().getGravity().mul(numberDisplaced), body.getWorldCenter());
	}
	public void jump()
	{
		if((fixturesUnderFoot > 0) && jumpTimeout <= 0)
		{
			body.applyLinearImpulse(new Vec2(0.0f, body.getMass()*30.0f), body.getWorldCenter());
			jumpTimeout = 20;
		}
	}
	public void addStillTime(float time)
	{
		stillTime++;
	}
	public void resetStillTime()
	{
		stillTime = 0;
	}
	public void setKeyTest(boolean key)
	{
		keyTest = key;
	}

	public void beginContact(Contact contact)
	{
		Object fixtureUserData = contact.getFixtureA().getUserData();
		if(fixtureUserData instanceof Integer)
		{
			int integer = (Integer)fixtureUserData;
			if(integer == -1)
			{
				fixturesUnderFoot++;
			}
		}
		fixtureUserData = contact.getFixtureB().getUserData();
		if(fixtureUserData instanceof Integer)
		{
			int integer = (Integer)fixtureUserData;
			if(integer == -1)
			{
				fixturesUnderFoot++;
			}
		}
	}

	public void endContact(Contact contact)
	{
		Object fixtureUserData = contact.getFixtureA().getUserData();
		if(fixtureUserData instanceof Integer)
		{
			int integer = (Integer)fixtureUserData;
			if(integer == -1)
			{
				fixturesUnderFoot--;
			}
		}
		fixtureUserData = contact.getFixtureB().getUserData();
		if(fixtureUserData instanceof Integer)
		{
			int integer = (Integer)fixtureUserData;
			if(integer == -1)
			{
				fixturesUnderFoot--;
			}
		}
	}

	public void postSolve(Contact arg0, ContactImpulse arg1)
	{
		
	}

	public void preSolve(Contact arg0, Manifold arg1)
	{
		
	}
}
