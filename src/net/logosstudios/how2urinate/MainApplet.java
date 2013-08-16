package net.logosstudios.how2urinate;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;



public class MainApplet extends Applet{
	private static final long serialVersionUID = 1L;
	Canvas parent;
	Thread gameThread;
	
	public void startLWJGL()
	{
		gameThread.start();
	}
	private void stopLWJGL()
	{
		Display.destroy();
		try{
			gameThread.join();
		}catch(InterruptedException e){
			e.printStackTrace();
		}
	}
	public void init()
	{
		System.out.println("init");
		Main main = new Main(this);
	}
	public void initMain(Thread thread)
	{
		System.out.println("initMain");
		gameThread = thread;
		setLayout(new BorderLayout());
		try{
			parent = new Canvas(){
				private static final long serialVersionUID = 1L;
				public final void addNotify(){
					super.addNotify();
					startLWJGL();
				}
				public final void removeNotify()
				{
					stopLWJGL();
					super.removeNotify();
				}
			};
			parent.setSize(Main.WIDTH, Main.HEIGHT);
			add(parent);
			parent.setFocusable(true);
			parent.requestFocus();
			parent.setIgnoreRepaint(true);
			setVisible(true);
		}catch(Exception e){
			System.err.println(e);
			throw new RuntimeException("Unable to create display");
		}
	}
	
	public void start()
	{
		
	}
	
	public void stop()
	{
		
	}
	
	public void destroy()
	{
		remove(parent);
		super.destroy();
	}
}
