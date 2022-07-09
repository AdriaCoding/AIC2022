package Murdak_v5;

import aic2022.user.*;

public class Movement {

    UnitController uc;
    Data data;
    Tools tools;

    public Movement(UnitController _uc, Data _data) {
        this.uc = _uc;
        this.data = _data;
    }

    private final int INF = Integer.MAX_VALUE;

    private boolean rotateRight = true;         // if I should rotate right or left
    private Location lastObstacleFound = null;  // latest obstacle I've found in my way
    private int minDistToTarget = INF;          // minimum distance I've been to the target while going around an obstacle
    private Location prevTarget = null;         // previous target

    void moveTo(Location target){
        // No target? ==> bye!
        if (target == null) return;

        // Different target? ==> previous data does not help!
        if (prevTarget == null || !target.isEqual(prevTarget)) resetMovement();

        // If I'm at a minimum distance to the target, I'm free!
        Location myLoc = uc.getLocation();
        int d = myLoc.distanceSquared(target);
        if (d <= minDistToTarget) resetMovement();

        // update data
        prevTarget = target;
        minDistToTarget = Math.min(d, minDistToTarget);

        // If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
        Direction dir = myLoc.directionTo(target);
        if (lastObstacleFound != null) dir = myLoc.directionTo(lastObstacleFound);

        // This should not happen for a single unit, but whatever
        if (uc.canMove(dir)) resetMovement();

        // I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
        // Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
        for (int i = 0; i < 16; ++i) {
            if (uc.canMove(dir)) {
                uc.move(dir);
                return;
            }
            Location newLoc = myLoc.add(dir);
            if (uc.isOutOfMap(newLoc)) rotateRight = !rotateRight;
                // If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
            else lastObstacleFound = myLoc.add(dir);

            if (rotateRight) dir = dir.rotateRight();
            else dir = dir.rotateLeft();
        }

        if (uc.canMove(dir)) uc.move(dir);
    }

    // clear some of the previous data
    void resetMovement(){
        lastObstacleFound = null;
        minDistToTarget = INF;
    }

    void explore() {

        if (uc.canMove(data.prefDir) && uc.getRound()%20 != 0) uc.move(data.prefDir);
        else {
            double r = Math.random();
            boolean done = false;
            if (r < 0.5) {
                for (int i = 0; i < 8; ++i) {
                    if (done) continue;
                    data.prefDir = data.prefDir.rotateLeft();
                    if (uc.canMove(data.prefDir)) {
                        uc.move(data.prefDir);
                        done = true;
                    }
                }
            } else {
                for (int i = 0; i < 8; ++i) {
                    if (done) continue;
                    data.prefDir = data.prefDir.rotateRight();
                    if (uc.canMove(data.prefDir)) {
                        uc.move(data.prefDir);
                        done = true;
                    }
                }
            }
        }
    }

    boolean doMicro(){

        UnitInfo[] unitsAround = uc.senseUnits();

        boolean enemyInSight = false;
        boolean combat = false;

        for (UnitInfo u : unitsAround) if (u.getTeam().equals(data.enemyTeam) || u.getTeam().equals(Team.NEUTRAL)) {
            enemyInSight = true;
        }



        if (uc.canMove() && enemyInSight) {

            //uc.println("Round:"+data.currentRound+", unit n "+uc.getInfo().getID()+": I'm doing micro");

            MicroInfo[] micro = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) {
                Location newLoc = uc.getLocation().add(data.dirs[i]);
                if (!uc.isOutOfMap(newLoc)) micro[i] = new MicroInfo(newLoc);
            }

            for (int i = 0; i < Math.min(unitsAround.length,15); ++i) {
                UnitInfo unit = unitsAround[i];
                if(unit.getTeam() == data.allyTeam) {
                    for (MicroInfo m : micro) m.updateAlly();
                }
                else{
                    if(! uc.isObstructed(unit.getLocation(), uc.getLocation() ) ) {
                        combat = true;
                        for (MicroInfo m : micro) m.updateEnemy(unit);
                    }
                }
            }
            if (combat) {

                int bestIndex = 8; //We start by considering staying put
                float maxPreference = -1000;

                for (int i = 8; i >= 0; --i) {
                    MicroInfo m = micro[i];
                    if (uc.getType() == UnitType.BARBARIAN) {
                        float pref = m.BarbarianPreference();
                        if (pref > maxPreference) {
                            maxPreference = pref;
                            bestIndex = i;
                        }
                    }
                    if (uc.getType() == UnitType.RANGER) {
                        float pref = m.RangerPreference();
                        if (pref > maxPreference) {
                            maxPreference = pref;
                            bestIndex = i;
                        }
                    }
                    if (uc.getType() == UnitType.EXPLORER) {
                        float pref = m.ExplorerPreference();
                        if (pref > maxPreference) {
                            maxPreference = pref;
                            bestIndex = i;
                        }
                    }
                    if (uc.getType() == UnitType.KNIGHT) {
                        float pref = m.KnightPreference();
                        if (pref > maxPreference) {
                            maxPreference = pref;
                            bestIndex = i;
                        }
                    }
                }

                //uc.println("bestDir: " + data.dirs[bestIndex] + ", preference: " + maxPreference);

                if (uc.canMove(data.dirs[bestIndex])) uc.move(data.dirs[bestIndex]);

                float minDanger = 1<<20; //2^20
                for(int i = 0; i < 8; ++i){
                    Direction currentDir = tools.dirsBFS[i];
                    float danger = 0;
                    for (int j = 0; j < 8; ++i){
                        danger += micro[bestIndex].directionDanger[j]* tools.angloid(currentDir,tools.dirsBFS[j]);
                    }
                    if (danger < minDanger){
                        uc.setOrientation(currentDir);
                        minDanger = danger;
                    }
                }
                return true;
            }
            else return  false;
        }
        return false;
    }

    class MicroInfo {

        //If we can move there or not. very important check
        boolean canMoveThere = false;
        //The maximum amount of damage we can take in that cell
        float maxDamage = 0;
        //All the directions where we will be facing our enemies.
        float[] directionDanger = new float[8];
        //The health of the lowest health enemy we see
        float minEnemyHealth = 1000;
        //If we can attack from this location.
        boolean canAttack = false;
        //If we can kill an enemy from this location.
        boolean canLastHit = false;
        //if we are closer to the enemy than we are now
        boolean closerToEnemy = false;
        //if we are farther to the enemy than we are now
        boolean fartherToEnemy = false;
        //if we are too close to attack to an enemy
        boolean tooCloseToEnemy = false;
        //moving diagonally is more expensive, so we try to avoid it.
        boolean isDiagonal;
        //The type of cell it is
        TileType tile;
        //If we are close enough to the enemy base for it to attack us
        boolean tooCloseToEnemyBase = false;

        //TODO mirar vida restante para asegurarse que no morimos.

        //boolean onRouteToTarget = false;

        Location loc;

        public MicroInfo(Location _loc) {

            this.loc = _loc;

            tile = uc.senseTileTypeAtLocation(loc);

            canMoveThere = uc.canMove(uc.getLocation().directionTo(loc));

            isDiagonal = (loc.directionTo(uc.getLocation()) == Direction.NORTHEAST ||
                    loc.directionTo(uc.getLocation()) == Direction.NORTHWEST ||
                    loc.directionTo(uc.getLocation()) == Direction.SOUTHEAST ||
                    loc.directionTo(uc.getLocation()) == Direction.SOUTHWEST);


            if(data.enemyBaseFound){
                if (uc.getLocation().distanceSquared(data.enemyBaseLoc) <= 32) tooCloseToEnemyBase = true;
            }

        }


        void updateEnemy(UnitInfo enemy) {

            if (!canMoveThere) return;
            Location enemyLocation = enemy.getLocation();

            int distToEnemy = loc.distanceSquared(enemyLocation);
            int currentDistToEnemy = uc.getLocation().distanceSquared(enemyLocation);
            //TODO mirar orientación de unidades enemigas.

            float maxRange = uc.getType().getStat(UnitStat.ATTACK_RANGE);
            float minRange = uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            float enemyEffectiveHealth = enemy.getHealth() + enemy.getType().getStat(UnitStat.DEFENSE);
            float enemyMaxRange = enemy.getType().getStat(UnitStat.ATTACK_RANGE);
            float enemyMinRange = enemy.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            if (uc.canAttack(enemyLocation)) {
                canAttack = true;
                if (enemyEffectiveHealth <= uc.getType().getStat(UnitStat.ATTACK)) canLastHit = true;
                if (minEnemyHealth > enemy.getHealth()) minEnemyHealth = enemy.getHealth();
            }

            if (distToEnemy < currentDistToEnemy) closerToEnemy = true;
            if (distToEnemy > currentDistToEnemy) fartherToEnemy = true;
            if (distToEnemy < uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE)) tooCloseToEnemy = true;
            float predictedDamage = enemy.getType().getStat(UnitStat.ATTACK) - uc.getType().getStat(UnitStat.DEFENSE);
            int directionCode = tools.dirCode((uc.getLocation().directionTo(enemyLocation)))-1;
            if (enemy.getType() == UnitType.ASSASSIN) {
                directionDanger[directionCode] = predictedDamage*9; //do not modify weight
            }
            else directionDanger[directionCode] = predictedDamage*5; //do not modify weight

            if (distToEnemy <= enemyMaxRange && distToEnemy >= enemyMinRange) {
                maxDamage += predictedDamage;
            }
        }

        void updateAlly() {
            //TODO consider nearby allies to be more aggressive in battle
        }

        float BarbarianPreference() {

            if (!canMoveThere) return -1000;
            float preference = 0;

            //preference for attacking
            if (canAttack) preference += 10;
            //preference for killing blows
            if (canLastHit) preference += 10;
            //preference for taking less damage
            preference -= maxDamage / 10;
            //preference for getting close (+) or apart (-) from the enemy
            if (closerToEnemy) preference += 10;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //forest tile detractor
            if (tile == TileType.FOREST) preference -= 0.5;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 20;

            return preference;

        }

        float KnightPreference() {

            if (!canMoveThere) return -1000;
            float preference = 0;

            //preference for attacking
            if (canAttack) preference += 10;
            //preference for killing blows
            if (canLastHit) preference += 14;
            //preference for taking less damage
            preference -= maxDamage / 6;
            //preference for getting close (+) or apart (-) from the enemy
            if (closerToEnemy) preference += 20;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //forest tile detractor
            if (tile == TileType.FOREST) preference -= 0.5;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 40;

            return preference;

        }

        float RangerPreference() {

            if (!canMoveThere) return -1000;
            float preference = 0;

            //Run away from other archers
            if (tooCloseToEnemy) preference -= 30;
            //preference for attacking
            if (canAttack) preference += 7;
            //preference for killing blows
            if (canLastHit) preference += 15;
            //preference for taking less damage
            preference -= maxDamage / 5;
            //preference for getting close to the enemy
            if (closerToEnemy) preference += 0;
            //preference for getting away from the enemy
            if (fartherToEnemy) preference += 0;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //forest tile protractor
            if (tile == TileType.FOREST) preference += 0.25;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 20;

            return preference;

        }

        float ExplorerPreference() {

            if (!canMoveThere) return -1000;
            float preference = 0;

            //preference for attacking
            if (canAttack) preference += 5;
            //preference for killing blows
            if (canLastHit) preference += 25;
            //preference for taking less damage
            preference -= maxDamage / 4;
            //preference for getting close to the enemy
            if (closerToEnemy) preference -= 10;
            //preference for getting away from the enemy
            if (fartherToEnemy) preference += 10;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //forest tile detractor
            if (tile == TileType.FOREST) preference -= 0.5;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 20;

            return preference;
        }
    }

}

