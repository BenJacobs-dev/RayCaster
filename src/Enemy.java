import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;


public class Enemy extends Rectangle implements DistComparable{
	public static int idCounter = 1;
	public double x, y, height, dist, ratio;
	public Rectangle minimap;
	public int mapSize, shotCounter, id;
	public boolean isShooter;
	
	public Enemy(double xPosIn, double yPosIn, ImagePattern img, int mapSizeIn, boolean isShooterIn) {  
		super(100, 100, img);
		x = xPosIn;
		y = yPosIn;
		dist = 0;
		minimap = new Rectangle((x-.5)*mapSizeIn, (y-.5)*mapSizeIn, mapSizeIn, mapSizeIn);
		minimap.setFill(img);
		ratio = img.getWidth()/img.getHeight();
		mapSize = mapSizeIn;
		shotCounter = 0;
		id = idCounter++;
		isShooter = isShooterIn;
	}
	
	public double[] getDist(double playerX, double playerY) {
		shotCounter++;
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
	
	public int move(int[][] map) {
		return -1;
	}

	public Projectile shootPlayer(Map map) {
		shotCounter = 0;
		double relDir = Math.atan((map.player.width-x)/(map.player.height-y));
		if((map.player.height-y) < 0) {
			relDir += Math.PI;
		}
		return new Projectile(x, y, relDir, new Color(Math.random(), Math.random(), Math.random(), 1), mapSize, 0.05, map.map, id);
	}
	
}
