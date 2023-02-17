package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;

public class ObjectList {
    private List<GameObject> gasCloudList;
    private List<GameObject> foodList;
    private List<GameObject> playerList;
    private List<GameObject> torpedoList;
    private List<GameObject> asteroidFieldList;

    public ObjectList(BotService botService){
        // Mencari gas cloud terdekat
            this.gasCloudList = botService.getGameState().getGameObjects()
                    .stream().filter(gas -> gas.getGameObjectType() == ObjectTypes.GAS_CLOUD)
                    .sorted(Comparator
                            .comparing(gas -> botService.getDistanceBetweenWithSize(botService.getBot(), gas)))
                    .collect(Collectors.toList());
            // Mencari makanan terdekat
            this.foodList = botService.getGameState().getGameObjects()
                    .stream()
                    .filter(item -> (botService.getDistanceFromBorder(item) >= botService.getBot().getSize()+10 && botService.isInsideGas(item, gasCloudList) == false
                            && (item.getGameObjectType() == ObjectTypes.FOOD || item.getGameObjectType() == ObjectTypes.SUPER_FOOD)))
                    .sorted(Comparator
                            .comparing(item -> botService.getDistanceBetween(botService.getBot(), item)))
                    .collect(Collectors.toList());
            // Mencari player terdekat
            this.playerList = botService.getGameState().getPlayerGameObjects()
                    .stream().filter(player -> player.getGameObjectType() == ObjectTypes.PLAYER)
                    .sorted(Comparator
                            .comparing(player -> botService.getDistanceBetweenWithSize(botService.getBot(), player)))
                    .collect(Collectors.toList());
            // List torpedo yang ditembakkan 
            this.torpedoList = botService.getGameState().getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TORPEDOSALVO)
                    .sorted(Comparator
                            .comparing(item -> botService.getDistanceBetweenWithSize(botService.getBot(), item)))
                    .collect(Collectors.toList());
            // list asteroid
            this.asteroidFieldList = botService.getGameState().getGameObjects()
                    .stream().filter(asteroid -> asteroid.getGameObjectType() == ObjectTypes.ASTEROID_FIELD)
                    .sorted(Comparator
                            .comparing(asteroid -> botService.getDistanceBetweenWithSize(botService.getBot(), asteroid)))
                    .collect(Collectors.toList());
    }

    // Getter
    public List<GameObject> getGasCloudList(){
        return this.gasCloudList;
    }
    public List<GameObject> getFoodList(){
        return this.foodList;
    }
    public List<GameObject> getPlayerList(){
        return this.playerList;
    }
    public List<GameObject> getTorpedoList(){
        return this.torpedoList;
    }
    public List<GameObject> getAsteroidFieldList(){
        return this.asteroidFieldList;
    }
}
