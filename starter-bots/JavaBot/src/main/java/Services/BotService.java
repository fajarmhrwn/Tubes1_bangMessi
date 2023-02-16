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
        System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        System.out.println("Bot Size = " + bot.getSize());
        System.out.println("Torpedo Count = " + bot.getTorpedoSalvoCount());
        System.out.println("Speed = " + bot.getSpeed());
        System.out.println("Heading = " + playerAction.getHeading());
        System.out.println("Shields = " + bot.getShieldCount());
        System.out.println("Supernova = " + bot.getSupernovaAvailable());
        System.out.println("Teleporter = " + bot.getTeleporterCount());
        System.out.println("Afterbuner Status = " + bot.isUsingAfterBurner);
        


        String outStatus = "no change heading";
        String outAction = "no change action";
        if (!gameState.getGameObjects().isEmpty()) {
            

            // Mencari gas cloud terdekat
            
            var gasCloudList = gameState.getGameObjects()
                    .stream().filter(gas -> gas.getGameObjectType() == ObjectTypes.GAS_CLOUD)
                    .sorted(Comparator
                            .comparing(gas -> getDistanceBetweenWithSize(bot, gas)))
                    .collect(Collectors.toList());
            // Mencari makanan terdekat
            var foodList = gameState.getGameObjects()
                    .stream()
                    .filter(item -> (getDistanceFromBorder(item) >= 50 && isInsideGas(item, gasCloudList) == false
                            && (item.getGameObjectType() == ObjectTypes.FOOD || item.getGameObjectType() == ObjectTypes.SUPER_FOOD)))
                    .sorted(Comparator
                            .comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            // Mencari player terdekat
            var playerList = gameState.getPlayerGameObjects()
                    .stream().filter(player -> player.getGameObjectType() == ObjectTypes.valueOf(1))
                    .sorted(Comparator
                            .comparing(player -> getDistanceBetweenWithSize(bot, player)))
                    .collect(Collectors.toList());
            // List torpedo yang ditembakkan 
            // printList(playerList);
            var torpedoList = gameState.getPlayerGameObjects()
                    .stream().filter(torpedo -> torpedo.getGameObjectType() == ObjectTypes.valueOf(6))
                    .sorted(Comparator
                            .comparing(torpedo -> getDistanceBetweenWithSize(bot, torpedo)))
                    .collect(Collectors.toList());

            // list asteroid
            var asteroidFieldList = gameState.getGameObjects()
                    .stream().filter(asteroid -> asteroid.getGameObjectType() == ObjectTypes.ASTEROID_FIELD)
                    .sorted(Comparator
                            .comparing(asteroid -> getDistanceBetweenWithSize(bot, asteroid)))
                    .collect(Collectors.toList());

            var probtorpedoList = gameState.getGameObjects().stream()
                .filter(torpedo -> torpedo.getGameObjectType() != ObjectTypes.GAS_CLOUD && torpedo.getGameObjectType() != ObjectTypes.FOOD && torpedo.getGameObjectType() != ObjectTypes.SUPER_FOOD && torpedo.getGameObjectType() != ObjectTypes.PLAYER && torpedo.getGameObjectType() != ObjectTypes.ASTEROID_FIELD).sorted(Comparator.comparing(torpedo -> getDistanceBetweenWithSize(bot,torpedo))).collect(Collectors.toList());


            var teleporterList = gameState.getPlayerGameObjects()
            .stream().filter(teleporter -> teleporter.getGameObjectType() == ObjectTypes.valueOf(10))
            .sorted(Comparator
                    .comparing(teleporter -> getDistanceBetweenWithSize(bot, teleporter)))
            .collect(Collectors.toList());

            
            // System.out.println("Torpedo di map :");
            System.out.println("List torpedo");
            // printList2(torpedoList);
            printList2(probtorpedoList);
            int enemySize = playerList.get(1).getSize();
            // System.out.println(enemySize);
            double enemyDistance = getDistanceBetweenWithSize(bot, playerList.get(1));
            int mySize = bot.getSize();

            bot.isChasingFood = false;

            if (enemyDistance <= 200 && enemySize > mySize) {
                // conditional kalau ada enemy yang deket dan lebih besar
                playerAction.heading = getHeadingAvoid(playerList.get(1));
                bot.isChasingPlayer = false;
                outStatus = "lari dari lawan";
            } else if ((2 * mySize > 3 * enemySize && enemyDistance <= 400)
                    || (mySize > enemySize && playerList.size() == 2)) {
                if (bot.isChasingPlayer) {
                    playerAction.action = PlayerActions.STARTAFTERBURNER;
                    outAction = "idupin afterburner";
                }
                bot.isChasingPlayer = true;
                playerAction.heading = getHeadingBetween(playerList.get(1));
                if (enemyDistance <= 10) {
                    bot.isChasingPlayer = false;
                }
            } else {
                // makan
                playerAction.heading = getHeadingBetween(foodList.get(0));
                bot.isChasingFood = true;
                bot.isChasingPlayer = false;
                outStatus = "makan";
            }

            var gasCloudOnSightList = getNearestObjectOnSight(playerAction.heading, gasCloudList);
            if (!(gasCloudOnSightList.size() == 0)
                    && getDistanceBetweenWithSize(bot, gasCloudOnSightList.get(0)) <= 50) {
                playerAction.heading = getHeadingFrom2(playerAction.heading, getHeadingAvoid(gasCloudOnSightList.get(0)));
                outStatus = "gascloudtolol";
            }

            if (getDistanceFromBorder(bot) <= 200) {
                // ketika terlalu mepet ujung
                playerAction.heading = getHeadingFrom2(playerAction.heading, getHeadingAvoidBorder());
                outStatus = "mepet border";
                if (getDistanceFromBorder(bot) <= 30) {
                    playerAction.heading = getHeadingAvoidBorder();
                    outStatus = "udah mepet banget dari border";
                }
            }

            if ((!bot.isChasingPlayer) && bot.isUsingAfterBurner == true) {
                playerAction.action = PlayerActions.STOPAFTERBURNER;
                outAction = "matiin afterburner";
            } else if ( bot.isUsingAfterBurner == false) {
                
                if(torpedoList.size() != 0 && getDistanceBetweenWithSize(bot, torpedoList.get(0))<=50 && bot.getShieldCount() != 0){
                    playerAction.action = PlayerActions.ACTIVATESHIELD;
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



            if (isTorpedoGoingToHitMe(torpedoList) ) {
                if(bot.getShieldCount() != 0  && bot.getSize() >= 30){
                    playerAction.action = PlayerActions.ACTIVATESHIELD;
                    outAction = "useshield";
                } else if(bot.getTorpedoSalvoCount() != 0 && bot.getSize() > 25  ){
                    playerAction.heading = getHeadingBetween(torpedoList.get(0));
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                    outStatus = "Torpedo musuh coba ditembak";
                }
                else{
                    playerAction.heading = getHeadingAvoid(torpedoList.get(0)) + 50;
                    playerAction.action = PlayerActions.STARTAFTERBURNER;
                    outStatus = "Torpedo musuh coba dihindari dengan afterburner, pindah haluan 50 derajat";

                }
                
            }

            System.out.println(getDistanceFromBorder(bot) - gameState.getWorld().getRadius() * 0.4);
            Position cenPosition = new Position(0,0);

            if((teleporterList.size() != 0)){
                for (int i = 0; i < teleporterList.size(); i++) {
                    if(teleporterList.get(i).getPosition().getX() == cenPosition.getX() && teleporterList.get(i).getPosition().getY() == cenPosition.getY()){
                        playerAction.action = PlayerActions.TELEPORT;
                        bot.teleporterLaunched = false;
                        outAction = "Teleport";
                    }
                }
            }


            else if( bot.teleporterLaunched == false && getDistanceFromBorder(bot) <= gameState.getWorld().getRadius() * 0.4 && playerList.size() <= 3 && bot.getSize() >= 40 && bot.getTeleporterCount() != 0){
                playerAction.heading = getHeadingAvoidBorder();
                playerAction.action = PlayerActions.FIRETELEPORTER;
                bot.teleporterLaunched = true;
                outAction = "Teleport fired";
            }


            





            
        }
        System.out.println(outAction);
        System.out.println(outStatus);
        if(playerAction.action == PlayerActions.STARTAFTERBURNER){
            bot.isUsingAfterBurner = true;
        }
        else if(playerAction.action == PlayerActions.STOPAFTERBURNER){
            bot.isUsingAfterBurner = false;
        }
        
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
            if (getDistanceBetweenWithSize(otherObject, gasCloudList.get(i)) <= 0) {
                insideGas = true;
            }
            i++;
        }
        return insideGas;
    }

    // private void printList(List<GameObject> listObject){
    //     listObject.stream().forEach(item -> System.out.println(item.getId()));
    // }

    private void printList2(List<GameObject> listObject){
        for (GameObject g : listObject)  {
            System.out.println(g+" "+g.getGameObjectType().value);  
  
        }  
     }

    public boolean isWayClear(List<GameObject> objectList, GameObject target){
        int headingPlayerToTarget = getHeadingBetween(target) % 180;
        for (GameObject object : objectList){
            // System.out.println("test angle isWay");
            // System.out.println(Math.abs(getHeadingBetween(object) - headingPlayerToTarget));
            // System.out.println(toDegrees(Math.atan2(object.getSize(), getDistanceBetween(bot, object))));
            if(Math.abs(getHeadingBetween(object) - headingPlayerToTarget) <= toDegrees(Math.atan2(object.getSize()/2, getDistanceBetween(bot, object)))){
                return false;
            }
        }
        return true;
    }

    public boolean isTorpedoGoingToHitMe(List<GameObject> torpedoList){
        for (GameObject torpedo : torpedoList){
            if (getDistanceBetweenWithSize(bot, torpedo) <= 5){
                return true;
            }
        }
        return false;
    }

}
