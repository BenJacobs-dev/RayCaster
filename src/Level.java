import java.util.HashMap;

public class Level {
    private HashMap<String, Map> maps;
    private String currentMap;
    private Player player;

    public Level(Player playerIn){
        maps = new HashMap<String, Map>();
        player = playerIn;
        currentMap = null;
    }

    public Level(Player playerIn, Map map){
        maps = new HashMap<String, Map>();
        player = playerIn;
        currentMap = map.name;
        maps.put(map.name, map);
    }

    public void loadLevels(){

    }

    public static Level makeMaze(int size, int wallPatternCount, Player playerIn){
        return new Level(playerIn, Map.makeMaze(size, wallPatternCount, playerIn));
    }

    public static Level makeCircle(int size, int wallPatternCount, Player playerIn, boolean inverted) {
        return new Level(playerIn, Map.makeCircle(size, wallPatternCount, playerIn, inverted));
    }

    public Map getCurMap(){
        return maps.get(currentMap);
    }

    public Player getPlayer(){
        return player;
    }

    public void changeMap(String mapName){
        if(!maps.containsKey(mapName)){
            System.out.println("MAP: " + mapName + " IS NOT PART OF THIS LEVEL!");
            return;
        }
        currentMap = mapName;
        Map map = maps.get(mapName);
        player.mapWidth = map.map.length;
        player.mapHeight = map.map[0].length;
    }

}
