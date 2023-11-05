import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.paint.*;
import javafx.event.*;
import javafx.collections.*;
import javafx.concurrent.*;
import java.awt.Robot;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class UI extends Application{

	int displayHeight = 1300, gridSize = 10, mapSize = 400/gridSize, FOV = 110, wallCounter, rfCounter, linesCounter;

	int displayWidth = (int)(displayHeight*1.5), displayHeightHalf = displayHeight>>1, displayWidthHalf = displayWidth>>1;
	
	Stage stage;
	Level level;
	Player player;
	ObservableList<Node> nodeList, mapGridList, castLinesList, gameWorldList, entityMiniMapList, uiList, linesList;
	ArrayList<Node> displayList;
	ArrayList<WallTilePoly> wallTilePolyList, rfTileList;
	ArrayList<Projectile> projectileList;
	ArrayList<Enemy> enemyList;
	ArrayList<StationaryEffect> sEffectList;
	ArrayList<int[]> rfIndexList;
	ArrayList<Line> cornerLinesList;
	double[][][] rfDistDirList;
	Circle playerSprite;
	Line dirLine;
	final double MIDLOOK = Math.PI/2, FULLROTATION = Math.PI*2, THIRDROTATION = Math.PI*3/2, PI = Math.PI, DEGTORAD = Math.PI/180;
	Robot robot;
	double vertLookPos = Math.PI/2;
	HashMap<KeyCode, Runnable> keyActions;
	DistComparator comparator;
	final Semaphore semaphore = new Semaphore(1);
	final AtomicBoolean running = new AtomicBoolean(false);
	LinkedList<KeyCode> keysPressed;
	Rectangle floor, sky;
	Font bigFont = new Font("Courier New", 32);
	ImagePattern[] explosionAnimation;
	Group castLines;
	Text fpsCounterText;
	Weapon pistol;
	
	double midLine, minOffset = PI - FOV*DEGTORAD/2, maxOffset = PI + FOV*DEGTORAD/2;
	
	int enemyCount = 0, time, fpsCounter;
	
	boolean first, makeMaze = false, makeCircle = false, invertedCircle = false, drawCastLines = false;
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	
    public void start(Stage primaryStage){
    	
    	keysPressed = new LinkedList<>();
    			
    	if(makeMaze) {
    		float sizeTemp = gridSize+1-(gridSize%2);
    		player = new Player(sizeTemp/2, sizeTemp/2, (int)sizeTemp, (int)sizeTemp);
    		level = Level.makeMaze(gridSize+1-(gridSize%2), 1, player);
    	}
		if(makeCircle){
			player = new Player(gridSize/2, gridSize/2, gridSize, gridSize);
			level = Level.makeCircle(gridSize, gridSize/2, player, invertedCircle);
		}
    	else {
    		player = new Player(gridSize/2, gridSize/2, gridSize, gridSize);
    		level = new Level(player, new Map(gridSize, gridSize, 7, 1, player, "Basic"));
    		//map = new Map(3, 100, 0, 1);
    	}

		pistol = new Weapon(player, displayHeight*0.5, displayHeight*0.5, displayWidth*.65, displayHeight*.65, new ImagePattern(new Image("Sprites/Pistol.png")));


    	stage = primaryStage;
		stage.setTitle("RayCaster");
		
		createKeyActions();
		comparator = new DistComparator();
		wallTilePolyList = new ArrayList<>();
		rfTileList = new ArrayList<>();
		projectileList = new ArrayList<>();
		enemyList = new ArrayList<>();
		sEffectList = new ArrayList<>();
		displayList = new ArrayList<>();
		rfIndexList = new ArrayList<>();
		cornerLinesList = new ArrayList<>();
		rfDistDirList = new double[gridSize][gridSize][3];

		createExplosionAnimation();
		
		floor = new Rectangle(displayWidth, displayHeightHalf+1, new LinearGradient(0, 1, 0, 0.2, true, CycleMethod.NO_CYCLE, new Stop[] { new Stop(0, Color.GREEN), new Stop(1, Color.BLACK)}));
		floor.setY(displayHeightHalf-1);
		sky = new Rectangle(displayWidth, displayHeightHalf+2, new LinearGradient(0, 0, 0, 0.8, true, CycleMethod.NO_CYCLE, new Stop[] { new Stop(0, Color.LIGHTBLUE), new Stop(1, Color.BLACK)}));
		
		Group group = new Group(), mapGridGroup = new Group(), displayGroup = new Group(), spriteMiniMap = new Group(), ui = new Group(), linesGroup = new Group();
		castLines = new Group();
		uiList = ui.getChildren();
		nodeList = group.getChildren();
		mapGridList = mapGridGroup.getChildren();
		castLinesList = castLines.getChildren();
		gameWorldList = displayGroup.getChildren();
		entityMiniMapList = spriteMiniMap.getChildren();
		linesList = linesGroup.getChildren();
		
		playerSprite = new Circle(mapSize/2, Color.BLUE);
		playerMoveUpdateGrid();
		
		dirLine = new Line();
		dirLine.setStroke(Color.RED);
		dirLine.setStrokeWidth(Math.max(mapSize/20, 1));
		updateDirLine();
		castLines.setVisible(drawCastLines);
		
		fpsCounterText = new Text(displayWidth-100, 40, "60");
		fpsCounterText.setFont(bigFont);

		// nodeList.add(floor);
		// nodeList.add(sky);
		nodeList.add(displayGroup);
		nodeList.add(mapGridGroup);
		nodeList.add(spriteMiniMap);
		nodeList.add(playerSprite);
		nodeList.add(dirLine);
		nodeList.add(castLines);
		nodeList.add(fpsCounterText);
		nodeList.add(ui);
		nodeList.add(linesGroup);
		
		createMapGrid();
		makeEnemyList();
		updateCastLines();
		updateUI();
		
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
				// System.out.println(event.getScreenX() + " - " + event.getScreenY());
			}
			
		});
		
		stage.setScene(scene);
		stage.show();

		double windowCenterX = displayWidthHalf+stage.getX(), windowCenterY = displayHeightHalf+stage.getY();
		robot.mouseMove((int)windowCenterX, (int)windowCenterY);
		
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent key) {
		        if(!keysPressed.contains(key.getCode())){
		        	keysPressed.add(key.getCode());
		        }
//		        if(keyActions.containsKey(key.getCode())) keyActions.get(key.getCode()).run();
//		        updateWallsSingle(FOV);
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
		fpsCounter++;
		if(time != System.currentTimeMillis()/1000) {
			fpsCounterText.setText(String.valueOf(fpsCounter));
			fpsCounter=0;
			time = (int)(System.currentTimeMillis()/1000);
		}
    	playerMoveUpdateGrid();
    	updateDirLine();
    	updateCastLines();
		updateEnemies();
		updateProjectiles();
		updateStationaryEffects();
		//updateFloorAndRoof();
		updateMap();
		updateCornerLines();
		updateDisplay();
		updateUI();
//    	Collections.sort(displayList, comparator);
//    	display.getChildren().setAll(displayList);
    	semaphore.release();
    }

	public void updateDisplay(){
		displayList.clear();
		displayList.addAll(wallTilePolyList.subList(0, wallCounter));
		displayList.addAll(projectileList);
		displayList.addAll(enemyList);
		displayList.addAll(sEffectList);
		// displayList.addAll(rfTileList.subList(0, rfCounter));
		Collections.sort(displayList, comparator);
		gameWorldList.setAll(displayList);
	}
    
    public void updateCastLines() {
		Map map = level.getCurMap();
    	castLinesList.clear();
    	wallCounter = 0; //displayList.clear();
		rfCounter = 0;
		double dir = player.dir;
		midLine = displayHeightHalf*(MIDLOOK-vertLookPos);
		// floor.setY(displayHeightHalf-midLine);
    	// floor.setHeight(displayHeight+midLine);
    	// sky.setHeight(displayHeightHalf-midLine);
		for(int i = 0; i < rfDistDirList.length; i++){
			for(int j = 0; j < rfDistDirList[0].length; j++){
				rfDistDirList[i][j][2] = -1;
			}
		}
		updateCastLinesUIIndividual(dir);
		// List<WallTilePoly> tempList = new ArrayList<WallTilePoly>(wallTilePolyList.subList(0, wallCounter));
		// Collections.sort(tempList, comparator);
		// //System.out.println(tempList.size());
		// gameWorldList.setAll(tempList);
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
    
    public void updateCastLinesUIIndividual(double dir) {
    	
    	if(!inFOV(dir)) return;
    	
		Map map = level.getCurMap();

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
					if(!(mapX <= 0 || mapY <= 0 || mapX >= map.map.length-1 || mapY >= map.map[0].length-1) && rfDistDirList[mapX][mapY][2] != 1) {
						calcDistAndDir(mapX, mapY, 1);
						calcDistAndDir(mapX, mapY+1, 0);
						calcDistAndDir(mapX+1, mapY+1, 0);
						calcDistAndDir(mapX+1, mapY, 0 );
					}
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
					if(!(mapX <= 0 || mapY <= 0 || mapX >= map.map.length-1 || mapY >= map.map[0].length-1) && rfDistDirList[mapX][mapY][2] != 1) {
						calcDistAndDir(mapX, mapY, 1);
						calcDistAndDir(mapX, mapY+1, 0);
						calcDistAndDir(mapX+1, mapY+1, 0);
						calcDistAndDir(mapX+1, mapY, 0 );
					}
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
			if(wallTilePolyList.size() <= wallCounter) {
				wallTilePolyList.add(new WallTilePoly());
    			wallTilePolyList.get(wallTilePolyList.size()-1).setStroke(Color.BLACK);
    		}
			WallTilePoly wallTile = wallTilePolyList.get(wallCounter++);
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
	    		
	    		colorDist = Math.min(temp3, 1000)/2000;
	    		wallTile.setFill(new Color(0.5+colorDist, 0, 0, 1));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
			}
	    	else {
	    		ObservableList<Double> list = wallTile.getPoints();
//	    		list.clear();
//	    		list.add(displayWidthHalf+displayWidthHalf*((player.dir-leftDir)/((FOV-10)*DEGTORAD/2)));
//	    		list.add(displayHeightHalf+displayHeightHalf/(Math.cos(player.dir-leftDir)*leftDist));
//	    		list.add(displayWidthHalf+displayWidthHalf*((player.dir-leftDir)/((FOV-10)*DEGTORAD/2)));
//	    		list.add(displayHeightHalf-displayHeightHalf/(Math.cos(player.dir-leftDir)*leftDist));
//	    		list.add(displayWidthHalf+displayWidthHalf*((player.dir-middleDir)/((FOV-10)*DEGTORAD/2)));
//	    		list.add(displayHeightHalf-displayHeightHalf/(Math.cos(player.dir-middleDir)*middleDist));
//	    		list.add(displayWidthHalf+displayWidthHalf*((player.dir-middleDir)/((FOV-10)*DEGTORAD/2)));
//	    		list.add(displayHeightHalf+displayHeightHalf/(Math.cos(player.dir-middleDir)*middleDist));
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
	    		
	    		colorDist = Math.min(temp3, 1000)/2000;
	    		wallTile.setFill(new Color(0.5+colorDist, 0, 0, 1));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
	    		
	    		if(wallTilePolyList.size() <= wallCounter) {
	    			wallTilePolyList.add(new WallTilePoly());
	    			wallTilePolyList.get(wallTilePolyList.size()-1).setStroke(Color.BLACK);
	    		}

	    		wallTile = wallTilePolyList.get(wallCounter++);
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
	    		
	    		colorDist = Math.min(temp3, 1000)/2000;
	    		wallTile.setFill(new Color(0.5+colorDist, 0, 0, 1));
	    		//wallTile.setFill(new Color(Math.random(),Math.random(),Math.random(),1));
	    		//displayList.add(wallTile);
        	}
        	createCastLine(rayX, rayY, Color.WHITE);
        	//System.out.println("Vert X: " + fRayX + ", Vert Y: " + fRayY + ", Horz X: "+ rayX + ", Horz Y: " + rayY);
        	
    }
    
    public void updateProjectiles(){
		Map map = level.getCurMap();
    	Color col;
    	double playerX = player.width, playerY = player.height, playerDir = player.dir, tempDir, fov = FOV*DEGTORAD, preX, preY;
    	int wallContact;
    	double[] distDir;
    	LinkedList<Projectile> deleteProjectiles = new LinkedList<>();
    	boolean passedPreCheck;
    	double midLine = displayHeightHalf*(MIDLOOK-vertLookPos);
    	for(Projectile projectile : projectileList) {
    		preX = projectile.x;
    		preY = projectile.y;
    		wallContact = projectile.move(map.map);
    		if(wallContact == -1 && !checkProjectileDamage(projectile)) {
    			distDir = projectile.getDist(playerX, playerY);
    			if((playerDir-fov/1.75 < 0 ? playerDir+fov/1.75 > distDir[1] || FULLROTATION+(playerDir-fov/1.75) < distDir[1] : false)) {
    				tempDir = FULLROTATION+(playerDir-fov/2) < distDir[1] ? FULLROTATION+playerDir : playerDir;
    				passedPreCheck = true;
    			}
    			else if((playerDir+fov/1.75 > FULLROTATION ? (playerDir+fov/1.75)-FULLROTATION > distDir[1] || playerDir-fov/1.75 < distDir[1] : false)) {
    				tempDir = (playerDir+fov/1.75)-FULLROTATION > distDir[1] ? playerDir-FULLROTATION : playerDir;
    				passedPreCheck = true;
    			}
    			else {
    				passedPreCheck = false;
    				tempDir = playerDir;
    			}
    			
    			if(passedPreCheck || (playerDir+fov/1.75 > distDir[1] && playerDir-fov/1.75 < distDir[1])) { 
    				projectile.setRadius(25/(Math.cos(player.dir-distDir[1])*distDir[0]));
    				projectile.setCenterY(displayHeightHalf-midLine);
    				projectile.setCenterX(displayWidthHalf-displayWidthHalf*(distDir[1]-tempDir)*2/fov);
    				col = (Color)(projectile.getFill());
    				col.darker();
    				projectile.setVisible(true);
    			}
    			else {
    				projectile.setVisible(false);
    			}	
    			updateProjectileOnMiniMap(projectile);
    		}
    		else {
    			if(wallContact != -2) {
					if(projectile.x >= 1 && projectile.y >= 1 && projectile.x < map.map.length-1 && projectile.y < map.map[0].length-1){
						map.map[(int)projectile.x][(int)projectile.y] = 0;
						updateMapTile((int)projectile.x, (int)projectile.y, Color.DARKSLATEGRAY);
					}
    				makeExplosion(projectile.x, projectile.y);
    			}
    			makeExplosion(preX, preY);
    			deleteProjectiles.add(projectile);
    		}
    	}
    	for(Projectile spriteDelete : deleteProjectiles) {
    		projectileList.remove(spriteDelete);
    		entityMiniMapList.remove(spriteDelete.minimap);
    		displayList.remove(spriteDelete);
    	}
    }

	public void updateMapTile(int x, int y, Color color){
		Map map = level.getCurMap();
    	((Shape)mapGridList.get(x*map.map.length+y)).setFill(color);
    }

    public boolean checkProjectileDamage(Projectile projectile) {
    	if(projectile.shooterId != 0 && projectile.dist < 0.25) {
    		player.hit(projectile);
    		updateUI();
    		return true;
    	}
    	for(Enemy enemy : enemyList) {
    		if(projectile.shooterId != enemy.id && projectile.checkCollide(enemy.x, enemy.y) < 0.3) {
				enemy.takeDamage(projectile.damage);
				if(enemy.isDead){
					enemyList.remove(enemy);
					entityMiniMapList.remove(enemy.minimap);
					displayList.remove(enemy);
				}
    			return true;
    		}
    	}
    	return false;
    }

	public void updateProjectileOnMiniMap(Projectile projectile) {
    	projectile.minimap.setCenterX(projectile.x*mapSize);
    	projectile.minimap.setCenterY(projectile.y*mapSize);
    }

	public void makeExplosion(double x, double y) {
    	StationaryEffect explosion = new StationaryEffect(x, y, mapSize, explosionAnimation);
    	sEffectList.add(explosion);
    	entityMiniMapList.add(explosion.minimap);
    	gameWorldList.add(explosion);
    }
    
    public void updateEnemies(){
    	double playerX = player.width, playerY = player.height, playerDir = player.dir, tempDir, fov = FOV*DEGTORAD;
		Map map = level.getCurMap();
    	double[] distDir;
    	boolean passedPreCheck;
    	double enemyWidth, enemyHeight, midLine = displayHeightHalf*(MIDLOOK-vertLookPos);;
    	for(Enemy enemy : enemyList) {
    		distDir = enemy.getDist(playerX, playerY);
    		if(enemy.isShooter && distDir[0] < 5 && enemy.shotCounter >= 100) {
    			createShootProjectile(enemy.shootPlayer(map));
    		}
    		if((playerDir-fov/1.75 < 0 ? playerDir+fov/1.75 > distDir[1] || FULLROTATION+(playerDir-fov/1.75) < distDir[1] : false)) {
    			tempDir = FULLROTATION+(playerDir-fov/2) < distDir[1] ? FULLROTATION+playerDir : playerDir;
    			passedPreCheck = true;
    		}
    		else if((playerDir+fov/1.75 > FULLROTATION ? (playerDir+fov/1.75)-FULLROTATION > distDir[1] || playerDir-fov/1.75 < distDir[1] : false)) {
    			tempDir = (playerDir+fov/1.75)-FULLROTATION > distDir[1] ? playerDir-FULLROTATION : playerDir;
    			passedPreCheck = true;
    		}
    		else {
    			passedPreCheck = false;
    			tempDir = playerDir;
    		}
    		
    		if(passedPreCheck || (playerDir+fov/1.75 > distDir[1] && playerDir-fov/1.75 < distDir[1])) {
    			enemyHeight = displayHeight*0.6/distDir[0];
    			enemyWidth = enemy.ratio*enemyHeight;
    			enemy.setHeight(enemyHeight);
    			enemy.setWidth(enemyWidth);
    			enemy.setY(displayHeightHalf-midLine-(enemyHeight/4));
    			enemy.setX(displayWidthHalf-(displayWidthHalf*(distDir[1]-tempDir)*2/fov)-(enemyWidth/2));
    			enemy.setVisible(true);
    			//System.out.println("Visible");
    		}
    		else {
    			//System.out.println("Invisible");
    			enemy.setVisible(false);
    		}	
    	}
    }
    
    public void createShootProjectile(Projectile projectile) {
    	projectile.getDist(player.width, player.height);
    	projectileList.add(projectile);
    	entityMiniMapList.add(projectile.minimap);
    	gameWorldList.add(projectile);
    		
    }

    public void updateStationaryEffects(){
    	double playerX = player.width, playerY = player.height, playerDir = player.dir, tempDir, fov = FOV*DEGTORAD;
    	double[] distDir;
    	boolean passedPreCheck;
    	double effectWidth, effectHeight, midLine = displayHeightHalf*(MIDLOOK-vertLookPos);
    	LinkedList<StationaryEffect> deleteEffects = new LinkedList<>();
    	for(StationaryEffect effect : sEffectList) {
    		distDir = effect.getDist(playerX, playerY);
    		if((playerDir-fov/1.75 < 0 ? playerDir+fov/1.75 > distDir[1] || FULLROTATION+(playerDir-fov/1.75) < distDir[1] : false)) {
    			tempDir = FULLROTATION+(playerDir-fov/2) < distDir[1] ? FULLROTATION+playerDir : playerDir;
    			passedPreCheck = true;
    		}
    		else if((playerDir+fov/1.75 > FULLROTATION ? (playerDir+fov/1.75)-FULLROTATION > distDir[1] || playerDir-fov/1.75 < distDir[1] : false)) {
    			tempDir = (playerDir+fov/1.75)-FULLROTATION > distDir[1] ? playerDir-FULLROTATION : playerDir;
    			passedPreCheck = true;
    		}
    		else {
    			passedPreCheck = false;
    			tempDir = playerDir;
    		}
    		
    		if(passedPreCheck || (playerDir+fov/1.75 > distDir[1] && playerDir-fov/1.75 < distDir[1])) {
    			effectHeight = displayHeight/distDir[0];
    			effectWidth = effect.getRatio()*effectHeight;
    			effect.setHeight(effectHeight);
    			effect.setWidth(effectWidth);
    			effect.setY(displayHeightHalf-midLine-(effectHeight/2));
    			effect.setX(displayWidthHalf-(displayWidthHalf*(distDir[1]-tempDir)*2/fov)-(effectWidth/2));
    			effect.setVisible(true);
    			//System.out.println("Visible");
    		}
    		else {
    			//System.out.println("Invisible");
    			effect.setVisible(false);
    		}	
    		if(effect.animationTimer >= 1) {
    			effect.animationTimer = 0;
    			if(!effect.animate()) {
    				deleteEffects.add(effect);
    			}
    		}
    		else {
    			effect.animationTimer++;
    		}
    	}
    	for(StationaryEffect spriteDelete : deleteEffects) {
    		sEffectList.remove(spriteDelete);
    		entityMiniMapList.remove(spriteDelete.minimap);
    		displayList.remove(spriteDelete);
    	}  	
    }

	public void calcDistAndDir(int x, int y, int tiled){
		double[] distDir = rfDistDirList[x][y];
		if(distDir[2] != -1) {
			distDir[2] = Math.max(distDir[2], tiled);
			return;
		}
		distDir[0] = PI + Math.atan((x-player.width)/(y-player.height));
		distDir[1]  = Math.sqrt((x-player.width)*(y-player.width)+(x-player.height)*(y-player.height));
		distDir[2] = tiled;
	}

	public void print2DArray(double[][][] array){
		for(int i = 0; i < array.length; i++){
			for(int j = 0; j < array[0].length; j++) {
				System.out.print(array[j][i][2] == -1 ? "■" : array[j][i][2] == 0 ? "A" : "▩");
			}
			System.out.println();
		}
		System.out.println();
	}

	public void updateMap(){
		for(int i = 0; i < rfDistDirList.length; i++){
			for(int j = 0; j < rfDistDirList[0].length; j++) {
				if(rfDistDirList[i][j][2] == -1) {
					updateMapTile(i, j, Color.DARKSLATEGRAY);
				}
				else if(rfDistDirList[i][j][2] == 0) {
					updateMapTile(i, j, Color.DARKGRAY);
				}
				else {
					updateMapTile(i, j, Color.GRAY);
				}
			}
		}
	}
	
	public void updateCornerLines(){
		linesCounter = 0;
		for(int i = 0; i < rfDistDirList.length; i++){
			for(int j = 0; j < rfDistDirList[0].length; j++) {
				if(cornerLinesList.size() <= linesCounter) {
					cornerLinesList.add(new Line());
					cornerLinesList.get(cornerLinesList.size()-1).setStroke(Color.BLACK);
				}
				Line curLine = cornerLinesList.get(linesCounter++);
				double[] distDir1 = rfDistDirList[i][j];
				double offset1 = PI-getOffsetFromView(distDir1[1]);
				double pos1x = displayWidthHalf+displayWidthHalf*((offset1)/((FOV-10)*DEGTORAD/2));
				double pos1y = Math.abs(displayHeightHalf/(Math.cos(offset1)*distDir1[0]));
				curLine.setEndX(pos1x);
				curLine.setEndY(displayHeightHalf-midLine+pos1y);
				curLine.setStartX(pos1x);
				curLine.setStartY(displayHeightHalf-midLine-pos1y);
			}
		}
		linesList.setAll(cornerLinesList.subList(0, linesCounter));
	}

	public void updateFloorAndRoof(){
		//System.out.println("Update Floor and Roof");
		rfCounter = 0;
		Map map = level.getCurMap();

		for(int i = 0; i < rfDistDirList.length; i++){
			for(int j = 0; j < rfDistDirList[0].length; j++){
				if((i <= 0 || j <= 0 || i >= map.map.length-1 || j >= map.map[0].length-1) && rfDistDirList[i][j][2] != 1) {
					continue;
				}
				if(false){

				double colorDist = Math.min(rfDistDirList[i][j][0], 1000)/2000;
				Color tileColor = new Color(0.5+colorDist, 0, 0, 1);
				updateMapTile(i, j, tileColor);
				
				if(rfTileList.size() <= rfCounter) {
					rfTileList.add(new WallTilePoly());
					rfTileList.get(rfTileList.size()-1).setStroke(Color.BLACK);
				}
				WallTilePoly roofTile = rfTileList.get(rfCounter++);
				roofTile.dist = 10000000;
				roofTile.setFill(tileColor);

				double[] distDir1 = rfDistDirList[i][j];
				double[] distDir2 = rfDistDirList[i+1][j];
				double[] distDir3 = rfDistDirList[i][j+1];
				double[] distDir4 = rfDistDirList[i+1][j+1];
				double offset1 = PI-getOffsetFromView(distDir1[1]);
				double offset2 = PI-getOffsetFromView(distDir2[1]);
				double offset3 = PI-getOffsetFromView(distDir3[1]);
				double offset4 = PI-getOffsetFromView(distDir4[1]);
				double pos1x = displayWidthHalf+displayWidthHalf*((offset1)/((FOV-10)*DEGTORAD/2));
				double pos1y = Math.abs(displayHeightHalf/(Math.cos(offset1)*distDir1[0]));
				double pos2x = displayWidthHalf+displayWidthHalf*((offset2)/((FOV-10)*DEGTORAD/2));
				double pos2y = Math.abs(displayHeightHalf/(Math.cos(offset2)*distDir2[0]));
				double pos3x = displayWidthHalf+displayWidthHalf*((offset3)/((FOV-10)*DEGTORAD/2));
				double pos3y = Math.abs(displayHeightHalf/(Math.cos(offset3)*distDir3[0]));
				double pos4x = displayWidthHalf+displayWidthHalf*((offset4)/((FOV-10)*DEGTORAD/2));
				double pos4y = Math.abs(displayHeightHalf/(Math.cos(offset4)*distDir4[0]));
				ObservableList<Double> list = roofTile.getPoints();
				list.clear();
				list.add(pos1x);
				list.add(displayHeightHalf-midLine+pos1y);
				list.add(pos2x);
				list.add(displayHeightHalf-midLine+pos2y);
				list.add(pos3x);
				list.add(displayHeightHalf-midLine+pos3y);
				list.add(pos4x);
				list.add(displayHeightHalf-midLine+pos4y);

				if(rfTileList.size() <= rfCounter) {
					rfTileList.add(new WallTilePoly());
					rfTileList.get(rfTileList.size()-1).setStroke(Color.BLACK);
				}
				WallTilePoly floorTile = rfTileList.get(rfCounter++);
				floorTile.dist = 10000000;
				floorTile.setFill(tileColor);
				
				list = floorTile.getPoints();
				list.clear();
				list.add(pos1x);
				list.add(displayHeightHalf-midLine-pos1y);
				list.add(pos2x);
				list.add(displayHeightHalf-midLine-pos2y);
				list.add(pos3x);
				list.add(displayHeightHalf-midLine-pos3y);
				list.add(pos4x);
				list.add(displayHeightHalf-midLine-pos4y);
				}
			}
		}
		// System.out.println(rfIndexList.size());
		// System.out.println(rfTileList.size());
	}

	public void makeEnemyList() {
    	ImagePattern img = new ImagePattern(new Image("Sprites/Man.png"));
    	Enemy enemy;
    	if(makeMaze) {
    		enemy = new Enemy((gridSize-1.5), (gridSize-1.5), img, mapSize, false, 1);
    		enemyList.add(enemy);
			displayList.add(enemy);
			entityMiniMapList.add(enemy.minimap);
    	}
		else if(makeCircle)	{
			Map map = level.getCurMap();
			for(int i = 0; i < enemyCount; i++) {
				double tempX = (gridSize-4)*Math.random()+2, tempY = (gridSize-4)*Math.random()+2;
				if(map.map[(int)tempX][(int)tempY] == 0){
					enemy = new Enemy(tempX, tempY, img, mapSize, true, 30);
					enemyList.add(enemy);
					displayList.add(enemy);
					entityMiniMapList.add(enemy.minimap);
				}
				else{
					i--;
				}
			}
		}
    	else {
    		for(int i = 0; i < enemyCount; i++) {
    			enemy = new Enemy((gridSize-4)*Math.random()+2, (gridSize-4)*Math.random()+2, img, mapSize, true, 30);
    			enemyList.add(enemy);
    			displayList.add(enemy);
    			entityMiniMapList.add(enemy.minimap);
    		}
    	}
    }

	public void updateUI() {
    	uiList.clear();
    	Text text = new Text(displayWidth-300, 40, "" + player.health);
    	text.setFont(bigFont);
    	uiList.add(text);
    	Rectangle rect = new Rectangle(displayWidthHalf-3, displayHeightHalf-3, 6, 6);
    	rect.setFill(Color.WHITE);
    	rect.setStroke(Color.BLACK);
    	uiList.add(rect);
		pistol.animate();
		uiList.add(pistol);
    }

    public void createMapGrid() {
		Map map = level.getCurMap();
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
    	Line castLine = new Line(player.width*mapSize, player.height*mapSize, x*mapSize, y*mapSize);
    	castLinesList.add(castLine);
    	castLine.setStroke(color);
    }
    
    public void mouseMove(MouseEvent mouseEvent) {
		double windowCenterX = displayWidthHalf+stage.getX(), windowCenterY = displayHeightHalf+stage.getY();
    	player.rotate((windowCenterX-mouseEvent.getScreenX())/displayWidth);
    	vertLookPos = Math.min(PI, Math.max(0, vertLookPos+2*(windowCenterY-mouseEvent.getScreenY())/displayHeight));
    	robot.mouseMove((int)windowCenterX, (int)windowCenterY);
    	//update();
    }
    
    public void createKeyActions() {
		Map map = level.getCurMap();
    	keyActions = new HashMap<>();
    	keyActions.put(KeyCode.W, () -> {player.move(true, map.map);});
    	keyActions.put(KeyCode.F, () -> {player.move(true, map.map);});
    	keyActions.put(KeyCode.A, () -> {player.strafe(true, map.map);});
    	keyActions.put(KeyCode.S, () -> {player.move(false, map.map);});
    	keyActions.put(KeyCode.D, () -> {player.strafe(false, map.map);});
    	keyActions.put(KeyCode.ENTER, () -> {drawCastLines = !drawCastLines; castLines.setVisible(drawCastLines);keysPressed.remove(KeyCode.ENTER);});
    	keyActions.put(KeyCode.SPACE, () -> {shoot();});
    	keyActions.put(KeyCode.G, () -> {shoot();});
    	keyActions.put(KeyCode.UP, () -> {vertLookPos = Math.min(PI, vertLookPos+0.1);});
    	keyActions.put(KeyCode.DOWN, () -> {vertLookPos = Math.max(0, vertLookPos-0.1);});
    	keyActions.put(KeyCode.LEFT, () -> {player.rotate(0.1);});
    	keyActions.put(KeyCode.RIGHT, () -> {player.rotate(-0.1);});
		keyActions.put(KeyCode.ESCAPE, () -> {openMenu(); keysPressed.remove(KeyCode.ESCAPE);});
    }

	public void shoot(){
		if(player.shotTimer >= 10) {
			Map map = level.getCurMap();
			Color color = new Color(Math.random(), Math.random(), Math.random(), 1);
			createShootProjectile(new Projectile(player.width, player.height, player.dir, color, mapSize, 0.1, map.map, 0, 10)); 
			player.shotTimer = 0;
			pistol.shoot();
		}
	}

	public void openMenu() {
		print2DArray(rfDistDirList);
	}
    
    public void createExplosionAnimation() {
    	explosionAnimation = new ImagePattern[9];
    	explosionAnimation[0] = new ImagePattern(new Image("Sprites/ExplosionAnimation/0.png"));
    	explosionAnimation[1] = new ImagePattern(new Image("Sprites/ExplosionAnimation/1.png"));
    	explosionAnimation[2] = new ImagePattern(new Image("Sprites/ExplosionAnimation/2.png"));
    	explosionAnimation[3] = new ImagePattern(new Image("Sprites/ExplosionAnimation/3.png"));
    	explosionAnimation[4] = new ImagePattern(new Image("Sprites/ExplosionAnimation/4.png"));
    	explosionAnimation[5] = new ImagePattern(new Image("Sprites/ExplosionAnimation/5.png"));
    	explosionAnimation[6] = new ImagePattern(new Image("Sprites/ExplosionAnimation/6.png"));
    	explosionAnimation[7] = new ImagePattern(new Image("Sprites/ExplosionAnimation/7.png"));
    	explosionAnimation[8] = new ImagePattern(new Image("Sprites/ExplosionAnimation/8.png"));
    }

    public void stop() {
		running.set(false);
	}
    
}
