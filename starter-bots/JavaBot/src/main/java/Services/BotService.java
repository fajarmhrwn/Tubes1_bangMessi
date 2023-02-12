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
        playerAction.action = PlayerActions.FORWARD;
        playerAction.heading = new Random().nextInt(360);

        if (!gameState.getGameObjects().isEmpty()) {
            // Mencari makanan terdekat
            var foodList = gameState.getGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD || item.getGameObjectType() == ObjectTypes.SUPER_FOOD)
                    .sorted(Comparator
                            .comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            // Mencari gas cloud terdekat
            var gasCloudList = gameState.getGameObjects()
                    .stream().filter(gas -> gas.getGameObjectType() == ObjectTypes.GAS_CLOUD)
                    .sorted(Comparator
                            .comparing(gas -> getDistanceBetweenWithSize(bot, gas)))
                    .collect(Collectors.toList());
            // Mencari player terdekat
            var playerList = gameState.getPlayerGameObjects()
                    .stream().filter(player -> player.getGameObjectType() == ObjectTypes.PLAYER)
                    .sorted(Comparator
                        .comparing(player -> getDistanceBetweenWithSize(bot, player)))
                    .collect(Collectors.toList());

            int enemySize = playerList.get(1).getSize();
            int mySize = bot.getSize();

            if (getDistanceBetween(bot, playerList.get(1)) <= getSaveDistance(playerList.get(1)) && enemySize > mySize){
                // conditional kalau ada enemy yang deket dan lebih besar
                playerAction.heading = getHeadingAvoid(playerList.get(1));
            }
            else if(mySize > enemySize && bot.getSize() >= 200){
                // kalau ada yg bisa dimakan
                playerAction.action = PlayerActions.START_AFTERBURNER;
                playerAction.heading = getHeadingBetween(playerList.get(1));
            } else {
                // makan
                playerAction.heading = getHeadingBetween(foodList.get(0));
            }

            if (getDistanceFromBorder() <= 400 ){
                // ketika terlalu mepet ujung 
                playerAction.heading = getHeadingFrom2(playerAction.heading, getHeadingAvoidBorder());
            }
            if (getDistanceBetweenWithSize(bot, gasCloudList.get(0)) <= 50 && isGonnaCrash(gasCloudList.get(0), playerAction.heading)){
                playerAction.heading = getHeadingAvoidObject(gasCloudList.get(0),playerAction.heading);
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
        Optional<GameObject> optionalBot = gameState.getPlayerGameObjects().stream().filter(gameObject -> gameObject.id.equals(bot.id)).findAny();
        optionalBot.ifPresent(bot -> this.bot = bot);
    }

    private double getDistanceBetween(GameObject object1, GameObject object2) {
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    private double getDistanceBetweenWithSize(GameObject object1, GameObject object2) {
        return getDistanceBetween(object1, object2) - ((object1.getSize()+object2.getSize())/2);
    }

    public double getDistanceFromBorder(){
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, 0, null, null, position, null, null, null, null, null, null);
        return gameState.getWorld().getRadius() - getDistanceBetween(bot,center) + bot.getSize();
    }

    private int getHeadingBetween(GameObject otherObject) {
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    private int getHeadingAvoid(GameObject otherObject) {
        return (getHeadingBetween(otherObject) + 180) % 360;
    }

    private int getHeadingAvoidBorder(){
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, null, null, null, position, null, null, null, null, null, null);
        return getHeadingBetween(center);
    }

    private int getHeadingFrom2(int heading1, int heading2){
        int min = Math.min(heading1, heading2);
        return (min + (heading2-heading1)/2 + 360) % 360;
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

    private int getSaveDistance(GameObject otherPlayer){
        int speedMax = 2*otherPlayer.getSpeed() - bot.getSpeed();
        return 10*speedMax;
    }

    private boolean isGonnaCrash(GameObject otherObject, int heading){
        int theta = toDegrees(Math.atan2((bot.getSize() + otherObject.getSize())/2, getDistanceBetween(bot, otherObject)));
        return Math.abs(heading-getHeadingBetween(otherObject)) <= theta;
    }

    private int getHeadingAvoidObject(GameObject otherObject, int heading){
        int theta = toDegrees(Math.atan2(otherObject.getSize()/2, getDistanceBetween(bot, otherObject)));
        int tilt = theta - Math.abs(heading-getHeadingBetween(otherObject));
        if (heading >= getHeadingBetween(otherObject)){
            return heading+tilt;
        } else {
            return heading-tilt;
        }
    }

}
