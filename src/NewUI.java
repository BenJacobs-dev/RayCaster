import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.text.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.scene.shape.*;
import javafx.scene.paint.*;
import javafx.scene.image.*;
import javafx.geometry.*;
import javafx.event.*;
import javafx.collections.*;
import javafx.beans.value.*;
import javafx.concurrent.*;
import java.awt.Robot;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.*;
import java.io.*;
import java.nio.file.*;


public class NewUI extends Application{

	Stage stage;
	Map map;
	ObservableList<Node> nodeList, mapGridList, castLinesList, wallTileList;
	ArrayList<WallTilePoly> displayList;
	int displaySize = 1000, gridSize = 10, mapSize = 250/gridSize, FOV = 100, wallCounter;
	Circle playerSprite;
	Line dirLine;
	final double MIDLOOK = Math.PI/2, FULLROTATION = Math.PI*2, THIRDROTATION = Math.PI*3/2, PI = Math.PI, DEGTORAD = Math.PI/180;
	Robot robot;
	HashMap<KeyCode, Runnable> keyActions;
	DistComparator comparator;
	final AtomicBoolean running = new AtomicBoolean(false);
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	
    public void start(Stage primaryStage){
    	
    	stage = primaryStage;
		stage.setTitle("RayCaster");
		
		createKeyActions();
		comparator = new DistComparator();
		displayList = new ArrayList<>();
		
		Group group = new Group(), mapGridGroup = new Group(), castLines = new Group(), wallTileGroup = new Group();
		nodeList = group.getChildren();
		mapGridList = mapGridGroup.getChildren();
		castLinesList = castLines.getChildren();
		wallTileList = wallTileGroup.getChildren();
		
		map = new Map(gridSize, gridSize, 7, 1);
		map.player.rotate(PI);
		
		playerSprite = new Circle(mapSize/2, Color.BLUE);
		playerMoveUpdateGrid();
		
		dirLine = new Line();
		dirLine.setStroke(Color.RED);
		dirLine.setStrokeWidth(Math.max(mapSize/20, 1));
		updateDirLine();
		
		nodeList.add(wallTileGroup);
		
		nodeList.add(mapGridGroup);
		nodeList.add(playerSprite);
		nodeList.add(dirLine);
		nodeList.add(castLines);
		
		createMapGrid();
		updateCastLines();
		
		Scene scene = new Scene(group, displaySize*1.5, displaySize);
		
		try {
			robot = new Robot();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		EventHandler<MouseEvent> moveMouse = this::mouseMove;
		scene.addEventHandler(MouseEvent.MOUSE_MOVED, moveMouse);
		scene.setCursor(Cursor.NONE);
		
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent key) {
		    	if(keyActions.containsKey(key.getCode())) keyActions.get(key.getCode()).run();
		    	update();
		    }
		});
		stage.setScene(scene);
		stage.show();
		
    }
    
    public void update() {
    	playerMoveUpdateGrid();
    	updateDirLine();
    	updateCastLines();
    }
    
    public void updateCastLines() {
    	castLinesList.clear();
    	wallCounter = 0; //displayList.clear();
		double fov = FOV*DEGTORAD, dir = map.player.dir;
		updateCastLinesUIIndividual(dir);
		List<WallTilePoly> tempList = displayList.subList(0, wallCounter);
		Collections.sort(tempList, comparator);
		System.out.println(tempList.size());
		wallTileList.setAll(tempList);
		for(int i = 0, j; i < map.map.length; i++)for(j = 0; j < map.map[i].length; j++)if(map.map[i][j] > 1000000) map.map[i][j] -= 1000000; 
    }
    
    public void updateCastLinesUIIndividual(double dir) {
    	
    	double min = dir+FOV*DEGTORAD/2, max = dir-FOV*DEGTORAD/2, test = map.player.dir;
    	//if(min > PI && map.player.dir < PI) min -= FULLROTATION;
    	//if(max < PI && map.player.dir > PI) max += FULLROTATION;
    	if(!(min > test && max < test)) {
//    		System.out.println();
//    		System.out.println(dir);
//    		System.out.println(map.player.dir);
//    		System.out.println(min);
//    		System.out.println(max);
//    		System.out.println(test);
    		return;
    	}
    	
    	if(dir > FULLROTATION) {dir-=FULLROTATION;}
    	else if(dir < 0) {dir+=FULLROTATION;}
    	
    	//Test that Cast Line is in FOV//
    	

    	
    	boolean possible = true;
    	
    	double rayX, rayY, xOffset = 0, yOffset = 0, aTan, nTan, distY, distX, fRayX, fRayY;
    	
    	int mapX = 0, mapY = 0;
    		
    		//RayCasting Code//
    		
    		distX = 1000000;
    		distY = 1000001;
    		
    		aTan=Math.tan(dir);
        	nTan=1/Math.tan(dir);
        	
        	//down
        	if(dir<MIDLOOK || dir>THIRDROTATION) {
        		rayY=(int)map.player.height+1.0001; 
        		rayX=(map.player.height-rayY)*-aTan+map.player.width; 
        		yOffset=1; 
        		xOffset=yOffset*aTan;
        	}
        	//up
        	else if(dir>MIDLOOK && dir<THIRDROTATION) {
        		rayY=(int)map.player.height-0.0001; 
        		rayX=(map.player.height-rayY)*-aTan+map.player.width; 
        		yOffset=-1; 
        		xOffset=yOffset*aTan;
        		}
        	else {
        		rayX=100000;
        		rayY=100001;
        		possible = false;
        	}
        	while(possible){
        		mapX = (int)rayX; mapY = (int)rayY;
        		if(mapX < 0 || mapY < 0 || mapX >= map.map.length || mapY >= map.map[0].length) {
        			possible = false;
        		}
        		else if(map.map[mapX][mapY] != 0) {
        			possible = false;
        		}
        		else {
        			rayX+=xOffset; 
        			rayY+=yOffset; 
        		}
    		}
    		//System.out.println("Angle = " + dir + ", xCur = " + rayX + ", xOffset = " + xOffset + ", yCur = " + rayY + ", yOffset = " + yOffset + ", ");

        	fRayX = rayX;
        	fRayY = rayY;
        	distY = Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height));

        	possible = true;
        	
        	if(dir==0 || dir==PI) {
        		rayX=100000; 
        		rayY=100001; 
        		possible = false;
           	}
        	//left
        	else if(dir<PI) {
        		rayX=(int)map.player.width+1.00001; 
        		rayY=(map.player.width-rayX)*-nTan+map.player.height; 
        		xOffset= 1; 
        		yOffset= xOffset*nTan;
        	}
        	//right
        	else {
        		rayX=(int)map.player.width-0.00001; 
        		rayY=(map.player.width-rayX)*-nTan+map.player.height; 
        		xOffset= -1; 
        		yOffset= xOffset*nTan;
        	}
        	
        	
        	while(possible){
        		mapX = (int)rayX; mapY = (int)rayY;
        		if(mapX < 0 || mapY < 0 || mapX >= map.map.length || mapY >= map.map[0].length) {
        			possible = false;
        		}
        		else if(map.map[mapX][mapY] != 0) {
        			possible = false;
        		}
        		else {
        			rayX+=xOffset; 
        			rayY+=yOffset; 
        		}
        	}
        	
        	//System.out.println("Angle = " + dir + ", xCur = " + rayX + ", xOffset = " + xOffset + ", yCur = " + rayY + ", yOffset = " + yOffset + ", ");	
        	
        	distX = Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height));
        	
        	if(distX > distY) {
        		distX = distY;
        		rayX = fRayX;
        		rayY = fRayY;
        	}
        	mapX = (int)rayX;
        	mapY = (int)rayY;
        	
        	if(map.map[mapX][mapY] > 1000000) {return;}
        	map.map[mapX][mapY] += 1000000;
        	
        	double leftDir, middleDir = -100000, rightDir, leftDist, middleDist = -100000, rightDist;
        	
        	if(mapX == Math.floor(map.player.width)) {
        		if(rayY > map.player.height) {
        			rightDir = FULLROTATION + Math.atan((mapX-map.player.width)/(mapY-map.player.height));
        			leftDir = Math.atan((mapX+1-map.player.width)/(mapY-map.player.height));
        			leftDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
            		rightDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
        			createCastLine(mapX, mapY, Color.RED); // -dir
        			updateCastLinesUIIndividual(rightDir - 0.00000001);
                	createCastLine(mapX+1, mapY, Color.BLUE);// +dir
        			updateCastLinesUIIndividual(leftDir + 0.00000001); 
        		}
        		else {
        			leftDir = PI + Math.atan((mapX-map.player.width)/(mapY+1-map.player.height));
        			rightDir = PI + Math.atan((mapX+1-map.player.width)/(mapY+1-map.player.height));
        			leftDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
            		rightDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
        			createCastLine(mapX, mapY+1, Color.GREEN); // +dir
        			updateCastLinesUIIndividual(leftDir + 0.00000001);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW); // -dir
                	updateCastLinesUIIndividual(rightDir - 0.00000001);
        		}
        	}
        	else if(mapY == Math.floor(map.player.height)) {
        		if(rayX > map.player.width) {
        			leftDir = MIDLOOK - Math.atan((mapY-map.player.height)/(mapX-map.player.width));
        			rightDir = MIDLOOK - Math.atan((mapY+1-map.player.height)/(mapX-map.player.width));
        			leftDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
            		rightDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
        			createCastLine(mapX, mapY, Color.RED);
        			updateCastLinesUIIndividual(leftDir + 0.00000001);
                	createCastLine(mapX, mapY+1, Color.GREEN);
                	updateCastLinesUIIndividual(rightDir - 0.00000001);
        		}
        		else {
        			leftDir = THIRDROTATION - Math.atan((mapY-map.player.height)/(mapX+1-map.player.width));
                	rightDir = THIRDROTATION - Math.atan((mapY+1-map.player.height)/(mapX+1-map.player.width));
                	leftDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
            		rightDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
        			createCastLine(mapX+1, mapY, Color.BLUE);
        			updateCastLinesUIIndividual(leftDir + 0.00000001);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW);
                	updateCastLinesUIIndividual(rightDir - 0.00000001);
        		}
        	}
        	else if(rayX > map.player.width) {
        		if(rayY > map.player.height) {
        			leftDir = Math.atan((mapX+1-map.player.width)/(mapY-map.player.height));
        			middleDir = Math.atan((mapX-map.player.width)/(mapY-map.player.height));
        			rightDir = Math.atan((mapX-map.player.width)/(mapY+1-map.player.height));
        			leftDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
        			middleDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
            		rightDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
                	createCastLine(mapX, mapY, Color.RED);
                	createCastLine(mapX, mapY+1, Color.GREEN);
                	updateCastLinesUIIndividual(rightDir-0.00000001);
                	createCastLine(mapX+1, mapY, Color.BLUE);
                	updateCastLinesUIIndividual(leftDir+0.00000001);
        		}
        		else {
        			leftDir = PI + Math.atan((mapX-map.player.width)/(mapY-map.player.height));
                	middleDir = PI + Math.atan((mapX-map.player.width)/(mapY+1-map.player.height));
                	rightDir = PI + Math.atan((mapX+1-map.player.width)/(mapY+1-map.player.height));
                	leftDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
        			middleDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
            		rightDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
                	createCastLine(mapX, mapY, Color.RED);
                	updateCastLinesUIIndividual(leftDir+0.00000001);
                	createCastLine(mapX, mapY+1, Color.GREEN);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW);
                	updateCastLinesUIIndividual(rightDir-0.00000001);
        		}
        	}
        	else {
        		if(rayY > map.player.height) {
        			leftDir = FULLROTATION + Math.atan((mapX+1-map.player.width)/(mapY+1-map.player.height));
                	middleDir = FULLROTATION + Math.atan((mapX+1-map.player.width)/(mapY-map.player.height));
                	rightDir = FULLROTATION + Math.atan((mapX-map.player.width)/(mapY-map.player.height));
                	leftDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
        			middleDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
            		rightDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
                	createCastLine(mapX, mapY, Color.RED);
                	updateCastLinesUIIndividual(rightDir-0.00000001);
                	createCastLine(mapX+1, mapY, Color.BLUE);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW);
                	updateCastLinesUIIndividual(leftDir+0.00000001);
        		}
        		else {
        			leftDir = PI + Math.atan((mapX-map.player.width)/(mapY+1-map.player.height));
                	middleDir = PI + Math.atan((mapX+1-map.player.width)/(mapY+1-map.player.height));
                	rightDir = PI + Math.atan((mapX+1-map.player.width)/(mapY-map.player.height));
                	leftDist = Math.sqrt((mapX-map.player.width)*(mapX-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
        			middleDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY+1-map.player.height)*(mapY+1-map.player.height));
            		rightDist = Math.sqrt((mapX+1-map.player.width)*(mapX+1-map.player.width)+(mapY-map.player.height)*(mapY-map.player.height));
        			createCastLine(mapX, mapY+1, Color.GREEN);
        			updateCastLinesUIIndividual(leftDir+0.00000001);
                	createCastLine(mapX+1, mapY, Color.BLUE);
                	updateCastLinesUIIndividual(rightDir-0.00000001);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW);
        		}
        	}
			if(displayList.size() <= wallCounter) {
				displayList.add(new WallTilePoly());
				displayList.get(displayList.size()-1).setFill(new Color(Math.random(),Math.random(),Math.random(),1));
			}
			WallTilePoly wallTile = displayList.get(wallCounter++);
			wallTile.dist = leftDist;
			if(middleDir < -1000) {
	    		ObservableList<Double> list = wallTile.getPoints();
	    		list.clear();
	    		list.add(750+750*((map.player.dir-leftDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500+500/(Math.cos(map.player.dir-leftDir)*leftDist));
	    		list.add(750+750*((map.player.dir-leftDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500-500/(Math.cos(map.player.dir-leftDir)*leftDist));
	    		list.add(750+750*((map.player.dir-rightDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500-500/(Math.cos(map.player.dir-rightDir)*rightDist));
	    		list.add(750+750*((map.player.dir-rightDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500+500/(Math.cos(map.player.dir-rightDir)*rightDist));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
			}
	    	else {
	    		ObservableList<Double> list = wallTile.getPoints();
	    		list.clear();
	    		list.add(750+750*((map.player.dir-leftDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500+500/(Math.cos(map.player.dir-leftDir)*leftDist));
	    		list.add(750+750*((map.player.dir-leftDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500-500/(Math.cos(map.player.dir-leftDir)*leftDist));
	    		list.add(750+750*((map.player.dir-middleDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500-500/(Math.cos(map.player.dir-middleDir)*middleDist));
	    		list.add(750+750*((map.player.dir-middleDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500+500/(Math.cos(map.player.dir-middleDir)*middleDist));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
	    		
	    		if(displayList.size() <= wallCounter) {
	    			displayList.add(new WallTilePoly());
	    			displayList.get(displayList.size()-1).setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		}
	    		wallTile = displayList.get(wallCounter++);
	    		list = wallTile.getPoints();
	    		list.clear();
	    		wallTile.dist = rightDist;
	    		list.add(750+750*((map.player.dir-middleDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500-500/(Math.cos(map.player.dir-middleDir)*middleDist));
	    		list.add(750+750*((map.player.dir-middleDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500+500/(Math.cos(map.player.dir-middleDir)*middleDist));
	    		list.add(750+750*((map.player.dir-rightDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500+500/(Math.cos(map.player.dir-rightDir)*rightDist));
	    		list.add(750+750*((map.player.dir-rightDir)/((FOV-10)*DEGTORAD/2)));
	    		list.add(500-500/(Math.cos(map.player.dir-rightDir)*rightDist));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
        	}
        	createCastLine(rayX, rayY, Color.WHITE);
        	//System.out.println("Vert X: " + fRayX + ", Vert Y: " + fRayY + ", Horz X: "+ rayX + ", Horz Y: " + rayY);
        	
    }
    
    
    public void createMapGrid() {
    	int width = map.map.length, height = map.map[0].length;
		Rectangle temp;
		for(int i, j = 0; j < height; j++) {
			for(i = 0; i < width; i++) {
				temp = new Rectangle(i*mapSize, j*mapSize, mapSize, mapSize);
				temp.setFill(map.map[i][j] != 0 ? Color.BLACK : Color.DARKSLATEGRAY);
				mapGridList.add(temp);
			}
		}
    }
    
    public void playerMoveUpdateGrid() {
    	playerSprite.setCenterX(map.player.width*mapSize);
    	playerSprite.setCenterY(map.player.height*mapSize);
    }
    
    public void updateDirLine() {
    	dirLine.setStartX(map.player.width*mapSize);
    	dirLine.setStartY(map.player.height*mapSize);
    	dirLine.setEndX(map.player.width*mapSize + Math.sin(map.player.dir) * mapSize);
    	dirLine.setEndY(map.player.height*mapSize + Math.cos(map.player.dir) * mapSize);
    }
    
    public void createCastLine(double x, double y, Paint color) {
    	Line castLine = new Line(map.player.width*mapSize, map.player.height*mapSize, x*mapSize, y*mapSize);
    	castLinesList.add(castLine);
    	castLine.setStroke(color);
    }
    
    public void mouseMove(MouseEvent mouseEvent) {
    	map.player.rotate((750-mouseEvent.getScreenX())/1500);
    	robot.mouseMove(750, 500);
    	update();
    }
    
    public void createKeyActions() {
    	keyActions = new HashMap<>();
    	keyActions.put(KeyCode.W, () -> {map.player.move(true, map.map);});
    	keyActions.put(KeyCode.F, () -> {map.player.move(true, map.map);});
    	keyActions.put(KeyCode.A, () -> {map.player.strafe(true, map.map);});
    	keyActions.put(KeyCode.S, () -> {map.player.move(false, map.map);});
    	keyActions.put(KeyCode.D, () -> {map.player.strafe(false, map.map);});
    	//keyActions.put(KeyCode.ENTER, () -> {drawCastLines = !drawCastLines; castLines.setVisible(drawCastLines);});
    	//keyActions.put(KeyCode.SPACE, () -> {if(map.player.shotTimer >= 10) {createShootProjectile(new Projectile(map.player.width, map.player.height, map.player.dir, new Color(Math.random(), Math.random(), Math.random(), 1), mapSize, 0.1, map.map, 0)); map.player.shotTimer = 0;}});
    	//keyActions.put(KeyCode.G, () -> {if(map.player.shotTimer >= 10) {createShootProjectile(new Projectile(map.player.width, map.player.height, map.player.dir, new Color(Math.random(), Math.random(), Math.random(), 1), mapSize, 0.1, map.map, 0)); map.player.shotTimer = 0;}});
    	//keyActions.put(KeyCode.UP, () -> {vertLookPos = Math.min(PI, vertLookPos+0.1);});
    	//keyActions.put(KeyCode.DOWN, () -> {vertLookPos = Math.max(0, vertLookPos-0.1);});
    	keyActions.put(KeyCode.LEFT, () -> {map.player.rotate(0.1);});
    	keyActions.put(KeyCode.RIGHT, () -> {map.player.rotate(-0.1);});
    }
    
    public void stop() {
		running.set(false);
	}
    
}
