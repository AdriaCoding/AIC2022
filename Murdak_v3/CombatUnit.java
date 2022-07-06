package Murdak_v3;

import aic2022.user.*;

public class CombatUnit extends Unit {

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
        ChestInfo[] chest = uc.senseChests(2);
        if (chest.length > 0){
            Direction dir = uc.getLocation().directionTo(chest[0].getLocation() );
            if(uc.canOpenChest(dir)) uc.openChest(dir);
        }
    }

    void move(){
        if(!movement.doMicro() ){

            Location accumulationTarget = tools.decodeLocation(uc.readOnSharedArray(data.accumulationCh));
            if (uc.getRound() < data.accumulationRound) movement.moveTo(accumulationTarget);
            else movement.explore();
        }
    }

    void useArtifact(){

        ArtifactInfo[] stuff = uc.getArtifacts();
        if (stuff.length > 0 && uc.canUseArtifact(0) ){
            uc.useArtifact(0);
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

}