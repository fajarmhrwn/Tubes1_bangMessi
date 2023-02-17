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
            
            int mySize = bot.getSize();
            System.out.println("Your size: ");
            System.out.println(mySize);

            // Mencari item di map
            ObjectList objectList = new ObjectList(this);
            
            var gasCloudList = objectList.getGasCloudList();
            var foodList = objectList.getFoodList();
            var playerList = objectList.getPlayerList();
            var torpedoList = objectList.getTorpedoList();
            var asteroidFieldList = objectList.getAsteroidFieldList();
            

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
                if(foodList.size()!=0){
                    // makan
                    playerAction.heading = getHeadingBetween(foodList.get(0));
                    outStatus = "makan";
                } else {
                    playerAction.heading = getHeadingAvoid(playerList.get(1));
                }
                
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
                if(torpedoList.size() != 0 && isTorpedoGoingToHitMe(torpedoList) && bot.getShieldCount() != 0){
                    playerAction.action = PlayerActions.USESHIELD;
                    outAction = "useshield";
                } else if(bot.getTorpedoSalvoCount() != 0 && bot.getSize() > 50  && isWayClear(Stream.concat(gasCloudList.stream(), asteroidFieldList.stream()).toList(), playerList.get(1))  ){
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

    public double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    public double getDistanceBetweenWithSize(GameObject object1, GameObject object2) {
        return getDistanceBetween(object1, object2) - (object1.getSize() + object2.getSize());
    }

    public double getDistanceFromBorder(GameObject object) {
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, 0, null, null, position, null, null, null, null, null, null);
        return gameState.getWorld().getRadius() - (getDistanceBetween(object, center) + object.getSize());
    }

    public int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    public int getHeadingAvoid(GameObject otherObject) {
        return (getHeadingBetween(otherObject) + 180) % 360;
    }

    public int getHeadingAvoidBorder() {
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, null, null, null, position, null, null, null, null, null, null);
        return getHeadingBetween(center);
    }

    public int getHeadingFrom2(int heading1, int heading2) {
        int min = Math.min(heading1, heading2);
        int newHeading1 = (heading2 - heading1 + 360) % 360;
        int newHeading2 = (heading1 - heading2 + 360) % 360;
        int minHeading = Math.min(newHeading1, newHeading2);
        return (min + minHeading / 2 + 360) % 360;
    }

    public int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

    public List<GameObject> getNearestObjectOnSight(int heading, List<GameObject> objectList) {
        var objectCollide = objectList.stream()
                .filter(object -> isCollide(heading, object) == true)
                .sorted(Comparator.comparing(gas -> getDistanceBetweenWithSize(bot, gas)))
                .collect(Collectors.toList());
        return objectCollide;
    }

    public boolean isCollide(int heading, GameObject otherObject) {
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

    public boolean isInsideGas(GameObject otherObject, List<GameObject> gasCloudList) {
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

    private boolean isTorpedoGoingToHitMe(List<GameObject> torpedoList){
        for (GameObject torpedo : torpedoList){
            if (getDistanceBetweenWithSize(bot, torpedo) <= 5){
                return true;
            }
        }
        return false;
    }

}
