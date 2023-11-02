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

public class Old_UI extends Application{

	private Stage stage;
	private Font bigFont = new Font("Courier New", 32);
	private Map map;
	private ObservableList<Node> nodeList, castLinesList, entityMiniMapList, mapGridList, uiList;
	private ArrayList<Node> displayList;
	private WallTile[] wallsList, upperWallsList;
	private LinkedList<WallTile> extraUpperWallsList;
	private LinkedList<Projectile> projectileList;
	private LinkedList<Enemy> enemyList;
	private LinkedList<StationaryEffect> sEffectList;
	private Line dirLine;
	private Circle playerSprite;
	private int mapSize = 300, gridSize = 10, fovMain = 90, wallCountMain = 1500, screenWidth = 1500, time, fpsCounter, maxdof = 15;
	private double vertLookPos = Math.PI/2;
	private boolean drawCastLines, makeMaze = true;
	private Group castLines, display;
	private final double MIDLOOK = Math.PI/2, FULLROTATION = Math.PI*2, THIRDROTATION = Math.PI*3/2, PI = Math.PI, DEGTORAD = Math.PI/180;
	private Rectangle floor, sky;
	private Robot robot;
	private ArrayList<double[]> colorList;
	private DistComparator comparator;
	private Text fpsCounterText;
	private HashMap<KeyCode, Runnable> keyActions;
	private LinkedList<KeyCode> keysPressed;
	private ImagePattern[] explosionAnimation;
	private ImagePattern[][] wallPatterns;
	private int counterWalls;
	private final Semaphore semaphore = new Semaphore(1);
	final AtomicBoolean running = new AtomicBoolean(false);
	
	public static void main(String[] args) {
		Application.launch(args);
	}
    public void start(Stage primaryStage){
    	stage = primaryStage;
		stage.setTitle("RayCaster");
		
		makeWallPatterns();
		
		comparator = new DistComparator();
		time = (int)(System.currentTimeMillis()/1000);
		mapSize /= gridSize;
		counterWalls = 0;
		
		if(makeMaze) {
			map = Map.makeMaze((gridSize & 1) != 1 ? ++gridSize : gridSize, wallPatterns.length-1);
		}
		else {
			map = new Map(gridSize, gridSize, 7, wallPatterns.length-1);
		}
		
		Group mapGrid = new Group();
		mapGridList = mapGrid.getChildren();
		
		
		floor = new Rectangle(screenWidth, 501, new LinearGradient(0, 1, 0, 0.2, true, CycleMethod.NO_CYCLE, new Stop[] { new Stop(0, Color.GREEN), new Stop(1, Color.BLACK)}));
		floor.setY(499);
		sky = new Rectangle(screenWidth, 502, new LinearGradient(0, 0, 0, 0.8, true, CycleMethod.NO_CYCLE, new Stop[] { new Stop(0, Color.LIGHTBLUE), new Stop(1, Color.BLACK)}));
		
		display = new Group();
		displayList = new ArrayList<>();
		
		wallsList = new WallTile[wallCountMain];
		upperWallsList = new WallTile[wallCountMain];
		projectileList = new LinkedList<>();
		enemyList = new LinkedList<>();
		keysPressed = new LinkedList<>();
		sEffectList = new LinkedList<>();
		extraUpperWallsList = new LinkedList<>();
		
		createExplosionAnimation();
		
		dirLine = new Line();
		dirLine.setStroke(Color.RED);
		dirLine.setStrokeWidth(Math.max(mapSize/20, 1));
		
		Group spriteMiniMap = new Group();
		entityMiniMapList = spriteMiniMap.getChildren();
		
		playerSprite = new Circle(mapSize/2, Color.BLUE);
		playerMoveUpdateGrid();
		
		castLines = new Group();
		castLinesList = castLines.getChildren();
		
		fpsCounterText = new Text(1430, 40, "60");
		fpsCounterText.setFont(bigFont);
		
		Group ui = new Group();
		uiList = ui.getChildren();
		
		Group group = new Group();
		nodeList = group.getChildren();
		
		nodeList.add(floor);
		nodeList.add(sky);
		nodeList.add(display);
		nodeList.add(mapGrid);
		nodeList.add(playerSprite);
		nodeList.add(dirLine);
		nodeList.add(castLines);
		nodeList.add(spriteMiniMap);
		nodeList.add(fpsCounterText);
		nodeList.add(ui);
		
		createMapGrid();
		makeEnemyList();
		createWalls(wallCountMain);
		createCastLines(wallCountMain);
		createKeyActions();
		updateUI();
		
		
		try {
			robot = new Robot();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		Scene scene = new Scene(group, screenWidth, 1000);
		
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent key) {
		        if(!keysPressed.contains(key.getCode())){
		        	keysPressed.add(key.getCode());
		        }
		        if(keyActions.containsKey(key.getCode())) keyActions.get(key.getCode()).run();
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
		
		EventHandler<MouseEvent> moveMouse = this::mouseMove;
		scene.addEventHandler(MouseEvent.MOUSE_MOVED, moveMouse);
		
		scene.setCursor(Cursor.NONE);
		stage.setScene(scene);
		stage.show();
		
		colorList = new ArrayList<>();
		makeColorList();
		
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
							updateDisplay(fovMain);
						});
					}
				}catch(Exception e){e.printStackTrace();}
				return null;
			}
		};
		
		new Thread(task).start();
    }
    
    public void updateDisplay(double fovMain) {
//    	Long start = System.currentTimeMillis();
//    	for(int i = 1000; i >=0; i--) {
    	for(KeyCode code : keysPressed) {
    		if(keyActions.containsKey(code)) keyActions.get(code).run();
    	}
    	map.player.shotTimer++;
    	fpsCounter++;
    	if(time != System.currentTimeMillis()/1000) {
    		fpsCounterText.setText(String.valueOf(fpsCounter));
    		fpsCounter=0;
    		time = (int)(System.currentTimeMillis()/1000);
    	}
    	removeExtraUpperWalls();
    	playerMoveUpdateGrid();
    	updateDirLine();
    	updateWalls(fovMain);
    	updateUpperWalls(fovMain);
    	updateEnemies(fovMain);
    	updateProjectiles(fovMain);
    	updateStationaryEffects(fovMain);
    	Collections.sort(displayList, comparator);
    	display.getChildren().setAll(displayList);
    	semaphore.release();
//    	System.out.println(displayList.size());
//    	}
//    	System.out.println(System.currentTimeMillis()-start);
    }
    
    public void createWalls(int recCount) {
    	WallTile cur;
    	for(int i = 0, size = screenWidth/recCount; i < recCount; i++) {
    		cur = new WallTile(i*size, 100, size, 100);
    		wallsList[i] = cur;
    		displayList.add(cur);
    		
    		cur = new WallTile(i*size, 100, size, 100);
    		upperWallsList[i] = cur;
    		displayList.add(cur);
    		
    	}
    }
    
    public void createCastLines(int size) {
    	Line temp;
    	for(; size >= 0; size--) {
    		temp = new Line();
    		temp.setStroke(Color.WHITE);
    		castLinesList.add(temp);
    	}
    }
    
    public void updateUI() {
    	uiList.clear();
    	Text text = new Text(1250, 40, "" + map.player.health);
    	text.setFont(bigFont);
    	uiList.add(text);
    	Rectangle rect = new Rectangle(747.5, 497.5, 5, 5);
    	rect.setFill(Color.WHITE);
    	rect.setStroke(Color.BLACK);
    	uiList.add(rect);
    }
    
    public void updateWalls(double fovIn) {
   	 
    	double fov = fovIn*DEGTORAD, recCount = wallsList.length;
    	double fovOffsetAmount = fov/(recCount-1);
    	double dir = map.player.dir + fov/2;
    	if(dir > FULLROTATION) {dir-=FULLROTATION;} 
    	double rectHeight;
    	WallTile curRect;
    	double midLine = 500*(MIDLOOK-vertLookPos);
    	Paint color, img;
    	double[] colorValues;
    	double colorDist;
    	counterWalls++;
    	
    	double rayX, rayY, xOffset = 0, yOffset = 0, aTan, nTan, distY, distX, fRayX, fRayY;
    	
    	for(int i = 0, mapX = 0, mapY = 0, dof, wallNumX = 0, wallNumY = 0, wallSliceNum; i < recCount; i++) {
    		
    		//RayCasting Code//
    		
    		distX = 1000000;
    		distY = 1000001;
    		
    		if(dir < 0) {dir+=FULLROTATION;}
    		
    		aTan=Math.tan(dir);
        	nTan=1/Math.tan(dir);
        	dof = 0;
        	
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
        		rayX=map.player.width; 
        		rayY=map.player.height; 
        		dof = maxdof;
        		wallNumY = 0;
        	}
        	while(dof < maxdof){
        		mapX = (int)rayX; mapY = (int)rayY;
        		if(mapX < 0 || mapY < 0 || mapX >= map.map.length || mapY >= map.map[0].length) {
        			dof = maxdof; 
        			wallNumY = 0;
        		}
        		else if(map.map[mapX][mapY] != 0) {
        			dof = maxdof+1;
        			wallNumY = map.map[mapX][mapY];
        		}
        		else {
        			rayX+=xOffset; 
        			rayY+=yOffset; 
        			dof++;}
    		}
        	if(dof == maxdof) {
        		wallNumY = 0;
        	}
    		//System.out.println("Angle = " + dir + ", xCur = " + rayX + ", xOffset = " + xOffset + ", yCur = " + rayY + ", yOffset = " + yOffset + ", ");

        	fRayX = rayX;
        	fRayY = rayY;
        	distY = Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height));

        	dof = 0;
        	
        	if(dir==0 || dir==PI) {
        		rayX=map.player.width; 
        		rayY=map.player.height; 
        		dof=maxdof;
        		wallNumX = 0;
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
        	
        	
        	while(dof < maxdof){
        		mapX = (int)rayX; mapY = (int)rayY;
        		if(mapX < 0 || mapY < 0 || mapX >= map.map.length || mapY >= map.map[0].length) {
        			dof = maxdof;
        			wallNumX = 0;
        		}
        		else if(map.map[mapX][mapY] != 0) {
        			dof = maxdof+1;
        			wallNumX = map.map[mapX][mapY];
        		}
        		else {
        			rayX+=xOffset; 
        			rayY+=yOffset; 
        			dof++;
        		}
        	}
        	if(dof == maxdof) {
        		wallNumX = 0;
        	}
        	//System.out.println("Angle = " + dir + ", xCur = " + rayX + ", xOffset = " + xOffset + ", yCur = " + rayY + ", yOffset = " + yOffset + ", ");	
        	
        	distX = Math.min(distY, Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height)));
        	
        	rectHeight = 1000/(Math.cos(map.player.dir-dir)*distX);
    		
    		//Rectangle Code//
        	
        	colorDist = Math.min(rectHeight, 1000)/2500;
        	
        	if(distX == distY) {
        		if(dir>MIDLOOK && dir<THIRDROTATION) {
        			wallSliceNum = (int)((counterWalls/3)+16*(fRayX%1));
        		}
        		else {
        			wallSliceNum = (counterWalls/3)+15 - (int)(16*(fRayX%1));
        		}
        		try {
        			img = wallPatterns[wallNumY][wallSliceNum%16];
        		}
        		catch(ArrayIndexOutOfBoundsException e) {
        			img = Color.BLACK;
        		}
        		if(drawCastLines) {
        			colorValues = colorList.get(wallNumY);
                   	color = new Color(colorValues[0]*colorDist, colorValues[1]*colorDist, colorValues[2]*colorDist, 1);
               		createCastLine(fRayX, fRayY, i, color);
               	}
        	}
        	else {
        		if(dir < PI) {
        			wallSliceNum = (int)((counterWalls/3)+16*(rayY%1));
        		}
        		else {
        			wallSliceNum = (counterWalls/3) + 15 - (int)(16*(rayY%1));
        		}
        		try {
        			img = wallPatterns[wallNumX][wallSliceNum%16];
        		}
        		catch(ArrayIndexOutOfBoundsException e) {
        			img = Color.BLACK;
        		}
       			if(drawCastLines) {
               		colorValues = colorList.get(wallNumX);
                   	color = new Color(colorValues[0]*colorDist, colorValues[1]*colorDist, colorValues[2]*colorDist, 1);
               		createCastLine(rayX, rayY, i, color);
               	}
        	}
        	//System.out.println("Vert X: " + fRayX + ", Vert Y: " + fRayY + ", Horz X: "+ rayX + ", Horz Y: " + rayY);
        	
    		curRect = wallsList[i];
    		curRect.dist = distX;
    		curRect.setVisible(true);
    		curRect.setHeight(rectHeight);
    		curRect.setFill(img);
    		curRect.setY(500-midLine-(rectHeight/2));
    		
    		dir -= fovOffsetAmount;
    	}
    	floor.setY(500-midLine);
    	floor.setHeight(1000+midLine);
    	sky.setHeight(500-midLine);
    	
    }
    
    public void updateWallsSingle(double fovIn) {
    	 
    	double fov = fovIn*DEGTORAD, recCount = wallsList.length;
    	double fovOffsetAmount = fov/(recCount-1);
    	double dir = map.player.dir + fov/2;
    	if(dir > FULLROTATION) {dir-=FULLROTATION;} 
    	double rectHeight;
    	WallTile curRect;
    	double midLine = 500*(MIDLOOK-vertLookPos);
    	Paint color, img;
    	double[] colorValues;
    	double colorDist;
    	
    	double rayX, rayY, rayXH, rayYH, rayXV, rayYV, xOffsetH, yOffsetH, xOffsetV, yOffsetV, curOffsetH, curOffsetV, aTan, nTan, distY, distX, fRayX, fRayY;
    	
    	for(int i = 0, mapX = 0, mapY = 0, dof, wallNumX = 0, wallNumY = 0, wallSliceNum; i < recCount; i++) {
    		
    		//RayCasting Code//
    		
    		distX = 1000000;
    		distY = 1000001;
    		
    		if(dir < 0) {dir+=FULLROTATION;}
    		
    		aTan=Math.tan(dir);
        	nTan=1/Math.tan(dir);
        	dof = 0;
        	
        	//Get All Offsets//
        	
        	//down
        	if(dir<MIDLOOK || dir>THIRDROTATION) {
        		yOffsetV=1; 
        		xOffsetV=yOffsetV*aTan;
        		rayYV=(int)map.player.height+1.00001; 
        		rayXV=(map.player.height-rayYV)*-aTan+map.player.width; 
        	}
        	//up
        	else if(dir>MIDLOOK && dir<THIRDROTATION) {
        		yOffsetV=-1; 
        		xOffsetV=yOffsetV*aTan;
        		rayYV=(int)map.player.height-0.00001; 
        		rayXV=(map.player.height-rayYV)*-aTan+map.player.width; 
        		}
        	else {
        		xOffsetV= 1000000; 
        		yOffsetV= 1000001;
        		rayXV = 1000000;
        		rayYV = 1000001;
        	}
        	
        	if(dir==0 || dir==PI) {
        		xOffsetH= 1000000; 
        		yOffsetH= 1000001;
        		rayXH = 1000000;
        		rayYH = 1000001;
        	}
        	//left
        	else if(dir<PI) {
        		xOffsetH= 1; 
        		yOffsetH= xOffsetH*nTan;
        		rayXH=(int)map.player.width+1.00001; 
        		rayYH=(map.player.width-rayXH)*-nTan+map.player.height;
        	}
        	//right
        	else {
        		xOffsetH= -1; 
        		yOffsetH= xOffsetH*nTan;
        		rayXH=(int)map.player.width-0.00001; 
        		rayYH=(map.player.width-rayXH)*-nTan+map.player.height; 
        	}
        	//if(Math.sqrt((map.player.width-rayXV)*(map.player.width-rayXV)+(map.player.height-rayYV)*(map.player.height-rayYV)) > Math.sqrt((map.player.width-rayXH)*(map.player.width-rayXH)+(map.player.height-rayYH)*(map.player.height-rayYH))) {
        	if(Math.abs(map.player.width-rayXV) > Math.abs(map.player.height-rayYH)) {
        		rayX = rayXH;
        		rayY = rayYH;
        	}
        	else {
        		rayX = rayXV;
        		rayY = rayYV;
        	}
        	
        	if(Math.abs(xOffsetV) > Math.abs(yOffsetH)) {
        		System.out.println("xOffsetH = " + xOffsetH + ", yOffsetH = " + yOffsetH);
        		createCastLine(rayX+xOffsetH, rayY+yOffsetH, i, Color.WHITESMOKE);
        	}
        	else {
        		System.out.println("xOffsetV = " + xOffsetV + ", yOffsetV = " + yOffsetV);
        		createCastLine(rayX+xOffsetV, rayY+yOffsetV,  i, Color.WHITESMOKE);
        	}
    	}
        	/*
        	//down
        	if(dir<MIDLOOK || dir>THIRDROTATION) {
        		rayY=(int)map.player.height+1.0001; 
        		rayX=(map.player.height-rayY)*-aTan+map.player.width; 
        		yOffsetV=1; 
        		xOffsetV=yOffsetV*aTan;
        	}
        	//up
        	else if(dir>MIDLOOK && dir<THIRDROTATION) {
        		rayY=(int)map.player.height-0.0001; 
        		rayX=(map.player.height-rayY)*-aTan+map.player.width; 
        		yOffsetV=-1; 
        		xOffsetV=yOffsetV*aTan;
        		}
        	else {
        		rayX=map.player.width; 
        		rayY=map.player.height; 
        		dof = maxdof;
        		wallNumY = 0;
        	}
        	while(dof < maxdof){
        		mapX = (int)rayX; mapY = (int)rayY;
        		if(mapX < 0 || mapY < 0 || mapX >= map.map.length || mapY >= map.map[0].length) {
        			dof = maxdof; 
        			wallNumY = 0;
        		}
        		else if(map.map[mapX][mapY] != 0) {
        			dof = maxdof+1;
        			wallNumY = map.map[mapX][mapY];
        		}
        		else {
        			rayX+=xOffsetV; 
        			rayY+=yOffsetV; 
        			dof++;}
    		}
        	if(dof == maxdof) {
        		wallNumY = 0;
        	}
    		//System.out.println("Angle = " + dir + ", xCur = " + rayX + ", xOffset = " + xOffset + ", yCur = " + rayY + ", yOffset = " + yOffset + ", ");

        	fRayX = rayX;
        	fRayY = rayY;
        	distY = Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height));

        	dof = 0;
        	
        	if(dir==0 || dir==PI) {
        		rayX=map.player.width; 
        		rayY=map.player.height; 
        		dof=maxdof;
        		wallNumX = 0;
        	}
        	//left
        	else if(dir<PI) {
        		rayX=(int)map.player.width+1.00001; 
        		rayY=(map.player.width-rayX)*-nTan+map.player.height; 
        		xOffsetH= 1; 
        		yOffsetH= xOffsetH*nTan;
        	}
        	//right
        	else {
        		rayX=(int)map.player.width-0.00001; 
        		rayY=(map.player.width-rayX)*-nTan+map.player.height; 
        		xOffsetH= -1; 
        		yOffsetH= xOffsetH*nTan;
        	}
        	
        	
        	while(dof < maxdof){
        		mapX = (int)rayX; mapY = (int)rayY;
        		if(mapX < 0 || mapY < 0 || mapX >= map.map.length || mapY >= map.map[0].length) {
        			dof = maxdof;
        			wallNumX = 0;
        		}
        		else if(map.map[mapX][mapY] != 0) {
        			dof = maxdof+1;
        			wallNumX = map.map[mapX][mapY];
        		}
        		else {
        			rayX+=xOffset; 
        			rayY+=yOffset; 
        			dof++;
        		}
        	}
        	if(dof == maxdof) {
        		wallNumX = 0;
        	}
        	//System.out.println("Angle = " + dir + ", xCur = " + rayX + ", xOffset = " + xOffset + ", yCur = " + rayY + ", yOffset = " + yOffset + ", ");	
        	
        	distX = Math.min(distY, Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height)));
        	
        	rectHeight = 1000/(Math.cos(map.player.dir-dir)*distX);
    		
    		//Rectangle Code//
        	
        	colorDist = Math.min(rectHeight, 1000)/1100;
        	
        	if(distX == distY) {
        		if(dir>MIDLOOK && dir<THIRDROTATION) {
        			wallSliceNum = (int)(16*(fRayX%1));
        		}
        		else {
        			wallSliceNum = 15 - (int)(16*(fRayX%1));
        		}
        		try {
        			img = wallPatterns[wallNumY][wallSliceNum];
        		}
        		catch(ArrayIndexOutOfBoundsException e) {
        			img = Color.BLACK;
        		}
        		if(drawCastLines) {
        			colorValues = colorList.get(wallNumY);
                   	color = new Color(colorValues[0]*colorDist, colorValues[1]*colorDist, colorValues[2]*colorDist, 1);
               		createCastLine(fRayX, fRayY, i, color);
               	}
        	}
        	else {
        		
        		if(dir < PI) {
        			wallSliceNum = (int)(16*(rayY%1));
        		}
        		else {
        			wallSliceNum = 15 - (int)(16*(rayY%1));
        		}
        		try {
        			img = wallPatterns[wallNumX][wallSliceNum];
        		}
        		catch(ArrayIndexOutOfBoundsException e) {
        			img = Color.BLACK;
        		}
       			if(drawCastLines) {
               		colorValues = colorList.get(wallNumX);
                   	color = new Color(colorValues[0]*colorDist, colorValues[1]*colorDist, colorValues[2]*colorDist, 1);
               		createCastLine(rayX, rayY, i, color);
               	}
        	}
        	//System.out.println("Vert X: " + fRayX + ", Vert Y: " + fRayY + ", Horz X: "+ rayX + ", Horz Y: " + rayY);
        	
    		curRect = wallsList[i];
    		curRect.dist = distX;
    		curRect.setVisible(true);
    		curRect.setHeight(rectHeight);
    		curRect.setFill(img);
    		curRect.setY(500-midLine-(rectHeight/2));
    		
    		dir -= fovOffsetAmount;
    	}
    	floor.setY(500-midLine);
    	floor.setHeight(1000+midLine);
    	sky.setHeight(500-midLine);
    	*/
    }
    
    public void updateUpperWalls(double fovIn) {
    	
    	double fov = fovIn*DEGTORAD, recCount = upperWallsList.length;
    	double fovOffsetAmount = fov/(recCount-1);
    	double dir = map.player.dir + fov/2;
    	if(dir > FULLROTATION) {dir-=FULLROTATION;} 
    	double rectHeight;
    	WallTile curRect;
    	double midLine = 500*(MIDLOOK-vertLookPos);
    	Color color;
    	double[] colorValues;
    	double colorDist;
    	
    	double rayX, rayY, xOffset = 0, yOffset = 0, aTan, nTan, distY, distX;
    	
    	for(int i = 0, mapX = 0, mapY = 0, dof, wallNumX = 0, wallNumY = 0; i < recCount; i++) {
    		
    		//RayCasting Code//
    		
    		distX = 1000000;
    		distY = 1000001;
    		
    		if(dir < 0) {dir+=FULLROTATION;}
    		
    		aTan=Math.tan(dir);
        	nTan=1/Math.tan(dir);
        	dof = 0;
        	
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
        		rayX=map.player.width; 
        		rayY=map.player.height; 
        		dof = maxdof;
        		wallNumY = 0;
        	}
        	while(dof < maxdof){
        		mapX = (int)rayX; mapY = (int)rayY;
        		if(mapX < 0 || mapY < 0 || mapX >= map.upperMap.length || mapY >= map.upperMap[0].length) {
        			dof = maxdof; 
        			wallNumY = 0;
        		}
        		else if(map.upperMap[mapX][mapY] != 0) {
        			if(map.map[mapX][mapY] == 10) {
        				distY = Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height));
        				rectHeight = 1000/(Math.cos(map.player.dir-dir)*distY);
        				colorValues = colorList.get(wallNumY);
        				colorDist = Math.min(rectHeight, 1000)/1100;
        				color = new Color(colorValues[0]*colorDist, colorValues[1]*colorDist, colorValues[2]*colorDist, 1);
        				extraUpperWallsList.add(new WallTile(i*screenWidth/wallCountMain, 500-midLine-(rectHeight/2)-rectHeight, screenWidth/wallCountMain, rectHeight, distY, color));
        				displayList.add(extraUpperWallsList.getLast());
        				rayX+=xOffset; 
            			rayY+=yOffset; 
            			dof++;
            			mapX = (int)rayX; mapY = (int)rayY;
            			if(mapX < 0 || mapY < 0 || mapX >= map.upperMap.length || mapY >= map.upperMap[0].length) {
                			dof = maxdof;
                			wallNumX = 0;
                			break;
                		}
            			else {
            				while(map.upperMap[mapX][mapY] != 0 && map.map[mapX][mapY] == 0 && dof < maxdof) {
            					rayX+=xOffset; 
            					rayY+=yOffset; 
            					dof++;
            					mapX = (int)rayX; mapY = (int)rayY;
            					if(mapX < 0 || mapY < 0 || mapX >= map.upperMap.length || mapY >= map.upperMap[0].length) {
            						dof = maxdof;
            						wallNumX = 0;
            						break;
            					}
                    		}
            			}
            			//System.out.println(distY);
        			}
        			else {
        				dof = maxdof+1;
        				wallNumY = map.upperMap[mapX][mapY];
        			}
        		}
        		else {
        			rayX+=xOffset; 
        			rayY+=yOffset; 
        			dof++;}
    		}
        	if(dof == maxdof) {
        		wallNumY = 0;
        	}
    		//System.out.println("Angle = " + dir + ", xCur = " + rayX + ", xOffset = " + xOffset + ", yCur = " + rayY + ", yOffset = " + yOffset + ", ");

        	distY = Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height));

        	dof = 0;
        	
        	if(dir==0 || dir==PI) {
        		rayX=map.player.width; 
        		rayY=map.player.height; 
        		dof=maxdof;
        		wallNumX = 0;
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
        	
        	
        	while(dof < maxdof){
        		mapX = (int)rayX; mapY = (int)rayY;
        		if(mapX < 0 || mapY < 0 || mapX >= map.upperMap.length || mapY >= map.upperMap[0].length) {
        			dof = maxdof;
        			wallNumX = 0;
        		}
        		else if(map.upperMap[mapX][mapY] != 0) {
        			if(map.map[mapX][mapY] == 10) {
        				distX = Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height));
        				rectHeight = 1000/(Math.cos(map.player.dir-dir)*distX);
//        				colorValues = colorList.get(wallNumX);
//        				colorDist = Math.min(rectHeight, 1000)/1100;
//                      color = new Color(colorValues[0]*colorDist, colorValues[1]*colorDist, colorValues[2]*colorDist, 1);
        				extraUpperWallsList.add(new WallTile(i*screenWidth/wallCountMain, 500-midLine-(rectHeight/2)-rectHeight, screenWidth/wallCountMain, rectHeight, distX, Color.WHITE));
        				displayList.add(extraUpperWallsList.getLast());
        				rayX+=xOffset; 
            			rayY+=yOffset; 
            			dof++;
            			mapX = (int)rayX; mapY = (int)rayY;
            			if(mapX < 0 || mapY < 0 || mapX >= map.upperMap.length || mapY >= map.upperMap[0].length) {
                			dof = maxdof;
                			wallNumX = 0;
                			break;
                		}
            			else {
            				while(map.upperMap[mapX][mapY] != 0 && map.map[mapX][mapY] == 0 && dof < maxdof) {
           						rayX+=xOffset; 
           						rayY+=yOffset; 
           						dof++;
           						mapX = (int)rayX; mapY = (int)rayY;
            					if(mapX < 0 || mapY < 0 || mapX >= map.upperMap.length || mapY >= map.upperMap[0].length) {
            						dof = maxdof;
            						wallNumX = 0;
            						break;
            					}
                    		}
            			}
        			}
        			else {
        				dof = maxdof+1;
        				wallNumX = map.upperMap[mapX][mapY];
        			}
        		}
        		else {
        			rayX+=xOffset; 
        			rayY+=yOffset; 
        			dof++;
        		}
        	}
        	if(dof == maxdof) {
        		wallNumX = 0;
        	}
        	//System.out.println("Angle = " + dir + ", xCur = " + rayX + ", xOffset = " + xOffset + ", yCur = " + rayY + ", yOffset = " + yOffset + ", ");	
        	
        	distX = Math.min(distY, Math.sqrt((rayX-map.player.width)*(rayX-map.player.width)+(rayY-map.player.height)*(rayY-map.player.height)));
        	
        	rectHeight = 1000/(Math.cos(map.player.dir-dir)*distX);
    		
    		//Rectangle Code//
        	
        	colorDist = Math.min(rectHeight, 1000)/1100;
        	
        	if(distX == distY) {
            	colorValues = colorList.get(wallNumY);
               	color = new Color(colorValues[0]*colorDist, colorValues[1]*colorDist, colorValues[2]*colorDist, 1);
        	}
        	else {
            	colorValues = colorList.get(wallNumX);
               	color = new Color(colorValues[0]*colorDist, colorValues[1]*colorDist, colorValues[2]*colorDist, 1);
        	}
        	
        	
    		curRect = upperWallsList[i];
    		curRect.dist = distX;
    		curRect.setVisible(true);
    		curRect.setHeight(rectHeight);
    		curRect.setFill(color);
    		curRect.setY(500-midLine-(rectHeight/2)-rectHeight);
    		//System.out.println(curRect.dist+"\n");
    		
    		dir -= fovOffsetAmount;
    	}
    	
    }
    
    public void removeExtraUpperWalls() {
    	for(WallTile wall : extraUpperWallsList) {
    		displayList.remove(wall);
    	}
    	extraUpperWallsList.clear();
    }
    
    public void updateProjectiles(double fovMain){
    	Color col;
    	double playerX = map.player.width, playerY = map.player.height, playerDir = map.player.dir, tempDir, fov = fovMain*DEGTORAD, preX, preY;
    	int wallContact;
    	double[] distDir;
    	LinkedList<Projectile> deleteProjectiles = new LinkedList<>();
    	boolean passedPreCheck;
    	double midLine = 500*(MIDLOOK-vertLookPos);
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
    				projectile.setRadius(25/(Math.cos(map.player.dir-distDir[1])*distDir[0]));
    				projectile.setCenterY(500-midLine);
    				projectile.setCenterX(750-750*(distDir[1]-tempDir)*2/fov);
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
    				map.map[(int)projectile.x][(int)projectile.y] = 0;
    				makeExplosion(projectile.x, projectile.y);
    				updateMapTile((int)projectile.x, (int)projectile.y, wallContact);
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
    
    public boolean checkProjectileDamage(Projectile projectile) {
    	if(projectile.shooterId != 0 && projectile.dist < 0.25) {
    		map.player.hit(projectile);
    		updateUI();
    		return true;
    	}/*
    	for(Enemy enemy : enemyList) {
    		if(projectile.shooterId != enemy.id && projectile.checkCollide(enemy.x, enemy.y) < 0.3) {
    			enemyList.remove(enemy);
        		entityMiniMapList.remove(enemy.minimap);
        		displayList.remove(enemy);
    			return true;
    		}
    	}*/
    	return false;
    }
    
    public void updateEnemies(double fovMain){
    	double playerX = map.player.width, playerY = map.player.height, playerDir = map.player.dir, tempDir, fov = fovMain*DEGTORAD;
    	double[] distDir;
    	boolean passedPreCheck;
    	double enemyWidth, enemyHeight, midLine = 500*(MIDLOOK-vertLookPos);;
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
    			enemyHeight = 600/distDir[0];
    			enemyWidth = enemy.ratio*enemyHeight;
    			enemy.setHeight(enemyHeight);
    			enemy.setWidth(enemyWidth);
    			enemy.setY(500-midLine-(enemyHeight/4));
    			enemy.setX(750-(750*(distDir[1]-tempDir)*2/fov)-(enemyWidth/2));
    			enemy.setVisible(true);
    			//System.out.println("Visible");
    		}
    		else {
    			//System.out.println("Invisible");
    			enemy.setVisible(false);
    		}	
    	}
    }
    
    public void updateStationaryEffects(double fovMain){
    	double playerX = map.player.width, playerY = map.player.height, playerDir = map.player.dir, tempDir, fov = fovMain*DEGTORAD;
    	double[] distDir;
    	boolean passedPreCheck;
    	double effectWidth, effectHeight, midLine = 500*(MIDLOOK-vertLookPos);
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
    			effectHeight = 1000/distDir[0];
    			effectWidth = effect.getRatio()*effectHeight;
    			effect.setHeight(effectHeight);
    			effect.setWidth(effectWidth);
    			effect.setY(500-midLine-(effectHeight/2));
    			effect.setX(750-(750*(distDir[1]-tempDir)*2/fov)-(effectWidth/2));
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
    
    public void updateProjectileOnMiniMap(Projectile projectile) {
    	projectile.minimap.setCenterX(projectile.x*mapSize);
    	projectile.minimap.setCenterY(projectile.y*mapSize);
    	
    }
    
    public void createCastLine(double x, double y, int index, Paint color) {
    	Line castLine = (Line)castLinesList.get(index);
    	castLine.setStartX(map.player.width*mapSize);
    	castLine.setStartY(map.player.height*mapSize);
    	castLine.setEndX(x*mapSize);
    	castLine.setEndY(y*mapSize);
    	castLine.setStroke(color);
    }
    
    public void playerMoveUpdateGrid() {
    	playerSprite.setCenterX(map.player.width*mapSize);
    	playerSprite.setCenterY(map.player.height*mapSize);
    }
    
    public void createMapGrid() {
    	int width = map.map.length, height = map.map[0].length;
		Rectangle temp;
		for(int i, j = 0; j < height; j++) {
			for(i = 0; i < width; i++) {
				temp = new Rectangle(i*mapSize, j*mapSize, mapSize, mapSize);
				temp.setFill(makeMaze || map.map[i][j] != 0 ? Color.BLACK : Color.DARKSLATEGRAY);
				mapGridList.add(temp);
			}
		}
    }
    
    public void updateMapTile(int x, int y, int color){
    	((Shape)mapGridList.get(y*map.map.length+x)).setFill(Color.DARKSLATEGRAY);
    }
    
    public void createKeyActions() {
    	keyActions = new HashMap<>();
    	keyActions.put(KeyCode.W, () -> {map.player.move(true, map.map);});
    	keyActions.put(KeyCode.F, () -> {map.player.move(true, map.map);});
    	keyActions.put(KeyCode.A, () -> {map.player.strafe(true, map.map);});
    	keyActions.put(KeyCode.S, () -> {map.player.move(false, map.map);});
    	keyActions.put(KeyCode.D, () -> {map.player.strafe(false, map.map);});
    	keyActions.put(KeyCode.ENTER, () -> {drawCastLines = !drawCastLines; castLines.setVisible(drawCastLines);});
    	keyActions.put(KeyCode.SPACE, () -> {if(map.player.shotTimer >= 10) {createShootProjectile(new Projectile(map.player.width, map.player.height, map.player.dir, new Color(Math.random(), Math.random(), Math.random(), 1), mapSize, 0.1, map.map, 0)); map.player.shotTimer = 0;}});
    	keyActions.put(KeyCode.G, () -> {if(map.player.shotTimer >= 10) {createShootProjectile(new Projectile(map.player.width, map.player.height, map.player.dir, new Color(Math.random(), Math.random(), Math.random(), 1), mapSize, 0.1, map.map, 0)); map.player.shotTimer = 0;}});
    	keyActions.put(KeyCode.UP, () -> {vertLookPos = Math.min(PI, vertLookPos+0.1);});
    	keyActions.put(KeyCode.DOWN, () -> {vertLookPos = Math.max(0, vertLookPos-0.1);});
    	keyActions.put(KeyCode.LEFT, () -> {map.player.rotate(0.1);});
    	keyActions.put(KeyCode.RIGHT, () -> {map.player.rotate(-0.1);});
    }

    public void createShootProjectile(Projectile projectile) {
    	projectile.getDist(map.player.width, map.player.height);
    	projectileList.add(projectile);
    	entityMiniMapList.add(projectile.minimap);
    	displayList.add(projectile);
    		
    }
    
    public void mouseMove(MouseEvent mouseEvent) {
    	map.player.rotate((750-mouseEvent.getScreenX())/1500);
    	vertLookPos = Math.min(PI, Math.max(0, vertLookPos+(500-mouseEvent.getScreenY())/1500));
    	robot.mouseMove(750, 500);
    	//updateDisplay(fovMain);
//		Platform.runLater(() -> {
//			robot.mouseMove(750, 500);
//		});
    }
    
    public void updateDirLine() {
    	dirLine.setStartX(map.player.width*mapSize);
    	dirLine.setStartY(map.player.height*mapSize);
    	dirLine.setEndX(map.player.width*mapSize + Math.sin(map.player.dir) * mapSize);
    	dirLine.setEndY(map.player.height*mapSize + Math.cos(map.player.dir) * mapSize);
    }
    
    public void makeColorList() {
    	colorList.add(new double[] {0,0,0});//Black
    	colorList.add(new double[] {1,0,0});//Red
    	colorList.add(new double[] {0,1,0});//Green
    	colorList.add(new double[] {0,0,1});//Blue
    	colorList.add(new double[] {1,1,0});//Yellow
    	colorList.add(new double[] {1,0,1});//Magenta
    	colorList.add(new double[] {0,1,1});//Cyan
    	colorList.add(new double[] {1,1,1});//White
    	colorList.add(new double[] {0.25,0.5,1});//IDK
    	colorList.add(new double[] {0.33,0.33,0.33});//IDK
    }
    
    public void makeWallPatterns() {
    	File[] folders = new File("src/Sprites/Walls").listFiles();
    	int size = folders.length, pictureNum, sliceNum;
    	String pictureName, sliceName;
    	wallPatterns = new ImagePattern[size][16];
    	for(File folder : folders) {
    		pictureName = folder.getName();
    		pictureNum = Integer.valueOf(pictureName.substring(0, pictureName.indexOf('-')));
    		for(File picture : folder.listFiles()) {
    			sliceName = picture.getName();
        		sliceNum = Integer.valueOf(sliceName.substring(0, sliceName.indexOf('-')));
        		try {
        		wallPatterns[pictureNum][sliceNum] = new ImagePattern(new Image("Sprites/Walls/" + pictureName + "/" + sliceName));
        		}
        		catch(IllegalArgumentException e) {
        			System.out.println("Sprites/Walls/" + pictureName + "/" + sliceName);
        			//System.exit(0);;
        		}
        	}
    	}
    	
    }
    
    public void makeExplosion(double x, double y) {
    	StationaryEffect explosion = new StationaryEffect(x, y, mapSize, explosionAnimation);
    	sEffectList.add(explosion);
    	entityMiniMapList.add(explosion.minimap);
    	displayList.add(explosion);
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
    
    public void makeEnemyList() {
    	ImagePattern img = new ImagePattern(new Image("Sprites/Man.png"));
    	Enemy enemy;
    	if(makeMaze) {
    		enemy = new Enemy((gridSize-1.5), (gridSize-1.5), img, mapSize, false);
    		enemyList.add(enemy);
			displayList.add(enemy);
			entityMiniMapList.add(enemy.minimap);
    	}
    	else {
    		for(int i = 0; i < 0; i++) {
    			enemy = new Enemy((gridSize-4)*Math.random()+2, (gridSize-4)*Math.random()+2, img, mapSize, true);
    			enemyList.add(enemy);
    			displayList.add(enemy);
    			entityMiniMapList.add(enemy.minimap);
    		}
    	}
    }
    
    public void stop() {
		running.set(false);
		semaphore.release(100);
	}

}