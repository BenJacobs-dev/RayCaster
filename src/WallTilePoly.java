import javafx.scene.shape.Polygon;


public class WallTilePoly extends Polygon implements DistComparable {

	public double dist;
	
	public WallTilePoly() {
		super();
		dist = 100;
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
