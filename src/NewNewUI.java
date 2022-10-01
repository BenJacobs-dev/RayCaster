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


public class NewNewUI extends Application{

	int displayHeight = 1000, gridSize = 10, mapSize = 400/gridSize, FOV = 100, wallCounter, displayCounter = 0;

	int displayWidth = (int)(displayHeight*1.5), displayHeightHalf = displayHeight>>1, displayWidthHalf = displayWidth>>1;
	
	Stage stage;
	Map map;
	Player player;
	ObservableList<Node> nodeList, mapGridList, castLinesList, wallTileList;
	ArrayList<WallTilePoly> displayList;
	Circle playerSprite;
	Line dirLine;
	final double MIDLOOK = Math.PI/2, FULLROTATION = Math.PI*2, THIRDROTATION = Math.PI*3/2, PI = Math.PI, DEGTORAD = Math.PI/180;
	Robot robot;
	private double vertLookPos = Math.PI/2;
	HashMap<KeyCode, Runnable> keyActions;
	DistComparator comparator;
	private final Semaphore semaphore = new Semaphore(1);
	final AtomicBoolean running = new AtomicBoolean(false);
	private LinkedList<KeyCode> keysPressed;
	private Rectangle floor, sky;
	
	int range = 10;
	double[][] distances, angles;
	int[][] walls;
	
	double midLine, minOffset = PI - FOV*DEGTORAD/2, maxOffset = PI + FOV*DEGTORAD/2;
	
	boolean first, makeMaze = false;
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	
    public void start(Stage primaryStage){
    	
    	keysPressed = new LinkedList<>();
    	distances = new double[range][range];
    	angles = new double[range][range];
    	walls = new int[range][range];
    	
    	
    	if(makeMaze) {
    		float sizeTemp = gridSize+1-(gridSize%2);
    		player = new Player(sizeTemp/2, sizeTemp/2, (int)sizeTemp, (int)sizeTemp);
    		map = Map.makeMaze(gridSize+1-(gridSize%2), 1, player);
    	}
    	else {
    		player = new Player(gridSize/2, gridSize/2, gridSize, gridSize);
    		map = new Map(gridSize, gridSize, 7, 1, player);
    		//map = new Map(3, 100, 0, 1);
    	}

    	stage = primaryStage;
		stage.setTitle("RayCaster");
		
		createKeyActions();
		comparator = new DistComparator();
		displayList = new ArrayList<>();
		
		floor = new Rectangle(displayWidth, displayHeightHalf+1, new LinearGradient(0, 1, 0, 0.2, true, CycleMethod.NO_CYCLE, new Stop[] { new Stop(0, Color.GREEN), new Stop(1, Color.BLACK)}));
		floor.setY(499);
		sky = new Rectangle(displayWidth, displayHeightHalf+2, new LinearGradient(0, 0, 0, 0.8, true, CycleMethod.NO_CYCLE, new Stop[] { new Stop(0, Color.LIGHTBLUE), new Stop(1, Color.BLACK)}));
		
		Group group = new Group(), mapGridGroup = new Group(), castLines = new Group(), wallTileGroup = new Group();
		nodeList = group.getChildren();
		mapGridList = mapGridGroup.getChildren();
		castLinesList = castLines.getChildren();
		wallTileList = wallTileGroup.getChildren();
		
		
		playerSprite = new Circle(mapSize/2, Color.BLUE);
		playerMoveUpdateGrid();
		
		dirLine = new Line();
		dirLine.setStroke(Color.RED);
		dirLine.setStrokeWidth(Math.max(mapSize/20, 1));
		updateDirLine();
		
		nodeList.add(floor);
		nodeList.add(sky);
		nodeList.add(wallTileGroup);
		
		nodeList.add(mapGridGroup);
		nodeList.add(playerSprite);
		nodeList.add(dirLine);
		nodeList.add(castLines);
		
		createMapGrid();
		updateCastLines();
		
		Scene scene = new Scene(group, displayWidth, displayHeight);
		
		try {
			robot = new Robot();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		EventHandler<MouseEvent> moveMouse = this::mouseMove;
		scene.addEventHandler(MouseEvent.MOUSE_MOVED, moveMouse);
		scene.setCursor(Cursor.NONE);
		scene.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
			public void handle(MouseEvent event) {
//				System.out.println(player.dir);
//				System.out.println(getAngle(player.width, player.height, gridSize*0.5, gridSize*0.5));
//				System.out.println(getOffsetFromView(getAngle(player.width, player.height, gridSize*0.5, gridSize*0.5)));
//				System.out.println();
		    	first = true;
			}
			
		});
		
		stage.setScene(scene);
		stage.show();
		
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent key) {
		        if(!keysPressed.contains(key.getCode())){
		        	keysPressed.add(key.getCode());
		        }
//		        if(keyActions.containsKey(key.getCode())) keyActions.get(key.getCode()).run();
//		        updateWallsSingle(fovMain);
//		        playerMoveUpdateGrid();
//		    	updateDirLine();
		    }
		});

		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent key) {
		        keysPressed.remove(key.getCode());
		    }
		});
		
		Task<Void> task = new Task<Void>() {
			
			long time;
			
			@Override
			protected Void call() throws Exception {
				try {
					time = System.currentTimeMillis();
					running.set(true);
					while(running.get()) {
						semaphore.acquire(1);
						Thread.sleep(Math.max(16+time-System.currentTimeMillis(), 0));
						time = System.currentTimeMillis();
						Platform.runLater(() -> {
							update();
						});
					}
				}catch(Exception e){e.printStackTrace();}
				return null;
			}
		};
		
		new Thread(task).start();
		
    }
    
    public void update() {
    	
    	for(KeyCode code : keysPressed) {
    		if(keyActions.containsKey(code)) keyActions.get(code).run();
    	}
    	player.shotTimer++;
    	//System.out.println(displayList.size());
//    	fpsCounter++;
//    	if(time != System.currentTimeMillis()/1000) {
//    		fpsCounterText.setText(String.valueOf(fpsCounter));
//    		fpsCounter=0;
//    		time = (int)(System.currentTimeMillis()/1000);
//    	}
    	playerMoveUpdateGrid();
    	updateDirLine();
    	calcDistancesAndAngles(player.dir);
    	updateCastLines();
//    	updateEnemies(fovMain);
//    	updateProjectiles(fovMain);
//    	updateStationaryEffects(fovMain);
//    	Collections.sort(displayList, comparator);
//    	display.getChildren().setAll(displayList);
    	semaphore.release();
    }
    
    public void updateCastLines() {
    	castLinesList.clear();
    	wallCounter = 0; //displayList.clear();
		double dir = player.dir;
		midLine = displayHeightHalf*(MIDLOOK-vertLookPos);
		floor.setY(500-midLine);
    	floor.setHeight(1000+midLine);
    	sky.setHeight(500-midLine);
    	updateWalls(player.dir);
		List<WallTilePoly> tempList = new ArrayList<WallTilePoly>(displayList.subList(0, wallCounter));
		Collections.sort(tempList, comparator);
		//System.out.println(tempList.size());
		wallTileList.setAll(tempList);
		for(int i = 0, j; i < map.map.length; i++)for(j = 0; j < map.map[i].length; j++)if(map.map[i][j] > 1000000) map.map[i][j] -= 1000000; 
    }
    
    public double getAngle(double sX, double sY, double eX, double eY) {
		eX -= sX;
		eY -= sY;
		double inRads = Math.atan2(-eY, eX);
	    if (inRads < 0)
	        inRads = -inRads;
	    else
	        inRads = 2 * Math.PI - inRads;
	    if(inRads >= 2*Math.PI) {
	    	inRads %= 2*Math.PI;
	    }
	    return inRads;
	}
    
    public double getOffsetFromView(double dir) {
    	double angle = dir - player.dir + PI;
    	if (angle < 0)
	        angle += FULLROTATION;
	    if(angle >= 2*Math.PI) {
	    	angle %= 2*Math.PI;
	    }
    	angle = angle > FULLROTATION ? angle - FULLROTATION : angle;
    	return angle;
    }
    
    public boolean inFOV(double dir) {
    	double offset = getOffsetFromView(dir);
    	return offset > minOffset && offset < maxOffset;
    }
    
    public void calcDistancesAndAngles(double dir) {
    	double deltaWidth, deltaHeight;
    	int mapIndexWidth, mapIndexHeight;
    	for(int i = 0, j = 0; i < range; i++) {
    		deltaWidth = player.width%1 + i - range/2;
    		
    		mapIndexWidth = (int)player.width + i - range/2;
    		for(j = 0; j < range; j++) {
    			// distance
    			deltaHeight = player.height%1 + j - range/2;
    			mapIndexHeight = (int)player.height + j - range/2;
    			if(isValidIndex2D(mapIndexWidth, mapIndexHeight, map.map)) {
    				walls[i][j] = map.map[mapIndexWidth][mapIndexHeight]; 
    				distances[i][j] = getDistance(0, 0, deltaWidth, deltaHeight); 
        			angles[i][j] =  getAngle(0, 0, deltaHeight, deltaWidth);
        			System.out.print(distances[i][j]+ "  " );
    			}
    			else {
    				walls[i][j] = 0;
    				angles[i][j] = 0;
    				distances[i][j] = 0; 
    			}
    			 
    		}
    		System.out.println();
    	}
    }
    
    public double getDistance(double x1, double y1, double x2, double y2) {
    	double diffX = x1 - x2, diffY = y1 - y2;
    	return Math.sqrt(diffX*diffX+diffY*diffY);
    }
    
    public boolean isValidIndex2D(int i, int j, int array[][]) {
    	return i >= 0 && j >= 0 && i < array.length && j < array[0].length;
    }
    
    public void updateWalls(double dir) {
    	for(int i = 0, j = 0; i < range-1; i++) {
    		for(j = 0; j < range-1; j++) {
    			if(walls[i][j] != 0 && inFOV(angles[i][j])) {
    				createCastLineNew((int)player.width + i - range/2, (int)player.height + j - range/2, Color.BLUE);
    				createWallTile(distances[i][j], angles[i][j], distances[i+1][j], angles[i+1][j]);
    				createWallTile(distances[i][j], angles[i][j], distances[i][j+1], angles[i][j+1]);
    				createWallTile(distances[i+1][j+1], angles[i+1][j+1], distances[i+1][j], angles[i+1][j]);
    				createWallTile(distances[i+1][j+1], angles[i+1][j+1], distances[i][j+1], angles[i][j+1]);
    			}
    		}
    	}
    }
    
    public void createWallTile(double dist1, double angle1, double dist2, double angle2) {
    	if(displayList.size() <= wallCounter) {
			displayList.add(new WallTilePoly());
			displayList.get(displayList.size()-1).setStroke(Color.BLACK);
		}
		WallTilePoly wallTile = displayList.get(wallCounter++);
		wallTile.dist = Math.max(dist1, dist2);
		double colorDist;
		ObservableList<Double> list = wallTile.getPoints();
		double offsetL = PI-getOffsetFromView(angle1), offsetR = PI-getOffsetFromView(angle2), temp1, temp2, temp3, temp4;
		temp1 = displayWidthHalf+displayWidthHalf*((offsetL)/((FOV-10)*DEGTORAD/2));
		temp2 = displayWidthHalf+displayWidthHalf*((offsetR)/((FOV-10)*DEGTORAD/2));
		temp3 = Math.abs(displayHeightHalf/(Math.cos(offsetL)*dist1));
		temp4 = Math.abs(displayHeightHalf/(Math.cos(offsetR)*dist2));
		list.clear();
		list.add(temp1);
		list.add(displayHeightHalf-midLine+temp3);
		list.add(temp1);
		list.add(displayHeightHalf-midLine-temp3);
		list.add(temp2);
		list.add(displayHeightHalf-midLine-temp4);
		list.add(temp2);
		list.add(displayHeightHalf-midLine+temp4);
		
		colorDist = Math.min(temp3, 1000)/1100;
		wallTile.setFill(new Color(colorDist, 0, 0, 1));
    }
    
    public void updateCastLinesUIIndividual(double dir) {
    	
    	if(!inFOV(dir)) return;
    	
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
        		rayY=(int)player.height+1.0001; 
        		rayX=(player.height-rayY)*-aTan+player.width; 
        		yOffset=1; 
        		xOffset=yOffset*aTan;
        	}
        	//up
        	else if(dir>MIDLOOK && dir<THIRDROTATION) {
        		rayY=(int)player.height-0.0001; 
        		rayX=(player.height-rayY)*-aTan+player.width; 
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
        	distY = Math.sqrt((rayX-player.width)*(rayX-player.width)+(rayY-player.height)*(rayY-player.height));

        	possible = true;
        	
        	if(dir==0 || dir==PI) {
        		rayX=100000; 
        		rayY=100001; 
        		possible = false;
           	}
        	//left
        	else if(dir<PI) {
        		rayX=(int)player.width+1.00001; 
        		rayY=(player.width-rayX)*-nTan+player.height; 
        		xOffset= 1; 
        		yOffset= xOffset*nTan;
        	}
        	//right
        	else {
        		rayX=(int)player.width-0.00001; 
        		rayY=(player.width-rayX)*-nTan+player.height; 
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
        	
        	distX = Math.sqrt((rayX-player.width)*(rayX-player.width)+(rayY-player.height)*(rayY-player.height));
        	
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
        	
        	if(mapX == Math.floor(player.width)) {
        		if(rayY > player.height) {
        			if(first) {
        				first = false;
        				System.out.println("1");
        			}
        			rightDir = FULLROTATION + Math.atan((mapX-player.width)/(mapY-player.height));
        			leftDir = Math.atan((mapX+1-player.width)/(mapY-player.height));
        			leftDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY-player.height)*(mapY-player.height));
            		rightDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY-player.height)*(mapY-player.height));
        			createCastLine(mapX, mapY, Color.RED); // -dir
        			updateCastLinesUIIndividual(rightDir - 0.00000001);
                	createCastLine(mapX+1, mapY, Color.BLUE);// +dir
        			updateCastLinesUIIndividual(leftDir + 0.00000001); 
        		}
        		else {
        			if(first) {
        				first = false;
        				System.out.println("2");
        			}
        			leftDir = PI + Math.atan((mapX-player.width)/(mapY+1-player.height));
        			rightDir = PI + Math.atan((mapX+1-player.width)/(mapY+1-player.height));
        			leftDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
            		rightDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
        			createCastLine(mapX, mapY+1, Color.GREEN); // +dir
        			updateCastLinesUIIndividual(leftDir + 0.00000001);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW); // -dir
                	updateCastLinesUIIndividual(rightDir - 0.00000001);
        		}
        	}
        	else if(mapY == Math.floor(player.height)) {
        		if(rayX > player.width) {
        			if(first) {
        				first = false;
        				System.out.println("3");
        			}
        			leftDir = MIDLOOK - Math.atan((mapY-player.height)/(mapX-player.width));
        			rightDir = MIDLOOK - Math.atan((mapY+1-player.height)/(mapX-player.width));
        			leftDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY-player.height)*(mapY-player.height));
            		rightDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
        			createCastLine(mapX, mapY, Color.RED);
        			updateCastLinesUIIndividual(leftDir + 0.00000001);
                	createCastLine(mapX, mapY+1, Color.GREEN);
                	updateCastLinesUIIndividual(rightDir - 0.00000001);
        		}
        		else {
        			if(first) {
        				first = false;
        				System.out.println("4");
        			}
        			leftDir = THIRDROTATION - Math.atan((mapY-player.height)/(mapX+1-player.width));
                	rightDir = THIRDROTATION - Math.atan((mapY+1-player.height)/(mapX+1-player.width));
                	leftDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY-player.height)*(mapY-player.height));
            		rightDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
        			createCastLine(mapX+1, mapY, Color.BLUE);
        			updateCastLinesUIIndividual(leftDir + 0.00000001);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW);
                	updateCastLinesUIIndividual(rightDir - 0.00000001);
        		}
        	}
        	else if(rayX > player.width) {
        		if(rayY > player.height) {
        			if(first) {
        				first = false;
        				System.out.println("5");
        			}
        			leftDir = Math.atan((mapX+1-player.width)/(mapY-player.height));
        			middleDir = Math.atan((mapX-player.width)/(mapY-player.height));
        			rightDir = Math.atan((mapX-player.width)/(mapY+1-player.height));
        			leftDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY-player.height)*(mapY-player.height));
        			middleDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY-player.height)*(mapY-player.height));
            		rightDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
                	createCastLine(mapX, mapY, Color.RED);
                	createCastLine(mapX, mapY+1, Color.GREEN);
                	updateCastLinesUIIndividual(rightDir-0.00000001);
                	createCastLine(mapX+1, mapY, Color.BLUE);
                	updateCastLinesUIIndividual(leftDir+0.00000001);
        		}
        		else {
        			if(first) {
        				first = false;
        				System.out.println("6");
        			}
        			leftDir = PI + Math.atan((mapX-player.width)/(mapY-player.height));
                	middleDir = PI + Math.atan((mapX-player.width)/(mapY+1-player.height));
                	rightDir = PI + Math.atan((mapX+1-player.width)/(mapY+1-player.height));
                	leftDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY-player.height)*(mapY-player.height));
        			middleDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
            		rightDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
                	createCastLine(mapX, mapY, Color.RED);
                	updateCastLinesUIIndividual(leftDir+0.00000001);
                	createCastLine(mapX, mapY+1, Color.GREEN);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW);
                	updateCastLinesUIIndividual(rightDir-0.00000001);
        		}
        	}
        	else {
        		if(rayY > player.height) {
        			if(first) {
        				first = false;
        				System.out.println("7");
        			}
        			leftDir = FULLROTATION + Math.atan((mapX+1-player.width)/(mapY+1-player.height));
                	middleDir = FULLROTATION + Math.atan((mapX+1-player.width)/(mapY-player.height));
                	rightDir = FULLROTATION + Math.atan((mapX-player.width)/(mapY-player.height));
                	leftDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
        			middleDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY-player.height)*(mapY-player.height));
            		rightDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY-player.height)*(mapY-player.height));
                	createCastLine(mapX, mapY, Color.RED);
                	updateCastLinesUIIndividual(rightDir-0.00000001);
                	createCastLine(mapX+1, mapY, Color.BLUE);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW);
                	updateCastLinesUIIndividual(leftDir+0.00000001);
        		}
        		else {
        			if(first) {
        				first = false;
        				System.out.println("8");
        			}
        			leftDir = PI + Math.atan((mapX-player.width)/(mapY+1-player.height));
                	middleDir = PI + Math.atan((mapX+1-player.width)/(mapY+1-player.height));
                	rightDir = PI + Math.atan((mapX+1-player.width)/(mapY-player.height));
                	leftDist = Math.sqrt((mapX-player.width)*(mapX-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
        			middleDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY+1-player.height)*(mapY+1-player.height));
            		rightDist = Math.sqrt((mapX+1-player.width)*(mapX+1-player.width)+(mapY-player.height)*(mapY-player.height));
        			createCastLine(mapX, mapY+1, Color.GREEN);
        			updateCastLinesUIIndividual(leftDir+0.00000001);
                	createCastLine(mapX+1, mapY, Color.BLUE);
                	updateCastLinesUIIndividual(rightDir-0.00000001);
                	createCastLine(mapX+1, mapY+1, Color.YELLOW);
        		}
        	}
//        	if((leftDir-FOV/1.75 < 0 ? leftDir+FOV/1.75 > dir || FULLROTATION+(leftDir-FOV/1.75) < dir : false)) {
//    			leftDir = FULLROTATION+(leftDir-FOV/2) < dir ? FULLROTATION+leftDir : leftDir;
//    		}
//    		else if((leftDir+FOV/1.75 > FULLROTATION ? (leftDir+FOV/1.75)-FULLROTATION > dir || leftDir-FOV/1.75 < dir : false)) {
//    			leftDir = (leftDir+FOV/1.75)-FULLROTATION > dir ? leftDir-FULLROTATION : leftDir;
//    		}
//        	if((rightDir-FOV/1.75 < 0 ? rightDir+FOV/1.75 > dir || FULLROTATION+(rightDir-FOV/1.75) < dir : false)) {
//    			rightDir = FULLROTATION+(rightDir-FOV/2) < dir ? FULLROTATION+rightDir : rightDir;
//    		}
//    		else if((rightDir+FOV/1.75 > FULLROTATION ? (rightDir+FOV/1.75)-FULLROTATION > dir || rightDir-FOV/1.75 < dir : false)) {
//    			rightDir = (rightDir+FOV/1.75)-FULLROTATION > dir ? rightDir-FULLROTATION : rightDir;
//    		}
			if(displayList.size() <= wallCounter) {
				displayList.add(new WallTilePoly());
    			displayList.get(displayList.size()-1).setStroke(Color.BLACK);
    		}
			WallTilePoly wallTile = displayList.get(wallCounter++);
			wallTile.dist = leftDist;
			double colorDist;
			if(middleDir < -1000) {
	    		ObservableList<Double> list = wallTile.getPoints();
	    		double offsetL = PI-getOffsetFromView(leftDir), offsetR = PI-getOffsetFromView(rightDir), temp1, temp2, temp3, temp4;
	    		temp1 = displayWidthHalf+displayWidthHalf*((offsetL)/((FOV-10)*DEGTORAD/2));
	    		temp2 = displayWidthHalf+displayWidthHalf*((offsetR)/((FOV-10)*DEGTORAD/2));
	    		temp3 = Math.abs(displayHeightHalf/(Math.cos(offsetL)*leftDist));
	    		temp4 = Math.abs(displayHeightHalf/(Math.cos(offsetR)*rightDist));
	    		list.clear();
	    		list.add(temp1);
	    		list.add(displayHeightHalf-midLine+temp3);
	    		list.add(temp1);
	    		list.add(displayHeightHalf-midLine-temp3);
	    		list.add(temp2);
	    		list.add(displayHeightHalf-midLine-temp4);
	    		list.add(temp2);
	    		list.add(displayHeightHalf-midLine+temp4);
	    		
	    		colorDist = Math.min(temp3, 1000)/1100;
	    		wallTile.setFill(new Color(colorDist, 0, 0, 1));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
			}
	    	else {
	    		ObservableList<Double> list = wallTile.getPoints();
//	    		list.clear();
//	    		list.add(750+750*((player.dir-leftDir)/((FOV-10)*DEGTORAD/2)));
//	    		list.add(500+500/(Math.cos(player.dir-leftDir)*leftDist));
//	    		list.add(750+750*((player.dir-leftDir)/((FOV-10)*DEGTORAD/2)));
//	    		list.add(500-500/(Math.cos(player.dir-leftDir)*leftDist));
//	    		list.add(750+750*((player.dir-middleDir)/((FOV-10)*DEGTORAD/2)));
//	    		list.add(500-500/(Math.cos(player.dir-middleDir)*middleDist));
//	    		list.add(750+750*((player.dir-middleDir)/((FOV-10)*DEGTORAD/2)));
//	    		list.add(500+500/(Math.cos(player.dir-middleDir)*middleDist));
	    		double offsetL = PI-getOffsetFromView(leftDir), offsetM = PI-getOffsetFromView(middleDir), temp1, temp2, temp3, temp4;
	    		temp1 = displayWidthHalf+displayWidthHalf*((offsetL)/((FOV-10)*DEGTORAD/2));
	    		temp2 = displayWidthHalf+displayWidthHalf*((offsetM)/((FOV-10)*DEGTORAD/2));
	    		temp3 = Math.abs(displayHeightHalf/(Math.cos(offsetL)*leftDist));
	    		temp4 = Math.abs(displayHeightHalf/(Math.cos(offsetM)*middleDist));
	    		list.clear();
	    		list.add(temp1);
	    		list.add(displayHeightHalf-midLine+temp3);
	    		list.add(temp1);
	    		list.add(displayHeightHalf-midLine-temp3);
	    		list.add(temp2);
	    		list.add(displayHeightHalf-midLine-temp4);
	    		list.add(temp2);
	    		list.add(displayHeightHalf-midLine+temp4);
	    		
	    		colorDist = Math.min(temp3, 1000)/1100;
	    		wallTile.setFill(new Color(colorDist, 0, 0, 1));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
	    		
	    		if(displayList.size() <= wallCounter) {
	    			displayList.add(new WallTilePoly());
	    			displayList.get(displayList.size()-1).setStroke(Color.BLACK);
	    		}

	    		wallTile = displayList.get(wallCounter++);
	    		list = wallTile.getPoints();
	    		wallTile.dist = rightDist;
	    		double offsetR = PI-getOffsetFromView(rightDir);
	    		temp1 = displayWidthHalf+displayWidthHalf*((offsetM)/((FOV-10)*DEGTORAD/2));
	    		temp2 = displayWidthHalf+displayWidthHalf*((offsetR)/((FOV-10)*DEGTORAD/2));
	    		temp3 = Math.abs(displayHeightHalf/(Math.cos(offsetM)*middleDist));
	    		temp4 = Math.abs(displayHeightHalf/(Math.cos(offsetR)*rightDist));
	    		list.clear();
	    		list.add(temp1);
	    		list.add(displayHeightHalf-midLine+temp3);
	    		list.add(temp1);
	    		list.add(displayHeightHalf-midLine-temp3);
	    		list.add(temp2);
	    		list.add(displayHeightHalf-midLine-temp4);
	    		list.add(temp2);
	    		list.add(displayHeightHalf-midLine+temp4);
	    		
	    		colorDist = Math.min(temp3, 1000)/1100;
	    		wallTile.setFill(new Color(colorDist, 0, 0, 1));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
        	}
        	createCastLine(rayX, rayY, Color.WHITE);
        	//System.out.println("Vert X: " + fRayX + ", Vert Y: " + fRayY + ", Horz X: "+ rayX + ", Horz Y: " + rayY);
        	
    }
    
    
    public void createMapGrid() {
    	int width = map.map.length, height = map.map[0].length;
		Rectangle temp;
		for(int i = 0, j; i < height; i++) {
			for(j = 0; j < width; j++) {
				temp = new Rectangle(i*mapSize, j*mapSize, mapSize, mapSize);
				temp.setFill(map.map[i][j] != 0 ? Color.BLACK : Color.DARKSLATEGRAY);
				mapGridList.add(temp);
			}
		}
    }
    
    public void playerMoveUpdateGrid() {
    	playerSprite.setCenterX(player.width*mapSize);
    	playerSprite.setCenterY(player.height*mapSize);
    }
    
    public void updateDirLine() {
    	dirLine.setStartX(player.width*mapSize);
    	dirLine.setStartY(player.height*mapSize);
    	dirLine.setEndX(player.width*mapSize + Math.sin(player.dir) * mapSize);
    	dirLine.setEndY(player.height*mapSize + Math.cos(player.dir) * mapSize);
    }
    
    public void createCastLine(double x, double y, Paint color) {
//    	Line castLine = new Line(player.width*mapSize, player.height*mapSize, x*mapSize, y*mapSize);
//    	castLinesList.add(castLine);
//    	castLine.setStroke(color);
    }
    
    public void createCastLineNew(double x, double y, Paint color) {
    	Line castLine = new Line(player.width*mapSize, player.height*mapSize, x*mapSize, y*mapSize);
    	castLinesList.add(castLine);
    	castLine.setStroke(color);
    }
    
    public void mouseMove(MouseEvent mouseEvent) {
    	player.rotate((displayWidthHalf-mouseEvent.getScreenX())/displayWidth);
    	vertLookPos = Math.min(PI, Math.max(0, vertLookPos+2*(displayHeightHalf-mouseEvent.getScreenY())/displayHeight));
    	robot.mouseMove(displayWidthHalf, displayHeightHalf);
    	//update();
    }
    
    public void createKeyActions() {
    	keyActions = new HashMap<>();
    	keyActions.put(KeyCode.W, () -> {player.move(true, map.map);});
    	keyActions.put(KeyCode.F, () -> {player.move(true, map.map);});
    	keyActions.put(KeyCode.A, () -> {player.strafe(true, map.map);});
    	keyActions.put(KeyCode.S, () -> {player.move(false, map.map);});
    	keyActions.put(KeyCode.D, () -> {player.strafe(false, map.map);});
    	//keyActions.put(KeyCode.ENTER, () -> {drawCastLines = !drawCastLines; castLines.setVisible(drawCastLines);});
    	//keyActions.put(KeyCode.SPACE, () -> {if(player.shotTimer >= 10) {createShootProjectile(new Projectile(player.width, player.height, player.dir, new Color(Math.random(), Math.random(), Math.random(), 1), mapSize, 0.1, map.map, 0)); player.shotTimer = 0;}});
    	//keyActions.put(KeyCode.G, () -> {if(player.shotTimer >= 10) {createShootProjectile(new Projectile(player.width, player.height, player.dir, new Color(Math.random(), Math.random(), Math.random(), 1), mapSize, 0.1, map.map, 0)); player.shotTimer = 0;}});
    	//keyActions.put(KeyCode.UP, () -> {vertLookPos = Math.min(PI, vertLookPos+0.1);});
    	//keyActions.put(KeyCode.DOWN, () -> {vertLookPos = Math.max(0, vertLookPos-0.1);});
    	keyActions.put(KeyCode.LEFT, () -> {player.rotate(0.1);});
    	keyActions.put(KeyCode.RIGHT, () -> {player.rotate(-0.1);});
    	keyActions.put(KeyCode.T, () -> printAngles());
    }
    
    public void printAngles() {
    	for(int i = 0; i < range; i++) {
    		for(int j = 0; j < range; j++) {
    			double val = ((int)(angles[i][j]*1000))/1000.0;
    			System.out.print(val);
    			System.out.print("  ");
    		}
    		System.out.println();
    	}
    }
    
    public void stop() {
		running.set(false);
	}
    
}
