import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;


public class Level1 extends Level{
	
	public Level1()
	{
		Body ground = null;
		BodyDef bd = new BodyDef();
     	bd.position.set(0.0f, 0.0f);
     	
     	ground = world.createBody(bd);
     	PolygonShape shape = new PolygonShape();
     	
     	FixtureDef fd = new FixtureDef();
     	fd.userData = "ground";
     	fd.shape = shape;
     	shape.setAsBox(20.0f, 0.9f, new Vec2(20.0f, 0.9f), 0.0f);
     	ground.createFixture(fd);
      
     	shape.setAsBox(0.9f, 7.5f, new Vec2(1.0f, 7.5f), 0.0f);
     	ground.createFixture(fd);
     	
     	shape.setAsBox(0.5f, 4.5f, new Vec2(4.75f, 7.5f), 45.0f);
     	ground.createFixture(fd);
      
     	shape.setAsBox(0.9f, 7.5f, new Vec2(21.0f, 11.5f), 0.0f);
     	ground.createFixture(fd);
     	
     	shape.setAsBox(0.9f, 7.5f, new Vec2(31.0f, 7.5f), 0.0f);
     	ground.createFixture(fd);
     	
     	shape.setAsBox(0.5f, 4.5f, new Vec2(17.2f, 7.5f), -45.0f);
     	ground.createFixture(fd);
		
		buttons.add(new Button(new Vec2(4.75f, 3.75f), world, new ActionCommand(){
			public void doCommand() {
				boxes.add(new Box(new Vec2(10.0f, 10.0f), world));
			}

			public void undoCommand() {
				
			}
	    }));
	    player = new Player(new Vec2(10.5f, 4.0f), world);
	    boxes.add(player);
	    camera = new Camera(player);
	}
}
