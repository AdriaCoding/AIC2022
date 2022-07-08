package Murdak_v3;

import aic2022.user.*;

public class Unit {

    UnitController uc;
    Data data;
    Tools tools;

    public void attack(){

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

    void getShrine(){
        for(ShrineInfo shrine : uc.senseShrines()){
            Location shrineLoc = shrine.getLocation();
            if(shrine.getOwner() != data.allyTeam && uc.canAttack(shrineLoc) ){
                    uc.attack(shrine.getLocation());
            }
            if(shrineLoc.distanceSquared(data.allyBase) > data.shrineDistanceThreshold && uc.canAttack(shrineLoc ) ){
                uc.attack(shrine.getLocation());
            }
        }
    }

    void senseStuff(){
        Location unit_loc = uc.getLocation();
        int unit_loc_code = uc.readOnSharedArray(tools.encodeLoc(unit_loc))%10;
        if (unit_loc_code == 0 ) {
            for (Location loc : uc.getVisibleLocations()) {
                int locCode = tools.encodeLoc(loc);
                int tileCode = uc.readOnSharedArray(locCode);
                if (tileCode == 0) {
                    uc.writeOnSharedArray(locCode, tools.tileType_code(uc.senseTileTypeAtLocation(loc)));
                }
                if (tools.getTileType(tileCode) == TileType.SHRINE) data.saveShrine(loc);
                if (tools.getTileType(tileCode) == TileType.DUNGEON_ENTRANCE) data.saveDungeon(loc);
                //little chivato
                if(!loc.isEqual(tools.decodeLoc(locCode))){
                    uc.println(loc.x +", " + loc.y + " Falla. El codi és: " + locCode + "\n");
                }
            }
        }

        if (unit_loc_code == tools.tileType_code(TileType.DUNGEON)) {
            for (ChestInfo chest : uc.senseChests()) data.saveChest(chest);
            float radius = uc.getInfo().getStat(UnitStat.VISION_RANGE);
            int neutralUnits =  uc.senseUnits(Team.NEUTRAL).length;
        }
    }


}
