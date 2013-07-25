
import org.iforce2d.Jb2dJson;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.contacts.Contact;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.Image;
import org.newdawn.slick.PackedSpriteSheet;
import org.newdawn.slick.SlickException;


public class Player implements ContactListener {
	public Body body;
	public float numberDisplaced;
	public boolean waterCollide;
	public static final float MAX_VELOCITY = 10.0f;
	private float scale;
	private Image currentTexture;
	private PackedSpriteSheet spriteSheet;
	private Fixture footSensor, playerFixture;
	private Fixture[] wallSensors;
	private int fixturesUnderFoot, fixturesByWall;
	private int jumpTimeout;
	private float stillTime;
	private boolean keyTest, faceRight, peeing, running;
	private float runningTick;
	
	public Player(Jb2dJson json)
	{
		super();
		try {
			spriteSheet = new PackedSpriteSheet("res/character_spritesheet.def");
		} catch (SlickException e) {
			e.printStackTrace();
		}
		currentTexture = spriteSheet.getSprite("player_idle.png");
		
		body = json.getBodyByName("player");
		footSensor = json.getFixtureByName("footSensor");
		wallSensors = json.getFixturesByName("wallSensor");
		playerFixture = json.getFixtureByName("playerFixture");
		scale = json.getImageByName("playerTexture").scale;
		faceRight = true;
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
		if(fixturesUnderFoot == 0 || (fixturesByWall > 0 && fixturesUnderFoot!=0))
		{
			playerFixture.setFriction(0.0f);
		}
		else
		{
			if(keyTest && stillTime > 0.2f)
			{
				playerFixture.setFriction(100.0f);
			}
			else
			{
				playerFixture.setFriction(0.4f);
			}
		}
		jumpTimeout--;
		if(Math.abs(body.getLinearVelocity().x)<3.5f || fixturesUnderFoot <= 0)
		{
			running = false;
			runningTick = 0;
		}
		
		if(running)
		{
			runningTick += 0.1f;
			currentTexture = spriteSheet.getSprite("player_running"+(int)((runningTick%3)+1)+".png");
		}
	}
	public void reset()
	{
		numberDisplaced = 0.0f;
		waterCollide = false;
	}
	public void draw()
	{
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		currentTexture.bind();
        GL11.glTranslatef(body.getPosition().x*Main.OPENGL_SCALE, body.getPosition().y*Main.OPENGL_SCALE, 0.0f);
        if(!faceRight)
        	GL11.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        float height = scale * Main.OPENGL_SCALE;
        float ratio = (float)currentTexture.getWidth()/currentTexture.getHeight();
        float width = height*ratio;
        float x = currentTexture.getTextureOffsetX();
        float y = currentTexture.getTextureOffsetY();
        float textureWidth = currentTexture.getTextureWidth();
        float textureHeight = currentTexture.getTextureHeight();
        GL11.glBegin(GL11.GL_QUADS);
        	GL11.glTexCoord2f(x, y); GL11.glVertex2f(-width/2, height/2);
	        GL11.glTexCoord2f(x+textureWidth, y); GL11.glVertex2f(width/2, height/2);
	        GL11.glTexCoord2f(x+textureWidth, y+textureHeight); GL11.glVertex2f(width/2, -height/2);
	        GL11.glTexCoord2f(x, y+textureHeight); GL11.glVertex2f(-width/2, -height/2);
       	GL11.glEnd();
       	GL11.glDisable(GL11.GL_TEXTURE_2D);
       	GL11.glPopMatrix();
	}
	public void mouseDown(float x)
	{
		if(x >= body.getPosition().x)
		{
			faceRight = true;
		}
		else if(x < body.getPosition().x)
		{
			faceRight = false;
		}
		peeing = true;
		currentTexture = spriteSheet.getSprite("player_pissing.png");
	}
	public void mouseUp()
	{
		peeing = false;
		currentTexture = spriteSheet.getSprite("player_idle.png");
	}
	public Fixture getPlayerFixture()
	{
		return playerFixture;
	}
	public Fixture getFootSensor()
	{
		return footSensor;
	}
	public Fixture[] getWallSensor()
	{
		return wallSensors;
	}
	//TODO tweak player movement to be more responsive
	public void moveRight()
	{
		if (body.getLinearVelocity().x <= MAX_VELOCITY)
			body.applyLinearImpulse(new Vec2(12.0f, 0.0f), body.getWorldCenter());
		if(!peeing)
			faceRight = true;
		running = true;
	}
	public void moveLeft()
	{
		if (body.getLinearVelocity().x >= -MAX_VELOCITY)
			body.applyLinearImpulse(new Vec2(-12.0f, 0.0f), body.getWorldCenter());
		if(!peeing)
			faceRight = false;
		running = true;
	}
	public void moveDown()
	{
		body.applyForce(body.getWorld().getGravity().mul(numberDisplaced), body.getWorldCenter());
	}
	public void jump()
	{
		if((fixturesUnderFoot > 0) && jumpTimeout <= 0)
		{
			body.applyLinearImpulse(new Vec2(0.0f, 85.0f), body.getWorldCenter());
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
		if(fixture.equals(wallSensors[0])||fixture.equals(wallSensors[1]))
		{
			fixturesByWall++;
		}
		fixture = contact.getFixtureB();
		if(!fixture.equals(playerFixture) && fixture.equals(footSensor))
		{
			fixturesUnderFoot++;
		}
		if(fixture.equals(wallSensors[0])||fixture.equals(wallSensors[1]))
		{
			fixturesByWall++;
		}
	}

	public void endContact(Contact contact)
	{
		Fixture fixture = contact.getFixtureA();
		if(!fixture.equals(playerFixture) && fixture.equals(footSensor))
		{
			fixturesUnderFoot--;
		}
		if(fixture.equals(wallSensors[0])||fixture.equals(wallSensors[1]))
		{
			fixturesByWall--;
		}
		fixture = contact.getFixtureB();
		if(!fixture.equals(playerFixture) && fixture.equals(footSensor))
		{
			fixturesUnderFoot--;
		}
		if(fixture.equals(wallSensors[0])||fixture.equals(wallSensors[1]))
		{
			fixturesByWall--;
		}
	}

	public void postSolve(Contact arg0, ContactImpulse arg1)
	{
		
	}

	public void preSolve(Contact arg0, Manifold arg1)
	{
		
	}
}
