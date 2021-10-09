import java.util.Comparator;
import javafx.scene.Node;

public class DistComparator implements Comparator<Node>{
	
	public int compare(Node objIn, Node otherIn) {
		DistComparable obj = (DistComparable)objIn;
		DistComparable other = (DistComparable)otherIn;
		if(other.getDist() == obj.getDist()) {
			return 0;
		}
		if(other.getDist() > obj.getDist()) {
			return 1;
		}
		return -1;
	}
}
