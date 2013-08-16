package net.logosstudios.how2urinate;
import java.io.IOException;

import org.iforce2d.Jb2dJsonImage;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.contacts.Contact;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;


public class Button implements ContactListener{
	private Fixture sensor;
	private Texture pressed, unpressed, current;
	private Jb2dJsonImage info;
	private ActionCommand action;
	private int numContacts;
	private boolean pressable;
	
	public Button(Fixture sensor, Jb2dJsonImage info, ActionCommand command, boolean pressable)
	{
		this.sensor = sensor;
		this.info = info;
		String imageLocation = info.file.substring(3);
		action = command;
		this.pressable = pressable;
		try {
			pressed = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream(Main.getLocation("res/button_pressed.png")));
			unpressed = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream(Main.getLocation(imageLocation)));
			current = unpressed;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void update()
	{
		if(numContacts > 0)
		{
			action.doCommand();
			if(pressable)
				current = pressed;
		}
		else
		{
			action.undoCommand();
			current = unpressed;
		}
	}
	
	public void draw()
	{
		float height = info.scale * Main.OPENGL_SCALE;
        float ratio = (float)current.getImageWidth()/current.getImageHeight();
        float width = height*ratio;
        float textureWidth = (float)current.getImageWidth()/Main.get2Fold(current.getImageWidth());
        float textureHeight = (float)current.getImageHeight()/Main.get2Fold(current.getImageHeight());
		GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPushMatrix();
		current.bind();
		GL11.glTranslatef((sensor.getBody().getPosition().x+info.center.x)*Main.OPENGL_SCALE, (sensor.getBody().getPosition().y+info.center.y)*Main.OPENGL_SCALE, 0.0f);
		GL11.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
        GL11.glBegin(GL11.GL_QUADS);
        	GL11.glTexCoord2f(textureWidth, textureHeight); GL11.glVertex2f(-width/2, -height/2);
        	GL11.glTexCoord2f(0.0f, textureHeight); GL11.glVertex2f(width/2, -height/2);
        	GL11.glTexCoord2f(0.0f, 0.0f); GL11.glVertex2f(width/2, height/2);
        	GL11.glTexCoord2f(textureWidth, 0.0f); GL11.glVertex2f(-width/2, height/2);
		GL11.glEnd();
		GL11.glPopMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
	}
	public void beginContact(Contact contact) {
		Fixture fixture = contact.getFixtureA();
		if(fixture.equals(sensor))
		{
			numContacts++;
		}
		fixture = contact.getFixtureB();
		if(fixture.equals(sensor))
		{
			numContacts++;
		}
	}
	public void endContact(Contact contact) {
		Fixture fixture = contact.getFixtureA();
		if(fixture.equals(sensor))
		{
			numContacts--;
		}
		fixture = contact.getFixtureB();
		if(fixture.equals(sensor))
		{
			numContacts--;
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
