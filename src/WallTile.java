import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;


public class WallTile extends Rectangle implements DistComparable {

	public double dist;
	
	public WallTile(double x, double y, double width, double height) {
		super(x,y,width,height);
		dist = 100;
	}
	
	public WallTile(double x, double y, double width, double height, double distIn, Paint color) {
		super(x,y,width,height);
		dist = distIn;
		setFill(color);
	}
	
	public int compareTo(DistComparable other) {
		if(other.getDist() == dist) {
			return 0;
		}
		if(other.getDist() > dist) {
			return -1;
		}
		return 1;
	}
	
	public double getDist() {
		return dist;
	}
	
}
