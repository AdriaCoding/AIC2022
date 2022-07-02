package Murdak;

import aic2022.user.*;

public class Movement {

    UnitController uc;
    Data data;

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

        UnitInfo[] enemiesAround = uc.senseUnits(data.enemyTeam);
        UnitInfo[] neutralsAround = uc.senseUnits(Team.NEUTRAL);

        boolean combat = (enemiesAround.length + neutralsAround.length > 0);

        if (uc.canMove() && combat) {
            MicroInfo[] micro = new MicroInfo[9];
            for (int i = 0; i < 9; ++i) {
                micro[i] = new MicroInfo(uc.getLocation().add(data.dirs[i]));
            }
            for (int i = 0; i < Math.min(enemiesAround.length,10); ++i) {
                UnitInfo enemy = enemiesAround[i];
                for (MicroInfo m : micro) m.update(enemy);
            }
            for (int i = 0; i < Math.min(enemiesAround.length,10); ++i) {
                UnitInfo enemy = neutralsAround[i];
                for (MicroInfo m : micro) m.update(enemy);
            }

            //Siempre nos podemos quedar quietos
            int bestIndex = 8;

            for (int i = 8; i >= 0; --i) {
                if (!uc.canMove( data.dirs[i]) ) continue;

                if (uc.getType() == UnitType.RANGER || uc.getType() == UnitType.MAGE) {
                    if (micro[i].isBetterRanged(micro[bestIndex])) bestIndex = i;
                }

                if (uc.getType() == UnitType.BARBARIAN || uc.getType() == UnitType.KNIGHT) {
                    if (micro[i].isBetterMelee(micro[bestIndex])) bestIndex = i;
                }
            }

            uc.move(data.dirs[bestIndex]);
            return true;
        }

        return false;

    }

    class MicroInfo {
        float maxDamage = 0;
        float minDistToEnemy = 1000;
        float minEnemyHealth = 1000;
        boolean canAttack = false;
        boolean isDiagonal = false;
        //boolean tooCloseToEnemyBase = false;
        //boolean onRouteToTarget = false;

        Location loc;

        public MicroInfo(Location _loc) {
            this.loc = _loc;

            if (loc.directionTo(uc.getLocation()) == Direction.NORTHEAST || loc.directionTo(uc.getLocation()) == Direction.NORTHWEST ||
                    loc.directionTo(uc.getLocation()) == Direction.SOUTHEAST || loc.directionTo(uc.getLocation()) == Direction.SOUTHWEST) {
                isDiagonal = true;
            }

            // if(loc.distanceSquared(data.enemyBase) <= 50 ) tooCloseToEnemyBase = true;

        }


        void update(UnitInfo enemy) {

            int d = loc.distanceSquared(enemy.getLocation());

            float maxRange = uc.getType().getStat(UnitStat.ATTACK_RANGE);
            float minRange = uc.getType().getStat(UnitStat.MIN_ATTACK_RANGE);
            boolean obstructed = uc.isObstructed(loc, enemy.getLocation());

            if (d <= maxRange && d >= minRange && !obstructed) {

                canAttack = true;

                if (minEnemyHealth > enemy.getHealth()) {
                    minEnemyHealth = enemy.getHealth();
                }

            }

            //Solo guardamos las distancias a unidades que podamos ver
            if (d < minDistToEnemy && !obstructed) {
                minDistToEnemy = d;
            }

            float enemyMaxRange = enemy.getType().getStat(UnitStat.ATTACK_RANGE);
            float enemyMinRange = enemy.getType().getStat(UnitStat.MIN_ATTACK_RANGE);

            if (d <= enemyMaxRange && d >= enemyMinRange) {
                maxDamage += enemy.getType().getStat(UnitStat.ATTACK);
            }
        }

        boolean isBetterRanged(Movement.MicroInfo mic) {

            float preference = 0;

            float dmg = uc.getType().getStat(UnitStat.ATTACK);
            int hp = uc.getInfo().getHealth();

            /*Prioriza no entrar en el rango de vision de la Base enemiga
            if(!tooCloseToEnemyBase && mic.tooCloseToEnemyBase) preference += 20;
            if(tooCloseToEnemyBase && !mic.tooCloseToEnemyBase) preference -= 20;
            */

            if (uc.canAttack()) {

                //Prioriza poder atacar
                if (canAttack && !mic.canAttack) preference += 7;
                if (!canAttack && mic.canAttack) preference -= 7;

                //Prioriza las casillas en las que puede hacer killingBlow
                if (minEnemyHealth <= dmg && mic.minEnemyHealth > dmg) preference += 7;
                if (minEnemyHealth > dmg && mic.minEnemyHealth <= dmg) preference -= 7;

            }

            //Prioriza las casillas en las que menos daño le pueden hacer
            if (maxDamage < mic.maxDamage) preference += (mic.maxDamage - maxDamage) / 3;
            if (maxDamage > mic.maxDamage) preference -= (maxDamage - mic.maxDamage) / 3;

            /*prioriza alejarse del enemigo
            if (minDistToEnemy > mic.minDistToEnemy) preference += 1;
            if (minDistToEnemy < mic.minDistToEnemy) preference -= 1;
            */

            if (!isDiagonal && mic.isDiagonal) preference += 0.5;
            if (isDiagonal && !mic.isDiagonal) preference -= 0.5;


            //Si las posiciones son equivalentes mejor no cambiar
            if (preference >= 0) return true;
            return false;

        }

        boolean isBetterMelee(Movement.MicroInfo mic) {

            float preference = 0;

            float dmg = uc.getType().getStat(UnitStat.ATTACK);
            int hp = uc.getInfo().getHealth();

                /*Prioriza no entrar en el rango de vision de la Base enemiga
                if(!tooCloseToEnemyBase && mic.tooCloseToEnemyBase) preference += 20;
                if(tooCloseToEnemyBase && !mic.tooCloseToEnemyBase) preference -= 20;
                */

            if (uc.canAttack()) {

                //Prioriza poder atacar
                if (canAttack && !mic.canAttack) preference += 10;
                if (!canAttack && mic.canAttack) preference -= 10;

                //Prioriza las casillas en las que puede hacer killingBlow
                if (minEnemyHealth <= dmg && mic.minEnemyHealth > dmg) preference += 10;
                if (minEnemyHealth > dmg && mic.minEnemyHealth <= dmg) preference -= 10;

            }

            //Prioriza las casillas en las que menos daño le pueden hacer
            if (maxDamage < mic.maxDamage) preference += (mic.maxDamage - maxDamage) / 5;
            if (maxDamage > mic.maxDamage) preference -= (maxDamage - mic.maxDamage) / 5;

            //prioriza acercarse al enemigo
            if (minDistToEnemy < mic.minDistToEnemy) preference += 1;
            if (minDistToEnemy > mic.minDistToEnemy) preference -= 1;

            if (!isDiagonal && mic.isDiagonal) preference += 0.5;
            if (isDiagonal && !mic.isDiagonal) preference -= 0.5;


            //Si las posiciones son equivalentes mejor no cambiar
            if (preference >= 0) return true;
            return false;

        }

    }


}
