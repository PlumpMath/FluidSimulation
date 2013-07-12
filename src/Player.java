import java.io.IOException;

import org.iforce2d.Jb2dJson;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.contacts.Contact;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;


public class Player implements ContactListener {
	public Body body;
	public float numberDisplaced;
	public boolean waterCollide;
	public static final float MAX_VELOCITY = 10.0f;
	private Texture playerTexture;
	private Fixture footSensor, playerFixture;
	private int fixturesUnderFoot;
	private int jumpTimeout;
	private float stillTime;
	private boolean keyTest;
	
	public Player(Jb2dJson json)
	{
		super();
		try {
			playerTexture = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream("res/player_idle.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		
		body = json.getBodyByName("player");
		footSensor = json.getFixtureByName("footSensor");
		playerFixture = json.getFixtureByName("playerFixture");
	}
	public void update()
	{
		body.applyForce(body.getWorld().getGravity().mul(-numberDisplaced), body.getPosition());
		if(waterCollide)
		{
			body.setLinearDamping(1.0f);
		}
		if(Math.abs(body.getLinearVelocity().x) >= MAX_VELOCITY)
		{
			float vx = Math.signum(body.getLinearVelocity().x) * MAX_VELOCITY;
			body.setLinearVelocity(new Vec2(vx, body.getLinearVelocity().y));
		}
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
	public void reset()
	{
		numberDisplaced = 0.0f;
		waterCollide = false;
	}
	public void draw()
	{
		PolygonShape shape = ((PolygonShape)playerFixture.getShape());
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		playerTexture.bind();
        GL11.glTranslatef(body.getPosition().x*Main.OPENGL_SCALE, body.getPosition().y*Main.OPENGL_SCALE, 0.0f);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        Vec2[] vertices = ((PolygonShape)playerFixture.getShape()).getVertices();
        float textureWidth = (float)playerTexture.getImageWidth()/Main.get2Fold(playerTexture.getImageWidth());
        float textureHeight = (float)playerTexture.getImageHeight()/Main.get2Fold(playerTexture.getImageHeight());
        GL11.glBegin(GL11.GL_QUADS);
        	GL11.glTexCoord2f(textureWidth, 0.0f); GL11.glVertex2f(vertices[0].x*Main.OPENGL_SCALE, vertices[0].y*Main.OPENGL_SCALE);
        	GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex2f(vertices[1].x*Main.OPENGL_SCALE, vertices[1].y*Main.OPENGL_SCALE);
	        GL11.glTexCoord2f(0.0f, textureHeight); GL11.glVertex2f(vertices[2].x*Main.OPENGL_SCALE, vertices[2].y*Main.OPENGL_SCALE);
	        GL11.glTexCoord2f(textureWidth, textureHeight); GL11.glVertex2f(vertices[3].x*Main.OPENGL_SCALE, vertices[3].y*Main.OPENGL_SCALE);
       	GL11.glEnd();
       	GL11.glDisable(GL11.GL_TEXTURE_2D);
       	GL11.glPopMatrix();
	}
	public Fixture getPlayerFixture()
	{
		return playerFixture;
	}
	public Fixture getFootSensor()
	{
		return footSensor;
	}
	//TODO tweak player movement to be more responsive
	public void moveRight()
	{
		if(body.getLinearVelocity().x <= MAX_VELOCITY)
			body.applyLinearImpulse(new Vec2(12.0f, 0.0f), body.getWorldCenter());
	}
	public void moveLeft()
	{
		if(body.getLinearVelocity().x >= -MAX_VELOCITY)
			body.applyLinearImpulse(new Vec2(-12.0f, 0.0f), body.getWorldCenter());
	}
	public void moveDown()
	{
		body.applyForce(body.getWorld().getGravity().mul(numberDisplaced), body.getWorldCenter());
	}
	public void jump()
	{
		if((fixturesUnderFoot > 0) && jumpTimeout <= 0)
		{
			body.applyLinearImpulse(new Vec2(0.0f, 80.0f), body.getWorldCenter());
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
		Fixture fixture = contact.getFixtureA();
		if(!fixture.equals(playerFixture) && fixture.equals(footSensor))
		{
			fixturesUnderFoot++;
		}
		fixture = contact.getFixtureB();
		if(!fixture.equals(playerFixture) && fixture.equals(footSensor))
		{
			fixturesUnderFoot++;
		}
	}

	public void endContact(Contact contact)
	{
		Fixture fixture = contact.getFixtureA();
		if(!fixture.equals(playerFixture) && fixture.equals(footSensor))
		{
			fixturesUnderFoot--;
		}
		fixture = contact.getFixtureB();
		if(!fixture.equals(playerFixture) && fixture.equals(footSensor))
		{
			fixturesUnderFoot--;
		}
	}

	public void postSolve(Contact arg0, ContactImpulse arg1)
	{
		
	}

	public void preSolve(Contact arg0, Manifold arg1)
	{
		
	}
}
