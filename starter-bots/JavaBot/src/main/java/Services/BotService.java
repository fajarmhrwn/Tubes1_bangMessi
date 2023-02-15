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

            int enemySize = playerList.get(1).getSize();
            double enemyDistance = getDistanceBetweenWithSize(bot, playerList.get(1));
            int mySize = bot.getSize();

            bot.isChasingFood = false;

            if (enemyDistance <= 200 && enemySize > mySize) {
                // conditional kalau ada enemy yang deket dan lebih besar
                playerAction.heading = getHeadingAvoid(playerList.get(1));
                bot.isChasingPlayer = false;
                System.out.println("lari dari lawan");
            } else if ((2 * mySize > 3 * enemySize && enemyDistance <= 200)
                    || (mySize > enemySize && playerList.size() == 2)) {
                if (bot.isChasingPlayer) {
                    playerAction.action = PlayerActions.STARTAFTERBURNER;
                    System.out.println("idupin afterburner");
                }
                bot.isChasingPlayer = true;
                playerAction.heading = getHeadingBetween(playerList.get(1));
                if (enemyDistance <= 10) {
                    bot.isChasingPlayer = false;
                }
                // kalau ada yg bisa dimakan
                // if( enemyDistance <= 100){
                // // playerAction.action = PlayerActions.STARTAFTERBURNER;
                // playerAction.action = PlayerActions.FIRETORPEDOES;
                // } else if( enemyDistance <= 5){
                // playerAction.action = PlayerActions.STOPAFTERBURNER;
                // } else {
                // if(bot.getTorpedoSalvoCount() >= 5){
                // playerAction.action = PlayerActions.FIRETORPEDOES;
                // }
                // }
            } else {
                // makan
                playerAction.heading = getHeadingBetween(foodList.get(0));
                bot.isChasingFood = true;
                bot.isChasingPlayer = false;
                System.out.println("makan");
            }

            var gasCloudOnSightList = getNearestObjectOnSight(playerAction.heading, gasCloudList);
            if (!(gasCloudOnSightList.size() == 0)
                    && getDistanceBetweenWithSize(bot, gasCloudOnSightList.get(0)) <= 50) {
                playerAction.heading = getHeadingFrom2(playerAction.heading, getHeadingAvoid(gasCloudOnSightList.get(0)));
                System.out.println("gascloudtolol");
            }

            if (getDistanceFromBorder(bot) <= 200) {
                // ketika terlalu mepet ujung
                playerAction.heading = getHeadingFrom2(playerAction.heading, getHeadingAvoidBorder());
                System.out.println("mepet border");
                if (getDistanceFromBorder(bot) <= 30) {
                    playerAction.heading = getHeadingAvoidBorder();
                    System.out.println("udah mepet banget dari border");
                }
            }

            if ((!bot.isChasingPlayer) && playerAction.action == PlayerActions.STARTAFTERBURNER) {
                playerAction.action = PlayerActions.STOPAFTERBURNER;
                System.out.println("matiin afterburner");
            } else if (playerAction.action != PlayerActions.STARTAFTERBURNER) {
                if(torpedoList.size() != 0 && getDistanceBetweenWithSize(bot, torpedoList.get(0))<=50 && bot.getShieldCount() != 0){
                    playerAction.action = PlayerActions.USESHIELD;
                    System.out.println("useshield");
                } else if(playerList.size()==2 && bot.getTorpedoSalvoCount() != 0 && bot.getSize() > 50){
                    playerAction.action = PlayerActions.FIRETORPEDOES;
                    System.out.println("TEMBPIKKKKKKKKKKKKKKKK");
                }else {
                    playerAction.action = PlayerActions.FORWARD;
                    System.out.println("maju");
                }
                
            }
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

}
