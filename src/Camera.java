import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;


public class Camera {
	private float screenX, screenY, screenOffsetX, screenOffsetY;
	private Player player;
	private Body boundary;
	
	public Camera(Player player, Body boundary)
	{
		this.player = player;
		this.boundary = boundary;
	}
	
	public void update()
	{
		/*
		if(Math.abs(screenOffsetX) <= maxOffsetX)
		{
			if(player.body.getLinearVelocity().x > 5.0f)
				screenOffsetX += 0.1f;
			else if(player.body.getLinearVelocity().x < 5.0f)
				screenOffsetX -= 0.1f;
		}
		*/
		//TODO Fix screen scrolling here
		
		screenX = player.body.getPosition().x*Main.OPENGL_SCALE - Main.WIDTH/2;
		screenY = player.body.getPosition().y*Main.OPENGL_SCALE - Main.HEIGHT/2;
		Vec2[] screenVertices = new Vec2[4];
		screenVertices[0] = new Vec2(screenX-Main.WIDTH/2, screenY-Main.HEIGHT/2);
		screenVertices[1] = new Vec2(screenX+Main.WIDTH/2, screenY-Main.HEIGHT/2);
		screenVertices[2] = new Vec2(screenX+Main.WIDTH/2, screenY+Main.HEIGHT/2);
		screenVertices[3] = new Vec2(screenX-Main.WIDTH/2, screenY+Main.HEIGHT/2);
		
		Fixture fixture = boundary.getFixtureList();
		Vec2 point = null;
		fixtureLoop:
		for(int i = 0; i < boundary.m_fixtureCount; i++)
		{
			System.out.println(i + ":");
			EdgeShape edge = (EdgeShape)fixture.getShape();
			Vec2 origin = new Vec2(edge.m_vertex1.x*Main.OPENGL_SCALE, edge.m_vertex1.y*Main.OPENGL_SCALE);
			Vec2 direction = new Vec2(edge.m_vertex2.x*Main.OPENGL_SCALE, edge.m_vertex2.y*Main.OPENGL_SCALE).sub(origin);
			point = null;
			boolean hit = false;
			for(float t = 0; t < 1.0f; t += 0.0001f)
			{
				point = origin.add(direction.mul(t));
				if(point.x > screenX-Main.WIDTH/2 && point.x < screenX+Main.WIDTH/2 && point.y > screenY-Main.HEIGHT/2 && point.y < screenY+Main.HEIGHT/2)
				{
					hit = true;
					break;
				}
			}
			System.out.println(hit);
			if(hit)
			{
				float shortestDistance = Float.MAX_VALUE;
				Vec2 vertex = new Vec2();
				for(int v = 0; v < screenVertices.length; v++)
				{
					float distance = point.sub(screenVertices[i]).length();
					if(distance < shortestDistance)
					{
						shortestDistance = distance;
						vertex = screenVertices[v];
					}
				}
				Vec2 displacement = vertex.sub(point);
				screenX = point.x + vertex.x;
				screenY = point.y + vertex.y;
				break fixtureLoop;
			}
			fixture = fixture.getNext();
		}
		
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glColor3f(0.0f, 0.0f, 0.0f);
		GL11.glLoadIdentity();
		GLU.gluLookAt(screenX, screenY, 1.0f, screenX, screenY, 0.0f, 0.0f, 1.0f, 0.0f);
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
