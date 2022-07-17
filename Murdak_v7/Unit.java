package Murdak_v7;

import aic2022.user.*;

public class Unit {

    UnitController uc;
    Data data;
    Tools tools;

    public void attack(){
        attackE();
        attackN();
    }

    public void attackE(){

        UnitInfo[] enemiesAround = uc.senseUnits(data.enemyTeam);
        Location target = uc.getLocation();
        float priority = 0;

        float attack = uc.getType().getStat(UnitStat.ATTACK);

        for (UnitInfo unit : enemiesAround){

            if(!uc.canAttack(unit.getLocation()) ) continue;

            float enemyMaxHealth = unit.getType().getStat(UnitStat.MAX_HEALTH);

            float unitPriority = targetPriority(unit);
            //prioriza atacar a matar
            if(unit.getHealth() <= attack) unitPriority += 50;
            else unitPriority = unitPriority * (float)(enemyMaxHealth /unit.getHealth());

            if (unitPriority > priority){
                priority = unitPriority;
                target = unit.getLocation();
            }
        }

        if (!target.isEqual(uc.getLocation()) ) uc.attack(target);

    }

    public void attackN(){

        UnitInfo[] enemiesAround = uc.senseUnits(Team.NEUTRAL);
        Location target = uc.getLocation();
        float priority = 0;

        float attack = uc.getType().getStat(UnitStat.ATTACK);

        for (UnitInfo unit : enemiesAround){

            if(!uc.canAttack(unit.getLocation()) ) continue;

            float enemyMaxHealth = unit.getType().getStat(UnitStat.MAX_HEALTH);

            float unitPriority = targetPriority(unit);
            //prioriza atacar a matar
            if(unit.getHealth() <= attack) unitPriority += 50;
            else unitPriority = unitPriority * (float)(enemyMaxHealth /unit.getHealth());

            if (unitPriority > priority){
                priority = unitPriority;
                target = unit.getLocation();
            }
        }

        if (!target.isEqual(uc.getLocation()) ) uc.attack(target);

    }

    public int targetPriority(UnitInfo unit) {
        if(unit.getType() == UnitType.CLERIC)       return 8;
        if(unit.getType() == UnitType.ASSASSIN)     return 7;
        if(unit.getType() == UnitType.MAGE)         return 6;
        if(unit.getType() == UnitType.EXPLORER)     return 5;
        if(unit.getType() == UnitType.RANGER)       return 4;
        if(unit.getType() == UnitType.BARBARIAN)    return 3;
        if(unit.getType() == UnitType.KNIGHT)       return 2;
        if(unit.getType() == UnitType.BASE)         return 1;

        return 0;
    }
    void senseStuff() {
        Location unitLoc = uc.getLocation();
        if (uc.senseTileTypeAtLocation(unitLoc).equals(TileType.DUNGEON)) return;
        if (uc.readOnSharedArray(tools.encodeLoc(unitLoc))%10 == 0) {
            for (Location loc : uc.getVisibleLocations()) {
                if (uc.isOutOfMap(loc)) continue;
                int locCode = tools.encodeLoc(loc);
                int tileCode = uc.readOnSharedArray(locCode);

                if (tileCode == 0) {
                    uc.writeOnSharedArray(locCode, tools.tileType_code(uc.senseTileTypeAtLocation(loc)));
                }
                else if (tileCode/10 != 0){         //try adding to BFS queue
                    for(int adjLoc : tools.adjacentCodes(locCode)){
                        if(tools.isValid(adjLoc, 1, false)){
                            boolean stop = false;
                            int BFSIterator = data.baseBFSCh;
                            while (!stop){          //check valid location is not repeated
                                int actualCode = uc.readOnSharedArray(BFSIterator);
                                if (actualCode == locCode) stop = true;
                                else if (actualCode != 0) ++BFSIterator;
                                else {
                                    uc.writeOnSharedArray(BFSIterator, locCode);
                                    uc.writeOnSharedArray(data.baseBFSCh - 1, BFSIterator);
                                    stop = true;
                                }
                            }
                            break;
                        }
                    }
                }
                if (tools.getTileType(tileCode) == null) continue;
                else if (tools.getTileType(tileCode) == TileType.SHRINE) data.saveShrine(loc);
                //else if (tools.getTileType(tileCode) == TileType.DUNGEON_ENTRANCE) data.currentDungeon = data.saveDungeon(loc);


                //Necessary Chivato
                if (!loc.isEqual(tools.decodeLoc(locCode))) {
                    uc.println("El encode a (" + loc.x + ", " + loc.y + ") Falla. El codi es: " + locCode + " i el decode torna " + tools.decodeLoc(locCode) + "\n");
                }
            }
        }
    }

    void getShrine(){
        for(ShrineInfo shrine : uc.senseShrines()){
            Location shrineLoc = shrine.getLocation();
            if(shrine.getOwner() != data.allyTeam && uc.canAttack(shrineLoc) ){
                    uc.attack(shrine.getLocation());
            }
            if(shrineLoc.distanceSquared(data.allyBase) > data.shrineDistanceThreshold && uc.canAttack(shrineLoc) ){
                uc.attack(shrine.getLocation());
            }
        }
    }

    void reportEnemies(){
        reportEnemyLocation();
        reportEnemyBaseLocation();
    }

    void reportEnemyLocation(){



        int r = (int) uc.getType().getStat(UnitStat.VISION_RANGE);

        if(data.enemyFound) {
            if(uc.canSenseLocation(data.enemyLoc)) {
                UnitInfo[] enemies = uc.senseUnits(r, data.allyTeam, true);
                if(enemies.length == 0) {
                    uc.writeOnSharedArray(data.enemyLocCh, 0);
                    uc.writeOnSharedArray(data.enemyFoundCh, 0);
                    data.enemyFound = false;
                }
            }
        } else{
            UnitInfo[] enemiesOnSight = uc.senseUnits(r, data.allyTeam, true);
            if (enemiesOnSight.length > 0) {
                Location loc = enemiesOnSight[0].getLocation();
                uc.writeOnSharedArray(data.enemyLocCh, tools.encodeLoc(loc));
                uc.writeOnSharedArray(data.enemyFoundCh, 1);
                data.enemyFound = true;
            }
        }

    }

    void reportEnemyBaseLocation() {

        if (!data.enemyBaseFound) {
            UnitInfo[] enemiesOnSight = uc.senseUnits(data.enemyTeam);
            for (UnitInfo enemy : enemiesOnSight) {
                if (enemy.getType() == UnitType.BASE) {
                    uc.writeOnSharedArray(data.enemyBaseLocCh, tools.encodeLoc(enemy.getLocation()));
                    uc.writeOnSharedArray(data.enemyBaseFoundCh, 1);
                    data.enemyBaseFound = true;
                }
            }
        }
    }

}
