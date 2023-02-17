package Services;

import Enums.*;
import Models.*;

import java.util.*;
import java.util.stream.*;
import java.lang.Math;

public class BotService {
    private GameObject bot;
    private PlayerAction playerAction;
    private GameState gameState;

    public BotService() {
        this.playerAction = new PlayerAction();
        this.gameState = new GameState();
    }

    public GameObject getBot() {
        return this.bot;
    }

    public void setBot(GameObject bot) {
        this.bot = bot;
    }

    public PlayerAction getPlayerAction() {
        return this.playerAction;
    }

    public void setPlayerAction(PlayerAction playerAction) {
        this.playerAction = playerAction;
    }

    public void computeNextPlayerAction(PlayerAction playerAction) {
        String outStatus = "no change heading";
        String outAction = "no change action";
        if (!gameState.getGameObjects().isEmpty()) {            
            // Mencari gas cloud terdekat
            int mySize = bot.getSize();
            System.out.println("Your size: ");
            System.out.println(mySize);
            var gasCloudList = gameState.getGameObjects()
                    .stream().filter(gas -> gas.getGameObjectType() == ObjectTypes.GAS_CLOUD)
                    .sorted(Comparator
                            .comparing(gas -> getDistanceBetweenWithSize(bot, gas)))
                    .collect(Collectors.toList());
            // Mencari makanan terdekat
            var foodList = gameState.getGameObjects()
                    .stream()
                    .filter(item -> (getDistanceFromBorder(item) >= mySize+10 && isInsideGas(item, gasCloudList) == false
                            && (item.getGameObjectType() == ObjectTypes.FOOD || item.getGameObjectType() == ObjectTypes.SUPER_FOOD)))
                    .sorted(Comparator
                            .comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            // Mencari player terdekat
            var playerList = gameState.getPlayerGameObjects()
                    .stream().filter(player -> player.getGameObjectType() == ObjectTypes.PLAYER)
                    .sorted(Comparator
                            .comparing(player -> getDistanceBetweenWithSize(bot, player)))
                    .collect(Collectors.toList());
            // List torpedo yang ditembakkan 
            var torpedoList = gameState.getPlayerGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.TORPEDO_SALVO)
                    .sorted(Comparator
                            .comparing(item -> getDistanceBetweenWithSize(bot, item)))
                    .collect(Collectors.toList());
            // list asteroid
            var asteroidFieldList = gameState.getGameObjects()
                    .stream().filter(asteroid -> asteroid.getGameObjectType() == ObjectTypes.ASTEROID_FIELD)
                    .sorted(Comparator
                            .comparing(asteroid -> getDistanceBetweenWithSize(bot, asteroid)))
                    .collect(Collectors.toList());

            // Data Player Terdekat
            int enemySize = playerList.get(1).getSize();
            double enemyDistance = getDistanceBetweenWithSize(bot, playerList.get(1));

            // Conditional Penentu Heading
            if (enemyDistance <= 200 && enemySize > mySize) {
                // conditional kalau ada enemy yang deket dan lebih besar
                playerAction.heading = getHeadingAvoid(playerList.get(1));
                outStatus = "lari dari lawan";
            } else if ((mySize > enemySize && enemyDistance <= 6*mySize)) {
                playerAction.heading = getHeadingBetween(playerList.get(1));
                if((enemyDistance<=2*mySize && enemyDistance>=0.5*mySize)){
                    bot.isUsingAfterBurner = true;
                }
            } else {
                // makan
                playerAction.heading = getHeadingBetween(foodList.get(0));
                outStatus = "makan";
            }

            var gasCloudOnSightList = getNearestObjectOnSight(playerAction.heading, gasCloudList);
            if (!(gasCloudOnSightList.size() == 0)){
                if (getDistanceBetweenWithSize(bot, gasCloudOnSightList.get(0)) <= 50) {
                    playerAction.heading = getHeadingFrom2(playerAction.heading, getHeadingAvoid(gasCloudOnSightList.get(0)));
                } else if (getDistanceBetweenWithSize(bot, gasCloudOnSightList.get(0)) <= 10){
                    playerAction.heading = getHeadingAvoid(gasCloudOnSightList.get(0));
                }
                    outStatus = "gascloudtolol";
            }

            if (getDistanceFromBorder(bot) <= 1*mySize) {
                // ketika terlalu mepet ujung
                playerAction.heading = getHeadingFrom2(playerAction.heading, getHeadingAvoidBorder());
                outStatus = "mepet border";
                if (getDistanceFromBorder(bot) <= 0.5*mySize) {
                    playerAction.heading = getHeadingAvoidBorder();
                    outStatus = "udah mepet banget dari border";
                }
            }

            // Conditional untuk tick selanjutnya
            if (bot.isUsingAfterBurner) {
                playerAction.action = PlayerActions.STARTAFTERBURNER;
                outAction = "idupin afterburner";
            } else if(getPlayerAction().getAction() == PlayerActions.STARTAFTERBURNER){
                playerAction.action = PlayerActions.STOPAFTERBURNER;
                outAction = "matiin afterburner";
            } else {
                if(torpedoList.size() != 0 && getDistanceBetweenWithSize(bot, torpedoList.get(0))<=50 && bot.getShieldCount() != 0){
                    playerAction.action = PlayerActions.USESHIELD;
                    outAction = "useshield";
                } else if(playerList.size()!=0 && bot.getTorpedoSalvoCount() != 0 && bot.getSize() > 50  && isWayClear(Stream.concat(gasCloudList.stream(), asteroidFieldList.stream()).toList(), playerList.get(1))  ){
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                    playerAction.heading = getHeadingBetween(playerList.get(1));
                    outAction = "TEMBPIKKKKKKKKKKKKKKKK";
                    
                }else {
                    playerAction.action = PlayerActions.FORWARD;
                    outAction = "maju";
                }
                
            }
            
        }
        System.out.println(outAction);
        System.out.println(outStatus);
        
        this.playerAction = playerAction;
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        updateSelfState();
    }

    private void updateSelfState() {
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream()
                .filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    private double getDistanceBetweenWithSize(GameObject object1, GameObject object2) {
        return getDistanceBetween(object1, object2) - (object1.getSize() + object2.getSize());
    }

    public double getDistanceFromBorder(GameObject object) {
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, 0, null, null, position, null, null, null, null, null, null);
        return gameState.getWorld().getRadius() - (getDistanceBetween(object, center) + object.getSize());
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int getHeadingAvoid(GameObject otherObject) {
        return (getHeadingBetween(otherObject) + 180) % 360;
    }

    private int getHeadingAvoidBorder() {
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, null, null, null, position, null, null, null, null, null, null);
        return getHeadingBetween(center);
    }

    private int getHeadingFrom2(int heading1, int heading2) {
        int min = Math.min(heading1, heading2);
        int newHeading1 = (heading2 - heading1 + 360) % 360;
        int newHeading2 = (heading1 - heading2 + 360) % 360;
        int minHeading = Math.min(newHeading1, newHeading2);
        return (min + minHeading / 2 + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

    // private int getHeadingAvoidObject(GameObject otherObject, int heading) {
    //     int tilt = 60 - Math.abs(heading - getHeadingBetween(otherObject));
    //     if (heading >= getHeadingBetween(otherObject)) {
    //         return heading + tilt;
    //     } else {
    //         return heading - tilt;
    //     }
    // }

    private List<GameObject> getNearestObjectOnSight(int heading, List<GameObject> objectList) {
        var objectCollide = objectList.stream()
                .filter(object -> isCollide(heading, object) == true)
                .sorted(Comparator.comparing(gas -> getDistanceBetweenWithSize(bot, gas)))
                .collect(Collectors.toList());
        return objectCollide;
    }

    private boolean isCollide(int heading, GameObject otherObject) {
        // Memprediksi lokasi bot di masa depan apakah akan collide dengan object atau
        // tidak
        int headingToObject = getHeadingBetween(otherObject);
        double distance = getDistanceBetween(bot, otherObject);
        if (heading != headingToObject && Math.abs(heading - headingToObject) < 90) {
            if (heading > headingToObject) {
                headingToObject = (heading - headingToObject + 360) % 360;
            } else {
                headingToObject = (headingToObject - heading + 360) % 360;
            }
            double heading1 = Math.toRadians(headingToObject);
            double heading2 = Math.toRadians(heading);
            Position position = new Position(
                    bot.getPosition().x + (int) (((distance / Math.cos(heading1))) * Math.cos(heading2)),
                    bot.getPosition().y + (int) (((distance / Math.cos(heading1))) * Math.sin(heading2)));
            GameObject futureBot = new GameObject(null, bot.getSize(), null, null, position, null, null, null, null,
                    null, null);
            return getDistanceBetween(futureBot, otherObject) < futureBot.getSize() + otherObject.getSize();
        }
        return true;
    }

    private boolean isInsideGas(GameObject otherObject, List<GameObject> gasCloudList) {
        boolean insideGas = false;
        int i = 0;
        while (i < gasCloudList.size() && !insideGas) {
            if (getDistanceBetweenWithSize(otherObject, gasCloudList.get(i)) <= 5) {
                insideGas = true;
            }
            i++;
        }
        return insideGas;
    }

    // private void printList(List<GameObject> listObject){
    //     listObject.stream().forEach(item -> System.out.println(item.getId()));
    // }

    public boolean isWayClear(List<GameObject> objectList, GameObject target){
        int headingPlayerToTarget = getHeadingBetween(target) % 180;
        for (GameObject object : objectList){
            System.out.println("teest isway");
            System.out.println(Math.abs(getHeadingBetween(object) - headingPlayerToTarget));
            System.out.println(toDegrees(Math.atan2(object.getSize(), getDistanceBetween(bot, object))));
            if(Math.abs(getHeadingBetween(object) - headingPlayerToTarget) <= toDegrees(Math.atan2(object.getSize()/2, getDistanceBetween(bot, object)))){
                return false;
            }
        }
        return true;
    }

}
