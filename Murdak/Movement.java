package Murdak;

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

        if (uc.canMove(data.prefDir)) uc.move(data.prefDir);
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

        boolean combat = false;



        for (UnitInfo u : unitsAround) if (u.getTeam().equals(data.enemyTeam) || u.getTeam().equals(Team.NEUTRAL)) {
            combat = true;
        }



        if (uc.canMove() && combat) {

            //uc.println("Round:"+data.currentRound+", unit n "+uc.getInfo().getID()+": I'm doing micro");

            MicroInfo[] micro = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) {
                micro[i] = new MicroInfo(uc.getLocation().add(data.dirs[i]));
            }

            //TODO: actualizar todas las casillas que ve un enemigo a la vez.
            for (int i = 0; i < Math.min(unitsAround.length,10); ++i) {
                UnitInfo enemy = unitsAround[i];
                if(enemy.getTeam() == data.allyTeam) continue;
                for (MicroInfo m : micro) m.update(enemy);
            }

            int bestIndex = 0; float maxPreference = -1000;

            for (int i = 0; i < 8; ++i){
                MicroInfo m = micro[i];
                if (uc.getType() == UnitType.BARBARIAN){
                    float pref = m.BarbarianPreference();
                    if (pref > maxPreference){maxPreference = pref; bestIndex = i;}
                }
                if (uc.getType() == UnitType.RANGER){
                    float pref = m.RangerPreference();
                    if (pref > maxPreference){maxPreference = pref; bestIndex = i;}
                }
            }
            uc.println("bestIndex: " + bestIndex + ", preference: " + maxPreference);

            if(uc.canMove(data.dirs[bestIndex]) ) uc.move(data.dirs[bestIndex]);
            return true;
        }
        return false;
    }

    class MicroInfo {

        //If we can move there or not. very important check
        boolean canMoveThere = false;
        float maxDamage = 0;
        float minEnemyHealth = 1000;
        //If we can attack from this location.
        boolean canAttack = false;
        //If we can kill an enemy from this location.
        boolean canLastHit = false;
        //if we are closer to the enemy than we are now
        boolean closerToEnemy = false;
        //moving diagonally is more expensive, so we try to avoid it.
        boolean isDiagonal;
        //TODO implementar checks para la base enemiga.
        boolean tooCloseToEnemyBase = false;

        //boolean onRouteToTarget = false;

        Location loc;

        public MicroInfo(Location _loc) {

            this.loc = _loc;

            canMoveThere = uc.canMove(uc.getLocation().directionTo(loc) );

            isDiagonal = (loc.directionTo(uc.getLocation()) == Direction.NORTHEAST ||
                    loc.directionTo(uc.getLocation()) == Direction.NORTHWEST ||
                    loc.directionTo(uc.getLocation()) == Direction.SOUTHEAST ||
                    loc.directionTo(uc.getLocation()) == Direction.SOUTHWEST);


            // if(loc.distanceSquared(data.enemyBase) <= 50 ) tooCloseToEnemyBase = true;

        }


        void update(UnitInfo enemy) {

            if (!canMoveThere) return;

            int distToEnemy = loc.distanceSquared(enemy.getLocation());
            int currentDistToEnemy = uc.getLocation().distanceSquared(enemy.getLocation());

            float maxRange = uc.getType().getStat(UnitStat.ATTACK_RANGE);
            float minRange = uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            //TODO hace isObstructed de casillas que no puede ver, mirar porque.
            //boolean obstructed = uc.isObstructed(loc, enemy.getLocation());
            boolean obstructed = false;

            float enemyEffectiveHealth = enemy.getHealth() + enemy.getType().getStat(UnitStat.DEFENSE);
            float enemyMaxRange = enemy.getType().getStat(UnitStat.ATTACK_RANGE);
            float enemyMinRange = enemy.getType().getStat(UnitStat.MIN_ATTACK_RANGE);
            //TODO mirar orientación de unidades enemigas.

            if (uc.canAttack(enemy.getLocation())) {
                canAttack = true;
                if (enemyEffectiveHealth <= uc.getType().getStat(UnitStat.ATTACK)) canLastHit = true;
                if (minEnemyHealth > enemy.getHealth()) minEnemyHealth = enemy.getHealth();
            }

            if (!obstructed && distToEnemy < currentDistToEnemy) closerToEnemy = true;

            if (distToEnemy <= enemyMaxRange && distToEnemy >= enemyMinRange) {
                maxDamage += enemy.getType().getStat(UnitStat.ATTACK) - uc.getType().getStat(UnitStat.DEFENSE);
            }
        }

        float BarbarianPreference() {

            if (!canMoveThere) return -1000;
            float preference = 0;

            //preference for attacking
            if (canAttack) preference += 7;
            //preference for killing blows
            if (canLastHit) preference += 10;
            //preference for taking less damage
            preference -= maxDamage / 3;
            //preference for getting close (+) or apart (-) from the enemy
            if (closerToEnemy) preference += 0.25;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 20;

            return preference;

        }

        float RangerPreference() {

            if (!canMoveThere) return -1000;
            float preference = 0;

            //preference for attacking
            if (canAttack) preference += 5;
            //preference for killing blows
            if (canLastHit) preference += 7;
            //preference for taking less damage
            preference -= maxDamage / 5;
            //preference for getting close (+) or apart (-) from the enemy
            if (closerToEnemy) preference -= 0;
            //diagonal movement detractor
            if (isDiagonal) preference -= 0.5;
            //Preference for not getting to close to the enemy base
            if (tooCloseToEnemyBase) preference -= 20;

            return preference;

        }
    }
}
