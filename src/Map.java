import java.util.ArrayList;
import java.util.LinkedList;


public class Map {
	
	public int[][] map;
	public int[][] upperMap;
	public Player player;
	public int mapCounter = 0;
	
	public Map(int width, int height, double floorRatio, int wallPatternCount) {
		map = new int[width][height];
		upperMap = new int[width][height];
		int ran;
		for(int i = 0, j; i < width; i++) {
			for(j = 0; j < height; j++){
				ran = (int)((Math.random())*floorRatio);
				if(ran == 1 || i == 0 || i == width-1 || j == 0 || j == height-1) {
					map[i][j] = (int)(Math.random()*wallPatternCount)+1;
				}
				else {
					map[i][j] = 0;
				}
				if(i == 0 || i == width-1 || j == 0 || j == height-1) {
					upperMap[i][j] = 1;
				}
				else {
					upperMap[i][j] = 0;
				}
			}
		}
		player = new Player(width/2, height/2, width, height);
	}
	
	private Map(int size, int wallPatternCount) {
		
		map = new int[size][size];
		upperMap = new int[size][size];
		for(int i = 0, j; i < size; i++) {
			for(j = 0; j < size; j++){
				map[i][j] = 1;//(int)(Math.random()*wallPatternCount)+1;
				if(i == 0 || i == size-1 || j == 0 || j == size-1) {
					upperMap[i][j] = 1;//(int)(Math.random()*wallPatternCount)+1;
				}
				else {
					upperMap[i][j] = 0;
				}
			}
		}
	}
	
	public Map() {
		map = new int[][] 
		   {{1,1,1,1,1,1,1,1,1,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,1,1,1,1,1,1,1,1,1}};
		upperMap = new int[][]
			{{1,1,0,0,0,1,1,1,1,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,1,0,1,1},
			{1,0,0,0,0,0,1,1,1,1},
			{1,0,0,0,0,0,0,0,1,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,1,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,0,0,0,0,0,0,0,0,1},
			{1,1,1,1,1,1,1,1,1,1}};
		player = new Player(5, 5, 10, 10);
	}
	
	public String toString() {
		int width = map.length, height = map[0].length;
		StringBuilder output = new StringBuilder((width+1)*height+1);
		
		for(int i, j = 0; j < height; j++) {
			for(i = 0; i < width; i++) {
				output.append(map[i][j]);
			}
			output.append('\n');
		}
		
		int position = ((int)player.width) + ((int)player.height)*(width+1);
		
		output.replace(position, position+1, "P");
		return output.toString();
	}
	
	public static Map makeMaze(int size, int wallPatternCount) {
		Map output = new Map(size, wallPatternCount);
		output.map[1][1] = 0;
		LinkedList<int[]> pos = new LinkedList<>();
		ArrayList<Integer> possibleSides = new ArrayList<>();
		pos.add(new int[] {1,1,1});
		int[] curPos, next;
		while(pos.size() != 0) {
			curPos = pos.remove();
			if(curPos[0]-2 > 0 && output.map[curPos[0]-2][curPos[1]] != 0) {possibleSides.add(0); if(curPos[2] == 0 && Math.random() < 0.25) {possibleSides.add(0);possibleSides.add(0);}}
			if(curPos[1]-2 > 0 && output.map[curPos[0]][curPos[1]-2] != 0) {possibleSides.add(1); if(curPos[2] == 1 && Math.random() < 0.25) {possibleSides.add(1);possibleSides.add(1);}}
			if(curPos[0]+2 < size && output.map[curPos[0]+2][curPos[1]] != 0) {possibleSides.add(2); if(curPos[2] == 0 && Math.random() < 0.25) {possibleSides.add(2);possibleSides.add(2);}}
			if(curPos[1]+2 < size && output.map[curPos[0]][curPos[1]+2] != 0) {possibleSides.add(3); if(curPos[2] == 1 && Math.random() < 0.25) {possibleSides.add(3);possibleSides.add(3);}}
			if((possibleSides.size() == 0)) {continue;}
			int dir = possibleSides.get((int)(Math.random()*possibleSides.size()));
			if(dir == 0) {
				output.map[curPos[0]-1][curPos[1]] = 0;
				output.map[curPos[0]-2][curPos[1]] = 0;
				next = new int[] {curPos[0]-2, curPos[1], 1};
				pos.add((int)(Math.random()*pos.size()*.5), next);
				pos.add((int)((Math.random()*.5+.5)*pos.size()), curPos);
		
			}
			else if(dir == 1) {
				output.map[curPos[0]][curPos[1]-1] = 0;
				output.map[curPos[0]][curPos[1]-2] = 0;
				next = new int[] {curPos[0], curPos[1]-2, 0};
				pos.add((int)(Math.random()*pos.size()*.5), next);
				pos.add((int)((Math.random()*.5+.5)*pos.size()), curPos);
			}
			else if(dir == 2) {
				output.map[curPos[0]+1][curPos[1]] = 0;
				output.map[curPos[0]+2][curPos[1]] = 0;
				next = new int[] {curPos[0]+2, curPos[1], 1};
				pos.add((int)(Math.random()*pos.size()*.5), next);
				pos.add((int)((Math.random()*.5+.5)*pos.size()), curPos);
			}
			else{
				output.map[curPos[0]][curPos[1]+1] = 0;
				output.map[curPos[0]][curPos[1]+2] = 0;
				next = new int[] {curPos[0], curPos[1]+2, 0};
				pos.add((int)(Math.random()*pos.size()*.5), next);
				pos.add((int)((Math.random()*.5+.5)*pos.size()), curPos);
			}
			possibleSides.clear();
		}
		output.player = new Player(1, 1, size, size);
		return output;
	}
}
