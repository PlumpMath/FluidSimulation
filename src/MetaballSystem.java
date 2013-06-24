import org.jbox2d.common.Vec2;
import org.lwjgl.opengl.GL11;


public class MetaballSystem {
	public Metaball[] balls;
	public float goo, threshold, minSize;
	
	public MetaballSystem(Metaball[] balls, float goo, float threshold)
	{
		this.balls = balls;
		this.goo = goo;
		this.threshold = threshold;
		float minSize = Float.MAX_VALUE;
		for(int i = 0; i < this.balls.length; i++)
		{
			if(this.balls[i].size < minSize)
				minSize = this.balls[i].size;
		}
	}
	
	public void drawBalls(float step)
	{
		for(int i = 0; i < balls.length; i++)
		{
			Metaball ball = balls[i];
			ball.initialPosition = trackBorder(new Vec2(ball.position.x, ball.position.y+1.0f));
			ball.edgePosition = ball.initialPosition;
			ball.tracking = true;
		}
		
		int loopIndex = 0;
		while(loopIndex < 200)
		{
			loopIndex += 1;
			for(int i = 0; i < balls.length; i++)
			{
				Metaball ball = balls[i];
				if(!ball.tracking)
					continue;
				
				Vec2 oldPos = ball.edgePosition;
				ball.edgePosition = differentialTangent(ball.edgePosition, step);
				ball.edgePosition = stepOnceTowardsBorder(ball.edgePosition);
				
				GL11.glPushMatrix();
				GL11.glBegin(GL11.GL_LINE);
					GL11.glColor3f(0.0f, 0.0f, 1.0f);
					GL11.glVertex3f(oldPos.x, oldPos.y, 0.0f);
					GL11.glVertex3f(ball.edgePosition.x, ball.edgePosition.y, 0.0f);
				GL11.glEnd();
				GL11.glPopMatrix();
				
				for(int j = 0; j < balls.length; j++)
				{
					Metaball ob = balls[j];
					Vec2 test = ob.initialPosition.sub(ball.edgePosition).abs();
					if((ob == null || loopIndex > 3) && (test.x < step && test.y < step))
						ball.tracking = false;
				}
			}
			
			int tracking = 0;
			for(int i = 0; i < balls.length; i++)
			{
				if(balls[i].tracking)
					tracking++;
			}
			if(tracking == 0)
				break;
		}
	}
	public Vec2 differentialTangent(Vec2 pos, float step)
	{
		Vec2 funcPos = calculateTangent(pos);
		funcPos = calculateTangent(pos.add(new Vec2(funcPos.x*step/2, funcPos.y*step/2)));
		return pos.add(new Vec2(step*funcPos.x, step*funcPos.y));
	}
	public Vec2 calculateForce(Vec2 pos)
	{
		Vec2 force = new Vec2();
		for(int i = 0; i < balls.length; i++)
		{
			Vec2 div = balls[i].position.sub(pos).abs();
			div = new Vec2((float)Math.pow(div.x, goo), (float)Math.pow(div.y, goo));
			if(div.x != 0 && div.y != 0)
			{
				float x = balls[i].size/div.x; //ballsize / div
				float y = balls[i].size/div.y;
				force = new Vec2(force.x+x, force.y+y);
			}
			else
			{
				force = new Vec2(force.x+100000, force.y+100000); // big number
			}
		}
		return force;
	}
	public Vec2 calculateNormal(Vec2 pos)
	{
		Vec2 normalized = new Vec2();
		for(int i = 0; i < balls.length; i++)
		{
			Vec2 div = balls[i].position.sub(pos).abs();
			div = new Vec2((float)Math.pow(div.x, goo+2), (float)Math.pow(div.y, goo+2));
			if(div.x != 0 && div.y != 0)
			{
				div = new Vec2(1.0f/div.x, 1.0f/div.y);
				Vec2 relPos = balls[i].position.sub(pos).abs();
				relPos = new Vec2(relPos.x / div.x, relPos.y / div.y);
				relPos = relPos.mul(-goo * balls[i].size);
				normalized = new Vec2(relPos.x+normalized.x, relPos.y+normalized.y);
			}
		}
		Vec2 absNormal = normalized.abs();
		return new Vec2(normalized.x/absNormal.x, normalized.y/absNormal.y);
	}
	public Vec2 calculateTangent(Vec2 pos)
	{
		Vec2 normal = calculateNormal(pos);
		return new Vec2(-normal.y, normal.x);
	}
	public Vec2 stepOnceTowardsBorder(Vec2 pos)
	{
		Vec2 force = calculateForce(pos);
		Vec2 normal = calculateNormal(pos);
		
		float first = (float)Math.pow((minSize / threshold), 1/goo);
		force = new Vec2(minSize/force.x, minSize/force.y);
		force = new Vec2((first-(float)Math.pow(force.x, 1/goo))+0.01f, (first-(float)Math.pow(force.y, 1/goo))+0.01f);
		normal = new Vec2(force.x*normal.x, force.y*normal.y);
		return pos.add(normal);
	}
	public Vec2 trackBorder(Vec2 pos)
	{
		Vec2 force = new Vec2(9999999, 9999999);
		while(force.x > threshold && force.y > threshold)
		{
			force = calculateForce(pos);
			pos = stepOnceTowardsBorder(pos);
		}
		return pos;
	}
	
}
