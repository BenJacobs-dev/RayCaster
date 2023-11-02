import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

public class Projectile extends Circle implements DistComparable {
	
	public double x, y, dir, speed, dist;
	public Circle minimap;
	public int shooterId;
	public int damage;
	
	public Projectile(double startX, double startY, double startDir, Color color, double mapSize, double speedIn, int[][] mapIn, int shooterIdIn, int damageIn) {
		super(100, color);
		x = startX;
		y = startY;
		dir = startDir;
		minimap = new Circle(startX*mapSize, startY*mapSize, mapSize/5, color);
		speed = speedIn;
		dist = 0;
		move(mapIn);
		move(mapIn);
		move(mapIn);
		move(mapIn);
		move(mapIn);
		shooterId = shooterIdIn;
		damage = damageIn;
	}
	
	public int move(int[][] map) {
		x += Math.sin(dir)*speed;
		y += Math.cos(dir)*speed;
		
		try {
			if(map[(int)x][(int)y] == 0) {
				return -1;
			}
			return map[(int)x][(int)y];
		}catch(Exception e) {return -2;}
	}
	
	public double[] getDist(double playerX, double playerY) {
		double[] output = new double[2];
		double relX = x-playerX, relY = y-playerY;
		output[0] = Math.sqrt(relX*relX+relY*relY);
		output[1] = Math.acos(relY/output[0]);
		if(relX < 0) {
			output[1] = Math.PI*2-output[1];
		}
		dist = output[0];
		return output; 
	}
	
	public double getDist() {
		return dist;
	}
	
	public double checkCollide(double xIn, double yIn) {
		return Math.sqrt((x-xIn)*(x-xIn)+(y-yIn)*(y-yIn));
	}
	
}
