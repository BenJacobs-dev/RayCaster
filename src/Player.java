
public class Player {
	public double dir, width, height, health, ammo;
	public final double FULLROTATION = 2*Math.PI, QUARTERROTATION = Math.PI/2;
	public int mapWidth, mapHeight, shotTimer;
	
	public Player(float widthIn, float heightIn, int mapWidthIn, int mapHeightIn) {
		dir = 0;
		width = widthIn;
		height = heightIn;
		mapWidth = mapWidthIn-1;
		mapHeight = mapHeightIn-1;
		shotTimer = 0;
		ammo = 30;
		health = 100;
	}
	
	public void rotate(double amount) {
		dir += amount;
		if(dir >= FULLROTATION)
			dir -= FULLROTATION;
		else if(dir < 0)
			dir += FULLROTATION;
	}
	
	public boolean move(boolean forward, int[][] map) {
		int w = (int)width, h = (int)height;
		double newW = Math.max(Math.min(mapWidth, width+Math.sin(dir)*0.05*(forward ? 1 : -1)), 0), newH = Math.max(Math.min(mapHeight, height+Math.cos(dir)*0.05*(forward ? 1 : -1)), 0);
		
		if(map[(int)newW][(int)newH] == 0) {
			width = newW;
			height = newH;
		}
		else if(map[(int)newW][h] == 0) {
			width = newW;
		}
		else if(map[w][(int)newH] == 0) {
			height = newH;
		}
		return w != (int)width || h != (int)height;
	}
	
	public boolean strafe(boolean left, int[][] map) {
		int w = (int)width, h = (int)height;
		double newW = Math.max(Math.min(mapWidth, width+Math.sin(dir + QUARTERROTATION)*0.05*(left ? 1 : -1)), 0), newH = Math.max(Math.min(mapHeight, height+Math.cos(dir+QUARTERROTATION)*0.05*(left ? 1 : -1)), 0);
		
		if(map[(int)newW][(int)newH] == 0) {
			width = newW;
			height = newH;
		}
		else if(map[(int)newW][h] == 0) {
			width = newW;
		}
		else if(map[w][(int)newH] == 0) {
			height = newH;
		}
		return w != (int)width || h != (int)height;
	}
	
	public void hit(Projectile projectile) {
		health -= 10;
	}
	
}
