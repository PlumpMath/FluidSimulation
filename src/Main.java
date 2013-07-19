import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;


public class Main {
	public static int WIDTH = 10, HEIGHT = 10;
	public static final float BOX2D_SCALE = 1.0f/30.0f;
	public static final float OPENGL_SCALE = 1.0f/BOX2D_SCALE;
	public static final float DT = 1.0f / 60.0f;
	//public static final Vec2 buoyancyForce = new Vec2(0.0f, 10.0f);
	
	static long variableYieldTime;
	public static long lastTime;
	
	//Rendering things
	private int shaderProgram;
    private int vertexShader;
    private int fragmentShader;
    
    public static Level currentLevel;
    public static int level = 1;
	
	public static void main(String[] args)
	{
		Main game = new Main();
		game.start();
	}
	
	public void initGL() {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glOrtho(0, WIDTH, 0, HEIGHT,-10.0f, 10.0f);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glHint(GL11.GL_POLYGON_SMOOTH, GL11.GL_NICEST);
		//GL11.glEnable(GL11.GL_POINT_SMOOTH);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glViewport(0, 0, WIDTH, HEIGHT);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		//GL11.glEnable(GL11.GL_CULL_FACE);
		//GL11.glCullFace(GL11.GL_BACK);
		//GL11.glPointSize(2);
	}
	public void initShaderProgram()
	{
		shaderProgram = GL20.glCreateProgram();
        vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        StringBuilder vertexShaderSource = new StringBuilder();
        StringBuilder fragmentShaderSource = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("src/shader.vs"));
            String line;
            while ((line = reader.readLine()) != null) {
                vertexShaderSource.append(line).append('\n');
            }
        } catch (IOException e) {
            System.err.println("Vertex shader wasn't loaded properly.");
            e.printStackTrace();
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        BufferedReader reader2 = null;
        try {
            reader2 = new BufferedReader(new FileReader("src/shader.fs"));
            String line;
            while ((line = reader2.readLine()) != null) {
                fragmentShaderSource.append(line).append('\n');
            }
        } catch (IOException e) {
            System.err.println("Fragment shader wasn't loaded properly.");
            Display.destroy();
            System.exit(1);
        } finally {
            if (reader2 != null) {
                try {
                    reader2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        GL20.glShaderSource(vertexShader, vertexShaderSource);
        GL20.glCompileShader(vertexShader);
        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Vertex shader wasn't able to be compiled correctly.");
        }
        GL20.glShaderSource(fragmentShader, fragmentShaderSource);
        GL20.glCompileShader(fragmentShader);
        if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("Fragment shader wasn't able to be compiled correctly.");
        }
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        GL20.glValidateProgram(shaderProgram);
	}
	public void destroyShaderProgram()
	{
		GL20.glDeleteProgram(shaderProgram);
		GL20.glDeleteShader(vertexShader);
		GL20.glDeleteShader(fragmentShader);
	}
	public void setTextureUnit0(int programId) {
	    int loc = GL20.glGetUniformLocation(programId, "texture1");
	    GL20.glUniform1i(loc, 0);
	}
	public void start()
	{
		try {
			DisplayMode[] modes = Display.getAvailableDisplayModes();
			int i;
			float ratio = 640.0f/480.0f;
			float biggest = Float.MIN_VALUE;
			for(i = 0; i < modes.length; i++)
			{
				float tempRatio = (float)modes[i].getWidth()/modes[i].getHeight();
				if(tempRatio == ratio)
				{
					if(tempRatio > biggest)
					{
						biggest = tempRatio;
						WIDTH = modes[i].getWidth();
						HEIGHT = modes[i].getHeight();
					}
				}
			}
			Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
			Display.setFullscreen(true);
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		initGL(); // init OpenGL 
		initShaderProgram();
		setTextureUnit0(shaderProgram);
		
		currentLevel = new Level(level);

		while (!Display.isCloseRequested()) {
			update();

			Display.update();
			sync((int)(1.0f/DT));
		}
		destroyShaderProgram();
		Display.destroy();
	}
	public static void nextLevel()
	{
		currentLevel = new Level(++level);
	}
	public void update()
	{
		currentLevel.pollInput();
		
		currentLevel.logic();
		
		// draw level
		currentLevel.drawFluid(shaderProgram);
		currentLevel.renderPlayer();
		currentLevel.renderObjects();
	}	
	public static int get2Fold(int fold)
	{
		int ret = 2;
		while(ret<fold)
			ret*=2;
		return ret;
	}
	public static void drawCircle(float cx, float cy, float r, int num_segments)
	{
		float theta = 2.0f * 3.1415926f / (float)num_segments; 
		float tangetial_factor = (float)Math.tan(theta);//calculate the tangential factor 

		float radial_factor = (float)Math.cos(theta);//calculate the radial factor 
		
		float x = r;//we start at angle = 0 

		float y = 0; 
	    
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		for(int ii = 0; ii < num_segments; ii++) 
		{ 
			GL11.glVertex2f(x + cx, y + cy);//output vertex 
	        
			//calculate the tangential vector 
			//remember, the radial vector is (x, y) 
			//to get the tangential vector we flip those coordinates and negate one of them 

			float tx = -y; 
			float ty = x; 
	        
			//add the tangential vector 

			x += tx * tangetial_factor; 
			y += ty * tangetial_factor; 
	        
			//correct using the radial factor 

			x *= radial_factor; 
			y *= radial_factor; 
		} 
		GL11.glEnd(); 
	}
	
	
	//Time keeping stuff
	/**
	 * An accurate sync method that adapts automatically
	 * to the system it runs on to provide reliable results.
	 * 
	 * @param fps The desired frame rate, in frames per second
	 */
	public static void sync(int fps) {
		if (fps <= 0) return;
		
		long sleepTime = 1000000000 / fps; // nanoseconds to sleep this frame
		// yieldTime + remainder micro & nano seconds if smaller than sleepTime
		long yieldTime = Math.min(sleepTime, variableYieldTime + sleepTime % (1000*1000));
		long overSleep = 0; // time the sync goes over by
		
		try {
			while (true) {
				long t = getTime() - lastTime;
				
				if (t < sleepTime - yieldTime) {
					Thread.sleep(1);
				}
				else if (t < sleepTime) {
					// burn the last few CPU cycles to ensure accuracy
					Thread.yield();
				}
				else {
					overSleep = t - sleepTime;
					break; // exit while loop
				}
			}
		} catch (InterruptedException e) {}
		
		lastTime = getTime() - Math.min(overSleep, sleepTime);
		
		// auto tune the time sync should yield
		if (overSleep > variableYieldTime) {
			// increase by 200 microseconds (1/5 a ms)
			variableYieldTime = Math.min(variableYieldTime + 200*1000, sleepTime);
		}
		else if (overSleep < variableYieldTime - 200*1000) {
			// decrease by 2 microseconds
			variableYieldTime = Math.max(variableYieldTime - 2*1000, 0);
		}
	}

	/**
	 * Get System Nano Time
	 * @return will return the current time in nano's
	 */
	public static long getTime() {
	    return (Sys.getTime() * 1000000000) / Sys.getTimerResolution();
	}

}
