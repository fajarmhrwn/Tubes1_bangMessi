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
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.FOOD)
                    .sorted(Comparator
                            .comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            // Mencari player terdekat
            var playerList = gameState.getPlayerGameObjects()
                    .stream().filter(item -> item.getGameObjectType() == ObjectTypes.PLAYER)
                    .sorted(Comparator
                        .comparing(item -> getDistanceBetween(bot, item)))
                    .collect(Collectors.toList());
            int enemySize = playerList.get(1).getSize();
            int mySize = bot.getSize();

            if (getDistanceBetween(bot, playerList.get(1)) <= 2 && enemySize > mySize){
                // conditional kalau ada enemy yang deket dan lebih besar
                playerAction.heading = (getHeadingBetween(playerList.get(1)) + 180) % 360;
            }
            else if (getDistanceFromBorder() <= 2){
                // ketika terlalu mepet ujung 
                playerAction.heading = getHeadingAvoidBorder();
            }
            else if(mySize > enemySize && bot.getSize() >= 30){
                // kalau ada yg bisa dimakan
                playerAction.heading = getHeadingBetween(playerList.get(1));
            } else {
                // makan
                playerAction.heading = getHeadingBetween(foodList.get(0));
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
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY) - (object1.getSize()+object2.getSize())/2;
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

    private int getHeadingAvoidBorder(){
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, null, null, null, position, null, null, null, null, null, null);
        return getHeadingBetween(center);
    }

    private int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

}
