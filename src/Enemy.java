import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;


public class Enemy extends Entity{
	public boolean isShooter;
	public boolean isDead;
	public int health;
	
	public Enemy(double xPosIn, double yPosIn, ImagePattern img, int mapSizeIn, boolean isShooterIn, int healthIn) {  
		super(xPosIn, yPosIn, img, mapSizeIn, () -> {});
		isShooter = isShooterIn;
		isDead = false;
		health = healthIn;
	}

	public Projectile shootPlayer(Map map) {
		shotCounter = 0;
		double relDir = Math.atan((map.player.width-x)/(map.player.height-y));
		if((map.player.height-y) < 0) {
			relDir += Math.PI;
		}
		return new Projectile(x, y, relDir, new Color(Math.random(), Math.random(), Math.random(), 1), mapSize, 0.05, map.map, id, 10);
	}
	
	public void takeDamage(int damage) {
		health -= damage;
		if(health <= 0) {
			isDead = true;
		}
	}

}
