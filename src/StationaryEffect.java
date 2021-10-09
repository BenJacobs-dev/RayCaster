import javafx.scene.shape.Rectangle;
import javafx.scene.paint.ImagePattern;


public class StationaryEffect extends Rectangle implements DistComparable{
	public double x, y, height, dist;
	public Rectangle minimap;
	public ImagePattern[] animation;
	public int animationIndex, animationTimer;
	
	public StationaryEffect(double xPosIn, double yPosIn, int mapSizeIn, ImagePattern[] animationIn) {  
		super(100, 100, animationIn[0]);
		x = xPosIn;
		y = yPosIn;
		dist = 0;
		minimap = new Rectangle((x-.5)*mapSizeIn, (y-.5)*mapSizeIn, mapSizeIn, mapSizeIn);
		minimap.setFill(animationIn[0]);
		animation = animationIn;
		animationIndex = 0;
		animationTimer = 0;
	}
	
	public double[] getDist(double playerX, double playerY) {
		double[] output = new double[2];
		double relX = x-playerX, relY = y-playerY;
		output[0] = Math.sqrt(relX*relX+relY*relY);
		output[1] = Math.acos(relY/output[0]);
		if(relX < 0) {
			output[1] = Math.PI*2-output[1];
		}
		dist = output[0]-.1;
		return output; 
	}
	
	public double getDist() {
		return dist;
	}
	
	public double getRatio() {
		return animation[animationIndex].getWidth()/animation[animationIndex].getHeight();
	}
	
	public boolean animate() {
		animationIndex++;
		if(animationIndex >= animation.length) {
			return false;
		}
		this.setFill(animation[animationIndex]);
		minimap.setFill(animation[animationIndex]);
		return true;
	}
	
}
