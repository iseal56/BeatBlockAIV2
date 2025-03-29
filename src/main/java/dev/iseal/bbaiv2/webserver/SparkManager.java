package dev.iseal.bbaiv2.webserver;

import dev.iseal.bbaiv2.misc.utils.ExceptionHandler;
import spark.Spark;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static spark.Spark.post;

public class SparkManager {

    //singleton stuff
    private static final SparkManager instance = new SparkManager();
    public static SparkManager getInstance() {
        return instance;
    }
    private SparkManager() {}

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition barelyUpdated = lock.newCondition();


    private int oldMisses = 0;
    private int oldBarelies = 0;

    private int maxHits = 0;
    private int misses = 0;
    private int barelies = 0;

    private int updateCounter = 0;

    public static boolean inGame = false;

    public void init() {
        // getting data
        post("/api/v1/postMaxHits", (req, res) -> {
            System.out.println(req.body());
            maxHits = Integer.parseInt(req.body());
            res.type("application/json");
            return "{\"status\":\"ok\"}";
        });
        post("/api/v1/postMisses", (req, res) -> {
            System.out.println(req.body());
            oldMisses = misses;
            misses = Integer.parseInt(req.body());
            res.type("application/json");
            return "{\"status\":\"ok\"}";
        });
        post("/api/v1/postBarelies", (req, res) -> {
            System.out.println(req.body());
            oldBarelies = barelies;
            postBarelies(req.body());
            res.type("application/json");
            return "{\"status\":\"ok\"}";
        });

        // game start and end
        post("api/v1/end", (req, res) -> {
            inGame = false;
            res.type("application/json");
            return "{\"status\":\"ok\"}";
        });
        post("api/v1/start", (req, res) -> {
            inGame = true;
            res.type("application/json");
            return "{\"status\":\"ok\"}";
        });
    }

    public float calculateGrade() {
        return (float) (((maxHits - misses -(barelies*0.25)) / maxHits) * 100);
    }

    public float calculateReward() {
        return (float) - ((misses - oldMisses) + ((barelies - oldBarelies) * 0.25));
    }

    public static boolean isInGame() {
        return inGame;
    }

    public void postBarelies(String newBarelies) {
        lock.lock();
        try {
            barelies = Integer.parseInt(newBarelies);
            updateCounter++;
            barelyUpdated.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait for new barelies
     * Because of the way the mod works, you can assume we got new errors as well
     */
    public void waitForNewBarelies() {
        lock.lock();
        try {
            int localCounter = updateCounter;
            while (updateCounter == localCounter) {
                if (!barelyUpdated.await(5, TimeUnit.SECONDS)) {
                    // took to long - it should take the time of a frame.
                    // if it takes longer, we have a problem

                    // assume game ended, and return.
                    inGame = false;
                    return;
                }
            }
        } catch (InterruptedException e) {
            ExceptionHandler.panic(e);
        } finally {
            lock.unlock();
        }
    }
}
