import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;


public class Button implements ContactListener{
	private Fixture sensor;
	private ActionCommand action;
	private int numContacts;
	
	public Button(Vec2 position, World world, ActionCommand command)
	{
		BodyDef bd = new BodyDef();
		bd.fixedRotation = true;
		bd.type = BodyType.STATIC;
     	bd.position.set(position);
     	
     	Body body = world.createBody(bd);
     	PolygonShape shape = new PolygonShape();
     	shape.setAsBox(0.3f, 0.3f, new Vec2(0.0f, 0.0f), 0.0f);
     	
     	FixtureDef fixture = new FixtureDef();
     	fixture.shape = shape;
     	fixture.userData = "button";
        fixture.isSensor = true;
        sensor = body.createFixture(fixture);
        
        action = command;
	}
	
	public void update()
	{
		if(numContacts > 0)
			action.doCommand();
		else
			action.undoCommand();
	}
	
	public void beginContact(Contact contact) {
		Object fixtureUserData = contact.getFixtureA().getUserData();
		if(fixtureUserData instanceof String)
		{
			String string = (String)fixtureUserData;
			if(!string.equals("ground"))
			{
				numContacts++;
			}
		}
		fixtureUserData = contact.getFixtureB().getUserData();
		if(fixtureUserData instanceof String)
		{
			String string = (String)fixtureUserData;
			if(!string.equals("ground"))
			{
				numContacts++;
			}
		}
	}
	public void endContact(Contact contact) {
		Object fixtureUserData = contact.getFixtureA().getUserData();
		if(fixtureUserData instanceof String)
		{
			String string = (String)fixtureUserData;
			if(!string.equals("ground"))
			{
				numContacts--;
			}
		}
		fixtureUserData = contact.getFixtureB().getUserData();
		if(fixtureUserData instanceof String)
		{
			String string = (String)fixtureUserData;
			if(!string.equals("ground"))
			{
				numContacts--;
			}
		}
	}

	@Override
	public void postSolve(Contact arg0, ContactImpulse arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preSolve(Contact arg0, Manifold arg1) {
		// TODO Auto-generated method stub
		
	}
}
