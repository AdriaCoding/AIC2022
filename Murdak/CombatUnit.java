package Murdak;

import aic2022.user.*;

public class CombatUnit {

    UnitController uc;
    Data data;
    Tools tools;
    Movement movement;

    void report() {
        reportMyself();
        //reportEnemies();
        //reportEnvironment();
    }

    // placeholder, this is overridden for each different unit
    void reportMyself() {
        // Report to the Comm Channel
        uc.writeOnSharedArray(data.unitReportCh, uc.readOnSharedArray(data.unitReportCh)+1);
        // Reset Next Slot
        uc.writeOnSharedArray(data.unitResetCh, 0);
    }

    void getShrine(){
        for(ShrineInfo shrines : uc.senseShrines()){
            if(shrines.getOwner() != data.allyTeam){
                if(uc.canAttack(shrines.getLocation())){
                    uc.attack(shrines.getLocation());
                }
            }
        }

    }

    // Should be used by explorers
    void senseStuff(){
        if (uc.senseTileTypeAtLocation(uc.getLocation()) == TileType.DUNGEON){

            for(ChestInfo chest : uc.senseChests()){
                data.saveChest(chest);
            }
        }
        else{
            for(Location loc : uc.getVisibleLocations()){
                TileType tileType = uc.senseTileTypeAtLocation(loc);
                if (tileType == TileType.SHRINE) data.saveShrine(loc);
                if (tileType == TileType.DUNGEON_ENTRANCE) data.saveDungeon(loc);
                            }
        }

    }
    void getChest(){
        ChestInfo[] chest = uc.senseChests();
        if (chest.length != 0){
            Direction dir = tools.isAdjacent(chest[0].getLocation(),uc.getLocation());
            if(dir != Direction.ZERO){
                if(uc.canOpenChest(dir)) uc.openChest(dir);
            }
        }
    }

    void move(){
        if(true){
            movement.explore();
        }
    }

    void enterDungeon(){
        if (uc.senseTileTypeAtLocation(uc.getLocation()) == TileType.DUNGEON) return;
        for (Location entrance : uc.senseVisibleTiles(TileType.DUNGEON_ENTRANCE)){
            Direction dir = tools.isAdjacent(entrance, uc.getLocation());
            if(dir != Direction.ZERO){
                if(uc.canEnterDungeon(dir,dir)) uc.enterDungeon(dir,dir);
            }
        }
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

}