import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;


public class Camera {
	private float screenX, screenY, screenOffsetX, screenOffsetY;
	private final float maxOffsetX = 20.0f, maxOffsetY = 20.0f;
	private Player player;
	
	public Camera(Player player)
	{
		this.player = player;
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
		
		screenX = player.body.getPosition().x - Main.BOX2D_WIDTH/3 - screenOffsetX;
		screenY = player.body.getPosition().y - Main.BOX2D_HEIGHT/3;
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
