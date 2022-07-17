package Murdak_v8;

import aic2022.user.*;

public class Movement {

    UnitController uc;
    Data data;
    Tools tools;

    public Movement(UnitController _uc, Data _data) {
        this.uc = _uc;
        this.data = _data;
        this.tools = new Tools(_uc, _data);
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
        int r = (int) Math.min(62,uc.getType().getStat(UnitStat.VISION_RANGE));
        UnitInfo[] unitsAround = uc.senseUnits(r);

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
                micro[i] = new MicroInfo(newLoc);
            }

            for (int i = 0; i < Math.min(unitsAround.length,10); ++i) {
                UnitInfo unit = unitsAround[i];
                if(unit.getTeam() == data.allyTeam) {
                    for (MicroInfo m : micro) m.updateAlly(unit);
                }
                else{
                    if(! uc.isObstructed(unit.getLocation(), uc.getLocation() ) ) {
                        combat = true;
                        for (MicroInfo m : micro){
                            if(unit.getType() == UnitType.EXPLORER) m.updateEnemyExplorer(unit);
                            else if(unit.getType() == UnitType.CLERIC) m.updateEnemyCleric(unit);
                            else m.updateEnemy(unit);
                        }
                    }
                }
            }
            if (combat) {

                //if we are in combat we move the prefred direction for explorers away from the enemy
                if(uc.getType() == UnitType.EXPLORER){
                    if(tools.randomInt(2) == 1) data.prefDir = data.prefDir.rotateRight();
                    else data.prefDir = data.prefDir.rotateLeft();
                }

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
                        //uc.println("pref in direction "+m.dir+" is " + pref);
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

                Direction d = data.dirs[bestIndex];

                //uc.println("bestDir: " + d + ", preference: " + maxPreference);
                if (!d.isEqual(Direction.ZERO) && uc.canMove(d)) uc.move(d);

                //ORIENTATION

                float minDanger = 1000;
                Direction bestOrientation = Direction.ZERO;

                for(Direction dir : data.dirs){
                    if (dir.equals(Direction.ZERO) ) continue;
                    float danger = 0;
                    for (int j = 0; j < 8; ++j){
                        danger += micro[bestIndex].directionDanger[j]* tools.angloid(dir,data.dirs[j]);
                    }
                    if (danger < minDanger){
                        bestOrientation = dir;
                        minDanger = danger;
                    }
                }

                if(!bestOrientation.isEqual(Direction.ZERO) )uc.setOrientation(bestOrientation);
                return true;
            }
            else return  false;
        }
        return false;
    }

    public class MicroInfo {

        //If we can move there or not. very important check
        boolean canMoveThere;
        //The direction which we would move
        Direction dir = Direction.ZERO;
        //The maximum amount of damage we can take in that cell
        float maxDamage = 0;
        //All the directions where we will be facing our enemies.
        float[] directionDanger = new float[8];
        //The number of allies that can help in battle
        float alliesAround = 0;
        //The min distance to an enemy
        float minDistToEnemy = 1000;
        //The health of the lowest health enemy we see
        float minEnemyHealth = 1000;
        //If we can attack from this location.
        boolean canAttack = false;
        //If we can kill an enemy from this location.
        boolean canLastHit = false;
        //if we are closer to the enemy than we are now
        boolean closerToEnemy = false;
        //if we are closer to the enemy cleric than we are now
        boolean closerToEnemyCleric = false;
        //if we are farther to the enemy than we are now
        boolean fartherToEnemy = false;
        //if we are too close to attack to an enemy
        boolean tooCloseToEnemy = false;
        //if we are too far to attack to an enemy after a step
        boolean tooFarFromEnemy = false;
        //moving diagonally is more expensive, so we try to avoid it.
        boolean isDiagonal;
        //The type of cell it is
        TileType tile = TileType.MOUNTAIN;
        //If we are close enough to the enemy base for it to attack us
        boolean tooCloseToEnemyBase = false;

        //TODO mirar vida restante para asegurarse que no morimos.

        //boolean onRouteToTarget = false;

        Location loc;

        public MicroInfo(Location _loc) {

            this.loc = _loc;

            if(!uc.isOutOfMap(loc)) {

                dir = uc.getLocation().directionTo(loc);

                tile = uc.senseTileTypeAtLocation(loc);

                canMoveThere = uc.canMove(uc.getLocation().directionTo(loc));

                isDiagonal = (loc.directionTo(uc.getLocation()) == Direction.NORTHEAST ||
                        loc.directionTo(uc.getLocation()) == Direction.NORTHWEST ||
                        loc.directionTo(uc.getLocation()) == Direction.SOUTHEAST ||
                        loc.directionTo(uc.getLocation()) == Direction.SOUTHWEST);


                if (data.enemyBaseFound) {
                    if (loc.distanceSquared(data.enemyBaseLoc) <= 46) tooCloseToEnemyBase = true;
                }
            }
            else canMoveThere = false;

        }


        void updateEnemy(UnitInfo enemy) {

            if (!canMoveThere) return;
            Location enemyLocation = enemy.getLocation();

            int distToEnemy = loc.distanceSquared(enemyLocation);
            int currentDistToEnemy = uc.getLocation().distanceSquared(enemyLocation);

            float enemyEffectiveHealth = enemy.getHealth() + enemy.getType().getStat(UnitStat.DEFENSE);
            float enemyMaxRange = enemy.getType().getStat(UnitStat.ATTACK_RANGE);
            float enemyMinRange = enemy.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            float maxRange = uc.getType().getStat(UnitStat.ATTACK_RANGE);
            float minRange = uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            if (uc.canAttack() && distToEnemy <= maxRange && distToEnemy >= minRange
                    && !uc.isObstructed(loc,enemyLocation)) {
                canAttack = true;
                if (enemyEffectiveHealth <= uc.getType().getStat(UnitStat.ATTACK)) canLastHit = true;
                if (minEnemyHealth > enemy.getHealth()) minEnemyHealth = enemy.getHealth();
            }

            if (distToEnemy > currentDistToEnemy) fartherToEnemy = true;
            if (distToEnemy < currentDistToEnemy && distToEnemy < minDistToEnemy){
                minDistToEnemy = distToEnemy;
                closerToEnemy = true;
                //if we are getting close to one enemy we do not care if we are getting farther to other
                tooFarFromEnemy = false;
                fartherToEnemy = false;
            }

            //RANGER STUFF
            if (distToEnemy < minRange)                     tooCloseToEnemy = true;
            if (distToEnemy > 48 && minDistToEnemy > 48)    tooFarFromEnemy = true;


            float predictedDamage = enemy.getType().getStat(UnitStat.ATTACK) - uc.getType().getStat(UnitStat.DEFENSE);

            int directionCode = tools.dirCode((uc.getLocation().directionTo(enemyLocation) ) );
            if (enemy.getType() == UnitType.ASSASSIN)directionDanger[directionCode] = predictedDamage*9;
            else directionDanger[directionCode] = predictedDamage*5; //do not modify weights



            if (distToEnemy <= enemyMaxRange && distToEnemy >= enemyMinRange
                    && !uc.isObstructed(loc,enemyLocation,data.enemyTeam) ){
                maxDamage += predictedDamage;
            }
        }

        void updateEnemyExplorer(UnitInfo enemy) {

            if (!canMoveThere) return;
            Location enemyLocation = enemy.getLocation();

            int distToEnemy = loc.distanceSquared(enemyLocation);
            int currentDistToEnemy = uc.getLocation().distanceSquared(enemyLocation);

            float enemyEffectiveHealth = enemy.getHealth() + enemy.getType().getStat(UnitStat.DEFENSE);
            float enemyMaxRange = enemy.getType().getStat(UnitStat.ATTACK_RANGE);
            float enemyMinRange = enemy.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            float maxRange = uc.getType().getStat(UnitStat.ATTACK_RANGE);
            float minRange = uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            if (uc.canAttack() && distToEnemy <= maxRange && distToEnemy >= minRange
                    && !uc.isObstructed(loc,enemyLocation)) {
                canAttack = true;
                if (enemyEffectiveHealth <= uc.getType().getStat(UnitStat.ATTACK)) canLastHit = true;
                if (minEnemyHealth > enemy.getHealth()) minEnemyHealth = enemy.getHealth();
            }

            //RANGER STUFF
            if (distToEnemy < uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE)) tooCloseToEnemy = true;

            float predictedDamage = enemy.getType().getStat(UnitStat.ATTACK) - uc.getType().getStat(UnitStat.DEFENSE);

            if (distToEnemy <= enemyMaxRange && distToEnemy >= enemyMinRange
                    && !uc.isObstructed(loc,enemyLocation,data.enemyTeam) ) {
                maxDamage += predictedDamage;
            }
        }

        void updateEnemyCleric(UnitInfo enemy) {

            if (!canMoveThere) return;
            Location enemyLocation = enemy.getLocation();

            int distToEnemy = loc.distanceSquared(enemyLocation);
            int currentDistToEnemy = uc.getLocation().distanceSquared(enemyLocation);
            float enemyEffectiveHealth = enemy.getHealth() + enemy.getType().getStat(UnitStat.DEFENSE);

            float maxRange = uc.getType().getStat(UnitStat.ATTACK_RANGE);
            float minRange = uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            if (uc.canAttack() && distToEnemy <= maxRange && distToEnemy >= minRange
                    && !uc.isObstructed(loc,enemyLocation)) {
                canAttack = true;
                if (enemyEffectiveHealth <= uc.getType().getStat(UnitStat.ATTACK)) canLastHit = true;
                if (minEnemyHealth > enemy.getHealth()) minEnemyHealth = enemy.getHealth();
            }

            if (distToEnemy < currentDistToEnemy) closerToEnemyCleric = true;
            if (distToEnemy < uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE)) tooCloseToEnemy = true;

        }

        void updateAlly(UnitInfo ally) {
            //TODO consider nearby allies to be more aggressive in battle

            if (!canMoveThere) return;

            Location allyLoc = ally.getLocation();
            if (!uc.isObstructed(loc,allyLoc) && loc.distanceSquared(allyLoc)<=36){
                if(ally.getType() == UnitType.RANGER) alliesAround += 0.25;
                if(ally.getType() == UnitType.BARBARIAN || ally.getType() == UnitType.KNIGHT) alliesAround += 1;
            }
        }

        float BarbarianPreference() {

            if (!canMoveThere) return -1000;
            float preference = 0;

            //preference for attacking
            if (canAttack) preference += 10;
            //preference for killing blows
            if (canLastHit) preference += 10;
            //preference for taking less damage
            preference -= maxDamage / 6;
            //preference for getting close (+) or apart (-) from the enemy
            if (closerToEnemy) preference += 10 + 4*alliesAround;
            //preference for getting close (+) or apart (-) from clerics
            if (closerToEnemyCleric) preference += 10;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //forest tile detractor
            if (tile == TileType.FOREST) preference -= 0.5;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 100;


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
            preference -= maxDamage / 8;
            //preference for getting close (+) or apart (-) from the enemy
            if (closerToEnemy) preference += 12 + 4*alliesAround;
            //preference for getting close (+) or apart (-) from Clerics
            if (closerToEnemyCleric) preference += 20;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //forest tile detractor
            if (tile == TileType.FOREST) preference -= 0.5;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 100;

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
            preference -= (maxDamage/2 - alliesAround);
            //preference for getting close to the enemy cleric
            if (!tooCloseToEnemy && closerToEnemyCleric) preference += 2;
            //preference for getting close enough to attack the enemy
            if (tooFarFromEnemy) preference -= 2;
            //preference for getting away from the enemy
            if (!tooFarFromEnemy && fartherToEnemy) preference += 5;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //forest tile protractor
            if (tile == TileType.FOREST) preference += 0.25;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 120;


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
            preference -= maxDamage;
            //preference for getting close to the enemy
            if (closerToEnemy) preference -= 10;
            //preference for getting away from the enemy
            if (fartherToEnemy) preference += 10;
            //preference for getting close (+) or apart (-) from Clerics
            if (closerToEnemyCleric) preference += 5;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //forest tile detractor
            if (tile == TileType.FOREST) preference -= 0.5;
            //preference to keep exploring
            if(dir == data.prefDir) preference += 0.25;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 120;

            return preference;
        }
    }
}

