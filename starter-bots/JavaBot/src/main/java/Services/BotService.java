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

            // Menentukan Heading Untuk Next Tick
            getHeading(mySize, enemySize, enemyDistance, playerList, foodList, gasCloudList);
            
            // Menentuken PlayerAction untuk next tick
            getNextAction(gasCloudList, playerList, torpedoList, asteroidFieldList);
            
        }
        // System.out.println(outAction);
        // System.out.println(outStatus);
        
        this.playerAction = playerAction;
        System.out.println("Your Action: ");
        System.out.println(playerAction.getAction());
        System.out.println("");
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

    private void getHeading(int mySize, int enemySize, double enemyDistance, List<GameObject> playerList, List<GameObject> foodList, List<GameObject> gasCloudList ) {
        // Conditional Penentu Heading
        if (enemyDistance <= 200 && enemySize > mySize) {
            // conditional kalau ada enemy yang deket dan lebih besar
            playerAction.heading = getHeadingAvoid(playerList.get(1));
            // outStatus = "lari dari lawan";
        } else if ((mySize > enemySize && enemyDistance <= 6*mySize)) {
            playerAction.heading = getHeadingBetween(playerList.get(1));
            if((enemyDistance<=2*mySize && enemyDistance>=0.5*mySize)){
                bot.isUsingAfterBurner = true;
            }
        } else {
            if(foodList.size()!=0){
                // makan
                playerAction.heading = getHeadingBetween(foodList.get(0));
                System.out.println("Eat food");
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
                System.out.println("Avoid Gas Cloud");
        }

        if (getDistanceFromBorder(bot) <= 1*mySize) {
            // ketika terlalu mepet ujung
            playerAction.heading = getHeadingFrom2(playerAction.heading, getHeadingAvoidBorder());
            System.out.println("Avoid Border");
            if (getDistanceFromBorder(bot) <= 0.5*mySize) {
                playerAction.heading = getHeadingAvoidBorder();
                System.out.println("Avoid Border with critical distance");
            }
        }
    }

    private void getNextAction(List<GameObject> gasCloudList, List<GameObject> playerList, List<GameObject> torpedoList, List<GameObject> asteroidFieldList){
        // Conditional untuk tick selanjutnya
        if (bot.isUsingAfterBurner) {
            // Cek apakah bot sedang menggunakan afterburner
            playerAction.action = PlayerActions.STARTAFTERBURNER;
            System.out.println("Start AfterBurner");
        } else if(getPlayerAction().getAction() == PlayerActions.STARTAFTERBURNER){
            // Cek apakah bot sudah harus mematikan afterburner
            playerAction.action = PlayerActions.STOPAFTERBURNER;
            System.out.println("Stop AfterBurner");
        } else {
            if(isTorpedoGoingToHitMe(torpedoList) && bot.getSize()>=25){
                // Cek apakah harus menggunakan shield
                playerAction.action = PlayerActions.USESHIELD;
                System.out.println("Using Shield");
            } else if(bot.getTorpedoSalvoCount() != 0 && bot.getSize() > 50  && isWayClear(Stream.concat(gasCloudList.stream(), asteroidFieldList.stream()).toList(), playerList.get(1))  ){
                // Cek apakah harus menembakkan torpedoes
                playerAction.action = PlayerActions.FIRETORPEDOES;
                playerAction.heading = getHeadingBetween(playerList.get(1));
                System.out.println("Firing Torpedoes"); 
            }else {
                // Cek apakah harus maju
                playerAction.action = PlayerActions.FORWARD;
                System.out.println("Going Forward");
            }   
        }
    }
    
    public double getDistanceBetween(GameObject object1, GameObject object2) {
        // Menemukan distance antara 2 object berdasaarkan koordinat object 
        var triangleX = Math.abs(object1.getPosition().x - object2.getPosition().x);
        var triangleY = Math.abs(object1.getPosition().y - object2.getPosition().y);
        return Math.sqrt(triangleX * triangleX + triangleY * triangleY);
    }

    public double getDistanceBetweenWithSize(GameObject object1, GameObject object2) {
        // Menemukan distance antara 2 object berdasaarkan koordinat object memperhitungkan size object
        return getDistanceBetween(object1, object2) - (object1.getSize() + object2.getSize());
    }

    public double getDistanceFromBorder(GameObject object) {
        // Menemukan distance object ke border
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, 0, null, null, position, null, null, null, null, null, null);
        return gameState.getWorld().getRadius() - (getDistanceBetween(object, center) + object.getSize());
    }

    public int getHeadingBetween(GameObject otherObject) {
        // Mendapatkan heading bot ke otherObject
        var direction = toDegrees(Math.atan2(otherObject.getPosition().y - bot.getPosition().y,
                otherObject.getPosition().x - bot.getPosition().x));
        return (direction + 360) % 360;
    }

    public int getHeadingAvoid(GameObject otherObject) {
        // Mendapatkan heading bot ke arah berlawanan otherObject
        return (getHeadingBetween(otherObject) + 180) % 360;
    }

    public int getHeadingAvoidBorder() {
        // Mendapatkan heading bot ke tengah map
        Position position = new Position(0, 0);
        GameObject center = new GameObject(null, null, null, null, position, null, null, null, null, null, null);
        return getHeadingBetween(center);
    }

    public int getHeadingFrom2(int heading1, int heading2) {
        // Mendapatkan resultan heading dari 2 heading diberikan
        int min = Math.min(heading1, heading2);
        int newHeading1 = (heading2 - heading1 + 360) % 360;
        int newHeading2 = (heading1 - heading2 + 360) % 360;
        int minHeading = Math.min(newHeading1, newHeading2);
        return (min + minHeading / 2 + 360) % 360;
    }

    public List<GameObject> getNearestObjectOnSight(int heading, List<GameObject> objectList) {
        // Mendapatkan list object yang berisi saringan list object yang ada di hadapan heading
        var objectCollide = objectList.stream()
                .filter(object -> isCollide(heading, object) == true)
                .sorted(Comparator.comparing(gas -> getDistanceBetweenWithSize(bot, gas)))
                .collect(Collectors.toList());
        return objectCollide;
    }

    public boolean isCollide(int heading, GameObject otherObject) {
        // Memprediksi lokasi bot di masa depan apakah akan collide dengan object atau tidak
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
        // Menentukan apakah suatu object berada di dalam gasCloud atau tidak
        boolean insideGas = false;
        int i = 0;
        while (i < gasCloudList.size() && !insideGas) {
            if (getDistanceBetweenWithSize(otherObject, gasCloudList.get(i)) <= 10) {
                insideGas = true;
            }
            i++;
        }
        return insideGas;
    }

    public boolean isWayClear(List<GameObject> objectList, GameObject target){
        // Menentukan apakah jalur didepan bot bersih dari object lain
        int headingPlayerToTarget = getHeadingBetween(target) % 180;
        for (GameObject object : objectList){
            // System.out.println("teest isway");
            // System.out.println(Math.abs(getHeadingBetween(object) - headingPlayerToTarget));
            // System.out.println(toDegrees(Math.atan2(object.getSize(), getDistanceBetween(bot, object))));
            if(Math.abs(getHeadingBetween(object) - headingPlayerToTarget) <= toDegrees(Math.atan2(object.getSize()/2, getDistanceBetween(bot, object)))){
                return false;
            }
        }
        return true;
    }

    private boolean isTorpedoGoingToHitMe(List<GameObject> torpedoList){
        // Menentukan apakah ada torpedo yang berada di dekat bot
        if(torpedoList.size()!=0){
            for (GameObject torpedo : torpedoList) {
                if(getDistanceBetweenWithSize(bot, torpedo) <= 30){
                    return true;
                }
            }
        }
        return false;
    }

    public int toDegrees(double v) {
        return (int) (v * (180 / Math.PI));
    }

}
