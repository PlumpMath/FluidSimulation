package net.logosstudios.how2urinate;
import java.io.IOException;

import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;


public class Camera {
	private float screenX, screenY;
	private Player player;
	private Body boundary;
	private Texture background;
	
	public Camera(Player player, Body boundary, String background)
	{
		this.player = player;
		this.boundary = boundary;
		try {
			this.background = TextureLoader.getTexture("PNG", ResourceLoader.getResourceAsStream(Main.getLocation(background)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void reset()
	{
		screenX = 0;
		screenY = 0;
	}
	public void update()
	{
		screenX = player.body.getPosition().x*Main.OPENGL_SCALE - Main.WIDTH/2;
		screenY = player.body.getPosition().y*Main.OPENGL_SCALE - Main.HEIGHT/2;
		float bodyPosX = screenX;
		float bodyPosY = screenY;
		
		Fixture fixture = boundary.getFixtureList();
		Vec2 max = new Vec2(Float.MIN_VALUE, Float.MIN_VALUE);
		Vec2 min = new Vec2(Float.MAX_VALUE, Float.MAX_VALUE);
		for(int i = 0; i < boundary.m_fixtureCount; i++)
		{
			EdgeShape edge = (EdgeShape)fixture.getShape();
			Vec2 vertex1 = new Vec2((fixture.getBody().getPosition().x+edge.m_vertex1.x)*Main.OPENGL_SCALE,
					(fixture.getBody().getPosition().y+edge.m_vertex1.y)*Main.OPENGL_SCALE);
			Vec2 vertex2 = new Vec2((fixture.getBody().getPosition().x+edge.m_vertex2.x)*Main.OPENGL_SCALE,
					(fixture.getBody().getPosition().y+edge.m_vertex2.y)*Main.OPENGL_SCALE);
			if(vertex1.x > max.x && vertex1.y > max.y)
				max = vertex1;
			if(vertex1.x < min.x && vertex1.y < min.y)
				min = vertex1;
			if(vertex2.x > max.x && vertex2.y > max.y)
				max = vertex2;
			if(vertex2.x < min.x && vertex2.y < min.y)
				min = vertex2;
			fixture = fixture.getNext();
		}
		
		if(bodyPosX+Main.WIDTH > max.x)
			screenX = max.x-Main.WIDTH;
		if(bodyPosY+Main.HEIGHT > max.y)
			screenY = max.y-Main.HEIGHT;
		if(bodyPosX <= min.x)
			screenX = min.x;
		if(bodyPosY < min.y)
			screenY = min.y;
		
		Vec2[] screenVertices = new Vec2[4];
		screenVertices[0] = new Vec2(screenX-Main.WIDTH/2, screenY-Main.HEIGHT/2);
		screenVertices[1] = new Vec2(screenX+Main.WIDTH/2, screenY-Main.HEIGHT/2);
		screenVertices[2] = new Vec2(screenX+Main.WIDTH/2, screenY+Main.HEIGHT/2);
		screenVertices[3] = new Vec2(screenX-Main.WIDTH/2, screenY+Main.HEIGHT/2);
		
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		//GL11.glColor3f(0.0f, 0.0f, 0.0f);
		GL11.glLoadIdentity();
		GLU.gluLookAt(screenX, screenY, 1.0f, screenX, screenY, 0.0f, 0.0f, 1.0f, 0.0f);
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glPushMatrix();
		background.bind();
		GL11.glTranslatef(Main.WIDTH/2, Main.HEIGHT/2, 0.0f);
		float textureWidth = (float)background.getImageWidth()/Main.get2Fold(background.getImageWidth());
        float textureHeight = (float)background.getImageHeight()/Main.get2Fold(background.getImageHeight());
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glTexCoord2f(0.0f, textureHeight);	GL11.glVertex2f(screenVertices[0].x, screenVertices[0].y);
		GL11.glTexCoord2f(textureWidth, textureHeight);	GL11.glVertex2f(screenVertices[1].x, screenVertices[1].y);
		GL11.glTexCoord2f(textureWidth, 0.0f);	GL11.glVertex2f(screenVertices[2].x, screenVertices[2].y);
		GL11.glTexCoord2f(0.0f, 0.0f);	GL11.glVertex2f(screenVertices[3].x, screenVertices[3].y);
		GL11.glEnd();
		GL11.glPopMatrix();
		GL11.glDisable(GL11.GL_TEXTURE_2D);
	}
	public boolean testBoundary(Vec2 point)
	{
		Fixture fixture = boundary.getFixtureList();
		Vec2 max = new Vec2(Float.MIN_VALUE, Float.MIN_VALUE);
		Vec2 min = new Vec2(Float.MAX_VALUE, Float.MAX_VALUE);
		for(int i = 0; i < boundary.m_fixtureCount; i++)
		{
			EdgeShape edge = (EdgeShape)fixture.getShape();
			Vec2 vertex1 = new Vec2((fixture.getBody().getPosition().x+edge.m_vertex1.x)*Main.OPENGL_SCALE,
					(fixture.getBody().getPosition().y+edge.m_vertex1.y)*Main.OPENGL_SCALE);
			Vec2 vertex2 = new Vec2((fixture.getBody().getPosition().x+edge.m_vertex2.x)*Main.OPENGL_SCALE,
					(fixture.getBody().getPosition().y+edge.m_vertex2.y)*Main.OPENGL_SCALE);
			if(vertex1.x > max.x && vertex1.y > max.y)
				max = vertex1;
			if(vertex1.x < min.x && vertex1.y < min.y)
				min = vertex1;
			if(vertex2.x > max.x && vertex2.y > max.y)
				max = vertex2;
			if(vertex2.x < min.x && vertex2.y < min.y)
				min = vertex2;
			fixture = fixture.getNext();
		}
		
		if((point.x < max.x && point.x > min.x) && (point.y < max.y && point.y > min.y))
			return true;
		return false;
	}
	
	public float getScreenX()
	{
		return screenX;
	}
	public float getScreenY()
	{
		return screenY;
	}
}
